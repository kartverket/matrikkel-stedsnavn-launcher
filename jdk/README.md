Disse java-VMene er basert på nedlastninger fra:

    https://jdk.java.net/12/

Disse kan egentlig brukes as-is, men er dessverre for store siden de inneholder mange moduler vi ikke trenger.
For java 8 dokumenterte Oracle hvilke filer man evt kunne ta bort, men etter dette blir det gjort annerledes.
Her må man kjøre jlink for å splitte opp. Dette er en kommando som ikke finnes i java 8, og kan derfor ikke kjøres
som del av vårt gradleoppsett enda.

Eksempel:

    ./windows/jdk-12/bin/jlink.exe --module-path windows/jdk-12/jmods --add-modules java.sql,java.desktop,java.naming,java.rmi,java.management,jdk.localedata --include-locales no --output jdk-12-windows/jre
    ./windows/jdk-12/bin/jlink.exe --module-path linux/jdk-12/jmods --add-modules java.sql,java.desktop,java.naming,java.rmi,java.management,jdk.localedata --include-locales no --output jdk-12-linux/jre
    ./windows/jdk-12/bin/jlink.exe --module-path osx/jdk-12.jdk/Contents/Home/jmods --add-modules java.sql,java.desktop,java.naming,java.rmi,java.management,jdk.localedata --include-locales no --output jdk-12-osx/jre

Vi må støtte samisk, og for å garantere at fonten inneholder disse tegnene legger vi ved egen font:

    cp -R fonts jdk-12-windows/jre/lib
    cp -R fonts jdk-12-linux/jre/lib
    cp -R fonts jdk-12-osx/jre/lib

Pakk til slutt sammen til zip:

    zip -r jdk-12-windows.zip jdk-12-windows
    zip -r jdk-12-linux.zip jdk-12-linux
    zip -r jdk-12-osx.zip jdk-12-osx
