package no.statkart.launcher.gradle.plugin;

import org.gradle.api.tasks.InputDirectory;

import java.io.File;

public class GetdownExtension {

    private File client;
    private File server;

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void client(File client) {
        this.client = client;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void server(File server) {
        this.server = server;
    }

    @InputDirectory
    File getClient() {
        return client;
    }

    @InputDirectory
    File getServer() {
        return server;
    }

}
