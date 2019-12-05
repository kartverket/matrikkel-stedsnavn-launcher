package no.statkart.launcher.gradle.plugin.packaging;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class ZipExtension implements PackagingExtension {

    private String topDirectory;

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void topDirectory(String topDirectory) {
        this.topDirectory = topDirectory;
    }

    @Override
    @SuppressWarnings("OctalInteger")
    public void execute(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        try (FileOutputStream fileOutputStream = new FileOutputStream(destination.toFile());
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
             ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(bufferedOutputStream)) {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path path = source.relativize(file);
                    if (topDirectory != null) {
                        path = Paths.get(topDirectory).resolve(path);
                    }
                    ZipArchiveEntry entry = new ZipArchiveEntry(file.toFile(), path.toString());
                    entry.setUnixMode(0755);
                    zaos.putArchiveEntry(entry);
                    Files.copy(file, zaos);
                    zaos.closeArchiveEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

}
