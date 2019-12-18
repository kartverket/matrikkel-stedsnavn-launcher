package no.statkart.launcher.client;

enum Konfigurasjonsverdi {
    TITLE("title"),
    VERSION("version"),
    TRY_CRENDENTIALS_USING_PATH("tryCredentialsUsingPath"),
    DEFAULT_SERVER("default.server"),
    DEFAULT_HEAP("default.heap"),
    WORK_WINDOWS("work.windows"),
    WORK_LINUX("work.linux"),
    WORK_OSX("work.osx");

    private final String key;

    Konfigurasjonsverdi(String key) {
        this.key = key;
    }

    String getKey() {
        return key;
    }

}
