package no.statkart.launcher.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Konfigurasjon {

    private final Path rot;
    private final Properties properties;

    public Konfigurasjon(Path rot) {
        this.rot = rot;
        this.properties = getProperties();
    }

    synchronized String get(Konfigurasjonsverdi key) {
        return properties.getProperty(key.getKey());
    }

    synchronized boolean is(Konfigurasjonsverdi key) {
        return "true".equalsIgnoreCase(get(key));
    }

    private Properties getProperties() {
        Path propPath = rot.resolve("client.properties");
        if (!Files.exists(propPath)) {
            throw new IllegalStateException("Finner ikke 'client.properties' i work-katalogen");
        }
        Properties p = new Properties();
        try {
            p.load(Files.newInputStream(propPath));
        } catch (IOException e) {
            throw new IllegalStateException("Klarte ikke lese 'client.properties' i work-katalogen");
        }
        return p;
    }

}
