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
import java.util.List;
import java.util.Locale;
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
public class Wrapper {

    private static final OS SYSTEM = currentSystem();

    /**
     * Denne metoden kan ikke kaste exception.
     * Isåfall låser den packr-bygde eksekverbare filen seg.
     */
    public static void main(String... args) {
        try {
            Work work = new Work(SYSTEM.finnRot());
            List<LoginParametre> forslagTilParametre = work.lesLoginParametre();
            LoginParametre loginParametre = Login.innhentGyldigeLoginParametre(forslagTilParametre);
            String workMappe = work.finnEllerOpprettWorkMappe(loginParametre);
            work.skrivLoginParametre(loginParametre);
            loggTilFil(workMappe + "/launcher.log");
            ikkeLoggPassordetFra(loginParametre);
            leggTilEkstraParametre(workMappe + "/extra.txt", loginParametre);
            GetdownApp.main(new String[]{workMappe});
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(String.format("Kunne ikke starte klienten: %s", e.getMessage()));
            System.exit(-1);
        }
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
            String rot = Konfigurasjon.get("work.windows")
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
            String rot = Konfigurasjon.get("work.linux")
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
            String rot = Konfigurasjon.get("work.osx")
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
    private static void ikkeLoggPassordetFra(LoginParametre loginParametre) {
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

    private static void leggTilEkstraParametre(String destination, LoginParametre loginParametre) throws IOException {
        Path p = Paths.get(destination);
        Files.writeString(p, "-Xmx" + loginParametre.getHeap() + "m");
    }

}
