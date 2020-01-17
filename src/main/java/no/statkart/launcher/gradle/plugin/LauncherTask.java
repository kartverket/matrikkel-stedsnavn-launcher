package no.statkart.launcher.gradle.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GFileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class LauncherTask extends DefaultTask {

    private final LauncherExtension utvidelse;

    public LauncherTask() {
        utvidelse = getProject().getExtensions().findByType(LauncherExtension.class);
        if (utvidelse == null) {
            throw new IllegalStateException("Launcher not configured");
        }
        dependsOn(utvidelse.getClasspath()); // build jars from includeBuilds
        dependsOn(utvidelse.getWebinfLibs()); // build jars from includeBuilds
    }

    @TaskAction
    @SuppressWarnings("unused")
    public void execute() throws IOException {
        lagKlienter();
        Map<String, String> artifacts = lagKlientinstallereForNedlasting();
        opprettTjenerWebapp(artifacts);
        opprettWar();
    }

    @OutputFile
    public Path getWarDestination() {
        return toAbsolutePath("build/launcher/launcher.war");
    }

    private void opprettWar() throws IOException {
        Path source = toAbsolutePath("build/launcher/war");
        Path destination = getWarDestination();
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

    private void opprettTjenerWebapp(Map<String, String> artifacts) throws IOException {
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
        for (Map.Entry<String, String> e : artifacts.entrySet()) {
            replace("build/launcher/war/WEB-INF/web.xml", "@@" + e.getKey() + "@@", e.getValue());
        }
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

    private Map<String, String> lagKlientinstallereForNedlasting() {
        Map<String, String> artifacts = new HashMap<>();
        artifacts.put("windows",
                utvidelse.getArtifactsUtvidelse().getWindows().execute(
                        toAbsolutePath("build/launcher/packr/windows"),
                        toAbsolutePath("build/launcher/war/download"),
                        utvidelse.getExecutable(),
                        utvidelse.getVersion()
                ));
        artifacts.put("linux",
                utvidelse.getArtifactsUtvidelse().getLinux().execute(
                        toAbsolutePath("build/launcher/packr/linux"),
                        toAbsolutePath("build/launcher/war/download"),
                        utvidelse.getExecutable(),
                        utvidelse.getVersion()
                ));
        artifacts.put("osx",
                utvidelse.getArtifactsUtvidelse().getOsx().execute(
                        toAbsolutePath("build/launcher/packr/osx"),
                        toAbsolutePath("build/launcher/war/download"),
                        utvidelse.getExecutable(),
                        utvidelse.getVersion()
                ));
        return artifacts;
    }

    private void lagKlienter() throws IOException {
        copyResources("lib/client", "build/launcher/lib/");
        copyResources("jdk", "build/launcher/jdk/");
        Jvm[] jvms = {Jvm.WINDOWS, Jvm.LINUX, Jvm.OSX};
        for (Jvm jvm: jvms) {
            jvm.download(utvidelse.getJvmUtvidelse().getUrl(jvm), toAbsolutePath("build/launcher/jdk"));
            jvm.unpack(toAbsolutePath("build/launcher/jdk"));
        }
        for (Jvm jvm: jvms) {
            packr(jvm);
        }
    }

    private void packr(Jvm jvm) throws IOException {
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
            if (jvm == Jvm.OSX) {
                File icns = utvidelse.getArtifactsUtvidelse().getOsx().getIcon();
                if (icns != null) {
                    config.iconResource = icns;
                }
            }
            config.jdk = "x";
            exec(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        copyResources("jdk/fonts", "build/launcher/packr/" + topDirectory(jvm) + "/jre/lib/fonts");
        copy(utvidelse.getGetdownUtvidelse().getClient(), "build/launcher/packr/" + topDirectory(jvm) + "/work");
        replace("build/launcher/packr/" + topDirectory(jvm) + "/work/client.properties", "@@version@@", utvidelse.getVersion());
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

    private void copy(File fromDir, String toDir) throws IOException {
        if (!fromDir.isDirectory()) {
            throw new IOException(fromDir + " must be a directory");
        }
        copy(fromDir.getAbsolutePath(), toDir);
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

    private void append(String content, String toFile) throws IOException {
        Path toFilePath = toAbsolutePath(toFile);
        Charset charset = StandardCharsets.UTF_8;
        Files.write(toFilePath, content.getBytes(charset), StandardOpenOption.APPEND);
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

    private Path toAbsolutePath(String relativePath) {
        Path root = getProject().getProjectDir().toPath();
        return root.resolve(relativePath);
    }

    @OutputFile
    public Provider<File> getPackrJar() {
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
            // Gjeldende workaround er å legge en hacket versjon av packr*.exe
            // NB: Denne må komme først på classpath!
            File windows = utvidelse.getArtifactsUtvidelse().getWindows().getIcon();
            if (windows != null) {
                execSpecs.classpath(windows);
            }
            execSpecs.classpath(getPackrJar());
        }).assertNormalExitValue();
    }

}
