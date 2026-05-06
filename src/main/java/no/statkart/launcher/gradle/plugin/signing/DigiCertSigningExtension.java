package no.statkart.launcher.gradle.plugin.signing;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DigiCertSigningExtension implements SigningExtension {
    private String smctlPath = "smctl";
    private String host = "https://clientauth.one.digicert.com";
    private String apiKey;
    private String clientCertificateFile;
    private String clientCertificatePassword;
    private String keypairAlias;

    private void smctlPath(String smctlPath) {
        this.smctlPath = smctlPath;
    }
    private void host(String host) {
        this.host = host;
    }
    private void apiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    private void clientCertificateFile(String clientCertificateFile) {
        this.clientCertificateFile = clientCertificateFile;
    }
    private void clientCertificatePassword(String clientCertificatePassword) {
        this.clientCertificatePassword = clientCertificatePassword;
    }
    private void keypairAlias(String keypairAlias) {
        this.keypairAlias = keypairAlias;
    }

    @Override
    public void execute(Path into) throws Exception {
        if (shouldSign(into)) {
            validateSetup();

            var pb = new ProcessBuilder(
                smctlPath,
                "sign",
                "--keypair-alias", keypairAlias,
                "--input", into.toAbsolutePath().toString(),
                "--simple",
                "--verbose"
            ).inheritIO();

            Map<String, String> env = pb.environment();
            env.put("SM_HOST", host);
            env.put("SM_API_KEY", apiKey);
            env.put("SM_CLIENT_CERT_FILE", clientCertificateFile);
            env.put("SM_CLIENT_CERT_PASSWORD", clientCertificatePassword);


            var process = pb.start();
            if (!process.waitFor(1, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new IllegalStateException("smctl sign timed out after 1 minute");
            }
            var exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalStateException(String.format("smctl sign failed with exit code: %d", exitCode));
            }
        }
    }

    private boolean shouldSign(Path path) {
        var name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        var isSupportedFile = name.endsWith(".exe");
        var digicertSigning = Boolean.parseBoolean(System.getenv("DIGICERT_SIGN"));

        return isSupportedFile && digicertSigning;
    }

    private void validateSetup() {
        assertNotNullOrBlank("smctlPath", smctlPath);
        assertNotNullOrBlank("host", host);
        assertNotNullOrBlank("apiKey", apiKey);
        assertNotNullOrBlank("clientCertificateFile", clientCertificateFile);
        assertNotNullOrBlank("clientCertificatePassword", clientCertificatePassword);
        assertNotNullOrBlank("keypairAlias", keypairAlias);

        try {
            var process = new ProcessBuilder(smctlPath, "--version")
                .inheritIO()
                .start();

            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("smctl --version timed out");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("smctl --version failed");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertNotNullOrBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
