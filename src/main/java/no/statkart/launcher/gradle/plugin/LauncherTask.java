package no.statkart.launcher.gradle.plugin;

import com.threerings.getdown.tools.Digester;
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
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    }

    @TaskAction
    public void execute() throws IOException, GeneralSecurityException {
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

    private void opprettTjenerWebapp(Map<String, String> artifacts) throws IOException, GeneralSecurityException {
        ServerExtension server = utvidelse.getServerUtvidelse();
        opprettPathingJar(server.getClasspath(), "build/launcher/war/vault/client-dependencies.jar");
        copy(server.getClasspath(), "build/launcher/war/vault");
        for (int i = 0; i < server.getLibraries().size(); i++) {
            copy(server.getLibraries().get(i), "build/launcher/war/vault/libraries/" + i);
        }
        copy(server.getWebinf(), "build/launcher/war/WEB-INF");
        copy(server.getMetainf(), "build/launcher/war/META-INF");
        String version = Objects.toString(getProject().getProperties().get("version"), null);
        if (version != null && !version.isEmpty()) {
            replace("build/launcher/war/META-INF/MANIFEST.MF", "@@version@@", version);
        }
        copyResources("lib/server", "build/launcher/war/WEB-INF/lib");
        copy(server.getWebinfLibs(), "build/launcher/war/WEB-INF/lib");
        for (Map.Entry<String, String> e : artifacts.entrySet()) {
            replace("build/launcher/war/WEB-INF/web.xml", "@@" + e.getKey() + "@@", e.getValue());
        }
        copy(server.getGetdown(), "build/launcher/war/vault");
        replace("build/launcher/war/vault/getdown.txt", "@@code@@", asCode(server.getClasspath()));
        opprettDigests();
    }

    private void opprettDigests() throws IOException, GeneralSecurityException {
        Path mappe = toAbsolutePath("build/launcher/war/vault");
        Digester.createDigests(mappe.toFile(), null, null, null);
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
        for (ClientExtension klient : utvidelse.getKlienter()) {
            artifacts.put(klient.getName(),
                    klient.execute(
                            toAbsolutePath("build/launcher/packr/" + klient.getName()),
                            toAbsolutePath("build/launcher/war/download"),
                            utvidelse.getVersion()
                    ));
        }
        return artifacts;
    }

    private void lagKlienter() throws IOException {
        copyResources("lib/client", "build/launcher/lib/");
        copyResources("jdk", "build/launcher/jdk/");
        Jvm[] jvms = {Jvm.WINDOWS, Jvm.LINUX, Jvm.OSX};
        for (Jvm jvm : jvms) {
            jvm.setURL(utvidelse.getJvmUtvidelse().getUrl(jvm));
            jvm.setDestinationDir(toAbsolutePath("build/launcher/jdk"));
            jvm.download();
            jvm.unpack();
        }
        // Dette steget må utføres etter at alle jvm'ene er lastet ned og pakket ut
        for (Jvm jvm : jvms) {
            jvm.jlink(
                    utvidelse.getJvmUtvidelse().getModules(),
                    utvidelse.getJvmUtvidelse().getLocales()
            );
        }
        for (ClientExtension klient : utvidelse.getKlienter()) {
            packr(klient);
        }
    }

    private void packr(ClientExtension klient) throws IOException {
        Jvm jvm = Jvm.fraAlias(klient.getArch()).orElseThrow();
        try {
            File bootClasspath = null;
            PackrConfig config = new PackrConfig();
            config.platform = jvm;
            config.executable = klient.getExecutable();
            config.mainClass = "no.statkart.launcher.client.Launcher";
            config.cacheJre = toAbsolutePath("build/launcher/jdk/" + jvm.getAlias() + "-min").toFile();
            config.outDir = toAbsolutePath("build/launcher/packr/" + klient.getName()).toFile();
            try (Stream<Path> paths = Files.list(toAbsolutePath("build/launcher/lib"))) {
                config.classpath = paths.map(Path::toString).collect(Collectors.toList());
            }
            if (jvm == Jvm.OSX) {
                config.iconResource = klient.getIcon();
            }
            if (jvm == Jvm.WINDOWS) {
                // Gjeldende workaround er å legge en hacket versjon av packr*.exe
                // NB: Denne må komme først på classpath!
                bootClasspath = klient.getIcon();
            }
            exec(config, bootClasspath);
            // MAT-12826, legg til en .bat i tillegg til .exe på windows
            if (jvm == Jvm.WINDOWS) {
                try (Stream<Path> paths = Files.list(toAbsolutePath("build/launcher/lib"))) {
                    String cp = paths.map(Path::getFileName).map(Path::toString).collect(Collectors.joining(";"));
                    append(
                            String.format("jre\\bin\\java -cp %s %s", cp, config.mainClass),
                            String.format("build/launcher/packr/%s/%s.bat", klient.getName(), config.executable)
                    );
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        copyResources("jdk/fonts", "build/launcher/packr/" + topDirectory(klient) + "/jre/lib/fonts");
        copy(klient.getGetdown(), "build/launcher/packr/" + topDirectory(klient) + "/work");
        replace("build/launcher/packr/" + topDirectory(klient) + "/work/client.properties", "@@version@@", utvidelse.getVersion());
    }

    private String topDirectory(ClientExtension klient) {
        if ("osx".equals(klient.getArch())) {
            return klient.getName() + "/Contents/Resources";
        }
        return klient.getName();
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
        Files.write(toFilePath, content.getBytes(charset), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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

    private void exec(PackrConfig config, File bootClasspath) {
        getProject().javaexec(execSpecs -> {
            config.decorateExecSpecs(execSpecs);
            execSpecs.setMain("com.badlogicgames.packr.Packr");
            if (bootClasspath != null) {
                execSpecs.classpath(bootClasspath);
            }
            execSpecs.classpath(getPackrJar());
        }).assertNormalExitValue();
    }

}
