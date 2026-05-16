@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Running Maven tests...
"C:\Users\Admin\.m2\wrapper\dists\apache-maven-3.9.15-bin\4rlcemksed9vjmkvgss0jpc4po\apache-maven-3.9.15\bin\mvn.cmd" test

if %ERRORLEVEL% EQU 0 (
    echo ===== ALL TESTS PASSED =====
) else (
    echo ===== TESTS FAILED =====
)
pause
