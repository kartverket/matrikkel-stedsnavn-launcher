package no.statkart.launcher.client;

enum Konfigurasjonsverdi {
    TITLE("title"),
    VERSION("version"),
    INPUT_MESSAGE("input.message"),
    INPUT_SERVER("input.server"),
    INPUT_CREDENTIALS("input.credentials"),
    INPUT_HEAP("input.heap"),
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
