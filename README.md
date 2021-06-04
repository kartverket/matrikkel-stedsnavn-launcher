# Plugin for gradle

Dette prosjektet lager en plugin til Gradle. Denne pluginen oppretter deretter installere for Windows, Linux og OSX 64-bits.
Etter at installasjonen er fullført, kan man starte en binærfil som viser en login-dialog.
Denne dialogen oppretter kontakt med en webserver som sjekker bruker og passord. Dersom dette er korrekt, laster startprogrammet
ned en klient som deretter startes.

# Versjonering
Ved oppdatering av JDK eller launcher plugin krever dette at man setter ny verdi i launcher.version. 

Ved oppdatering av klient trenger man å deploye ny versjon av war fil for server. Launcher versjon beholdes.


# Eksempel på bruk
Se [matrikkelen](https://bitbucket.statkart.no/projects/MAT/repos/matrikkel/browse/client/client.gradle) for en faktisk implementasjon.
Kikk i de filene og mappene til matrikkelklienten som er referert over for å se hva som trengs av ekstra oppsett.

Her følger et generelt gradle oppsett:
```
import org.apache.commons.compress.archivers.sevenz.SevenZMethod

plugins {
    id 'no.statkart.launcher' version '1.5.0'
}

def keystore = 'launcher/keystore/selfsign.p12'
def keystore_alias = 'selfsign'
def keystore_password = rootProject.file('launcher/keystore/selfsign.txt').text

def devbinBaseUrl = ...
def jvm11Version = 'hotspot_11.x.x_bb'

launcher {
    // Versjonen på klient-starteren, denne representerer en spesifik versjon av JDK og launcher-rammeverk.
    // Verdien må endres når JDK eller rammeverket oppdateres.
    version '2.1.0'
    jvm {
        urlWindows = "$devbinBaseUrl/bin/java/jdk/OpenJDK11U-jdk_x64_windows_$jvm11Version.zip"
        urlLinux = "$devbinBaseUrl/bin/java/jdk/OpenJDK11U-jdk_x64_linux_$jvm11Version.tar.gz"
        urlOsx = "$devbinBaseUrl/bin/java/jdk/OpenJDK11U-jdk_x64_osx_$jvm11Version.tar.gz"
        modules = ['java.sql', 'java.desktop', 'java.naming', 'java.rmi', 'java.management', 'jdk.localedata', 'jdk.jdwp.agent']
        locales = ['nb', 'nn']
    }
    server {
        //must have compatible launcher version 
        oldestAllowedClientVersion '2.0.0' //defaults to launcher.version
        
        webinf project.file('launcher/webinf')
        metainf project.file('launcher/metainf')
        webinfLibs configurations.serverRuntimeClasspath, tasks.serverJar
        classpath configurations.runtimeClasspath, tasks.jar
        getdown project.file('launcher/getdown/server')
    }
    clients {
        'starter-windows' {
            arch 'windows'
            executable 'start'
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
        'starter-windows-plain' {
            arch 'windows'
            executable 'start'
            getdown project.file('launcher/getdown/client')
            packaging 'zip'
        }
        'starter-linux' {
            arch 'linux'
            executable 'start'
            getdown project.file('launcher/getdown/client')
            packaging 'targz'
        }
        'starter-osx' {
            arch 'osx'
            executable 'start'
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

