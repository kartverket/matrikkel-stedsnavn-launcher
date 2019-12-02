package no.statkart.launcher.gradle.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.JavaExecSpec;
import org.gradle.util.GFileUtils;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class LauncherTask extends DefaultTask {
    private final LauncherExtension utvidelse;

    public LauncherTask() {
        utvidelse = getProject().getExtensions().findByType(LauncherExtension.class);
        dependsOn(utvidelse.getClasspath()); // build jars from includeBuilds
        dependsOn(utvidelse.getWebinfLibs()); // build jars from includeBuilds
    }

    @TaskAction
    @SuppressWarnings("unused")
    public void execute() throws IOException {
        lagKlienter();
        lagKlientinstallereForNedlasting();
        opprettTjenerWebapp();
        opprettWar();
    }

    private void opprettWar() throws IOException {
        Path source = toAbsolutePath("build/launcher/war");
        Path destination = toAbsolutePath("build/launcher/launcher.war");
        Files.createDirectories(destination.getParent());
        try (FileOutputStream fileOutputStream = new FileOutputStream(destination.toFile());
             JarOutputStream jos = new JarOutputStream(fileOutputStream)) {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path path = source.relativize(file);
                    ZipEntry zipEntry = new ZipEntry(path.toString().replace('\\', '/'));
                    jos.putNextEntry(zipEntry);
                    Files.copy(file, jos);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void opprettTjenerWebapp() throws IOException {
        opprettPathingJar(utvidelse.getClasspath(), "build/launcher/war/vault/client-dependencies.jar");
        copy(utvidelse.getClasspath(), "build/launcher/war/vault");
        copy(utvidelse.getWebinf(), "build/launcher/war/WEB-INF");
        copy(utvidelse.getMetainf(), "build/launcher/war/META-INF");
        String version = (String) getProject().getProperties().get("version");
        if (version != null && !version.isEmpty()) {
            replace("build/launcher/war/META-INF/MANIFEST.MF", "@@version@@", version);
        }
        copyResources("lib/server", "build/launcher/war/WEB-INF/lib");
        copy(utvidelse.getWebinfLibs(), "build/launcher/war/WEB-INF/lib");
        copy(utvidelse.getGetdownUtvidelse().getServer(), "build/launcher/war/vault");
        replace("build/launcher/war/vault/getdown.txt", "@@code@@", asCode(utvidelse.getClasspath()));
    }

    private String asCode(FileCollection files) {
        StringBuilder sb = new StringBuilder();
        sb.append("code = client-dependencies.jar\n");
        files.forEach(f ->
                sb.append("resource = ").append(f.getName()).append('\n')
        );
        return sb.toString();
    }

    private void replace(String file, String token, String replacement) throws IOException {
        Path filePath = toAbsolutePath(file);
        if (Files.exists(filePath)) {
            Charset charset = StandardCharsets.UTF_8;
            String content = Files.readString(filePath, charset);
            content = content.replace(token, replacement);
            Files.writeString(filePath, content, charset);
        }
    }

    private void opprettPathingJar(FileCollection files, String toFile) throws IOException {
        StringBuilder sb = new StringBuilder();
        files.forEach(f ->
                sb.append(f.getName()).append(' ')
        );
        Manifest manifest = new Manifest();
        Attributes attr = manifest.getMainAttributes();
        attr.putValue("Manifest-Version", "1.0");
        attr.putValue("Class-Path", sb.toString());

        Path toFilePath = toAbsolutePath(toFile);
        Files.createDirectories(toFilePath.getParent());
        Files.deleteIfExists(toFilePath);
        Files.createFile(toFilePath);
        try (FileOutputStream fos = new FileOutputStream(toFilePath.toFile());
             JarOutputStream jos = new JarOutputStream(fos);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ZipEntry zipEntry = new ZipEntry("META-INF/MANIFEST.MF");
            jos.putNextEntry(zipEntry);
            manifest.write(baos);
            jos.write(baos.toByteArray());
        }
    }

    private void lagKlientinstallereForNedlasting() {
        utvidelse.getArtifakterUtvidelse().getWindows().execute(
                toAbsolutePath("build/launcher/packr/windows"),
                toAbsolutePath("build/launcher/war/download")
        );
        utvidelse.getArtifakterUtvidelse().getLinux().execute(
                toAbsolutePath("build/launcher/packr/linux"),
                toAbsolutePath("build/launcher/war/download")
        );
        utvidelse.getArtifakterUtvidelse().getOSX().execute(
                toAbsolutePath("build/launcher/packr/osx"),
                toAbsolutePath("build/launcher/war/download")
        );
    }

    private void lagKlienter() throws IOException {
        copyResources("lib/client", "build/launcher/lib/");
        copyResources("jdk", "build/launcher/jdk/");
        packr(Jvm.WINDOWS);
        packr(Jvm.LINUX);
        packr(Jvm.OSX);
    }

    private void packr(Jvm jvm) throws IOException {
        jvm.download(utvidelse.getJvmUtvidelse().getUrl(jvm), toAbsolutePath("build/launcher/jdk"));
        jvm.unpack(toAbsolutePath("build/launcher/jdk"));
        jvm.jlink(
                toAbsolutePath("build/launcher/jdk"),
                utvidelse.getJvmUtvidelse().getModules(),
                utvidelse.getJvmUtvidelse().getLocales()
        );
        try {
            PackrConfig config = new PackrConfig();
            config.platform = jvm;
            config.executable = utvidelse.getExecutable();
            config.mainClass = "no.statkart.launcher.client.Wrapper";
            config.cacheJre = toAbsolutePath("build/launcher/jdk/" + jvm.getAlias() + "-min").toFile();
            config.outDir = toAbsolutePath("build/launcher/packr/" + jvm.getAlias()).toFile();
            try (Stream<Path> paths = Files.list(toAbsolutePath("build/launcher/lib"))) {
                config.classpath = paths.map(Path::toString).collect(Collectors.toList());
            }
            String icons = utvidelse.getIcons();
            if (icons != null) {
                config.iconResource = toAbsolutePath(utvidelse.getIcons()).toFile();
            }
            config.jdk = "x";
            exec(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        copyResources("jdk/fonts", "build/launcher/packr/" + topDirectory(jvm) + "/jre/lib/fonts");
        copy(utvidelse.getGetdownUtvidelse().getClient(), "build/launcher/packr/" + topDirectory(jvm) + "/work");
    }

    private String topDirectory(Jvm jvm) {
        if (jvm == Jvm.OSX) {
            return jvm.getAlias() + "/Contents/Resources";
        }
        return jvm.getAlias();
    }

    private void copy(FileCollection files, String toDir) {
        Path toDirPath = toAbsolutePath(toDir);
        files.forEach(f -> {
            Path p = f.toPath();
            Path target = toDirPath.resolve(p.getFileName());
            try {
                Files.createDirectories(target.getParent());
                Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void copy(String fromDir, String toDir) throws IOException {
        Path fromDirPath = toAbsolutePath(fromDir);
        Path toDirPath = toAbsolutePath(toDir);
        try (Stream<Path> stream = Files.walk(fromDirPath)) {
            stream.forEachOrdered(source -> {
                if (!Files.isDirectory(source)) {
                    try {
                        Path target = toDirPath.resolve(fromDirPath.relativize(source));
                        Files.createDirectories(target.getParent());
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    private List<String> getResources(String resourceDir) throws IOException {
        URL resourceURL = getClass().getClassLoader().getResource(resourceDir);
        if (resourceURL == null) {
            throw new IllegalStateException("Fant ikke resourceDir=" + resourceDir);
        }
        List<String> resultat = new ArrayList<>();
        JarURLConnection urlcon = ((JarURLConnection) resourceURL.openConnection());
        try (JarFile jar = urlcon.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (jarEntry.isDirectory()) {
                    continue;
                }
                String name = jarEntry.getName();
                if (name.startsWith(resourceDir)) {
                    resultat.add(name);
                }
            }
        }
        return resultat;
    }

    private void copyResources(String resourceDir, String toDir) throws IOException {
        for (String resourceFile : getResources(resourceDir)) {
            copyResource(resourceFile, toDir);
        }
    }

    private void copyResource(String resourceFile, String toDir) throws IOException {
        Path resourcePath = Paths.get(resourceFile);
        Path target = toAbsolutePath(toDir).resolve(resourcePath.getFileName());
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceFile)) {
            if (is != null) {
                Files.createDirectories(target.getParent());
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    @Nested
    private LauncherExtension getUtvidelse() {
        return utvidelse;
    }

    private Path toAbsolutePath(String relativePath) {
        Path root = getProject().getProjectDir().toPath();
        return root.resolve(relativePath);
    }


    private static class PackrConfig {
        Jvm platform;
        String jdk;
        String executable;
        List<String> classpath;
        List<String> removePlatformLibs;
        String mainClass;
        List<String> vmArgs;
        String minimizeJre;
        File cacheJre;
        List<File> resources;
        File outDir;
        File platformLibsOutDir;
        File iconResource;
        String bundleIdentifier;

        boolean verbose;

        /**
         * For command line reference, see com.badlogicgames.packr.PackrCommandLine:
         * http://github.com/libgdx/packr/blob/master/src/main/java/com/badlogicgames/packr/PackrCommandLine.java
         *
         * 'longName' tar to bindestreker mens 'shortName' tar en bindestrek foran argumentnavnet.
         * For mer informasjon se http://jewelcli.lexicalscope.com/usage.html
         */
        private void decorateExecSpecs(JavaExecSpec execSpec) {
            if (platform == Jvm.OSX) {
                execSpec.args("--platform", "mac");
            } else  if (platform == Jvm.LINUX) {
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

            execSpec.args("--icon", iconResource);

            if (verbose) {
                execSpec.args("-v");
            }
        }
    }

    @OutputFile
    private Provider<File> getPackrJar() {
        return getProject().provider(() -> {
            URL url = LauncherTask.class.getResource("/lib/packr/packr-2.0-SNAPSHOT.jar");
            File file = getProject().file("build/packr/packr.jar");
            GFileUtils.copyURLToFile(url, file);
            return file;
        });
    }

    private void exec(PackrConfig config) {
        getProject().javaexec(execSpecs -> {
            config.decorateExecSpecs(execSpecs);
            execSpecs.setMain("com.badlogicgames.packr.Packr");
            //Gjeldende workaround er å legge en hacket versjon av packr*.exe
            //NB: Denne må komme først på classpath!
            if (utvidelse.getWindowsIcons() != null) {
                execSpecs.classpath(utvidelse.getWindowsIcons());
            }
            execSpecs.classpath(getPackrJar());

        }).assertNormalExitValue();
    }


}
