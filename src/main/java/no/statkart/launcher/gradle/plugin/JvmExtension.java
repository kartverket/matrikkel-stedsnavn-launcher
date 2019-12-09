package no.statkart.launcher.gradle.plugin;

import org.gradle.api.tasks.Input;

import java.util.List;

public class JvmExtension {

    private String urlLinux;
    private String urlOsx;
    private String urlWindows;
    private List<String> modules;
    private List<String> locales;

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void urlLinux(String urlLinux) {
        this.urlLinux = urlLinux;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void urlOsx(String urlOsx) {
        this.urlOsx = urlOsx;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void urlWindows(String urlWindows) {
        this.urlWindows = urlWindows;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void modules(List<String> modules) {
        this.modules = modules;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void locales(List<String> locales) {
        this.locales = locales;
    }

    String getUrl(Jvm jvm) {
        if (jvm == Jvm.LINUX) {
            return urlLinux;
        }
        if (jvm == Jvm.OSX) {
            return urlOsx;
        }
        return urlWindows;
    }

    @Input
    List<String> getModules() {
        return modules;
    }

    @Input
    List<String> getLocales() {
        return locales;
    }

}
