package no.statkart.launcher.gradle.plugin.signing;

import java.nio.file.Path;

public interface SigningExtension {
    void execute(Path into) throws Exception;
}
