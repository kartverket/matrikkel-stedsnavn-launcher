# Plugin for gradle

Dette prosjektet lager en plugin til Gradle. Denne pluginen oppretter deretter installere for Windows, Linux og OSX 64-bits.
Etter at installasjonen er fullført, kan man starte en binærfil som viser en login-dialog.
Denne dialogen oppretter kontakt med en webserver som sjekker bruker og passord. Dersom dette er korrekt, laster startprogrammet
ned en klient som deretter startes.

# Eksempel på bruk fra matrikkelklientens build.xml:
```
import org.apache.commons.compress.archivers.sevenz.SevenZMethod

plugins {
    id 'no.statkart.launcher' version '1.2.2'
}

/**
 * Denne verdien endres når sluttbruker må re-installere klienten.
 * Typisk er dette når man oppdaterer java-vm'en til klienten, eller oppgradere getdown/packr/loginvinduet osv.
 */
ext.launcherVersion = '1.0'

def keystore = rootProject.file(System.getenv('keystore') ?: 'launcher/keystore/selfsign.p12') as String
def keystore_alias = System.getenv('keystore_alias') ?: 'selfsign'
def keystore_password = System.getenv('keystore_password') ?: rootProject.file('launcher/keystore/selfsign.txt').text

launcher {
    jvm {
        modules = ['java.sql', 'java.desktop', 'java.naming', 'java.rmi', 'java.management', 'jdk.localedata']
        locales = ['no']
    }
    getdown {
        client 'launcher/getdown/client'
        server 'launcher/getdown/server'
    }
    classpath configurations.runtime, tasks.jar
    executable 'matrikkelklient'
    metainf 'launcher/metainf'
    webinf 'launcher/webinf'
    webinfLibs configurations.serverRuntime, tasks.serverJar

    icons 'launcher/icons/program.icns'
    windowsIcons 'launcher/lib'
    artifakter {
        windows {
            output "Matrikkelklient-${launcherVersion}-installer.exe"
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
        linux {
            output "matrikkelklient-linux-${launcherVersion}.tar.gz"
            packaging 'targz'
            packagingConfig {
                topDirectory "matrikkelklient-linux-${launcherVersion}"
            }
        }
        osx {
            output "matrikkelklient-osx-${launcherVersion}.zip"
            packaging 'zip'
            packagingConfig {
                topDirectory "matrikkelklient-osx-${launcherVersion}.app"
            }
        }
    }
}
```
