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
    // Dette er versjonen på matrikkelens launcher
    version '1.2.4'
    jvm {
        urlWindows = 'http://devbin.statkart.no:8070/bin/java/jdk/openjdk-12.0.2_windows-x64_bin.zip'
        urlLinux = 'http://devbin.statkart.no:8070/bin/java/jdk/openjdk-12.0.2_linux-x64_bin.tar.gz'
        urlOsx = 'http://devbin.statkart.no:8070/bin/java/jdk/openjdk-12.0.2_osx-x64_bin.tar.gz'
        modules = ['java.sql', 'java.desktop', 'java.naming', 'java.rmi', 'java.management', 'jdk.localedata']
        locales = ['no']
    }
    getdown {
        client project.file('launcher/getdown/client')
        server project.file('launcher/getdown/server')
    }
    webinf project.file('launcher/webinf')
    metainf project.file('launcher/metainf')
    webinfLibs configurations.serverRuntimeClasspath, tasks.serverJar
    executable 'matrikkelklient'
    classpath configurations.runtimeClasspath, tasks.jar
    artifacts {
        windows {
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
            icon project.file('launcher/icon/windows')
        }
        linux {
            packaging 'targz'
        }
        osx {
            packaging 'targz'
            icon project.file('launcher/icon/osx/program.icns')
        }
    }
}
tasks['assemble'].dependsOn(tasks['launcher'])
```
Kikk i de filene og mappene til matrikkelklienten som er referert over for å se hva som trengs av ekstra oppsett.
