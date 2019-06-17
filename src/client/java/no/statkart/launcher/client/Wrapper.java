package no.statkart.launcher.client;

import com.threerings.getdown.launcher.GetdownApp;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Klasse som wrapper getdown:
 * <ul>
 * <li>
 * Kopierer "work"-mappen som ligger i cwd til brukerens OS-spesifikke applikasjonskonfigurasjon.
 * Dette er fordi filene her må kunne skrives til (konfigurasjon, artifakter, logg, osv).
 * </li>
 * <li>
 * Har en egen loginboks - fordi klientartifaktene ligger bak basic auth.
 * </li>
 * <li>
 * Viderefører bruker/passord til klientens login.
 * </li>
 * </ul>
 */
public class Wrapper {

    static final String WORK_SOURCE = "work";

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    private static final OS[] SYSTEMS = new OS[]{new Windows(), new Mac(), new Linux()};

    /**
     * Denne metoden kan ikke kaste exception.
     * Isåfall låser den packr-bygde eksekverbare filen seg.
     */
    public static void main(String... args) {
        try {
            List<URL> forslagTilTjenere = finnForslagTilTjenere();
            Credentials credentials = Login.innhentGyldigTjenerOgBrukernavnOgPassord
                    (Konfigurasjon.get("tittel"), forslagTilTjenere);
            String work = finnEllerOpprettWorkMappe(new URL(credentials.getServer()));
            loggTilFil(work + "/launcher.log");
            ikkeLoggPassordetFra(credentials);
            GetdownApp.main(new String[]{work});
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(String.format("Kunne ikke starte klienten: %s", e.getMessage()));
            System.exit(-1);
        }
    }

    private static List<URL> finnForslagTilTjenere() throws IOException {
        List<URL> tjenere = new ArrayList<>();
        for (OS system : SYSTEMS) {
            if (system.match()) {
                tjenere.addAll(tilURLsSortertAccessTime(system.finnRot()));
            }
        }
        URL PROD = new URL(Konfigurasjon.get("standardUrl"));
        if (!tjenere.contains(PROD)) {
            tjenere.add(PROD);
        }
        return tjenere;
    }

    /**
     * Finn arbeidsmappen som vi har skriverettigheter til.
     * Opprett den dersom den ikke finnes fra før.
     */
    private static String finnEllerOpprettWorkMappe(URL tjener) throws IOException {
        for (OS system : SYSTEMS) {
            if (system.match()) {
                String work = opprettWorkMappe(system.finnRot(), tjener);
                if (work != null) {
                    return work;
                }
            }
        }
        throw new IllegalStateException("Ukjent OS: " + OS_NAME);
    }

    private static String opprettWorkMappe(Path rot, URL tjener) throws IOException {
        return kopierWorkTil(rot.resolve(urlTilMappenavn(tjener)), tjener);
    }

    private static List<URL> tilURLsSortertAccessTime(Path rot) throws IOException {
        if (!rot.toFile().exists()) {
            return Collections.emptyList();
        }
        Map<URL, Long> urlAccessTimes = tilURLAccessTimes(rot);
        List<Long> accessTimes = new ArrayList<>(urlAccessTimes.values());
        accessTimes.sort(Comparator.reverseOrder());
        List<URL> resultat = new ArrayList<>();
        for (Long accessTime : accessTimes) {
            Optional<URL> url = urlAccessTimes.entrySet().stream()
                    .filter(e -> e.getValue().equals(accessTime))
                    .map(Map.Entry::getKey)
                    .findFirst();
            if (url.isPresent()) {
                resultat.add(url.get());
                urlAccessTimes.remove(url.get());
            }
        }
        return resultat;
    }

    private static Map<URL, Long> tilURLAccessTimes(Path rot) throws IOException {
        return Files.walk(rot)
                .filter(f -> f.endsWith("touch.txt"))
                .map(Wrapper::tilURLAccessTime)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Optional<Map.Entry<URL, Long>> tilURLAccessTime(Path touch) {
        try {
            URL url = mappenavnTilUrl(touch.getParent().getFileName().toString());
            Long millis = Long.parseLong(new String(Files.readAllBytes(touch)));
            return Optional.of(new AbstractMap.SimpleEntry<>(url, millis));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    interface OS {
        boolean match();

        Path finnRot() throws IOException;
    }

    static class Windows implements OS {
        @Override
        public boolean match() {
            return OS_NAME.contains("win");
        }

        @Override
        public Path finnRot() {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData == null) {
                throw new IllegalStateException("Fant ikke variablen LOCALAPPDATA");
            }
            String rot = Konfigurasjon.get("work.windows")
                    .replace("%HOME%", localAppData);
            return Paths.get(rot);
        }
    }

    static class Linux implements OS {
        @Override
        public boolean match() {
            return OS_NAME.contains("nux");
        }

        @Override
        public Path finnRot() {
            String home = System.getProperty("user.home");
            if (home == null) {
                throw new IllegalStateException("Fant ikke variablen user.home");
            }
            String rot = Konfigurasjon.get("work.linux")
                    .replace("%HOME%", home);
            return Paths.get(rot);
        }
    }

    static class Mac implements OS {
        @Override
        public boolean match() {
            return OS_NAME.contains("mac");
        }

        @Override
        public Path finnRot() {
            String home = System.getProperty("user.home");
            if (home == null) {
                throw new IllegalStateException("Fant ikke variablen user.home");
            }
            String rot = Konfigurasjon.get("work.osx")
                    .replace("%HOME%", home);
            return Paths.get(rot);
        }
    }

    private static String kopierWorkTil(Path destination, URL url) throws IOException {
        Path source = Paths.get(WORK_SOURCE);
        Files.createDirectories(destination);
        // Registrer når denne tjeneren sist ble benyttet
        Files.write(
                destination.resolve("touch.txt"),
                ("" + System.currentTimeMillis()).getBytes(),
                StandardOpenOption.CREATE
        );
        Files.walk(source).forEach(fil -> kopierMenIkkeOverskriv(source, fil, destination, url));
        return destination.toString();
    }

    private static void kopierMenIkkeOverskriv(Path fraMappe, Path fraFil, Path tilMappe, URL tjener) {
        try {
            Path tilFil = tilMappe.resolve(fraMappe.relativize(fraFil));
            if (!Files.exists(tilFil)) {
                if (skalPatches(tilFil)) {
                    streamCopy(Files.newInputStream(fraFil), Files.newOutputStream(tilFil), patch(tjener));
                } else {
                    Files.copy(fraFil, tilFil);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean skalPatches(Path fil) {
        return fil.endsWith("getdown.txt");
    }

    private static Function<String, String> patch(URL tjener) {
        return (String s) -> s.replaceAll("[a-z]+://[^/]+", tjener.toExternalForm());
    }

    private static void streamCopy(InputStream input, OutputStream output, Function<String, String> function) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(input));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(output))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = function.apply(line);
                bw.write(line);
                bw.newLine();
            }
        }
    }

    private static void loggTilFil(String destination) throws IOException {
        File logFile = new File(destination);
        PrintStream logOut = new PrintStream(
                new BufferedOutputStream(new FileOutputStream(logFile)), true);
        System.setOut(logOut);
        System.setErr(logOut);
        // Fortell getdown at vi allerede har satt opp loggingen
        System.setProperty("no_log_redir", "true");
    }

    /**
     * Pass på at passordet aldri blir skrevet til disk (logg).
     */
    private static void ikkeLoggPassordetFra(Credentials credentials) {
        Logger logger = Logger.getLogger("");
        for (Handler handler : logger.getHandlers()) {
            handler.setFilter(record -> {
                record.setMessage(fjernPassord(record.getMessage(), credentials.getPass()));
                return true;
            });
        }
    }

    private static String fjernPassord(String input, String passord) {
        return input.replaceAll("\\b\\Q" + passord + "\\E\\b", "***");
    }

    private static String urlTilMappenavn(URL url) {
        String mappenavn = url.toExternalForm().replaceAll("^([a-z]+)://([^/]+).*$", "$1_$2");
        mappenavn = mappenavn.replace(':', '_');
        return mappenavn;
    }

    private static URL mappenavnTilUrl(String mappenavn) throws MalformedURLException {
        return new URL(mappenavn.replaceFirst("_", "://").replaceFirst("_", ":"));
    }

}
