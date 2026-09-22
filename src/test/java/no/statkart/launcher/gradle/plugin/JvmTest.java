package no.statkart.launcher.gradle.plugin;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.GROUP_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JvmTest {

    private static final int BASE_OCTAL = 8;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testGetPermissionsFull() {
        assertThat(new NixPermissionSetter().getPermissions(Integer.parseInt("777", BASE_OCTAL))).containsExactlyInAnyOrder(
                OWNER_READ,
                OWNER_WRITE,
                OWNER_EXECUTE,
                GROUP_READ,
                GROUP_WRITE,
                GROUP_EXECUTE,
                OTHERS_READ,
                OTHERS_WRITE,
                OTHERS_EXECUTE
        );
    }

    @Test
    public void testGetPermissionsNoExec() {
        assertThat(new NixPermissionSetter().getPermissions(Integer.parseInt("666", BASE_OCTAL))).containsExactlyInAnyOrder(
                OWNER_READ,
                OWNER_WRITE,
                GROUP_READ,
                GROUP_WRITE,
                OTHERS_READ,
                OTHERS_WRITE
        );
    }

    @Test
    public void testGetPermissions750() {
        assertThat(new NixPermissionSetter().getPermissions(Integer.parseInt("750", BASE_OCTAL))).containsExactlyInAnyOrder(
                OWNER_READ,
                OWNER_WRITE,
                OWNER_EXECUTE,
                GROUP_READ,
                GROUP_EXECUTE
        );
    }

    @Test
    public void testGetPermissions421() {
        assertThat(new NixPermissionSetter().getPermissions(Integer.parseInt("421", BASE_OCTAL))).containsExactlyInAnyOrder(
                OWNER_READ,
                GROUP_WRITE,
                OTHERS_EXECUTE
        );
    }

    @Test
    public void testGetPermissions000() {
        assertThat(new NixPermissionSetter().getPermissions(Integer.parseInt("000", BASE_OCTAL))).isEmpty();
    }

    @Test
    public void testGetPermissionThrowsIfCalledWithInvalidValues() {
        assertThatThrownBy(() -> new NixPermissionSetter().getPermissions(Integer.parseInt("1000", BASE_OCTAL)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid mode 01000");
        assertThatThrownBy(() -> new NixPermissionSetter().getPermissions(-1))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid mode 037777777777");
    }

    @Test
    public void testThatNixPermissionSetterSetsPermissions() throws IOException {
        NixPermissionSetter permissionSetter = spy(new NixPermissionSetter());
        when(permissionSetter.getPermissions(anyInt())).thenReturn(new HashSet<>() {
        });
        doNothing().when(permissionSetter).setPermissions(any(), any());
        permissionSetter.apply(new TarArchiveEntry(""), Path.of(""));
        verify(permissionSetter).setPermissions(any(), any());
    }

    /**
     * Simulerer eldre JDK-er (f.eks. 17/21) der {@code jmods} allerede er bundlet inni
     * hoved-JDK-arkivet. Ingen jmods-URL skal da være nødvendig.
     */
    @Test
    public void testLegacyJdkWithBundledJmodsStillWorks() throws IOException {
        Path downloadDir = temporaryFolder.newFolder("downloads").toPath();
        Path destinationDir = temporaryFolder.newFolder("dest").toPath();

        Path jdkArchive = downloadDir.resolve("jdk-legacy_x64_linux.tar.gz");
        writeTarGz(jdkArchive, List.of(
                "jdk-legacy/bin/java",
                "jdk-legacy/jmods/java.base.jmod"
        ));

        Jvm jvm = Jvm.LINUX;
        jvm.setURL(jdkArchive.toUri().toURL().toString());
        jvm.setJmodsURL(null);
        jvm.setDestinationDir(destinationDir);

        jvm.download();
        jvm.unpack();

        Path unpackedJdk = destinationDir.resolve(jvm.getAlias());
        Path jmodsDir = jvm.getJModsDirectory(unpackedJdk);
        assertThat(jmodsDir).isDirectory();
        assertThat(jmodsDir.getFileName().toString()).isEqualTo("jmods");
    }

    /**
     * Simulerer JDK 25+ der Adoptium/Temurin publiserer {@code jmods} som et separat
     * artefakt. Når en jmods-URL er konfigurert, skal denne lastes ned/pakkes ut og
     * brukes fremfor å lete inni hoved-JDK-arkivet.
     */
    @Test
    public void testJdk25StyleSplitArchivesWorkWhenJmodsUrlConfigured() throws IOException {
        Path downloadDir = temporaryFolder.newFolder("downloads").toPath();
        Path destinationDir = temporaryFolder.newFolder("dest").toPath();

        Path jdkArchive = downloadDir.resolve("jdk25_x64_linux.tar.gz");
        // Hoved-JDK-arkivet inneholder IKKE jmods, slik som ekte Temurin 25-arkiver.
        writeTarGz(jdkArchive, List.of(
                "jdk-25/bin/java"
        ));

        Path jmodsArchive = downloadDir.resolve("jmods25_x64_linux.tar.gz");
        writeTarGz(jmodsArchive, List.of(
                "jdk-25/jmods/java.base.jmod"
        ));

        Jvm jvm = Jvm.LINUX;
        jvm.setURL(jdkArchive.toUri().toURL().toString());
        jvm.setJmodsURL(jmodsArchive.toUri().toURL().toString());
        jvm.setDestinationDir(destinationDir);

        jvm.download();
        jvm.unpack();

        // Hoved-JDK-arkivet mangler fortsatt jmods.
        Path unpackedJdk = destinationDir.resolve(jvm.getAlias());
        assertThatThrownBy(() -> jvm.getJModsDirectory(unpackedJdk))
                .isInstanceOf(java.util.NoSuchElementException.class);

        // Men den separat utpakkede jmods-mappen skal inneholde jmods.
        Path unpackedJmods = destinationDir.resolve(jvm.getAlias() + "-jmods");
        Path jmodsDir = jvm.getJModsDirectory(unpackedJmods);
        assertThat(jmodsDir).isDirectory();
        assertThat(jmodsDir.getFileName().toString()).isEqualTo("jmods");
    }

    /**
     * Uten jmods bundlet i hoved-arkivet og uten en konfigurert jmods-URL som fallback,
     * skal feilen være tydelig og diagnostiserbar (ikke en bar "No value present").
     */
    @Test
    public void testMissingJmodsWithNoFallbackUrlProducesClearErrorMessage() throws IOException {
        Path dirWithoutJmods = temporaryFolder.newFolder("jdk-without-jmods").toPath();
        Files.createDirectories(dirWithoutJmods.resolve("bin"));
        Files.createFile(dirWithoutJmods.resolve("bin").resolve("java"));

        Jvm jvm = Jvm.LINUX;
        assertThatThrownBy(() -> jvm.getJModsDirectory(dirWithoutJmods))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("No jmods directory found under")
                .hasMessageContaining("jmodsUrlLinux");
    }

    private void writeTarGz(Path archive, List<String> filesToCreate) throws IOException {
        Files.createDirectories(archive.getParent());
        try (OutputStream fos = Files.newOutputStream(archive);
             GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(fos);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {
            for (String fileName : filesToCreate) {
                byte[] content = "content".getBytes();
                TarArchiveEntry entry = new TarArchiveEntry(fileName);
                entry.setSize(content.length);
                entry.setMode(Integer.parseInt("644", 8));
                taos.putArchiveEntry(entry);
                taos.write(content);
                taos.closeArchiveEntry();
            }
        }
    }

}

