package no.statkart.launcher.gradle.plugin.packaging;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class TarGzExtension implements PackagingExtension {

    private final String arch;

    private File icon;
    private String name;
    private String version;

    public TarGzExtension(String arch) {
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
    public File getIcon() {
        return icon;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void icon(File icon) {
        this.icon = icon;
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
            leggTilSymlink(topDirectory, "jre/lib/jli", taos);
            leggTilSymlink(topDirectory, "jre/lib/amd64", taos);
        }
        return toFile;
    }

    private void leggTilSymlink(String topDirectory, String mappe, TarArchiveOutputStream output) throws IOException {
        Path path = Paths.get(mappe);
        if ("osx".equals(arch)) {
            path = Paths.get(topDirectory).resolve("Contents").resolve("Resources").resolve(path);
        } else {
            path = Paths.get(topDirectory).resolve(path);
        }
        TarArchiveEntry link = new TarArchiveEntry(path.toString(), TarConstants.LF_SYMLINK);
        link.setLinkName(".");
        output.putArchiveEntry(link);
        output.closeArchiveEntry();
    }

    private String toFilename() {
        return name + "-" + arch + "-" + version + ".tar.gz";
    }

    private String tilTopDirectory() {
        if ("osx".equals(arch)) {
            return name + "-" + arch + "-" + version + ".app";
        }
        return name + "-" + arch + "-" + version;
    }

}
