package no.statkart.launcher.gradle.plugin;

public class GetdownExtension {

    private String client;
    private String server;

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void client(String client) {
        this.client = client;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void server(String server) {
        this.server = server;
    }

    String getClient() {
        return client;
    }

    String getServer() {
        return server;
    }

}
