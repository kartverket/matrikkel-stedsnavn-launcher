package no.statkart.launcher.gradle.plugin;

import org.gradle.api.tasks.InputFile;

import java.io.File;

public class IconExtension {

    /**
     * Mappe med {@code packr-windows-x64.exe} fil for Windows med eget ikon.
     * Workaround for manglende støtte for icon i Windows --> https://github.com/libgdx/packr/issues/131
     * <p>
     * packr støtter dessverre ikke .ico-filer direkte.  For å fikse dette må vi endre ikonet på forhånd i exe-fila som
     * leveres med packr.  Dette gjøres f.eks. med ResourceHacker (http://www.angusj.com/resourcehacker/):
     * <pre>{@code
     *   C:\>ResourceHacker.exe -open packr-windows-x64.exe -save packr-windows-x64-kartverket.exe -action addskip -res program.ico -mask ICONGROUP,MAINICON,
     * }
     * </pre>
     * Her blir da "-kartverket.exe"-fila den nye packr-windows-x64.exe med nytt ikon. "program.ico" er ikonet vi laget over.
     */
    private File windows;

    /**
     * Referanse til .icns-fil. Benyttet av osx.
     */
    private File osx;

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void windows(File windows) {
        this.windows = windows;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void osx(File osx) {
        this.osx = osx;
    }

    @InputFile
    File getWindows() {
        return windows;
    }

    @InputFile
    File getOsx() {
        return osx;
    }

}
