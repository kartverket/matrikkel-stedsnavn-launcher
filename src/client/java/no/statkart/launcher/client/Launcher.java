package no.statkart.launcher.client;

import io.github.bekoenig.getdown.launcher.GetdownApp;

import javax.swing.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * Klasse som wrapper getdown:
 * <ul>
 * <li>
 * Kopierer "work"-mappen som ligger i cwd til brukerens OS-spesifikke applikasjonskonfigurasjon.
 * Dette er fordi filene her må kunne skrives til (konfigurasjon, artifakter, logg, osv).
 * </li>
 * <li>
 * Har en egen potensiell dialogboks - man kan trenge å spørre etter tjener, bruker/passord
 * dersom BasicAuthentication beskytter vault på tjeneren, heapstørrelse på klient-vm'en.
 * </li>
 * <li>
 * Er tilrettelagt for skif-applikasjoner, men ikke et krav:
 * Setter skif.server_url og skif.single_vm, og potensielt skif.server_username og skif.server_password.
 * </li>
 * </ul>
 */
public class Launcher {

    private final StandardOppsett standard;

    /**
     * Denne metoden kan ikke kaste exception.
     * Isåfall låser den packr-bygde eksekverbare filen seg.
     */
    public static void main(String... args) {
        try {
            StandardOppsett standard = new StandardOppsett(Path.of("work"));
            new Launcher(standard).start(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.printf("Kunne ikke starte klienten: %s%n", e.getMessage());
            System.exit(-1);
        }
    }

    Launcher(StandardOppsett standard) {
        this.standard = standard;
    }

    void start(String... args) throws Exception {
        Input input = new Input(standard);
        Parametre parametre = finnParametre(input);
        if (parametre == null) {
            System.exit(0);
        }
        parametre.kontroller();
        Work work = standard.getWork();
        String workMappe = work.finnEllerOpprettWorkMappe(parametre);
        work.skrivLoginParametre(parametre);
        loggTilFil(workMappe + "/launcher.log");
        ikkeLoggPassordetFra(parametre);
        leggTilEkstraParametre(workMappe + "/extra.txt", parametre, args);
        if (!klientAkseptert(parametre)) {
            JOptionPane.showMessageDialog(
                    new JFrame(),
                    "Vennligst last ned matrikkelstarteren på nytt. Du må oppdatere til nyeste versjon.",
                    input.inputTittel(),
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(-1);
        }
        brukTjeneradresseFra(parametre);
        registrerKlientversjon();
        GetdownApp.main(new String[]{workMappe});
    }

    private Parametre finnParametre(Input input) throws Exception {
        List<Parametre> forslagTilParametre = standard.getWork().lesInputParametre();
        fyllInnManglendeStandardVerdier(forslagTilParametre);
        Parametre param = standard.getParametre();
        if (input.visTjener()) {
            if (forslagTilParametre.isEmpty()) {
                forslagTilParametre.add(param);
            }
        } else {
            forslagTilParametre = Collections.singletonList(
                    forslagTilParametre.stream()
                            .filter(p -> Objects.equals(param.getTjener(), p.getTjener()))
                            .findFirst().orElse(param)
            );
        }
        return input.innhentParametre(forslagTilParametre);
    }

    private boolean klientAkseptert(Parametre param) {
        TjenerKontroll kontroll = new TjenerKontroll(standard);
        return kontroll.klientAkseptert(param);
    }

    private void fyllInnManglendeStandardVerdier(List<Parametre> paramList) {
        paramList.forEach(p -> {
            if (p.getHeap() == null) {
                p.medHeap(standard.getParametre().getHeap());
            }
        });
    }

    private void loggTilFil(String destination) throws IOException {
        File logFile = new File(destination);
        PrintStream logOut = new PrintStream(
                new BufferedOutputStream(new FileOutputStream(logFile)), true);
        System.setOut(logOut);
        System.setErr(logOut);
        // Fortell getdown at vi allerede har satt opp loggingen
        System.setProperty("no_log_redir", "true");
    }

    /**
     * Pass på at passordet aldri blir skrevet til disk (logg).
     */
    private void ikkeLoggPassordetFra(Parametre loginParametre) {
        if (loginParametre.getPassord() == null) {
            return;
        }
        Logger logger = Logger.getLogger("");
        for (Handler handler : logger.getHandlers()) {
            handler.setFilter(record -> {
                record.setMessage(fjernPassord(record.getMessage(), loginParametre.getPassord()));
                return true;
            });
        }
    }

    private String fjernPassord(String input, char[] passord) {
        return input.replaceAll("\\b\\Q" + String.copyValueOf(passord) + "\\E\\b", "***");
    }

    private void leggTilEkstraParametre(String destination, Parametre loginParametre, String[] args) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("-Xmx" + loginParametre.getHeap() + "m");
        lines.addAll(Arrays.asList(args));
        Path p = Paths.get(destination);
        Files.write(p, lines);
    }

    private void brukTjeneradresseFra(Parametre parametre) {
        // Brukes i getdown.txt for å vite hvor man skal laste ned klienten fra
        System.setProperty("appbase_domain", parametre.getTjener());
        // Brukes av klienten for å vite hvor den skal bruke tjenester fra
        System.setProperty("app.skif.server_url", parametre.getTjener());
        System.setProperty("app.skif.single_vm", "false");
    }

    private void registrerKlientversjon() {
        String versjon = standard.getKonfigurasjon().get(Konfigurasjonsverdi.VERSION);
        if (versjon != null) {
            System.setProperty("app.launcher.version", versjon);
        }
    }

}
