package no.statkart.launcher.gradle.plugin;

import org.gradle.api.tasks.Input;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JvmExtension {

    private String urlLinux;
    private String urlOsx;
    private String urlWindows;
    private String jmodsUrlLinux;
    private String jmodsUrlOsx;
    private String jmodsUrlWindows;
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

    /**
     * URL til et separat "jmods"-artefakt. Kreves kun for Temurin/OpenJDK 25 og nyere,
     * der jmods ikke lenger er bundlet inni hoved-JDK-arkivet. For eldre JDK-versjoner
     * (17/21 osv.) trengs denne ikke settes, siden jmods da allerede finnes inni JDK-arkivet.
     */
    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void jmodsUrlLinux(String jmodsUrlLinux) {
        this.jmodsUrlLinux = jmodsUrlLinux;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void jmodsUrlOsx(String jmodsUrlOsx) {
        this.jmodsUrlOsx = jmodsUrlOsx;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void jmodsUrlWindows(String jmodsUrlWindows) {
        this.jmodsUrlWindows = jmodsUrlWindows;
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

    String getJmodsUrl(Jvm jvm) {
        if (jvm == Jvm.LINUX) {
            return jmodsUrlLinux;
        }
        if (jvm == Jvm.OSX) {
            return jmodsUrlOsx;
        }
        return jmodsUrlWindows;
    }

    @Input
    List<Jvm> getConfiguredJvms() {
        return Stream.of(Jvm.values())
                .filter(jvm -> getUrl(jvm) != null)
                .collect(Collectors.toList());
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
