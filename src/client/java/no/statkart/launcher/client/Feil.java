package no.statkart.launcher.client;

import javax.net.ssl.SSLException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

class Feil {

    private final IOException exception;
    private final Credentials credentials;

    Feil(IOException exception, Credentials credentials) {
        this.exception = exception;
        this.credentials = credentials;
    }

    boolean erTjenerfeil() {
        return exception instanceof MalformedURLException
                || exception instanceof FileNotFoundException;
    }

    boolean erBrukerPassordFeil() {
        return !erTjenerfeil();
    }

    Credentials getCredentials() {
        return credentials;
    }

    String tilFeilmelding() {
        if (exception instanceof MalformedURLException) {
            return "Feil inntastet tjeneradresse. Skal være på formatet <protokoll>://<tjener>[:<port>]/";
        }
        if (harSuffiks(credentials.getServer())) {
            return "Feil inntastet tjeneradresse. Skal bare ha tjenernavn.";
        }
        if (erUgyldigTjenerformat(credentials.getServer())) {
            return "Feil inntastet tjeneradresse. Skal være på formatet <protokoll>://<tjener>[:<port>]/";
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
        return "Ugyldig brukernavn og/eller passord";
    }

    private boolean harSuffiks(String tjener) {
       try {
           return !"".equals(new URL(tjener).getPath().replaceFirst("/", ""));
       } catch (MalformedURLException e) {
           return true;
       }
    }

    private boolean erUgyldigTjenerformat(String tjener) {
        return tjener == null || !tjener.matches("^[\\w]+://[^/]+/?$");
    }

}
