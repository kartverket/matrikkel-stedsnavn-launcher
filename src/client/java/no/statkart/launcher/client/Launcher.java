package no.statkart.launcher.client;

import com.threerings.getdown.launcher.GetdownApp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.Logger;

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
public class Launcher {

    private static final OS SYSTEM = currentSystem();

    /**
     * Denne metoden kan ikke kaste exception.
     * Isåfall låser den packr-bygde eksekverbare filen seg.
     */
    public static void main(String... args) {
        try {
            Work work = new Work(SYSTEM.finnRot());
            Parametre parametre = finnParametre(work);
            if (parametre == null) {
                System.exit(0);
            }
            parametre.kontroller();
            String workMappe = work.finnEllerOpprettWorkMappe(parametre);
            work.skrivLoginParametre(parametre);
            loggTilFil(workMappe + "/launcher.log");
            ikkeLoggPassordetFra(parametre);
            leggTilEkstraParametre(workMappe + "/extra.txt", parametre, args);
            registrerLauncherVersjon();
            brukTjeneradresseFra(parametre);
            GetdownApp.main(new String[]{workMappe});
        } catch (Exception e) {
            e.printStackTrace();
            System.err.printf("Kunne ikke starte klienten: %s%n", e.getMessage());
            System.exit(-1);
        }
    }

    private static Parametre finnParametre(Work work) throws Exception {
        Parametre standard = new Parametre()
                .medTjener(Konfigurasjon.get(Konfigurasjonsverdi.DEFAULT_SERVER))
                .medHeap(Konfigurasjon.get(Konfigurasjonsverdi.DEFAULT_HEAP));
        if (!Input.vis()) {
            return standard;
        }
        List<Parametre> forslagTilParametre = work.lesInputParametre();
        fyllInnManglendeStandardVerdier(forslagTilParametre, standard);
        if (Input.visTjener()) {
            if (forslagTilParametre.isEmpty()) {
                forslagTilParametre.add(standard);
            }
        } else {
            forslagTilParametre = Collections.singletonList(
                    forslagTilParametre.stream()
                            .filter(p -> Objects.equals(standard.getTjener(), p.getTjener()))
                            .findFirst().orElse(standard)
            );
        }
        return Input.innhentParametre(forslagTilParametre);
    }

    private static void fyllInnManglendeStandardVerdier(List<Parametre> paramList, Parametre standard) {
        paramList.forEach(p -> {
            if (p.getHeap() == null) {
                p.medHeap(standard.getHeap());
            }
        });
    }

    private static OS currentSystem() {
        String name = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (name.contains("nux")) {
            return new Linux();
        }
        if (name.contains("mac")) {
            return new Mac();
        }
        return new Windows();
    }

    interface OS {
        Path finnRot() throws IOException;
    }

    static class Windows implements OS {
        @Override
        public Path finnRot() {
            String appdata = System.getenv("APPDATA");
            if (appdata == null) {
                throw new IllegalStateException("Fant ikke variablen APPDATA");
            }
            String rot = Konfigurasjon.get(Konfigurasjonsverdi.WORK_WINDOWS)
                    .replace("%HOME%", appdata);
            return Paths.get(rot);
        }
    }

    static class Linux implements OS {
        @Override
        public Path finnRot() {
            String home = System.getProperty("user.home");
            if (home == null) {
                throw new IllegalStateException("Fant ikke variablen user.home");
            }
            String rot = Konfigurasjon.get(Konfigurasjonsverdi.WORK_LINUX)
                    .replace("%HOME%", home);
            return Paths.get(rot);
        }
    }

    static class Mac implements OS {
        @Override
        public Path finnRot() {
            String home = System.getProperty("user.home");
            if (home == null) {
                throw new IllegalStateException("Fant ikke variablen user.home");
            }
            String rot = Konfigurasjon.get(Konfigurasjonsverdi.WORK_OSX)
                    .replace("%HOME%", home);
            return Paths.get(rot);
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
    private static void ikkeLoggPassordetFra(Parametre loginParametre) {
        if (loginParametre.getPassord() == null) {
            return;
        }
        Logger logger = Logger.getLogger("");
        for (Handler handler : logger.getHandlers()) {
            handler.setFilter(record -> {
                record.setMessage(fjernPassord(record.getMessage(), loginParametre.getPassord()));
                return true;
            });
        }
    }

    private static String fjernPassord(String input, char[] passord) {
        return input.replaceAll("\\b\\Q" + String.copyValueOf(passord) + "\\E\\b", "***");
    }

    private static void leggTilEkstraParametre(String destination, Parametre loginParametre, String[] args) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("-Xmx" + loginParametre.getHeap() + "m");
        lines.addAll(Arrays.asList(args));
        Path p = Paths.get(destination);
        Files.write(p, lines);
    }

    private static void brukTjeneradresseFra(Parametre parametre) {
        // Brukes i getdown.txt for å vite hvor man skal laste ned klienten fra
        System.setProperty("appbase_domain", parametre.getTjener());
        // Brukes av klienten for å vite hvor den skal bruke tjenester fra
        System.setProperty("app.skif.server_url", parametre.getTjener());
        System.setProperty("app.skif.single_vm", "false");
    }

    /**
     * Versjonen brukes til å sjekke om brukeren må oppdatere launcheren sin.
     */
    private static void registrerLauncherVersjon() {
        String versjon = Konfigurasjon.get(Konfigurasjonsverdi.VERSION);
        if (versjon != null) {
            System.setProperty("app.launcher.version", versjon);
        }
    }

}