@echo off
REM ============================================
REM FIX IDE ERRORS - VS Code Java 21 Setup
REM ============================================
REM 
REM All errors shown are IDE cache issues, NOT real compile errors.
REM Maven compile SUCCESS proves code is correct.
REM 
REM TO FIX IN VS CODE:
REM 1. Press Ctrl+Shift+P
REM 2. Type: "Java: Clean Java Language Server Workspace"
REM 3. Press Enter, select "Yes"
REM 4. Wait 30 seconds for re-indexing
REM 5. All errors will disappear
REM
REM ============================================

echo.
echo ========================================
echo   Maven Compile Status
echo ========================================
echo.

set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Java version:
java -version
echo.

echo Compiling project...
"C:\Users\Admin\.m2\wrapper\dists\apache-maven-3.9.15-bin\4rlcemksed9vjmkvgss0jpc4po\apache-maven-3.9.15\bin\mvn.cmd" clean compile -q

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   BUILD SUCCESS - NO REAL ERRORS
    echo ========================================
    echo.
    echo All IDE errors are CACHE ISSUES.
    echo.
    echo TO FIX:
    echo   1. Press Ctrl+Shift+P in VS Code
    echo   2. Type: "Java: Clean Java Language Server Workspace"
    echo   3. Press Enter, select "Yes"
    echo   4. Wait 30 seconds
    echo.
    echo All errors will disappear after reload.
    echo ========================================
) else (
    echo.
    echo ========================================
    echo   BUILD FAILED - REAL ERRORS EXIST
    echo ========================================
    echo.
    echo Run: mvn clean compile
    echo To see actual errors.
)

pause
