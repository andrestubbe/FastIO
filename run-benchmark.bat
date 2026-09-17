@echo off
setlocal
chcp 65001 > nul
cd /d "%~dp0"

echo ===================================================
echo  Building FastIO Native & Main Project
echo ===================================================
call compile.bat
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Native build failed!
    pause
    exit /b %ERRORLEVEL%
)

call mvn clean install -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] FastIO install failed!
    pause
    exit /b %ERRORLEVEL%
)

echo ===================================================
echo  Building JMH Benchmark Uber-JAR
echo ===================================================
cd examples\Benchmark
call mvn clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Benchmark packaging failed!
    pause
    exit /b %ERRORLEVEL%
)

echo ===================================================
echo  Running JMH Benchmarks (Throughput: ops/ms)
echo ===================================================
java -Djava.library.path=..\..\release;..\..\src\main\resources\native -jar target\benchmarks.jar -f 1 -wi 2 -i 3 -tu ms -bm thrpt
cd ..\..
pause
