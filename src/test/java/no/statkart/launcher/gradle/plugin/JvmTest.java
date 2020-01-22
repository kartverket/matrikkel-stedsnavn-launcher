package no.statkart.launcher.gradle.plugin;

import org.junit.Test;

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

public class JvmTest {

    private static final int BASE_OCTAL = 8;

    @Test
    public void testGetPermissionsFull() {
        assertThat(Jvm.LINUX.getPermissions(Integer.parseInt("777", BASE_OCTAL))).containsExactlyInAnyOrder(
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
        assertThat(Jvm.LINUX.getPermissions(Integer.parseInt("666", BASE_OCTAL))).containsExactlyInAnyOrder(
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
        assertThat(Jvm.LINUX.getPermissions(Integer.parseInt("750", BASE_OCTAL))).containsExactlyInAnyOrder(
                OWNER_READ,
                OWNER_WRITE,
                OWNER_EXECUTE,
                GROUP_READ,
                GROUP_EXECUTE
        );
    }

    @Test
    public void testGetPermissions421() {
        assertThat(Jvm.LINUX.getPermissions(Integer.parseInt("421", BASE_OCTAL))).containsExactlyInAnyOrder(
                OWNER_READ,
                GROUP_WRITE,
                OTHERS_EXECUTE
        );
    }

    @Test
    public void testGetPermissions000() {
        assertThat(Jvm.LINUX.getPermissions(Integer.parseInt("000", BASE_OCTAL))).isEmpty();
    }

    @Test
    public void testGetPermissionThrowsIfCalledWithInvalidValues() {
        assertThatThrownBy(() -> Jvm.LINUX.getPermissions(Integer.parseInt("1000", BASE_OCTAL)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid mode 01000");
        assertThatThrownBy(() -> Jvm.LINUX.getPermissions(-1))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid mode 037777777777");
    }
}
