package no.statkart.launcher.client;

import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

class Parametre {

    private String tjener;
    private String brukernavn;
    private char[] passord;
    private String heap;
    private long oppdatert;

    Parametre() {
    }

    Parametre(Properties properties) {
        medTjener(properties.getProperty("tjener"));
        medBrukernavn(properties.getProperty("brukernavn"));
        medHeap(properties.getProperty("heap"));
        oppdatert = Long.parseLong(properties.getProperty("oppdatert"));
    }

    Properties tilProperties() {
        Properties p = new Properties();
        if (tjener != null) {
            p.setProperty("tjener", tjener);
        }
        if (brukernavn != null) {
            p.setProperty("brukernavn", brukernavn);
        }
        if (heap != null) {
            p.setProperty("heap", heap);
        }
        p.setProperty("oppdatert", Long.toString(oppdatert));
        return p;
    }

    Parametre medTjener(String tjener) {
        this.tjener = tjener;
        setOppdatert();
        return this;
    }

    Parametre medBrukernavn(String brukernavn) {
        this.brukernavn = brukernavn;
        setOppdatert();
        return this;
    }

    Parametre medPassord(char[] passord) {
        this.passord = passord == null ? null : Arrays.copyOf(passord, passord.length);
        setOppdatert();
        return this;
    }

    Parametre medHeap(String heap) {
        this.heap = heap;
        setOppdatert();
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

    private void setOppdatert() {
        this.oppdatert = System.currentTimeMillis();
    }

    void kontroller() throws Exception {
        kontrollerHeap();
        kontrollerTjener();
    }

    @SuppressWarnings("all")
    void kontrollerHeap() {
        Integer.parseInt(heap);
    }

    void kontrollerTjener() throws Exception {
        new URL(tjener);
    }

    @Override
    public String toString() {
        return tilProperties().toString();
    }

    public Feil tilFeil() {
        try {
            kontroller();
            return null;
        } catch (Exception e) {
            return new Feil(e, this);
        }
    }

}
