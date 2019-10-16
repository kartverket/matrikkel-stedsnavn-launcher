package no.statkart.launcher.gradle.plugin;

import groovy.lang.Closure;
import org.gradle.api.file.FileCollection;
import org.gradle.util.ConfigureUtil;

public class LauncherExtension {

    private FileCollection classpath;
    private String executable;
    private String webinf;
    private String metainf;
    private FileCollection webinfLibs;
    private String icons;

    private JvmExtension jvmExt;
    private GetdownExtension getdownExt;
    private ArtifakterExtension artifakterExt;

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void classpath(FileCollection classpath) {
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
    public void metainf(String metainf) {
        this.metainf = metainf;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void webinfLibs(FileCollection webinfLibs) {
        this.webinfLibs = webinfLibs;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void icons(String icons) {
        this.icons = icons;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void jvm(Closure c) {
        jvmExt = new JvmExtension();
        ConfigureUtil.configure(c, jvmExt);
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

    FileCollection getClasspath() {
        return classpath;
    }

    String getExecutable() {
        return executable;
    }

    String getWebinf() {
        return webinf;
    }

    String getMetainf() {
        return metainf;
    }

    FileCollection getWebinfLibs() {
        return webinfLibs;
    }

    String getIcons() {
        return icons;
    }

    JvmExtension getJvmUtvidelse() {
        return jvmExt;
    }

    GetdownExtension getGetdownUtvidelse() {
        return getdownExt;
    }

    ArtifakterExtension getArtifakterUtvidelse() {
        return artifakterExt;
    }

}
