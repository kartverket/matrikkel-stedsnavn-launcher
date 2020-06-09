package no.statkart.launcher.gradle.plugin.packaging;

import java.io.IOException;
import java.nio.file.Path;

public interface PackagingExtension {

    void setArch(String arch);

    void setName(String name);

    void setVersion(String version);

    Path execute(Path fromDir, Path toDir) throws IOException;

}
