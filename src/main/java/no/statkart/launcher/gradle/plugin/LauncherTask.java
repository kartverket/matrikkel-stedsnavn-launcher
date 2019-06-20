package no.statkart.launcher.gradle.plugin;

import com.badlogicgames.packr.Packr;
import com.badlogicgames.packr.PackrConfig;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class LauncherTask extends DefaultTask {

    private static final String JDK_VERSION = "12";

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
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
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
        copyResources("lib/server", "build/launcher/war/WEB-INF/lib");
        copy(utvidelse().getWebinfLibs(), "build/launcher/war/WEB-INF/lib");
        copy(getGetdown().getServer(), "build/launcher/war/vault");
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
        Charset charset = StandardCharsets.UTF_8;
        String content = new String(Files.readAllBytes(filePath), charset);
        content = content.replace(token, replacement);
        Files.write(filePath, content.getBytes(charset));
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
        ArtifakterExtension a = getArtifakter();
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

    private void lagKlienter() throws IOException {
        packr(PackrConfig.Platform.Windows64, "windows");
        packr(PackrConfig.Platform.Linux64, "linux");
        packr(PackrConfig.Platform.MacOS, "osx");
    }

    private void packr(PackrConfig.Platform platform, String alias) throws IOException {
        String jdk = "jdk-" + JDK_VERSION + "-" + alias;
        copyResources("lib/client", "build/launcher/lib/");
        copyResource("jdk/" + jdk + ".zip", "build/launcher/jdk/");
        unzip("build/launcher/jdk/" + jdk + ".zip", "build/launcher/jdk/");
        try {
            PackrConfig config = new PackrConfig();
            config.platform = platform;
            config.executable = utvidelse().getExecutable();
            config.mainClass = "no.statkart.launcher.client.Wrapper";
            config.cacheJre = new File("build/launcher/jdk/" + jdk);
            config.outDir = new File("build/launcher/packr/" + alias);
            config.classpath = Files.list(toAbsolutePath("build/launcher/lib"))
                .map(Path::toString)
                .collect(Collectors.toList());
            String icons = utvidelse().getIcons();
            if (icons != null) {
                config.iconResource = new File(utvidelse().getIcons());
            }
            config.jdk = "x";
            config.vmArgs = Collections.emptyList();
            new Packr().pack(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if ("osx".equals(alias)) {
            alias = "osx/Contents/Resources";
        }
        copy(getGetdown().getClient(), "build/launcher/packr/" + alias + "/work");
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

    private void unzip(String zipFile, String toDir) throws IOException {
        Path inputPath = toAbsolutePath(zipFile);
        Path outputDirPath = toAbsolutePath(toDir);
        try (FileSystem zipFs = FileSystems.newFileSystem(inputPath, null)) {
            Path zipRoot = zipFs.getPath("/");
            Files.walkFileTree(zipRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path target = outputDirPath.resolve(zipRoot.relativize(file).toString());
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private ArtifakterExtension getArtifakter() {
        return utvidelse().getArtifakterUtvidelse();
    }

    private GetdownExtension getGetdown() {
        return utvidelse().getGetdownUtvidelse();
    }

    private LauncherExtension utvidelse() {
        return getProject().getExtensions().findByType(LauncherExtension.class);
    }

    private Path toAbsolutePath(String relativePath) {
        Path root = getProject().getProjectDir().toPath();
        return root.resolve(relativePath);
    }

}
