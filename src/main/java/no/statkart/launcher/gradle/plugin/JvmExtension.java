package no.statkart.launcher.gradle.plugin;

import java.util.List;

public class JvmExtension {

    private List<String> modules;
    private List<String> locales;

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

    List<String> getModules() {
        return modules;
    }

    List<String> getLocales() {
        return locales;
    }

}
