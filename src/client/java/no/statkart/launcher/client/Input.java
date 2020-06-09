package no.statkart.launcher.client;

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Base64;
import java.util.List;

/**
 * Denne klassen har en viktig sideeffekt:
 * Standard autentisering settes statisk vha {@link Authenticator}.
 * Det vil si at enhver påfølgende URLConnection vil prøve å koble til med dette brukernavn/passord-paret.
 * <p/>
 * Dermed kan klientartifaktene som ligger bak BASIC AUTH lastes ned av getdown.
 */
class Input {

    /**
     * Input-boks trengs dersom server/credentials/heap er enablet i properties
     * og/eller det ikke er gitt default-verdier for server og/eller heap.
     */
    static boolean vis() {
        return visTjener() || visBrukerPassord() || visHeap();
    }

    static Parametre innhentParametre(List<Parametre> tidligereInputParametre) {
        String tittel = Konfigurasjon.get(Konfigurasjonsverdi.TITLE);
        String versjon = Konfigurasjon.get(Konfigurasjonsverdi.VERSION);
        String melding = Konfigurasjon.get(Konfigurasjonsverdi.INPUT_MESSAGE);
        if (visBrukerPassord() && !harValidatePath()) {
            throw new IllegalArgumentException("Beskyttet tilkobling, men client.properties mangler "
                    + Konfigurasjonsverdi.INPUT_CREDENTIALS_VALIDATE.getKey());
        }
        Feil feil = null;
        Parametre input;
        do {
            input = new InputDialog()
                    .medTittel(tittel)
                    .medVersjon(versjon)
                    .medMelding(melding)
                    .visHeap(visHeap())
                    .visTjener(visTjener())
                    .visBrukerPassord(visBrukerPassord())
                    .medTidligereInputParametre(tidligereInputParametre)
                    .medTidligereFeil(feil)
                    .innhentInputParametre();
            feil = kontroller(input);
        } while (feil != null);
        return input;
    }

    private static Feil kontroller(Parametre input) {
        if (input != null) {
            try {
                input.kontroller();
                if (visBrukerPassord()) {
                    kontrollerBeskyttetTilkobling(input);
                    registrerInnlogging(input);
                }
            } catch (Exception e) {
                return new Feil(e, input);
            }
        }
        return null;
    }

    private static void kontrollerBeskyttetTilkobling(Parametre param) throws Exception {
        URL tst = new URL(param.getTjener() + Konfigurasjon.get(Konfigurasjonsverdi.INPUT_CREDENTIALS_VALIDATE));
        HttpURLConnection conn = (HttpURLConnection) tst.openConnection();
        // Bruker ikke Authenticator til innloggingen fordi den kan medføre mange tjenerkall.
        // Brukeren hadde da risikert å bli utestengt ved galt u/p på første forsøk.
        conn.setRequestProperty("Authorization", "Basic " + encode(param.getBrukernavn(), param.getPassord()));
        conn.getInputStream();
    }

    private static String encode(String user, char[] pass) {
        String p = pass == null ? "" : String.copyValueOf(pass);
        return Base64.getEncoder().encodeToString((user + ":" + p).getBytes());
    }

    private static void registrerInnlogging(Parametre param) {
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
    private static void loggInnIKlienten(Parametre param) {
        System.setProperty("app.skif.server_username", param.getBrukernavn());
        System.setProperty("app.skif.server_password", String.copyValueOf(param.getPassord()));
    }

    static boolean visTjener() {
        return Konfigurasjon.is(Konfigurasjonsverdi.INPUT_SERVER)
                || Konfigurasjon.get(Konfigurasjonsverdi.DEFAULT_SERVER) == null;
    }

    static boolean visBrukerPassord() {
        return Konfigurasjon.is(Konfigurasjonsverdi.INPUT_CREDENTIALS);
    }

    static boolean visHeap() {
        return Konfigurasjon.is(Konfigurasjonsverdi.INPUT_HEAP)
                || Konfigurasjon.get(Konfigurasjonsverdi.DEFAULT_HEAP) == null;
    }

    static boolean harValidatePath() {
        return Konfigurasjon.get(Konfigurasjonsverdi.INPUT_CREDENTIALS_VALIDATE) != null;
    }

}
