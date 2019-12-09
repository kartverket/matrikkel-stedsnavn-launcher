package no.statkart.launcher.gradle.plugin;

import groovy.lang.Closure;
import org.gradle.api.tasks.Nested;
import org.gradle.util.ConfigureUtil;

public class ArtifactsExtension {

    private final ArtifactExtension windowsExt = new ArtifactExtension("windows");
    private final ArtifactExtension linuxExt = new ArtifactExtension("linux");
    private final ArtifactExtension osxExt = new ArtifactExtension("osx");

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

    @Nested
    ArtifactExtension getWindows() {
        return windowsExt;
    }

    @Nested
    ArtifactExtension getLinux() {
        return linuxExt;
    }

    @Nested
    ArtifactExtension getOsx() {
        return osxExt;
    }

}
