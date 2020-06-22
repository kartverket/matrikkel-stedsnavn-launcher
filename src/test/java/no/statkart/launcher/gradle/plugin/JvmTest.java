package no.statkart.launcher.gradle.plugin;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;

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

}
