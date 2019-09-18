package no.statkart.launcher.gradle.plugin;

import com.badlogicgames.packr.Packr;
import com.badlogicgames.packr.PackrConfig;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
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
        String content = Files.readString(filePath, charset);
        content = content.replace(token, replacement);
        Files.writeString(filePath, content, charset);
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

    private enum JVM {
        WINDOWS(
                "windows",
                "openjdk-12_windows-x64_bin.zip",
                "jdk-12/jmods"
        ),
        LINUX(
                "linux",
                "openjdk-12_linux-x64_bin.tar.gz",
                "jdk-12/jmods"
        ),
        OSX(
                "osx",
                "openjdk-12_osx-x64_bin.tar.gz",
                "jdk-12.jdk/Contents/Home/jmods"
        );

        private final String alias;
        private final String artifact;
        private final String jmodsPath;

        JVM(String alias, String artifact, String jmodsPath) {
            this.alias = alias;
            this.artifact = artifact;
            this.jmodsPath = jmodsPath;
        }

        boolean isZip() {
            return artifact.endsWith(".zip");
        }

        boolean isTarGz() {
            return artifact.endsWith(".tar.gz");
        }
    }

    private PackrConfig.Platform toPackrPlatform(JVM jvm) {
        return jvm == JVM.LINUX ? PackrConfig.Platform.Linux64
                : jvm == JVM.OSX ? PackrConfig.Platform.MacOS
                : PackrConfig.Platform.Windows64;
    }

    private String toWorkDir(String alias) {
        if ("osx" .equals(alias)) {
            return "osx/Contents/Resources/work";
        }
        return alias + "/work";
    }

    private void unpack(JVM jvm, String dir) throws IOException {
        String source = dir + "/" + jvm.artifact;
        String destination = dir + "/" + jvm.alias;
        if (jvm.isZip()) {
            unzip(source, destination);
        } else if (jvm.isTarGz()) {
            untargz(source, destination);
        } else {
            throw new RuntimeException("Unknown artifact compression method: " + jvm.artifact);
        }
    }

    private void jlink(JVM jvm, String dir) throws IOException {
        Path source = toAbsolutePath(dir).resolve(jvm.alias);
        Path destination = toAbsolutePath(dir).resolve(jvm.alias + "-min").resolve("jre");
        String programnavn = isCurrentlyRunningWindows() ? "jlink.exe" : "jlink";
        Path p = Path.of(System.getProperty("java.home"), "bin", programnavn);
        String cmd = p.toString()
                + " --module-path " + jvm.jmodsPath
                + " --add-modules " + String.join(",", getJvm().getModules())
                + " --include-locales " + String.join(",", getJvm().getLocales())
                + " --output " + destination;
        System.out.println("jlink cwd=" + source.toFile());
        System.out.println("jlink cmd=" + cmd);
        Process prosess = Runtime.getRuntime().exec(cmd, null, source.toFile());
        try {
            prosess.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(streamToString(prosess.getInputStream()));
        System.err.println(streamToString(prosess.getErrorStream()));
    }

    private String streamToString(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8);
    }

    private boolean isCurrentlyRunningWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.US).contains("windows");
    }

    private void lagKlienter() throws IOException {
        packr(JVM.WINDOWS);
        packr(JVM.LINUX);
        packr(JVM.OSX);
    }

    private void packr(JVM jvm) throws IOException {
        copyResources("lib/client", "build/launcher/lib/");
        copyResource("jdk/" + jvm.artifact, "build/launcher/jdk/");
        unpack(jvm, "build/launcher/jdk");
        jlink(jvm, "build/launcher/jdk");
        try {
            PackrConfig config = new PackrConfig();
            config.platform = toPackrPlatform(jvm);
            config.executable = utvidelse().getExecutable();
            config.mainClass = "no.statkart.launcher.client.Wrapper";
            config.cacheJre = toAbsolutePath("build/launcher/jdk/" + jvm.alias + "-min").toFile();
            config.outDir = toAbsolutePath("build/launcher/packr/" + jvm.alias).toFile();
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
        copy(getGetdown().getClient(), "build/launcher/packr/" + toWorkDir(jvm.alias));
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
            Files.walkFileTree(zipRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path target = outputDirPath.resolve(zipRoot.relativize(file).toString());
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void untargz(String targzFile, String toDir) throws IOException {
        Path inputPath = toAbsolutePath(targzFile);
        Path outputDirPath = toAbsolutePath(toDir);
        try (FileInputStream fileInputStream = new FileInputStream(inputPath.toFile());
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(bufferedInputStream);
             TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipInputStream)) {
            TarArchiveEntry entry;
            while ((entry = tarArchiveInputStream.getNextTarEntry()) != null) {
                Path path = outputDirPath.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                } else {
                    Files.createDirectories(path.getParent());
                    Files.copy(tarArchiveInputStream, path, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private JvmExtension getJvm() {
        return utvidelse().getJvmUtvidelse();
    }

    private GetdownExtension getGetdown() {
        return utvidelse().getGetdownUtvidelse();
    }

    private ArtifakterExtension getArtifakter() {
        return utvidelse().getArtifakterUtvidelse();
    }

    private LauncherExtension utvidelse() {
        return getProject().getExtensions().findByType(LauncherExtension.class);
    }

    private Path toAbsolutePath(String relativePath) {
        Path root = getProject().getProjectDir().toPath();
        return root.resolve(relativePath);
    }

}
