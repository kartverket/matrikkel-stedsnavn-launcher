package no.statkart.launcher.gradle.plugin;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.util.ConfigureUtil;

import javax.annotation.Nullable;

public class LauncherExtension {

    private final ConfigurableFileCollection classpath;
    private String executable;
    private String webinf;
    private String metainf;
    private final ConfigurableFileCollection webinfLibs;
    /**
     * Icon - kun for OSX
     */
    private String icons;
    /**
     * Mappe med {@code packr-windows-x64.exe} fil for Windows med eget ikon.
     * Workaround for manglende støtte for icon i Windows --> https://github.com/libgdx/packr/issues/131
     *
     * packr støtter dessverre ikke .ico-filer direkte.  For å fikse dette må vi endre ikonet på forhånd i exe-fila som
     * leveres med packr.  Dette gjøres f.eks. med ResourceHacker (http://www.angusj.com/resourcehacker/):
     * <pre>{@code
     *   C:\>ResourceHacker.exe -open packr-windows-x64.exe -save packr-windows-x64-kartverket.exe -action addskip -res program.ico -mask ICONGROUP,MAINICON,
     * }
     * </pre>
     * Her blir da "-kartverket.exe"-fila den nye packr-windows-x64.exe med nytt ikon. "program.ico" er ikonet vi laget over.
     */
    @Nullable
    private Object windowsIcons;
    private JvmExtension jvmExt;
    private GetdownExtension getdownExt;
    private ArtifakterExtension artifakterExt;

    public LauncherExtension(Project project) {
        classpath = project.files();
        webinfLibs = project.files();
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void classpath(Object... classpath) {
        this.classpath.from(classpath);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void executable(String executable) {
        this.executable = executable;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void webinf(String webinf) {
        this.webinf = webinf;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void metainf(String metainf) {
        this.metainf = metainf;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void webinfLibs(Object... webinfLibs) {
        this.webinfLibs.from(webinfLibs);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void icons(String icons) {
        this.icons = icons;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void windowsIcons(Object path) {
        this.windowsIcons = path;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void jvm(Closure c) {
        jvmExt = new JvmExtension();
        ConfigureUtil.configure(c, jvmExt);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void getdown(Closure c) {
        getdownExt = new GetdownExtension();
        ConfigureUtil.configure(c, getdownExt);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void artifakter(Closure c) {
        artifakterExt = new ArtifakterExtension();
        ConfigureUtil.configure(c, artifakterExt);
    }

    FileCollection getClasspath() {
        return classpath;
    }

    String getExecutable() {
        return executable;
    }

    String getWebinf() {
        return webinf;
    }

    String getMetainf() {
        return metainf;
    }

    FileCollection getWebinfLibs() {
        return webinfLibs;
    }

    String getIcons() {
        return icons;
    }

    Object getWindowsIcons() {
        return windowsIcons;
    }

    JvmExtension getJvmUtvidelse() {
        return jvmExt;
    }

    GetdownExtension getGetdownUtvidelse() {
        return getdownExt;
    }

    ArtifakterExtension getArtifakterUtvidelse() {
        return artifakterExt;
    }

}
