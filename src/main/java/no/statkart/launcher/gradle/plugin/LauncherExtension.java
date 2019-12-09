package no.statkart.launcher.gradle.plugin;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.util.ConfigureUtil;

import java.io.File;

public class LauncherExtension {

    private final ConfigurableFileCollection classpath;
    private final ConfigurableFileCollection webinfLibs;

    private File webinf;
    private File metainf;

    private String version;
    private String executable;

    private JvmExtension jvmExt;
    private GetdownExtension getdownExt;
    private ArtifactsExtension artifactsExtension;
    private IconExtension iconExt;

    public LauncherExtension(Project project) {
        classpath = project.files();
        webinfLibs = project.files();
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void classpath(Object... classpath) {
        this.classpath.from(classpath);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void version(String version) {
        this.version = version;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void executable(String executable) {
        this.executable = executable;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void webinf(File webinf) {
        this.webinf = webinf;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void metainf(File metainf) {
        this.metainf = metainf;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void webinfLibs(Object... webinfLibs) {
        this.webinfLibs.from(webinfLibs);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void jvm(Closure<?> c) {
        jvmExt = new JvmExtension();
        ConfigureUtil.configure(c, jvmExt);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void getdown(Closure<?> c) {
        getdownExt = new GetdownExtension();
        ConfigureUtil.configure(c, getdownExt);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void artifacts(Closure<?> c) {
        artifactsExtension = new ArtifactsExtension();
        ConfigureUtil.configure(c, artifactsExtension);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void icon(Closure<?> c) {
        iconExt = new IconExtension();
        ConfigureUtil.configure(c, iconExt);
    }

    @InputFiles
    FileCollection getClasspath() {
        return classpath;
    }

    @Input
    String getVersion() {
        return version;
    }

    @Input
    String getExecutable() {
        return executable;
    }

    @InputFile
    File getWebinf() {
        return webinf;
    }

    @InputFile
    File getMetainf() {
        return metainf;
    }

    @InputFiles
    FileCollection getWebinfLibs() {
        return webinfLibs;
    }

    @Nested
    IconExtension getIconUtvidelse() {
        return iconExt;
    }

    @Nested
    JvmExtension getJvmUtvidelse() {
        return jvmExt;
    }

    @Nested
    GetdownExtension getGetdownUtvidelse() {
        return getdownExt;
    }

    @Nested
    ArtifactsExtension getArtifactsUtvidelse() {
        return artifactsExtension;
    }

}
