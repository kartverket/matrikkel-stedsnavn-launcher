package no.statkart.launcher.client;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Klasse som håndterer kommunikasjon med tjeneren _før_ getdown trer inn i prosessen.
 */
public class TjenerKontroll {

    private final StandardOppsett standard;

    TjenerKontroll(StandardOppsett standard) {
        this.standard = standard;
    }

    /**
     * Undersøk om man kan hente getdown.txt fra tjeneren uten bruker/passord.
     */
    Optional<Feil> utenBrukerPassord(Parametre param) {
        try {
            URL url = tilGetDownUrl(param.getTjener());
            check(url);
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
     * Undersøk om man kan hente getdown.txt fra tjeneren med gitt bruker/passord.
     * <p>
     * Bruker ikke Authenticator til innloggingen fordi den kan medføre mange tjenerkall.
     * Brukeren hadde da risikert å bli utestengt ved galt u/p på første forsøk.
     */
    Optional<Feil> medBrukerPassord(Parametre param) {
        try {
            param.kontroller();
            URL url = tilGetDownUrl(param.getTjener());
            check(url, param.getBrukernavn(), param.getPassord());
            registrerInnlogging(param);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(new Feil(e, param));
        }
    }

    /**
     * Undersøk om denne klienten er akseptert av tjeneren.  Klientens versjon
     * sendes inn, og kontrolleres mot tjenerens konfigurerte minimumsversjon.
     */
    boolean klientAkseptert(Parametre param) {
        try {
            URL url = tilKlientValideringUrl(param.getTjener(), finnKlientVersjon());
            check(url);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private URL tilGetDownUrl(String baseUrl) throws IOException {
        String contextPath = standard.getContextPath();
        return new URL(baseUrl + contextPath + "/vault/getdown.txt");
    }

    private URL tilKlientValideringUrl(String baseUrl, String klientVersjon) throws IOException {
        String contextPath = standard.getContextPath();
        String query = String.format
                ("version=%s", URLEncoder.encode(klientVersjon, StandardCharsets.UTF_8));
        return new URL(baseUrl + contextPath + "/validate?" + query);
    }

    private String finnKlientVersjon() {
        return standard.getKonfigurasjon().get(Konfigurasjonsverdi.VERSION);
    }

    /**
     * Standard autentisering settes statisk vha {@link Authenticator}.
     * Det vil si at enhver påfølgende URLConnection vil prøve å koble til med dette brukernavn/passord-paret.
     * <p/>
     * Klientartifakter som ligger bak BASIC AUTH kan dermed lastes ned av getdown sømløst.
     */
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

    private void check(URL url) throws IOException {
        check(url, null, null);
    }

    private void check(URL url, String brukernavn, char[] passord) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            if (brukernavn != null && passord != null) {
                conn.setRequestProperty("Authorization", "Basic " + encode(brukernavn, passord));
            }
            conn.getInputStream();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String encode(String user, char[] pass) {
        String p = pass == null ? "" : String.copyValueOf(pass);
        return Base64.getEncoder().encodeToString((user + ":" + p).getBytes());
    }

}