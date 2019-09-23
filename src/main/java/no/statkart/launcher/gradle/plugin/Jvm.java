package no.statkart.launcher.gradle.plugin;

import com.badlogicgames.packr.PackrConfig;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;

enum Jvm {
    WINDOWS(
            "windows",
            "openjdk-13_windows-x64_bin.zip",
            "jdk-13/jmods"
    ),
    LINUX(
            "linux",
            "openjdk-13_linux-x64_bin.tar.gz",
            "jdk-13/jmods"
    ),
    OSX(
            "osx",
            "openjdk-13_osx-x64_bin.tar.gz",
            "jdk-13.jdk/Contents/Home/jmods"
    );

    private final String alias;
    private final String artifact;
    private final String jmodsPath;

    Jvm(String alias, String artifact, String jmodsPath) {
        this.alias = alias;
        this.artifact = artifact;
        this.jmodsPath = jmodsPath;
    }

    String getAlias() {
        return alias;
    }

    String getArtifact() {
        return artifact;
    }

    PackrConfig.Platform toPackrPlatform() {
        return this == Jvm.LINUX ? PackrConfig.Platform.Linux64
                : this == Jvm.OSX ? PackrConfig.Platform.MacOS
                : PackrConfig.Platform.Windows64;
    }

    private boolean isZip() {
        return artifact.endsWith(".zip");
    }

    private boolean isTarGz() {
        return artifact.endsWith(".tar.gz");
    }

    void unpack(Path dir) throws IOException {
        Path source = dir.resolve(artifact);
        Path destination = dir.resolve(alias);
        if (isZip()) {
            unzip(source, destination);
        } else if (isTarGz()) {
            untargz(source, destination);
        } else {
            throw new RuntimeException("Unknown artifact compression method: " + artifact);
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

    void jlink(Path dir, List<String> modules, List<String> locales) throws IOException {
        Path source = dir.resolve(alias);
        Path destination = dir.resolve(alias + "-min").resolve("jre");
        String programnavn = isCurrentlyRunningWindows() ? "jlink.exe" : "jlink";
        Path p = Path.of(System.getProperty("java.home"), "bin", programnavn);
        String cmd = p.toString()
                + " --module-path " + jmodsPath
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
