package no.statkart.launcher.client;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class StandardOppsettTest {

    @Test
    public void context_path_skal_plukkes_ut_av_getdown() throws Exception {
        assertThat(contextPathFra("appbase = https://.../fooklient/vault")).isEqualTo("/fooklient");
        assertThat(contextPathFra("appbase = https://.../fooklient/vault/")).isEqualTo("/fooklient");
        assertThat(contextPathFra("appbase = https://www.matrikkel.no/matrikkelklient/vault/")).isEqualTo("/matrikkelklient");
        assertThat(contextPathFra("appbase = file://asdf.asdf:7002/qwerty/vault/")).isEqualTo("/qwerty");
        assertThat(contextPathFra("appbase = file://asdf.asdf:7002/qwerty/vault")).isEqualTo("/qwerty");
        assertThat(contextPathFra("appbase = file://asdf.asdf:7002/vault/")).isEqualTo("");
    }

    private String contextPathFra(String getdownTxt) throws IOException {
        Path getdown = opprettGetdown(getdownTxt);
        return new StandardOppsett(getdown.getParent()).getContextPath();
    }

    private Path opprettGetdown(String content) throws IOException {
        Path p = Path.of("build", "test", "getdown.txt");
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
        return p;
    }

}