package no.statkart.launcher.gradle.plugin;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiFunction;

class NoopPermissionSetter implements BiFunction<TarArchiveEntry, Path, IOException> {

    @Override
    public IOException apply(TarArchiveEntry tarArchiveEntry, Path path) {
        return null;
    }

}
