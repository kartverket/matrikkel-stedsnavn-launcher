package no.statkart.launcher.client;

import java.util.Arrays;
import java.util.Properties;

class LoginParametre {

    private String tjener;
    private String brukernavn;
    private char[] passord;
    private String heap = Konfigurasjon.get(Konfigurasjonsverdi.DEFAULT_HEAP);
    private long oppdatert = System.currentTimeMillis();

    LoginParametre() {
    }

    LoginParametre(Properties properties) {
        medTjener(properties.getProperty("tjener"));
        medBrukernavn(properties.getProperty("brukernavn"));
        medHeap(properties.getProperty("heap"));
        medOppdatert(Long.parseLong(properties.getProperty("oppdatert")));
    }

    Properties tilProperties() {
        Properties p = new Properties();
        p.setProperty("tjener", tjener);
        p.setProperty("brukernavn", brukernavn);
        p.setProperty("heap", heap);
        p.setProperty("oppdatert", Long.toString(oppdatert));
        return p;
    }

    LoginParametre medTjener(String tjener) {
        this.tjener = tjener;
        return this;
    }

    LoginParametre medBrukernavn(String brukernavn) {
        this.brukernavn = brukernavn;
        return this;
    }

    LoginParametre medPassord(char[] passord) {
        this.passord = passord == null ? null : Arrays.copyOf(passord, passord.length);
        return this;
    }

    LoginParametre medHeap(String heap) {
        this.heap = heap;
        return this;
    }

    LoginParametre medOppdatert(long oppdatert) {
        this.oppdatert = oppdatert;
        return this;
    }

    String getTjener() {
        return tjener;
    }

    String getBrukernavn() {
        return brukernavn;
    }

    char[] getPassord() {
        return passord;
    }

    String getHeap() {
        return heap;
    }

    long getOppdatert() {
        return oppdatert;
    }

}
