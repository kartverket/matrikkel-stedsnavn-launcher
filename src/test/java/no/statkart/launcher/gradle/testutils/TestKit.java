package no.statkart.launcher.gradle.testutils;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

public class TestKit {
    protected Path projectPath;

    protected Path webinfPath;
    protected Path metainfPath;
    protected Path serverPath;

    public Path path(String relativePath) {
        return projectPath.resolve(relativePath);
    }

    public File file(String relativePath) {
        return projectPath.resolve(relativePath).toFile();
    }


    protected GradleRunner createGradleRunner() {
        return GradleRunner.create()
                .withProjectDir(projectPath.toFile())
                .withPluginClasspath();
    }

    @Before
    public void createTempDir() throws IOException {
        projectPath = Files.createTempDirectory("launcherTest");

        webinfPath = path("launcher/webinf");
        metainfPath = path("launcher/metainf");
        serverPath = path("launcher/getdown/server");

        Files.createDirectories(webinfPath);
        Files.createDirectories(metainfPath);
        Files.createDirectories(serverPath);
    }

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void succeeded(Description description) {
            try {
                deleteRecursively(projectPath);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }

        @Override
        protected void failed(Throwable e, Description description) {
            System.err.println("Test failed! Leaving generated files in directory " + file(""));
        }
    };


    static void deleteRecursively(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    protected File writeFileUTF8(String relativePath, CharSequence... lines) throws IOException {
        return writeFileUTF8(path(relativePath), lines);
    }

    protected static File writeFileUTF8(Path destination, CharSequence... lines) throws IOException {
        Files.createDirectories(destination.getParent());
        Files.write(destination, Arrays.asList(lines), StandardCharsets.UTF_8,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        return destination.toFile();
    }

    protected File standardGetdownTXT(String relativePath) throws IOException {
        return writeFileUTF8(relativePath,
                "appbase = https://ignored/myclient/vault", //URL from which the client is downloaded (host and port is ignored)
                "",
                "class = foo.com.myclient.MainFrameLauncher", //main entry point for the client application
                "",
                "jvmarg = -Xmx750m",
                "jvmarg = -Xms256m",
                "",
                "@@code@@", //Application jar files
                ""
        );
    }
}
