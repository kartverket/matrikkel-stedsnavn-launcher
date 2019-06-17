package no.statkart.launcher.gradle.plugin;

import net.jsign.KeyStoreUtils;
import net.jsign.PESigner;
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
        KeyStore keystore = KeyStoreUtils.load(new File(store), "PKCS12", password, null);
        PESigner signer = new PESigner(keystore, alias, password);
        signer.withTimestamping(false);
        signer.sign(new PEFile(into.toFile()));
    }

}
