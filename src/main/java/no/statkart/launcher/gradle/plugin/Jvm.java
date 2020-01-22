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
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

enum Jvm {
    WINDOWS("windows"),
    LINUX("linux"),
    OSX("osx");

    private static final int MAX_MODE = 511; // Octal 0777
    private static final int MIN_MODE = 0;
    private final String alias;

    Jvm(String alias) {
        this.alias = alias;
    }

    String getAlias() {
        return alias;
    }

    void download(String urlString, Path destinationDir) throws IOException {
        URL url = new URL(urlString);
        Path destination = destinationDir.resolve(Paths.get(url.getPath()).getFileName());
        if (Files.isRegularFile(destination)) {
            System.out.println("Using existing jvm at " + destination);
            return;
        }
        System.out.println("Downloading jvm to " + destination);
        try (InputStream in = url.openStream()) {
            Files.copy(in, destination);
        }
    }

    void unpack(Path dir) throws IOException {
        Path source = getArtifact(dir);
        Path destination = dir.resolve(alias);
        if (Files.exists(destination)) {
            return;
        }
        if (isZip(source)) {
            unzip(source, destination);
        } else if (isTarGz(source)) {
            untargz(source, destination);
        } else {
            throw new RuntimeException("Unknown artifact compression method: " + source);
        }
    }

    void jlink(Path dir, List<String> modules, List<String> locales) throws IOException {
        Path source = dir.resolve(alias);
        Path destination = dir.resolve(alias + "-min").resolve("jre");
        if (Files.exists(destination)) {
            return;
        }
        Jvm currentOS = jvmOfCurrentlyRunningOS();
        Optional<Path> jlink = Files.walk(dir.resolve(currentOS.alias))
                .filter(path -> path.endsWith("jlink") || path.endsWith("jlink.exe"))
                .findFirst();
        if (jlink.isEmpty()) {
            throw new IllegalArgumentException("Cannot find jlink in " + dir);
        }
        String cmd = jlink.get().toString()
                + " --module-path " + getJModsDirectory(source)
                + " --add-modules " + String.join(",", modules)
                + " --include-locales " + String.join(",", locales)
                + " --output " + destination;
//        System.out.println("jlink cwd=" + source.toFile());
//        System.out.println("jlink cmd=" + cmd);
        Process prosess = Runtime.getRuntime().exec(cmd, null, source.toFile());
        try {
            prosess.waitFor();
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
    }

    private boolean isZip(Path path) {
        return path.getFileName().toString().endsWith(".zip");
    }

    private boolean isTarGz(Path path) {
        return path.getFileName().toString().endsWith(".tar.gz");
    }

    private Path getArtifact(Path dir) throws IOException {
        String regex = "^.*[^a-zA-Z0-9]" + alias + "[^a-zA-Z0-9].*$";
        try (Stream<Path> paths = Files.walk(dir, 1)) {
            return paths.filter(path -> path.getFileName().toString().matches(regex))
                    .findFirst().orElseThrow();
        }
    }

    private Path getJModsDirectory(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(path -> Files.isDirectory(path) && path.getFileName().toString().equals("jmods"))
                    .findFirst().orElseThrow();
        }
    }

    private void unzip(Path inputPath, Path outputDirPath) throws IOException {
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
                    Files.setPosixFilePermissions(path, getPermissions(entry.getMode()));
                }
            }
        }
    }

    private final static Map<Integer, PosixFilePermission> allPermissions = new HashMap<>() {{
        put(8, PosixFilePermission.OWNER_READ);
        put(7, PosixFilePermission.OWNER_WRITE);
        put(6, PosixFilePermission.OWNER_EXECUTE);
        put(5, PosixFilePermission.GROUP_READ);
        put(4, PosixFilePermission.GROUP_WRITE);
        put(3, PosixFilePermission.GROUP_EXECUTE);
        put(2, PosixFilePermission.OTHERS_READ);
        put(1, PosixFilePermission.OTHERS_WRITE);
        put(0, PosixFilePermission.OTHERS_EXECUTE);
    }};

    Set<PosixFilePermission> getPermissions(int mode) {
        if (mode > MAX_MODE || mode < MIN_MODE) {
            throw new RuntimeException("Invalid mode 0" + Integer.toOctalString(mode));
        }
        Set<PosixFilePermission> result = new HashSet<>();
        for (int bit = 0; bit < 9; bit++) {
            int set = (mode >> bit) & 1;
            if (set == 1) {
                result.add(allPermissions.get(bit));
            }
        }
        return result;
    }

    private static Jvm jvmOfCurrentlyRunningOS() {
        String name = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (name.contains("nux")) {
            return Jvm.LINUX;
        }
        if (name.contains("mac")) {
            return Jvm.OSX;
        }
        return Jvm.WINDOWS;
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

}
