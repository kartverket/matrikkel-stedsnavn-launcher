package no.statkart.launcher.gradle.plugin;

import groovy.lang.Closure;
import org.gradle.api.artifacts.Configuration;
import org.gradle.util.ConfigureUtil;

public class LauncherExtension {

    private Configuration classpath;
    private String executable;
    private String webinf;
    private String icons;

    private GetdownExtension getdownExt;
    private ArtifakterExtension artifakterExt;

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void classpath(Configuration classpath) {
        this.classpath = classpath;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void executable(String executable) {
        this.executable = executable;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void webinf(String webinf) {
        this.webinf = webinf;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void icons(String icons) {
        this.icons = icons;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void getdown(Closure c) {
        getdownExt = new GetdownExtension();
        ConfigureUtil.configure(c, getdownExt);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void artifakter(Closure c) {
        artifakterExt = new ArtifakterExtension();
        ConfigureUtil.configure(c, artifakterExt);
    }

    Configuration getClasspath() {
        return classpath;
    }

    String getExecutable() {
        return executable;
    }

    String getWebinf() {
        return webinf;
    }

    String getIcons() {
        return icons;
    }

    GetdownExtension getGetdownUtvidelse() {
        return getdownExt;
    }

    ArtifakterExtension getArtifakterUtvidelse() {
        return artifakterExt;
    }

}
