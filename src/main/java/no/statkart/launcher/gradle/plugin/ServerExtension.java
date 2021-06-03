package no.statkart.launcher.gradle.plugin;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;

import java.io.File;

public class ServerExtension {

    private final ConfigurableFileCollection classpath;
    private final ConfigurableFileCollection webinfLibs;

    private File webinf;
    private File metainf;
    private File getdown;

    public ServerExtension(Project project) {
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
    public void webinfLibs(Object... webinfLibs) {
        this.webinfLibs.from(webinfLibs);
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
    public void getdown(File getdown) {
        this.getdown = getdown;
    }

    @InputFiles
    FileCollection getClasspath() {
        return classpath;
    }

    @InputFiles
    FileCollection getWebinfLibs() {
        return webinfLibs;
    }

    @InputDirectory
    File getWebinf() {
        return webinf;
    }

    @InputDirectory
    File getMetainf() {
        return metainf;
    }

    @InputDirectory
    File getGetdown() {
        return getdown;
    }

}
