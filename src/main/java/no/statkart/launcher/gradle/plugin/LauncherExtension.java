package no.statkart.launcher.gradle.plugin;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;

public class LauncherExtension {

    final Project project;
    private final NamedDomainObjectContainer<ClientExtension> clients;

    private String version;
    private JvmExtension jvmExt;
    private ServerExtension serverExt;

    public LauncherExtension(Project project) {
        this.project = project;
        this.clients = project.container(ClientExtension.class);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void version(String version) {
        this.version = version;
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void jvm(Action<JvmExtension> action) {
        jvmExt = new JvmExtension();
        action.execute(jvmExt);
    }
    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void server(Action<ServerExtension> action) {
        serverExt = new ServerExtension(this);
        action.execute(serverExt);
    }

    // Kalles vha refleksjon av gradle
    @SuppressWarnings("unused")
    public void clients(Action<? super NamedDomainObjectContainer<ClientExtension>> action) {
        action.execute(clients);
    }

    @Input
    String getVersion() {
        return version;
    }

    @Nested
    JvmExtension getJvmUtvidelse() {
        return jvmExt;
    }

    @Nested
    ServerExtension getServerUtvidelse() {
        return serverExt;
    }

    @Nested
    ClientExtension[] getKlienter() {
        return clients.toArray(new ClientExtension[0]);
    }

}
