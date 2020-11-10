package no.statkart.launcher.client;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Base64;
import java.util.Optional;

public class TjenerKontroll {

    private final String testPath;

    TjenerKontroll(String testPath) {
        this.testPath = testPath;
    }

    Optional<Feil> utenBrukerPassord(Parametre param) {
        try {
            URL tst = tilURL(param.getTjener());
            HttpURLConnection conn = (HttpURLConnection) tst.openConnection();
            conn.getInputStream();
            return Optional.empty();
        } catch (IOException e) {
            Feil feil = new Feil(e, param);
            // Ikke en brukerfeil dersom tjeneren krever bruker/passord, og vi ikke la det ved
            if (feil.erBrukerPassordFeil()) {
                feil = new Feil(e, param, false);
            }
            return Optional.of(feil);
        }
    }

    /**
     * Bruker ikke Authenticator til innloggingen fordi den kan medføre mange tjenerkall.
     * Brukeren hadde da risikert å bli utestengt ved galt u/p på første forsøk.
     */
    Optional<Feil> medBrukerPassord(Parametre param) {
        try {
            param.kontroller();
            URL tst = tilURL(param.getTjener());
            HttpURLConnection conn = (HttpURLConnection) tst.openConnection();
            conn.setRequestProperty("Authorization", "Basic " + encode(param.getBrukernavn(), param.getPassord()));
            conn.getInputStream();
            registrerInnlogging(param);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(new Feil(e, param));
        }
    }

    private static String encode(String user, char[] pass) {
        String p = pass == null ? "" : String.copyValueOf(pass);
        return Base64.getEncoder().encodeToString((user + ":" + p).getBytes());
    }

    private URL tilURL(String baseUrl) throws IOException {
        return new URL(baseUrl + testPath);
    }

    private void registrerInnlogging(Parametre param) {
        loggInnIKlienten(param);
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(param.getBrukernavn(), param.getPassord());
            }
        });
    }

    /**
     * System properties med prefiks "app." vil videreføres til prosessen
     * som startes av getdown - uten prefikset.
     * <p/>
     * Dette fører til at passordet vil være synlig i prosesslista, men
     * denne informasjonen er transient.
     */
    private void loggInnIKlienten(Parametre param) {
        System.setProperty("app.skif.server_username", param.getBrukernavn());
        System.setProperty("app.skif.server_password", String.copyValueOf(param.getPassord()));
    }

}