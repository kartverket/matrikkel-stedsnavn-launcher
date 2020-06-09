package no.statkart.launcher.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

class Konfigurasjon {

    private static Properties properties;

    static synchronized String get(Konfigurasjonsverdi key) {
        if (properties == null) {
            properties = getProperties();
        }
        return properties.getProperty(key.getKey());
    }

    static synchronized boolean is(Konfigurasjonsverdi key) {
        return "true".equalsIgnoreCase(get(key));
    }

    private static Properties getProperties() {
        Path propPath = Paths.get(Work.SOURCE).resolve("client.properties");
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
