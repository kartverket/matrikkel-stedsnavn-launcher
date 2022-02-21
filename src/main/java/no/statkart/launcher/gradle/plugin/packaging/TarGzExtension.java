package no.statkart.launcher.gradle.plugin.packaging;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class TarGzExtension implements PackagingExtension {

    private String arch;
    private String name;
    private String version;

    @Override
    public void setArch(String arch) {
        this.arch = arch;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    @SuppressWarnings("OctalInteger")
    public Path execute(Path fromDir, Path toDir) throws IOException {
        Files.createDirectories(toDir);
        Path toFile = toDir.resolve(toFilename());
        String topDirectory = tilTopDirectory();
        try (FileOutputStream fileOutputStream = new FileOutputStream(toFile.toFile());
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
             GzipCompressorOutputStream gzipOutputStream = new GzipCompressorOutputStream(bufferedOutputStream);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gzipOutputStream)) {
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            Files.walkFileTree(fromDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path path = fromDir.relativize(file);
                    path = Paths.get(topDirectory).resolve(path);
                    TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), path.toString());
                    entry.setMode(0755);
                    taos.putArchiveEntry(entry);
                    Files.copy(file, taos);
                    taos.closeArchiveEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return toFile;
    }

    private String toFilename() {
        return name + "-" + version + ".tar.gz";
    }

    private String tilTopDirectory() {
        if ("osx".equals(arch)) {
            return name + "-" + version + ".app";
        }
        return name + "-" + version;
    }

}