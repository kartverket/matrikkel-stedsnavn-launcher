package no.statkart.launcher.gradle.plugin;

import net.jsign.AuthenticodeSigner;
import net.jsign.KeyStoreUtils;
import net.jsign.pe.PEFile;

import java.io.File;
import java.nio.file.Path;
import java.security.KeyStore;

public class SigningExtension {

    private String store;
    private String alias;
    private String password;

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void store(String store) {
        this.store = store;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    private void alias(String alias) {
        this.alias = alias;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    private void password(String password) {
        this.password = password;
    }

    void execute(Path into) throws Exception {
        // Støtter bare windows .exe-filer
        if (into.getFileName().toString().endsWith(".exe")) {
            KeyStore keystore = KeyStoreUtils.load(new File(store), "PKCS12", password, null);
            AuthenticodeSigner signer = new AuthenticodeSigner(keystore, alias, password);
            signer.withTimestamping(false);
            signer.sign(new PEFile(into.toFile()));
        }
    }

}
