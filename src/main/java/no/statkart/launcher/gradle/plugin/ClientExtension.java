package no.statkart.launcher.gradle.plugin;

import groovy.lang.Closure;
import no.statkart.launcher.gradle.plugin.packaging.PackagingExtension;
import no.statkart.launcher.gradle.plugin.packaging.SevenZExtension;
import no.statkart.launcher.gradle.plugin.packaging.TarGzExtension;
import no.statkart.launcher.gradle.plugin.packaging.ZipExtension;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.util.ConfigureUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientExtension {

    private final String name;

    private String arch;
    private String executable;
    private File icon;
    private File getdown;
    private PackagingExtension packagingExt;
    private SigningExtension signingExt;

    public ClientExtension(String name) {
        this.name = name;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void arch(String arch) {
        this.arch = arch;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void executable(String executable) {
        this.executable = executable;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void icon(File icon) {
        this.icon = icon;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void getdown(File getdown) {
        this.getdown = getdown;
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
    public void packagingConfig(Closure<?> c) {
        ConfigureUtil.configure(c, packagingExt);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void signing(Closure<?> c) {
        signingExt = new SigningExtension();
        ConfigureUtil.configure(c, signingExt);
    }

    public String getName() {
        return name;
    }

    @Input
    public String getArch() {
        return arch;
    }

    @Input
    public String getExecutable() {
        return executable;
    }

    @InputFile
    public File getIcon() {
        return icon;
    }

    @InputFile
    public File getGetdown() {
        return getdown;
    }

    String execute(Path fromDirPath, Path toDirPath, String version) {
        try {
            Files.createDirectories(toDirPath);
            packagingExt.setArch(arch);
            packagingExt.setName(name);
            packagingExt.setVersion(version);
            Path toFilePath = packagingExt.execute(fromDirPath, toDirPath);
            if (signingExt != null) {
                signingExt.execute(toFilePath);
            }
            return toFilePath.getFileName().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
