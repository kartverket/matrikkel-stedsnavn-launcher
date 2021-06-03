# Plugin for gradle

Dette prosjektet lager en plugin til Gradle. Denne pluginen oppretter deretter installere for Windows, Linux og OSX 64-bits.
Etter at installasjonen er fullført, kan man starte en binærfil som viser en login-dialog.
Denne dialogen oppretter kontakt med en webserver som sjekker bruker og passord. Dersom dette er korrekt, laster startprogrammet
ned en klient som deretter startes.

# Eksempel på bruk fra matrikkelklientens build.xml:
```
import org.apache.commons.compress.archivers.sevenz.SevenZMethod

plugins {
    id 'no.statkart.launcher' version '1.2.4'
}

def keystore = rootProject.file(System.getenv('keystore') ?: 'launcher/keystore/selfsign.p12') as String
def keystore_alias = System.getenv('keystore_alias') ?: 'selfsign'
def keystore_password = System.getenv('keystore_password') ?: rootProject.file('launcher/keystore/selfsign.txt').text

launcher {
    // Dette er versjonen på matrikkelstarteren
    version '1.3.0'
    jvm {
        urlWindows = devbinBaseUrl + 'bin/java/jdk/OpenJDK11U-jdk_x64_windows_hotspot_11.0.7_10.zip'
        urlLinux = devbinBaseUrl + 'bin/java/jdk/OpenJDK11U-jdk_x64_linux_hotspot_11.0.7_10.tar.gz'
        urlOsx = devbinBaseUrl + 'bin/java/jdk/OpenJDK11U-jdk_x64_osx_hotspot_11.0.7_10.tar.gz'
        modules = ['java.sql', 'java.desktop', 'java.naming', 'java.rmi', 'java.management', 'jdk.localedata', 'jdk.jdwp.agent']
        locales = ['nb', 'nn']
    }
    server {
        oldestAllowedClientVersion '1.3.0' //defaults to launcher.version
        webinf project.file('launcher/webinf')
        metainf project.file('launcher/metainf')
        webinfLibs configurations.serverRuntimeClasspath, tasks.serverJar
        classpath configurations.runtimeClasspath, tasks.jar
        getdown project.file('launcher/getdown/server')
    }
    clients {
        'matrikkelstart-windows' {
            arch 'windows'
            executable 'matrikkelstart'
            icon project.file('launcher/icon/windows')
            getdown project.file('launcher/getdown/client')
            packaging '7z'
            packagingConfig {
                method SevenZMethod.DEFLATE
                sfx project.file('launcher/7z/7zsd_Deflate_x64.sfx')
                sfxConfig project.file('launcher/7z/7zsd_config.txt')
            }
            signing {
                store keystore
                alias keystore_alias
                password keystore_password
            }
        }
        'matrikkelstart-windows-plain' {
            arch 'windows'
            executable 'matrikkelstart'
            getdown project.file('launcher/getdown/client')
            packaging 'zip'
        }
        'matrikkelstart-linux' {
            arch 'linux'
            executable 'matrikkelstart'
            getdown project.file('launcher/getdown/client')
            packaging 'targz'
        }
        'matrikkelstart-osx' {
            arch 'osx'
            executable 'matrikkelstart'
            icon project.file('launcher/icon/osx/program.icns')
            getdown project.file('launcher/getdown/client')
            packaging 'targz'
        }
    }
}
tasks['launcher'].dependsOn(tasks['jar'])
tasks['launcher'].dependsOn(tasks['serverJar'])
tasks['assemble'].dependsOn(tasks['launcher'])
```
Kikk i de filene og mappene til matrikkelklienten som er referert over for å se hva som trengs av ekstra oppsett.
