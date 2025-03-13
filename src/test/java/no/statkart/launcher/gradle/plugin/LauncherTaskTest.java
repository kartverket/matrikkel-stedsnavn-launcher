package no.statkart.launcher.gradle.plugin;

import no.statkart.launcher.gradle.testutils.TestKit;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

public class LauncherTaskTest extends TestKit {

    @Test
    public void notUpToDateWhenVersionChanges() throws Exception {
        standardBuildGradle();
        standardGetdownTXT("launcher/getdown/server/getdown.txt");
        assertThat(
                createGradleRunner()
                        .withArguments("launcher", "-PmyVersion=1", "-S")
                        .withDebug(true)
                        .build()
                        .getTasks())
                .extracting(BuildTask::getOutcome)
                .containsExactly(TaskOutcome.SUCCESS);
        assertThat(
                createGradleRunner()
                        .withArguments("launcher", "-PmyVersion=2")
                        .build()
                        .getTasks())
                .extracting(BuildTask::getOutcome)
                .containsExactly(TaskOutcome.SUCCESS);
        assertThat(
                createGradleRunner()
                        .withArguments("launcher", "-PmyVersion=2")
                        .build()
                        .getTasks())
                .extracting(BuildTask::getOutcome)
                .containsExactly(TaskOutcome.UP_TO_DATE);
    }

    @Test
    public void buildWebinfIsCleanedBetweenRuns() throws Exception {
        standardBuildGradle();
        standardGetdownTXT("launcher/getdown/server/getdown.txt");
        writeFileUTF8("launcher/webinf/foo.txt", "bar");
        assertThat(
                createGradleRunner()
                        .withArguments("launcher", "-PmyVersion=1", "-S")
                        .withDebug(true)
                        .build()
                        .getTasks())
                .extracting(BuildTask::getOutcome)
                .containsExactly(TaskOutcome.SUCCESS);
        assertThat(path("build/launcher/war/WEB-INF/foo.txt")).exists();
        Files.delete(path("launcher/webinf/foo.txt"));
        assertThat(
                createGradleRunner()
                        .withArguments("launcher", "-PmyVersion=2")
                        .build()
                        .getTasks())
                .extracting(BuildTask::getOutcome)
                .containsExactly(TaskOutcome.SUCCESS);
        assertThat(path("build/launcher/war/WEB-INF/foo.txt")).doesNotExist();
    }

    @Test
    public void buildLibIsCleanedInRun() throws Exception {
        standardBuildGradle();
        standardGetdownTXT("launcher/getdown/server/getdown.txt");
        writeFileUTF8("build/launcher/lib/foo.txt", "bar");
        assertThat(
                createGradleRunner()
                        .withArguments("launcher", "-PmyVersion=1", "-S")
                        .withDebug(true)
                        .build()
                        .getTasks())
                .extracting(BuildTask::getOutcome)
                .containsExactly(TaskOutcome.SUCCESS);
        assertThat(path("build/launcher/lib/foo.txt")).doesNotExist();
    }

    private void standardBuildGradle() throws IOException {
        writeFileUTF8("build.gradle",
                "  plugins { ",
                "    id 'no.statkart.launcher' ",
                "  } ",
                "   ",
                "  launcher { ",
                "    version project.property('myVersion') ",
                "    jvm { ",
                "      modules = ['java.sql', 'java.desktop', 'java.naming', 'java.rmi', 'java.management', 'jdk.localedata', 'jdk.jdwp.agent'] ",
                "      locales = ['nb', 'nn'] ",
                "    }",
                "    server { ",
                "      webinf project.file('launcher/webinf') ",
                "      metainf project.file('launcher/metainf') ",
                "      getdown project.file('launcher/getdown/server') ",
                "    } ",
                "  } ",
                ""
        );
    }

}
