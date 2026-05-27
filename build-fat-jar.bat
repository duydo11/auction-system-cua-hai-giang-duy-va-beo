@echo off
echo ========================================
echo Building Auction System Fat JARs
echo ========================================
echo.

set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
set PATH=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin;%PATH%

echo [1/2] Cleaning previous builds...
call C:\Users\Admin\.m2\wrapper\dists\apache-maven-3.9.15-bin\4rlcemksed9vjmkvgss0jpc4po\apache-maven-3.9.15\bin\mvn.cmd clean

echo.
echo [2/2] Building fat JARs (this may take a few minutes)...
call C:\Users\Admin\.m2\wrapper\dists\apache-maven-3.9.15-bin\4rlcemksed9vjmkvgss0jpc4po\apache-maven-3.9.15\bin\mvn.cmd -DskipTests package

echo.
echo ========================================
echo Build Complete!
echo ========================================
echo.
echo Server JAR:
echo   server\target\server-1.0-SNAPSHOT-jar-with-dependencies.jar
echo.
echo Client JAR:
echo   client\target\client-1.0-SNAPSHOT-jar-with-dependencies.jar
echo.
echo To run:
echo   1. Start server: java -jar server\target\server-1.0-SNAPSHOT-jar-with-dependencies.jar
echo   2. Start client: java -jar client\target\client-1.0-SNAPSHOT-jar-with-dependencies.jar
echo.
pause
