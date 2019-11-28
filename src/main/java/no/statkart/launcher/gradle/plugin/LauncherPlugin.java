package no.statkart.launcher.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class LauncherPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // Navn på blokken som konfigurerer denne pluginen i gradle-fila
        project.getExtensions().create("launcher", LauncherExtension.class, project);
        // Navn på tasken som kjører denne pluginen
        project.getTasks().create("launcher", LauncherTask.class);
    }

}
