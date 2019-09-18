package no.statkart.launcher.gradle.plugin.packaging;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

public class SevenZExtension implements PackagingExtension {

    private String topDirectory;

    private SevenZMethod method;
    private File sfx;
    private File sfxConfig;

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void topDirectory(String topDirectory) {
        this.topDirectory = topDirectory;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void method(SevenZMethod method) {
        this.method = method;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void sfx(File sfx) {
        this.sfx = sfx;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void sfxConfig(File sfxConfig) {
        this.sfxConfig = sfxConfig;
    }

    @Override
    public void execute(Path fromDir, Path toFile) throws IOException {
        Path tmp = toFile.getParent().resolve(toFile.getFileName().toString() + ".tmp");
        opprett7z(fromDir, tmp);
        lagSelfExtracting7z(tmp, toFile);
        slett(tmp);
    }

    /**
     * Må bruke commons compress for å pakke ut zip-fil, patche og lagre som 7z.
     * 7z brukes for å støtte open source "self extracting zip"-funksjonalitet.
     */
    private void opprett7z(Path source, Path destination) throws IOException {
        // Det er bare en enkelt mappe på toppnivå i artifaktet
        Files.createDirectories(destination.getParent());
        try (SevenZOutputFile sevenZOutputFile = new SevenZOutputFile(destination.toFile())) {
            sevenZOutputFile.setContentCompression(method);
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path path = source.relativize(file);
                    if (topDirectory != null) {
                        path = Paths.get(topDirectory).resolve(path);
                    }
                    SevenZArchiveEntry entry = new SevenZArchiveEntry();
                    entry.setName(path.toString());
                    sevenZOutputFile.putArchiveEntry(entry);
                    int count;
                    byte[] b = new byte[1024];
                    InputStream is = Files.newInputStream(file);
                    while ((count = is.read(b)) > 0) {
                        sevenZOutputFile.write(b, 0, count);
                    }
                    sevenZOutputFile.closeArchiveEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * For å lage en eksekverbar fil trenger vi bare legge sfx'en, konfigurasjonen og 7z-pakka etter hverandre.
     * Se f.eks. https://olegscherbakov.github.io/7zSFX/.
     */
    private void lagSelfExtracting7z(Path source, Path destination) throws IOException {
        Path sfxPath = sfx.toPath();
        Path sfxConfigPath = sfxConfig.toPath();
        String klientversjon = //destination.getFileName().toString().replaceAll("^.*-(\\d+\\.\\d+)-.*$", "$1");
                getClass().getPackage().getImplementationVersion();
        String configInnhold = Files.readString(sfxConfigPath);
        configInnhold = configInnhold.replaceAll("%klientversjon%", klientversjon);
        try (FileChannel out = FileChannel.open(destination, CREATE, WRITE)) {
            append(out, sfxPath);
            append(out, configInnhold);
            append(out, source);
        }
    }

    private void slett(Path fil) throws IOException {
        Files.delete(fil);
    }

    private void append(FileChannel out, Path source) throws IOException {
        try (FileChannel in = FileChannel.open(source, READ)) {
            for (long p = 0, l = in.size(); p < l; ) {
                p += in.transferTo(p, l - p, out);
            }
        }
    }

    private void append(FileChannel out, String innhold) throws IOException {
        out.write(ByteBuffer.wrap(innhold.getBytes(StandardCharsets.UTF_8)));
    }

}
