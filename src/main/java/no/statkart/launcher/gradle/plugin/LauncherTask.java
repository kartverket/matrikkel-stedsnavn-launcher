package no.statkart.launcher.gradle.plugin;

import com.badlogicgames.packr.Packr;
import com.badlogicgames.packr.PackrConfig;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction;

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
        opprettPathingJar(utvidelse().getClasspath(), "build/launcher/war/vault/client-dependencies.jar");
        copy(utvidelse().getClasspath(), "build/launcher/war/vault");
        copy(utvidelse().getWebinf(), "build/launcher/war/WEB-INF");
        copy(utvidelse().getMetainf(), "build/launcher/war/META-INF");
        String version = (String) getProject().getProperties().get("version");
        if (version != null && !version.isEmpty()) {
            replace("build/launcher/war/META-INF/MANIFEST.MF", "@@version@@", version);
        }
        copyResources("lib/server", "build/launcher/war/WEB-INF/lib");
        copy(utvidelse().getWebinfLibs(), "build/launcher/war/WEB-INF/lib");
        copy(utvidelse().getGetdownUtvidelse().getServer(), "build/launcher/war/vault");
        replace("build/launcher/war/vault/getdown.txt", "@@code@@", asCode(utvidelse().getClasspath()));
    }

    private String asCode(FileCollection files) {
        StringBuilder sb = new StringBuilder();
        sb.append("code = client-dependencies.jar\n");
        files.forEach(f ->
                sb.append("resource = ").append(f.getName()).append("\n")
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
                sb.append(f.getName()).append(" ")
        );
        Manifest manifest = new Manifest();
        Attributes attr = manifest.getMainAttributes();
        attr.putValue("Manifest-Version", "1.0");
        attr.putValue("Class-Path", sb.toString());

        Path toFilePath = toAbsolutePath(toFile);
        Files.createDirectories(toFilePath.getParent());
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
        ArtifakterExtension a = utvidelse().getArtifakterUtvidelse();
        a.getWindows().execute(
                toAbsolutePath("build/launcher/packr/windows"),
                toAbsolutePath("build/launcher/war/download")
        );
        a.getLinux().execute(
                toAbsolutePath("build/launcher/packr/linux"),
                toAbsolutePath("build/launcher/war/download")
        );
        a.getOSX().execute(
                toAbsolutePath("build/launcher/packr/osx"),
                toAbsolutePath("build/launcher/war/download")
        );
    }

    private String toWorkDir(String alias) {
        if ("osx".equals(alias)) {
            return "osx/Contents/Resources/work";
        }
        return alias + "/work";
    }

    private void lagKlienter() throws IOException {
        packr(Jvm.WINDOWS);
        packr(Jvm.LINUX);
        packr(Jvm.OSX);
    }

    private void packr(Jvm jvm) throws IOException {
        copyResources("lib/client", "build/launcher/lib/");
        copyResource("jdk/" + jvm.getArtifact(), "build/launcher/jdk/");
        jvm.unpack(toAbsolutePath("build/launcher/jdk"));
        jvm.jlink(
                toAbsolutePath("build/launcher/jdk"),
                utvidelse().getJvmUtvidelse().getModules(),
                utvidelse().getJvmUtvidelse().getLocales()
        );
        try {
            PackrConfig config = new PackrConfig();
            config.platform = jvm.toPackrPlatform();
            config.executable = utvidelse().getExecutable();
            config.mainClass = "no.statkart.launcher.client.Wrapper";
            config.cacheJre = toAbsolutePath("build/launcher/jdk/" + jvm.getAlias() + "-min").toFile();
            config.outDir = toAbsolutePath("build/launcher/packr/" + jvm.getAlias()).toFile();
            config.classpath = Files.list(toAbsolutePath("build/launcher/lib"))
                    .map(Path::toString)
                    .collect(Collectors.toList());
            String icons = utvidelse().getIcons();
            if (icons != null) {
                config.iconResource = toAbsolutePath(utvidelse().getIcons()).toFile();
            }
            config.jdk = "x";
            config.vmArgs = Collections.emptyList();
            new Packr().pack(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        copyResources("jdk/fonts", "build/launcher/packr/" + topDirectory(jvm) + "/jre/lib/fonts");
        copy(utvidelse().getGetdownUtvidelse().getClient(), "build/launcher/packr/" + topDirectory(jvm) + "/work");
    }

    String topDirectory(Jvm jvm) {
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

    private LauncherExtension utvidelse() {
        return getProject().getExtensions().findByType(LauncherExtension.class);
    }

    private Path toAbsolutePath(String relativePath) {
        Path root = getProject().getProjectDir().toPath();
        return root.resolve(relativePath);
    }

}
