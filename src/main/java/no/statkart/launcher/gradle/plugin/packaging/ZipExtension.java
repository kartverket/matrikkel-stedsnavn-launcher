package no.statkart.launcher.gradle.plugin.packaging;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipExtension implements PackagingExtension {

    private String topDirectory;

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void topDirectory(String topDirectory) {
        this.topDirectory = topDirectory;
    }

    @Override
    public void execute(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destination.toFile()))) {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path path = source.relativize(file);
                    if (topDirectory != null) {
                        path = Paths.get(topDirectory).resolve(path);
                    }
                    zos.putNextEntry(new ZipEntry(path.toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

}
