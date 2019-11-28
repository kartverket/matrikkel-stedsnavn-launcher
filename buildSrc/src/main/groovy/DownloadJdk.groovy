import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GFileUtils

import javax.inject.Inject
import java.nio.file.Paths

class DownloadJdk extends DefaultTask {
    @Input
    URL url
    @OutputFile
    File file

    @Inject
    DownloadJdk(String urlString) {
        url = new URL(urlString)
        file = project.file('build/jdk/' + filenameFrom(url))
    }

    @TaskAction
    void fetch() {
        if (file.exists()) {
            logger.lifecycle("Using existing file $file ...")
        } else {
            logger.lifecycle("Downloading file to $file ...")
            GFileUtils.copyURLToFile(url, file)
        }
    }

    @CompileStatic
    static String filenameFrom(URL url) {
        return Paths.get(url.getPath()).getFileName().toString()
    }
}