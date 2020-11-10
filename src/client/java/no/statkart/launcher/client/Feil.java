package no.statkart.launcher.client;

import javax.net.ssl.SSLException;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

class Feil {

    private final Exception exception;
    private final Parametre inputParametre;
    private final boolean redFlag;

    Feil(Exception exception, Parametre inputParametre) {
        this(exception, inputParametre, true);
    }

    Feil(Exception exception, Parametre inputParametre, boolean redFlag) {
        this.inputParametre = inputParametre;
        this.exception = exception;
        this.redFlag = redFlag;
    }

    boolean erTjenerfeil() {
        return exception instanceof MalformedURLException
                || exception instanceof FileNotFoundException
                || exception instanceof SSLException
                || exception instanceof ConnectException
                || exception instanceof UnknownHostException
                || harTjenersuffiks()
                || erUgyldigTjenerformat();
    }

    boolean erHeapFeil() {
        return exception instanceof NumberFormatException;
    }

    boolean erBrukerPassordFeil() {
        return !erTjenerfeil() && !erHeapFeil();
    }

    Parametre getInputParametre() {
        return inputParametre;
    }

    boolean erRedFlag() {
        return redFlag;
    }

    String tilFeilmelding() {
        if (exception instanceof MalformedURLException) {
            return "Feil inntastet tjeneradresse. Skal være på formatet <protokoll>://<tjener>[:<port>]";
        }
        if (harTjenersuffiks()) {
            return "Feil inntastet tjeneradresse. Skal bare ha tjenernavn.";
        }
        if (erUgyldigTjenerformat()) {
            return "Feil inntastet tjeneradresse. Skal være på formatet <protokoll>://<tjener>[:<port>]";
        }
        if (exception instanceof FileNotFoundException) {
            return "Tjenerfeil. Vennligst kontroller om dette er en gyldig tjener.";
        }
        if (exception instanceof SSLException) {
            return "Sertifikatfeil. Vennligst kontroller nettverksoppsettet.";
        }
        if (exception instanceof ConnectException
                || exception instanceof UnknownHostException) {
            return "Ikke kontakt med tjener, sjekk nettilkobling";
        }
        if (exception instanceof NumberFormatException) {
            return "Minnestørrelse må skrives inn som et heltall";
        }
        return "Gyldig brukernavn og passord er påkrevd";
    }

    private boolean harTjenersuffiks() {
        try {
            String tjener = inputParametre.getTjener();
            return !"".equals(new URL(tjener).getPath().replaceFirst("/", ""));
        } catch (MalformedURLException e) {
            return true;
        }
    }

    private boolean erUgyldigTjenerformat() {
        String tjener = inputParametre.getTjener();
        return tjener == null || !tjener.matches("^[\\w]+://[^/]+/?$");
    }

}