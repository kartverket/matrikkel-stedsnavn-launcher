package no.statkart.launcher.gradle.plugin;

import groovy.lang.Closure;
import no.statkart.launcher.gradle.plugin.packaging.PackagingExtension;
import no.statkart.launcher.gradle.plugin.packaging.SevenZExtension;
import no.statkart.launcher.gradle.plugin.packaging.TarGzExtension;
import no.statkart.launcher.gradle.plugin.packaging.ZipExtension;
import org.gradle.util.ConfigureUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public class ArtifactExtension {

    private final String arch;

    private PackagingExtension packagingExt;
    private SigningExtension signingExt;

    ArtifactExtension(String arch) {
        this.arch = arch;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void packaging(String packaging) {
        if ("7z".equals(packaging)) {
            packagingExt = new SevenZExtension(arch);
        } else if ("targz".equals(packaging)) {
            packagingExt = new TarGzExtension(arch);
        } else if ("zip".equals(packaging)) {
            packagingExt = new ZipExtension(arch);
        } else {
            throw new IllegalArgumentException("Ukjent innpakkingsmetode '" + packaging + "'");
        }
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void packagingConfig(Closure<?> c) {
        ConfigureUtil.configure(c, packagingExt);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void signing(Closure<?> c) {
        signingExt = new SigningExtension();
        ConfigureUtil.configure(c, signingExt);
    }

    public PackagingExtension getPackagingConfig() {
        return packagingExt;
    }

    void execute(Path fromDirPath, Path toDirPath, String name, String version) {
        try {
            Files.createDirectories(toDirPath);
            packagingExt.setName(name);
            packagingExt.setVersion(version);
            Path toFilePath = packagingExt.execute(fromDirPath, toDirPath);
            if (signingExt != null) {
                signingExt.execute(toFilePath);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
