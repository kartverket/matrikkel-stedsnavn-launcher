package no.statkart.launcher.client;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.List;
import java.util.Optional;

class Login {

    /**
     * Denne metoden har en viktig sideeffekt:
     * Standard autentisering settes statisk.  Det vil si at enhver påfølgende
     * URLConnection vil prøve å koble til med dette brukernavn/passord-paret.
     * <p/>
     * Dette betyr at klientartifaktene som ligger bak
     * BASIC AUTH kan lastes ned av getdown.
     */
    static Credentials innhentGyldigTjenerOgBrukernavnOgPassord(String tittel, List<URL> tjenere) {
        String versjon = Login.class.getPackage().getImplementationVersion();
        Credentials gyldigCredentials;
        Feil feil = null;
        do {
            Optional<Credentials> credentials = new LoginDialog()
                    .medTittel(tittel)
                    .medVersjon(versjon)
                    .medTidligereFeil(feil)
                    .medForslagTilTjenere(tjenere)
                    .innhentTjenerOgBrukernavnOgPassord();
            if (!credentials.isPresent()) {
                System.exit(0);
            }
            String user = credentials.get().getUser();
            String pass = credentials.get().getPass();
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass.toCharArray());
                }
            });
            gyldigCredentials = credentials.get();
            try {
                URL tst = new URL(gyldigCredentials.getServer() + "/launcher/getdown.txt");
                tst.openConnection().getInputStream();
                loggInnIKlienten(gyldigCredentials);
                registrerLauncherVersjon(versjon);
            } catch (IOException e) {
                feil = new Feil(e, gyldigCredentials);
                gyldigCredentials = null;
            }
        } while (gyldigCredentials == null);
        return gyldigCredentials;
    }

    /**
     * System properties med prefiks "app." vil videreføres til prosessen
     * som startes av getdown - uten prefikset.
     * <p/>
     * Dette fører til at passordet vil være synlig i prosesslista, men
     * denne informasjonen er transient.
     */
    private static void loggInnIKlienten(Credentials credentials) {
        System.setProperty("app.skif.server_username", credentials.getUser());
        System.setProperty("app.skif.server_password", credentials.getPass());
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
