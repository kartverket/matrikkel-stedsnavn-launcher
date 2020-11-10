package no.statkart.launcher.client;

import java.net.Authenticator;
import java.util.List;
import java.util.Optional;

/**
 * Denne klassen har en viktig sideeffekt:
 * Standard autentisering settes statisk vha {@link Authenticator}.
 * Det vil si at enhver påfølgende URLConnection vil prøve å koble til med dette brukernavn/passord-paret.
 * <p/>
 * Dermed kan klientartifaktene som ligger bak BASIC AUTH lastes ned av getdown.
 */
class Input {

    static Parametre innhentParametre(List<Parametre> tidligereInputParametre, TjenerKontroll kontroll) {
        Optional<Feil> feil = Optional.empty();
        if (!visHeap() && !visTjener()) {
            feil = kontroll.utenBrukerPassord(tidligereInputParametre.get(0));
            if (feil.isEmpty()) {
                return tidligereInputParametre.get(0);
            }
        }
        String tittel = Konfigurasjon.get(Konfigurasjonsverdi.TITLE);
        String versjon = Konfigurasjon.get(Konfigurasjonsverdi.VERSION);
        String melding = Konfigurasjon.get(Konfigurasjonsverdi.INPUT_MESSAGE);
        Parametre input;
        do {
            boolean brukerPassord = visBrukerPassord() || feil.isPresent() && feil.get().erBrukerPassordFeil();
            input = new InputDialog()
                    .medTittel(tittel)
                    .medVersjon(versjon)
                    .medMelding(melding)
                    .visHeap(visHeap())
                    .visTjener(visTjener())
                    .visBrukerPassord(brukerPassord)
                    .medTidligereInputParametre(tidligereInputParametre)
                    .medTidligereFeil(feil.orElse(null))
                    .innhentInputParametre();
            if (input == null) {
                // Bruker har valgt Avbryt
                return null;
            }
            if (brukerPassord) {
                feil = kontroll.medBrukerPassord(input);
            } else {
                feil = kontroll.utenBrukerPassord(input);
            }
        } while (feil.isPresent());
        return input;
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

}