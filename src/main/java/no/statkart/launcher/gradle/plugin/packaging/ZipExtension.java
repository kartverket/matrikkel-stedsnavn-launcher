package no.statkart.launcher.gradle.plugin.packaging;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

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

public class ZipExtension implements PackagingExtension {

    private final String arch;

    private File icon;
    private String name;
    private String version;

    public ZipExtension(String arch) {
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
             ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(bufferedOutputStream)) {
            Files.walkFileTree(fromDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path path = fromDir.relativize(file);
                    path = Paths.get(topDirectory).resolve(path);
                    ZipArchiveEntry entry = new ZipArchiveEntry(file.toFile(), path.toString());
                    entry.setUnixMode(0755);
                    zaos.putArchiveEntry(entry);
                    Files.copy(file, zaos);
                    zaos.closeArchiveEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return toFile;
    }

    private String toFilename() {
        return name + "-" + arch + "-" + version + ".zip";
    }

    private String tilTopDirectory() {
        if ("osx".equals(arch)) {
            return name + "-" + arch + "-" + version + ".app";
        }
        return name + "-" + arch + "-" + version;
    }

}
