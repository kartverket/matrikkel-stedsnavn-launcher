package no.statkart.launcher.gradle.plugin.packaging;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
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
             GzipCompressorOutputStream gzipOutputStream = new GzipCompressorOutputStream(bufferedOutputStream);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gzipOutputStream)) {
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path path = source.relativize(file);
                    if (topDirectory != null) {
                        path = Paths.get(topDirectory).resolve(path);
                    }
                    TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), path.toString());
                    entry.setMode(0755);
                    taos.putArchiveEntry(entry);
                    Files.copy(file, taos);
                    taos.closeArchiveEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
            leggTilSymlink("jre/lib/jli", taos);
            leggTilSymlink("jre/lib/amd64", taos);
        }
    }

    private void leggTilSymlink(String mappe, TarArchiveOutputStream output) throws IOException {
        Path path = Paths.get(mappe);
        if (topDirectory != null) {
            if (topDirectory.endsWith(".app")) {
                path = Paths.get(topDirectory).resolve("Contents").resolve("Resources").resolve(path);
            } else {
                path = Paths.get(topDirectory).resolve(path);
            }
        }
        TarArchiveEntry link = new TarArchiveEntry(path.toString(), TarConstants.LF_SYMLINK);
        link.setLinkName(".");
        output.putArchiveEntry(link);
        output.closeArchiveEntry();
    }

}
