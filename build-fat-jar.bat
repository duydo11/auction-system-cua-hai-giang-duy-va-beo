@echo off
setlocal EnableDelayedExpansion

echo ========================================
echo Building Auction System Fat JARs
echo ========================================
echo.

call :ensure_jdk21
if errorlevel 1 exit /b 1

call :ensure_maven
if errorlevel 1 exit /b 1

echo.
echo Using Java:
java -version
echo.
echo Using Maven:
call mvn -version
if errorlevel 1 goto build_failed

echo.
echo [1/2] Cleaning previous builds...
call mvn clean
if errorlevel 1 goto build_failed

echo.
echo [2/2] Building fat JARs (this may take a few minutes)...
call mvn -Dmaven.test.skip=true package
if errorlevel 1 goto build_failed

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
exit /b 0

:ensure_jdk21
echo Checking JDK 21...
where java >nul 2>nul
if errorlevel 1 (
    echo Java was not found in PATH.
    goto find_jdk21
)

call :read_java_version
if "%JAVA_MAJOR%"=="21" (
    echo JDK 21 is already active.
    exit /b 0
)

echo Current Java is not JDK 21. Detected version: %JAVA_VERSION%

:find_jdk21
echo Trying to find JDK 21 automatically...
set "JDK21_HOME="

for /d %%d in (
    "C:\Program Files\Eclipse Adoptium\jdk-21*"
    "C:\Program Files\Java\jdk-21*"
    "C:\Program Files\Microsoft\jdk-21*"
    "C:\Users\%USERNAME%\.jdks\jdk-21*"
    "C:\Users\%USERNAME%\.jdks\temurin-21*"
    "C:\Users\%USERNAME%\.jdks\openjdk-21*"
) do (
    if exist "%%~d\bin\java.exe" (
        set "JDK21_HOME=%%~d"
        goto use_found_jdk21
    )
)

echo.
echo JDK 21 was not found automatically.
echo.
echo Options:
echo   1. Install JDK 21 automatically with winget
echo   2. I will install JDK 21 manually
echo   3. Exit
echo.
set /p JDK_CHOICE=Choose an option [1/2/3]: 

if "%JDK_CHOICE%"=="1" goto install_jdk21_winget
if "%JDK_CHOICE%"=="2" goto manual_jdk21_help
exit /b 1

:install_jdk21_winget
where winget >nul 2>nul
if errorlevel 1 (
    echo.
    echo winget was not found on this Windows installation.
    goto manual_jdk21_help
)

echo.
echo Installing Eclipse Temurin JDK 21 with winget...
echo This may ask for confirmation or administrator permission.
winget install EclipseAdoptium.Temurin.21.JDK --accept-package-agreements --accept-source-agreements
if errorlevel 1 (
    echo.
    echo JDK 21 installation failed or was cancelled.
    goto manual_jdk21_help
)

echo.
echo JDK 21 installation finished. Searching again...
goto find_jdk21

:manual_jdk21_help
echo.
echo Please install JDK 21, then run this script again.
echo Recommended download:
echo   https://adoptium.net/temurin/releases/?version=21
echo.
echo After installing, open a NEW terminal and check:
echo   java -version
echo.
echo It should show version 21.x, for example:
echo   openjdk version "21.0.11"
echo.
echo If Java still shows another version, set JAVA_HOME/PATH manually, for example:
echo   set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
echo   set PATH=%%JAVA_HOME%%\bin;%%PATH%%
echo   java -version
echo   build-fat-jar.bat
echo.
pause
exit /b 1

:use_found_jdk21
echo Found JDK 21 at:
echo   %JDK21_HOME%
set "JAVA_HOME=%JDK21_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call :read_java_version
if not "%JAVA_MAJOR%"=="21" (
    echo Found Java path did not activate JDK 21 correctly.
    goto manual_jdk21_help
)
echo Switched this terminal session to JDK 21.
exit /b 0

:read_java_version
set "JAVA_VERSION="
set "JAVA_MAJOR="
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VERSION=%%~v"
for /f "tokens=1 delims=." %%m in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%~m"
exit /b 0

:ensure_maven
echo Checking Maven...
where mvn >nul 2>nul
if errorlevel 1 (
    echo.
    echo Maven was not found in PATH.
    echo Please install Maven 3.9+ and add Maven bin directory to PATH.
    echo Download: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)
exit /b 0

:build_failed
echo.
echo Build failed. Please check the error above.
pause
exit /b 1
