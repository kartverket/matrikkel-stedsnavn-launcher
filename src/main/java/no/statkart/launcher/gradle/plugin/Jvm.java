package no.statkart.launcher.gradle.plugin;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

enum Jvm {
    WINDOWS("windows", new NoopPermissionSetter()),
    LINUX("linux", new NixPermissionSetter()),
    OSX("osx", new NixPermissionSetter());

    private final String alias;
    private final BiFunction<TarArchiveEntry, Path, IOException> permissionSetter;

    private Jvm currentOsJvm;
    private URL url;
    private Path destinationDir;

    static Optional<Jvm> fraAlias(String alias) {
        for (Jvm jvm : values()) {
            if (jvm.alias.equals(alias)) {
                return Optional.of(jvm);
            }
        }
        return Optional.empty();
    }

    Jvm(String alias, BiFunction<TarArchiveEntry, Path, IOException> permissionSetter) {
        this.alias = alias;
        this.permissionSetter = permissionSetter;
    }

    String getAlias() {
        return alias;
    }

    void setURL(String urlString) throws IOException {
        this.url = new URL(urlString);
    }

    void setDestinationDir(Path destinationDir) {
        this.destinationDir = destinationDir;
    }

    void download() throws IOException {
        checkState();
        Path filename = Paths.get(url.getPath()).getFileName();
        Path destination = destinationDir.resolve(filename);
        if (Files.isRegularFile(destination)) {
            System.out.println("Using existing jvm at " + destination);
            return;
        }
        Files.createDirectories(destinationDir);
        System.out.println("Downloading jvm to " + destination);
        try (InputStream in = url.openStream()) {
            Files.copy(in, destination);
        }
    }

    void unpack() throws IOException {
        checkState();
        Path destination = destinationDir.resolve(alias);
        if (Files.exists(destination)) {
            return;
        }
        Path filename = Paths.get(url.getPath()).getFileName();
        Path source = destinationDir.resolve(filename);
        if (isZip(source)) {
            unzip(source, destination);
        } else if (isTarGz(source)) {
            untargz(source, destination);
        } else {
            throw new RuntimeException("Unknown artifact compression method: " + source);
        }
    }

    void jlink(List<String> modules, List<String> locales) throws IOException {
        checkState();
        Path source = destinationDir.resolve(alias);
        Path destination = destinationDir.resolve(alias + "-min").resolve("jre");
        if (Files.exists(destination)) {
            return;
        }
        Path jlink = findJlinkExecutable(destinationDir, jvmOfCurrentlyRunningOS());
        String[] cmd = {jlink.toString()
                , "--module-path", getJModsDirectory(source).toString()
                , "--add-modules", String.join(",", modules)
                , "--include-locales", String.join(",", locales)
                , "--output", destination.toString()
        };
//        System.out.println("jlink cwd=" + source.toFile());
//        System.out.println("jlink cmd=" + String.join(" ", cmd));
        Process prosess = Runtime.getRuntime().exec(cmd, null, source.toFile());
        int status;
        try {
            status = prosess.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String stdout = streamToString(prosess.getInputStream());
        if (!stdout.isEmpty()) {
            System.out.println(stdout);
        }
        String stderr = streamToString(prosess.getErrorStream());
        if (!stderr.isEmpty()) {
            System.err.println(stderr);
        }
        if (status != 0) {
            System.err.println("jlink returned status " + status + "\n\tcommand: " + String.join(" ", cmd));
        }
    }

    private static Path findJlinkExecutable(Path dir, Jvm jvm) throws IOException {
        Path suitableJdkDir = dir.resolve(jvm.alias);
        try (Stream<Path> files = Files.walk(suitableJdkDir)) {
            return files.filter(path -> path.endsWith("jlink") || path.endsWith("jlink.exe"))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Cannot find jlink in " + suitableJdkDir));
        }
    }

    private boolean isZip(Path path) {
        return path.getFileName().toString().endsWith(".zip");
    }

    private boolean isTarGz(Path path) {
        return path.getFileName().toString().endsWith(".tar.gz");
    }

    private Path getJModsDirectory(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(path -> Files.isDirectory(path) && path.getFileName().toString().equals("jmods"))
                    .findFirst().orElseThrow();
        }
    }

    private void unzip(Path inputPath, Path outputDirPath) throws IOException {
        //noinspection RedundantCast (for Java 13, som overloader newFileSystem())
        try (FileSystem zipFs = FileSystems.newFileSystem(inputPath, (ClassLoader) null)) {
            Path zipRoot = zipFs.getPath("/");
            Files.walkFileTree(zipRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path target = outputDirPath.resolve(zipRoot.relativize(file).toString());
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void untargz(Path inputPath, Path outputDirPath) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(inputPath.toFile());
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(bufferedInputStream);
             TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipInputStream)) {
            TarArchiveEntry entry;
            while ((entry = tarArchiveInputStream.getNextTarEntry()) != null) {
                Path path = outputDirPath.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                } else {
                    Files.createDirectories(path.getParent());
                    Files.copy(tarArchiveInputStream, path, StandardCopyOption.REPLACE_EXISTING);
                    setPermissions(entry, path);
                }
            }
        }
    }

    void setPermissions(TarArchiveEntry entry, Path path) throws IOException {
        IOException thrown = jvmOfCurrentlyRunningOS().permissionSetter.apply(entry, path);
        if (thrown != null) {
            throw thrown;
        }
    }

    private Jvm jvmOfCurrentlyRunningOS() {
        if (currentOsJvm == null) {
            String name = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
            if (name.contains("nux")) {
                currentOsJvm = Jvm.LINUX;
            } else if (name.contains("mac")) {
                currentOsJvm = Jvm.OSX;
            } else {
                currentOsJvm = Jvm.WINDOWS;
            }
        }
        return currentOsJvm;
    }

    private String streamToString(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8).trim();
    }

    private void checkState() {
        if (url == null) {
            throw new IllegalStateException("Remote URL to artifact not set");
        }
        if (destinationDir == null) {
            throw new IllegalStateException("Destination directory not set");
        }
    }

}
