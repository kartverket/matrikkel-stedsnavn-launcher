package no.statkart.launcher.gradle.plugin.packaging;

import java.io.IOException;
import java.nio.file.Path;

public interface PackagingExtension {

    void execute(Path fromDir, Path toFile) throws IOException;

}
