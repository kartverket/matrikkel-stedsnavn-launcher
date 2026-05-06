package no.statkart.launcher.gradle.plugin.signing;

import net.jsign.AuthenticodeSigner;
import net.jsign.KeyStoreBuilder;
import net.jsign.KeyStoreType;
import net.jsign.pe.PEFile;

import java.io.File;
import java.nio.file.Path;
import java.security.KeyStore;

public class KeystoreSigningExtension implements SigningExtension {
    private String store;
    private String alias;
    private String password;

    public void store(String store) {
        this.store = store;
    }

    private void alias(String alias) {
        this.alias = alias;
    }

    private void password(String password) {
        this.password = password;
    }

    public void execute(Path into) throws Exception {
        // Støtter bare windows .exe-filer
        if (into.getFileName().toString().endsWith(".exe")) {
            KeyStore keystore = new KeyStoreBuilder()
                .keystore(new File(store))
                .storetype(KeyStoreType.PKCS12)
                .storepass(password)
                .build();
            AuthenticodeSigner signer = new AuthenticodeSigner(keystore, alias, password);
            signer.withTimestamping(false);
            signer.sign(new PEFile(into.toFile()));
        }
    }

}
