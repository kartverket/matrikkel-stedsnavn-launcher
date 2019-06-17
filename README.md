# Plugin for gradle

Dette prosjektet lager en plugin for gradle.  Denne pluginen oppretter deretter installere for windows, linux og osx 64-bits.
Etter at installasjonen er fullført, kan man starte en binærfil som viser en login-dialog.
Denne dialogen oppretter kontakt med en webserver som sjekker bruker og passord.  Dersom dette er korrekt, laster startprogrammet
ned en klient som deretter startes.

# Eksempel på bruk fra matrikkelklientens build.xml:
```
import org.apache.commons.compress.archivers.sevenz.SevenZMethod

buildscript {
    dependencies {
        classpath files('launcher/lib')
        classpath 'no.statkart.launcher:launcher:1.2'
    }
}
apply plugin: 'no.statkart.launcher'
def launcherVersion = '1.2'

def keystore = rootProject.file(System.getenv('keystore') ?: 'launcher/keystore/selfsign.p12') as String
def keystore_alias = System.getenv('keystore_alias') ?: 'selfsign'
def keystore_password = System.getenv('keystore_password') ?: rootProject.file('launcher/keystore/selfsign.txt').text

launcher {
    getdown {
        client 'launcher/getdown/client'
        server 'launcher/getdown/server'
    }
    classpath configurations.runtime
    executable 'matrikkelklient'
    webinf 'launcher/webinf'
    icons 'launcher/program.icns'
    artifakter {
        windows {
            output "Matrikkelklient-${launcherVersion}-installer.exe"
            packaging '7z'
            packagingConfig {
                method SevenZMethod.DEFLATE
                sfx project.file('launcher/7zsd_Deflate_x64.sfx')
                sfxConfig project.file('launcher/7zsd_config.txt')
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
