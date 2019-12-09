package no.statkart.launcher.gradle.plugin;

import org.gradle.process.JavaExecSpec;

import java.io.File;
import java.util.List;

public class PackrConfig {

    Jvm platform;
    String jdk;
    String executable;
    List<String> classpath;
    List<String> removePlatformLibs;
    String mainClass;
    File cacheJre;
    File outDir;
    File iconResource;
    boolean verbose;

    //    List<String> vmArgs;
    //    String minimizeJre;
    //    List<File> resources;
    //    File platformLibsOutDir;
    //    String bundleIdentifier;

    /**
     * For command line reference, see com.badlogicgames.packr.PackrCommandLine:
     * http://github.com/libgdx/packr/blob/master/src/main/java/com/badlogicgames/packr/PackrCommandLine.java
     * <p>
     * 'longName' tar to bindestreker mens 'shortName' tar en bindestrek foran argumentnavnet.
     * For mer informasjon se http://jewelcli.lexicalscope.com/usage.html
     */
    void decorateExecSpecs(JavaExecSpec execSpec) {
        if (platform == Jvm.OSX) {
            execSpec.args("--platform", "mac");
        } else if (platform == Jvm.LINUX) {
            execSpec.args("--platform", "linux64");
        } else {
            execSpec.args("--platform", "windows64");
        }
        execSpec.args("--jdk", jdk);
        execSpec.args("--executable", executable);
        execSpec.args("--classpath").args(classpath);
        if (removePlatformLibs != null) {
            execSpec.args("--removelibs").args(removePlatformLibs);
        }
        execSpec.args("--mainclass", mainClass);
        if (cacheJre != null) {
            execSpec.args("--cachejre", cacheJre);
        }
        execSpec.args("--output", outDir);
        if (iconResource != null) {
            execSpec.args("--icon", iconResource);
        }
        if (verbose) {
            execSpec.args("-v");
        }
    }

}
