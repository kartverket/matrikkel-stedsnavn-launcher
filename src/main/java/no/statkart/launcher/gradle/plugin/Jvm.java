package no.statkart.launcher.gradle.plugin;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

enum Jvm {
    WINDOWS("windows"),
    LINUX("linux"),
    OSX("osx");

    private final String alias;

    Jvm(String alias) {
        this.alias = alias;
    }

    String getAlias() {
        return alias;
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
        String programnavn = isCurrentlyRunningWindows() ? "jlink.exe" : "jlink";
        Path p = Path.of(System.getProperty("java.home"), "bin", programnavn);
        String cmd = p.toString()
                + " --module-path " + getJModsDirectory(source)
                + " --add-modules " + String.join(",", modules)
                + " --include-locales " + String.join(",", locales)
                + " --output " + destination;
        // System.out.println("jlink cwd=" + source.toFile());
        // System.out.println("jlink cmd=" + cmd);
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
                }
            }
        }
    }

    private boolean isCurrentlyRunningWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.US).contains("windows");
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
