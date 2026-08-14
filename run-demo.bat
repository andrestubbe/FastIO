@echo off
echo [FastIO] Building Native Library...
call compile.bat
if %ERRORLEVEL% NEQ 0 exit /b 1

echo [FastIO] Building Core Project...
call mvn clean install -DskipTests -q
if %ERRORLEVEL% NEQ 0 exit /b 1

echo [FastIO] Running Demo...
cd examples\Demo
call mvn compile exec:java -DskipTests
cd ..\..
pause
