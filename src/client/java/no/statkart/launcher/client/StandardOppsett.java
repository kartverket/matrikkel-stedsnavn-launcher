package no.statkart.launcher.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class StandardOppsett {

    private static final Pattern CONTEXT_PATH = Pattern.compile("[a-z]+://[^/]+(/.*)?/vault/?$");

    private final Path rot;

    private Konfigurasjon konfigurasjon;
    private Parametre parametre;
    private String contextPath;
    private Work work;

    StandardOppsett(Path rot) {
        this.rot = rot;
    }

    Path getRot() {
        return rot;
    }

    Konfigurasjon getKonfigurasjon() {
        if (konfigurasjon == null) {
            konfigurasjon = new Konfigurasjon(rot);
        }
        return konfigurasjon;
    }

    Parametre getParametre() {
        if (parametre == null) {
            parametre = new Parametre()
                    .medTjener(getKonfigurasjon().get(Konfigurasjonsverdi.DEFAULT_SERVER))
                    .medHeap(getKonfigurasjon().get(Konfigurasjonsverdi.DEFAULT_HEAP));
        }
        return parametre;
    }

    String getContextPath() throws IOException {
        if (contextPath == null) {
            Path getdownTxt = rot.resolve("getdown.txt");
            Properties properties = new Properties();
            try (InputStream is = Files.newInputStream(getdownTxt)) {
                properties.load(is);
            }
            String appbase = properties.getProperty("appbase");
            Matcher m = CONTEXT_PATH.matcher(appbase);
            if (!m.matches()) {
                throw new RuntimeException("Feilkonfigurert appbase i getdown.txt");
            }
            contextPath = m.group(1);
            if (contextPath == null) {
                contextPath = "";
            }
        }
        return contextPath;
    }

    Work getWork() throws IOException {
        if (work == null) {
            work = new Work(this, currentSystem().finnMappeMedSkriverettigheter());
        }
        return work;
    }

    private OS currentSystem() {
        String name = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (name.contains("nux")) {
            return new Linux();
        }
        if (name.contains("mac")) {
            return new Mac();
        }
        return new Windows();
    }

    private interface OS {
        Path finnMappeMedSkriverettigheter() throws IOException;
    }

    private class Windows implements OS {
        @Override
        public Path finnMappeMedSkriverettigheter() {
            String appdata = System.getenv("APPDATA");
            if (appdata == null) {
                throw new IllegalStateException("Fant ikke variablen APPDATA");
            }
            String rot = getKonfigurasjon().get(Konfigurasjonsverdi.WORK_WINDOWS)
                    .replace("%HOME%", appdata);
            return Paths.get(rot);
        }
    }

    private class Linux implements OS {
        @Override
        public Path finnMappeMedSkriverettigheter() {
            String home = System.getProperty("user.home");
            if (home == null) {
                throw new IllegalStateException("Fant ikke variablen user.home");
            }
            String rot = getKonfigurasjon().get(Konfigurasjonsverdi.WORK_LINUX)
                    .replace("%HOME%", home);
            return Paths.get(rot);
        }
    }

    private class Mac implements OS {
        @Override
        public Path finnMappeMedSkriverettigheter() {
            String home = System.getProperty("user.home");
            if (home == null) {
                throw new IllegalStateException("Fant ikke variablen user.home");
            }
            String rot = getKonfigurasjon().get(Konfigurasjonsverdi.WORK_OSX)
                    .replace("%HOME%", home);
            return Paths.get(rot);
        }
    }

}