@echo off
setlocal
chcp 65001 > nul
cd /d "%~dp0"

echo ===================================================
echo  FastIO Demo
echo ===================================================
echo [1/3] Building Native Library...
call compile.bat
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Native build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/3] Building FastIO Core...
call mvn clean install -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Core build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [3/3] Running Demo...
java -Djava.library.path=release;src\main\resources\native -cp "target\classes;src\main\resources" io.github.andrestubbe.fastio.Demo
pause
