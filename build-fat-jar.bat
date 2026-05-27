@echo off
echo ========================================
echo Building Auction System Fat JARs
echo ========================================
echo.

echo Checking Java and Maven...
java -version
where mvn >nul 2>nul
if errorlevel 1 (
    echo.
    echo Maven was not found in PATH.
    echo Please install Maven 3.9+ and add Maven bin directory to PATH.
    pause
    exit /b 1
)

mvn -version

echo.
echo [1/2] Cleaning previous builds...
call mvn clean
if errorlevel 1 goto build_failed

echo.
echo [2/2] Building fat JARs (this may take a few minutes)...
call mvn -DskipTests package
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

:build_failed
echo.
echo Build failed. Please check the error above.
pause
exit /b 1
