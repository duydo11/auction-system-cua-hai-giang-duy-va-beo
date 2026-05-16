@echo off
REM Set Java 21 path
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

REM Verify Java
echo Checking Java version...
java -version

REM Run Maven
echo.
echo Running Maven clean compile...
"C:\Users\Admin\.m2\wrapper\dists\apache-maven-3.9.15-bin\4rlcemksed9vjmkvgss0jpc4po\apache-maven-3.9.15\bin\mvn.cmd" clean compile

REM Check result
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ===== BUILD SUCCESS =====
    pause
) else (
    echo.
    echo ===== BUILD FAILED =====
    pause
)
