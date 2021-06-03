package no.statkart.launcher.gradle.plugin;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ServerExtension {

    private final Project project;
    private final ConfigurableFileCollection classpath;
    private final ConfigurableFileCollection webinfLibs;
    private final List<FileCollection> libraries = new ArrayList<>();

    private File webinf;
    private File metainf;
    private File getdown;
    private String oldestAllowedClientVersion;

    public ServerExtension(Project project) {
        this.project = project;
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
    public void libraries(Object... libraries) {
        this.libraries.add(project.files().from(libraries));
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

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void oldestAllowedClientVersion(String oldestAllowedClientVersion) {
        this.oldestAllowedClientVersion = oldestAllowedClientVersion;
    }

    @InputFiles
    FileCollection getClasspath() {
        return classpath;
    }

    @InputFiles
    List<FileCollection> getLibraries() {
        return libraries;
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

    @Input
    String getOldestAllowedClientVersion() {
        return oldestAllowedClientVersion;
    }

}