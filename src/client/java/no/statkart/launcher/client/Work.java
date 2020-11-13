package no.statkart.launcher.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class Work {

    private static final String CACHE = "cache.properties";

    private final StandardOppsett standard;
    private final Path rot;

    Work(StandardOppsett standard, Path rot) {
        this.standard = standard;
        this.rot = rot;
    }

    /**
     * Finn arbeidsmappen som vi har skriverettigheter til.
     * Opprett den dersom den ikke finnes fra før.
     */
    String finnEllerOpprettWorkMappe(Parametre loginParametre) throws IOException {
        String tjener = loginParametre.getTjener();
        String mappenavn = tjenerTilMappenavn(tjener);
        Path destination = rot.resolve(mappenavn);
        Files.createDirectories(destination);
        try (Stream<Path> paths = Files.walk(standard.getRot()).filter(Files::isRegularFile)) {
            paths.forEach(fil -> kopier(standard.getRot(), fil, destination));
        }
        return destination.toString();
    }

    private static void kopier(Path fraMappe, Path fraFil, Path tilMappe) {
        try {
            Path tilFil = tilMappe.resolve(fraMappe.relativize(fraFil));
            Files.copy(fraFil, tilFil, REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    void skrivLoginParametre(Parametre loginParametre) throws IOException {
        String tjener = loginParametre.getTjener();
        String mappenavn = tjenerTilMappenavn(tjener);
        Path destination = rot.resolve(mappenavn);
        Files.createDirectories(destination);
        Path p = destination.resolve(CACHE);
        try (OutputStream os = Files.newOutputStream(p)) {
            loginParametre.tilProperties().store(os, "Cached login details");
        }
        Files.deleteIfExists(destination.resolve("touch.txt"));
    }

    List<Parametre> lesInputParametre() throws IOException {
        if (!Files.exists(rot)) {
            return new ArrayList<>();
        }
        try (Stream<Path> paths = Files.walk(rot)) {
            return paths.filter(this::erFilMedLoginHistorikk)
                    .map(this::tilProperties)
                    .map(Parametre::new)
                    .sorted(Comparator.comparingLong(Parametre::getOppdatert).reversed())
                    .collect(Collectors.toList());
        }
    }

    private boolean erFilMedLoginHistorikk(Path p) {
        return p.endsWith(CACHE) || p.endsWith("touch.txt");
    }

    private Properties tilProperties(Path p) {
        Properties properties = new Properties();
        try {
            if (p.endsWith(CACHE)) {
                try (InputStream is = Files.newInputStream(p)) {
                    properties.load(is);
                    return properties;
                }
            } else {
                // Legacy-støtte for touch.txt
                properties.setProperty("tjener", mappenavnTilTjener(p.getParent().getFileName().toString()));
                properties.setProperty("oppdatert", new String(Files.readAllBytes(p)));
                properties.setProperty("heap", standard.getKonfigurasjon().get(Konfigurasjonsverdi.DEFAULT_HEAP));
                return properties;
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(p.toString());
        }
    }

    private static String mappenavnTilTjener(String mappenavn) {
        return mappenavn.replaceFirst("_", "://").replaceFirst("_", ":");
    }

    private static String tjenerTilMappenavn(String tjener) {
        String mappenavn = tjener.replaceAll("^([a-z]+)://([^/]+).*$", "$1_$2");
        mappenavn = mappenavn.replace(':', '_');
        return mappenavn;
    }

}