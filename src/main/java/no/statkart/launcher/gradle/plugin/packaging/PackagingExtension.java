package no.statkart.launcher.gradle.plugin.packaging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface PackagingExtension {

    void setName(String name);

    void setVersion(String version);

    File getIcon();

    Path execute(Path fromDir, Path toDir) throws IOException;

}
