package no.statkart.launcher.gradle.plugin;

import no.statkart.launcher.gradle.testutils.TestKit;
import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;

public class LauncherTaskTest extends TestKit {

    @Test
    public void notUpToDateWhenVersionChanges() throws Exception {
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

        standardGetdownTXT("launcher/getdown/server/getdown.txt");

        Assertions.assertThat(
                createGradleRunner()
                        .withArguments("launcher", "-PmyVersion=1", "-S")
                        .withDebug(true)
                        .build()
                        .getTasks())
                .extracting(BuildTask::getOutcome)
                .containsExactly(TaskOutcome.SUCCESS);

        Assertions.assertThat(
                createGradleRunner()
                        .withArguments("launcher", "-PmyVersion=2")
                        .build()
                        .getTasks())
                .extracting(BuildTask::getOutcome)
                .containsExactly(TaskOutcome.SUCCESS);

        Assertions.assertThat(
                createGradleRunner()
                        .withArguments("launcher", "-PmyVersion=2")
                        .build()
                        .getTasks())
                .extracting(BuildTask::getOutcome)
                .containsExactly(TaskOutcome.UP_TO_DATE);
    }
}
