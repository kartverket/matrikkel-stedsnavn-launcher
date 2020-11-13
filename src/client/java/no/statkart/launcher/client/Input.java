package no.statkart.launcher.client;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

class Input {

    private final StandardOppsett standard;

    Input(StandardOppsett standard) {
        this.standard = standard;
    }

    Parametre innhentParametre(List<Parametre> tidligereInputParametre) {
        TjenerKontroll kontroll = new TjenerKontroll(standard);
        Optional<Feil> feil = Optional.empty();
        if (!visHeap() && !visTjener()) {
            feil = kontroll.utenBrukerPassord(tidligereInputParametre.get(0));
            if (feil.isEmpty()) {
                return tidligereInputParametre.get(0);
            }
        }
        String melding = standard.getKonfigurasjon().get(Konfigurasjonsverdi.INPUT_MESSAGE);
        Path iconPath = standard.getRot().resolve("login.png");
        Parametre input;
        do {
            boolean brukerPassord = visBrukerPassord() || feil.isPresent() && feil.get().erBrukerPassordFeil();
            input = new InputDialog()
                    .medTittel(inputTittel())
                    .medMelding(melding)
                    .medIconPath(iconPath)
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

    String inputTittel() {
        String tittel = standard.getKonfigurasjon().get(Konfigurasjonsverdi.TITLE);
        String versjon = standard.getKonfigurasjon().get(Konfigurasjonsverdi.VERSION);
        return versjon == null
                ? tittel + " (uversjonert)"
                : tittel + " " + versjon;
    }

    boolean visTjener() {
        return standard.getKonfigurasjon().is(Konfigurasjonsverdi.INPUT_SERVER)
                || standard.getKonfigurasjon().get(Konfigurasjonsverdi.DEFAULT_SERVER) == null;
    }

    boolean visBrukerPassord() {
        return standard.getKonfigurasjon().is(Konfigurasjonsverdi.INPUT_CREDENTIALS);
    }

    boolean visHeap() {
        return standard.getKonfigurasjon().is(Konfigurasjonsverdi.INPUT_HEAP)
                || standard.getKonfigurasjon().get(Konfigurasjonsverdi.DEFAULT_HEAP) == null;
    }

}