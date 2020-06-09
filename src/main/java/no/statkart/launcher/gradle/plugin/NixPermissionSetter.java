package no.statkart.launcher.gradle.plugin;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

class NixPermissionSetter implements BiFunction<TarArchiveEntry, Path, IOException> {

    private static final int MAX_MODE = 511; // Octal 0777
    private static final int MIN_MODE = 0;

    private final static Map<Integer, PosixFilePermission> allPermissions = new HashMap<>() {{
        put(8, PosixFilePermission.OWNER_READ);
        put(7, PosixFilePermission.OWNER_WRITE);
        put(6, PosixFilePermission.OWNER_EXECUTE);
        put(5, PosixFilePermission.GROUP_READ);
        put(4, PosixFilePermission.GROUP_WRITE);
        put(3, PosixFilePermission.GROUP_EXECUTE);
        put(2, PosixFilePermission.OTHERS_READ);
        put(1, PosixFilePermission.OTHERS_WRITE);
        put(0, PosixFilePermission.OTHERS_EXECUTE);
    }};

    Set<PosixFilePermission> getPermissions(int mode) {
        if (mode > MAX_MODE || mode < MIN_MODE) {
            throw new RuntimeException("Invalid mode 0" + Integer.toOctalString(mode));
        }
        Set<PosixFilePermission> result = new HashSet<>();
        for (int bit = 0; bit < 9; bit++) {
            int set = (mode >> bit) & 1;
            if (set == 1) {
                result.add(allPermissions.get(bit));
            }
        }
        return result;
    }

    protected void setPermissions(Path path, Set<PosixFilePermission> permissions) throws IOException {
        Files.setPosixFilePermissions(path, permissions);
    }

    @Override
    public IOException apply(TarArchiveEntry entry, Path path) {
        try {
            setPermissions(path, getPermissions(entry.getMode()));
        } catch (IOException e) {
            return e;
        }
        return null;
    }

}
