package no.statkart.launcher.gradle.plugin;

import groovy.lang.Closure;
import no.statkart.launcher.gradle.plugin.packaging.PackagingExtension;
import no.statkart.launcher.gradle.plugin.packaging.SevenZExtension;
import no.statkart.launcher.gradle.plugin.packaging.TarGzExtension;
import no.statkart.launcher.gradle.plugin.packaging.ZipExtension;
import org.gradle.util.ConfigureUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public class ArtifaktExtension {

    private String output;
    private PackagingExtension packagingExt;
    private SigningExtension signingExt;

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void output(String output) {
        this.output = output;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void packaging(String packaging) {
        if ("7z".equals(packaging)) {
            packagingExt = new SevenZExtension();
        } else if ("targz".equals(packaging)) {
            packagingExt = new TarGzExtension();
        } else if ("zip".equals(packaging)) {
            packagingExt = new ZipExtension();
        } else {
            throw new IllegalArgumentException("Ukjent innpakkingsmetode '" + packaging + "'");
        }
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void packagingConfig(Closure c) {
        ConfigureUtil.configure(c, packagingExt);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void signing(Closure c) {
        signingExt = new SigningExtension();
        ConfigureUtil.configure(c, signingExt);
    }

    void execute(Path fromDirPath, Path toDirPath) {
        try {
            Files.createDirectories(toDirPath);
            Path toFilePath = toDirPath.resolve(output);
            packagingExt.execute(fromDirPath, toFilePath);
            if (signingExt != null) {
                signingExt.execute(toFilePath);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
