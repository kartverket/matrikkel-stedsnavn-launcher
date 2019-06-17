package no.statkart.launcher.gradle.plugin;

import groovy.lang.Closure;
import org.gradle.util.ConfigureUtil;

public class ArtifakterExtension {

    private final ArtifaktExtension windowsExt = new ArtifaktExtension();
    private final ArtifaktExtension linuxExt = new ArtifaktExtension();
    private final ArtifaktExtension osxExt = new ArtifaktExtension();

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public <T> void windows(Closure<T> c) {
        ConfigureUtil.configure(c, windowsExt);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public <T> void linux(Closure<T> c) {
        ConfigureUtil.configure(c, linuxExt);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public <T> void osx(Closure<T> c) {
        ConfigureUtil.configure(c, osxExt);
    }

    ArtifaktExtension getWindows() {
        return windowsExt;
    }

    ArtifaktExtension getLinux() {
        return linuxExt;
    }

    ArtifaktExtension getOSX() {
        return osxExt;
    }

}
