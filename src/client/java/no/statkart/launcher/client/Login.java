package no.statkart.launcher.client;

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
    static LoginParametre innhentGyldigeLoginParametre(String tittel, List<LoginParametre> tidligereLoginParametre) {
        String versjon = Login.class.getPackage().getImplementationVersion();
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
            String user = loginParametre.get().getBrukernavn();
            char[] pass = loginParametre.get().getPassord();
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });
            gyldigeLoginParametre = loginParametre.get();
            try {
                Integer.parseInt(gyldigeLoginParametre.getHeap());
                URL tst = new URL(gyldigeLoginParametre.getTjener() + Konfigurasjon.get("tryCredentialsUsing"));
                tst.openConnection().getInputStream();
                loggInnIKlienten(gyldigeLoginParametre);
                registrerLauncherVersjon(versjon);
            } catch (Exception e) {
                feil = new Feil(e, gyldigeLoginParametre);
                gyldigeLoginParametre = null;
            }
        } while (gyldigeLoginParametre == null);
        return gyldigeLoginParametre;
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
