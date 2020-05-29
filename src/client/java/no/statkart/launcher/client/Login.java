package no.statkart.launcher.client;

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Denne klassen har en viktig sideeffekt:
 * Standard autentisering settes statisk vha {@link Authenticator}.
 * Det vil si at enhver påfølgende URLConnection vil prøve å koble til med dette brukernavn/passord-paret.
 * <p/>
 * Dermed kan klientartifaktene som ligger bak BASIC AUTH lastes ned av getdown.
 */
class Login {

    static LoginParametre innhentGyldigeLoginParametre(List<LoginParametre> tidligereLoginParametre) {
        String tittel = Konfigurasjon.get(Konfigurasjonsverdi.TITLE);
        String versjon = Konfigurasjon.get(Konfigurasjonsverdi.VERSION);
        registrerLauncherVersjon(versjon);
        LoginParametre gyldigeLoginParametre;
        Feil feil = null;
        do {
            Optional<LoginParametre> loginParametre = new LoginDialog()
                    .medTittel(tittel)
                    .medVersjon(versjon)
                    .medTidligereFeil(feil)
                    .medTidligereLoginParametre(tidligereLoginParametre)
                    .innhentLoginParametre();
            if (loginParametre.isEmpty()) {
                System.exit(0);
            }
            gyldigeLoginParametre = loginParametre.get();
            try {
                Integer.parseInt(gyldigeLoginParametre.getHeap());
                URL tst = new URL(gyldigeLoginParametre.getTjener() + Konfigurasjon.get(Konfigurasjonsverdi.TRY_CRENDENTIALS_USING_PATH));
                // Bruker ikke Authenticator til innloggingen fordi den kan medføre mange tjenerkall.
                // Brukeren hadde da risikert å bli utestengt ved galt u/p på første forsøk.
                HttpURLConnection conn = (HttpURLConnection) tst.openConnection();
                conn.setRequestProperty("Authorization", "Basic " + encode
                        (gyldigeLoginParametre.getBrukernavn(), gyldigeLoginParametre.getPassord()));
                conn.getInputStream();
                ferdigstillInnlogging(gyldigeLoginParametre);
            } catch (Exception e) {
                feil = new Feil(e, gyldigeLoginParametre);
                gyldigeLoginParametre = null;
            }
        } while (gyldigeLoginParametre == null);
        return gyldigeLoginParametre;
    }

    private static void ferdigstillInnlogging(LoginParametre loginParametre) {
        loggInnIKlienten(loginParametre);
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(loginParametre.getBrukernavn(), loginParametre.getPassord());
            }
        });
    }

    private static String encode(String user, char[] pass) {
        return Base64.getEncoder().encodeToString((user + ":" + String.copyValueOf(pass)).getBytes());
    }

    /**
     * System properties med prefiks "app." vil videreføres til prosessen
     * som startes av getdown - uten prefikset.
     * <p/>
     * Dette fører til at passordet vil være synlig i prosesslista, men
     * denne informasjonen er transient.
     */
    private static void loggInnIKlienten(LoginParametre loginParametre) {
        System.setProperty("app.skif.server_username", loginParametre.getBrukernavn());
        System.setProperty("app.skif.server_password", String.copyValueOf(loginParametre.getPassord()));
    }

    /**
     * Versjonen brukes til å sjekke om brukeren må oppdatere launcheren sin.
     */
    private static void registrerLauncherVersjon(String versjon) {
        if (versjon != null) {
            System.setProperty("app.launcher.version", versjon);
        }
    }

}
