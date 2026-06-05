@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo [FastIO] Running Demo (via JitPack)...
cd examples\target
call mvn compile exec:java -Dexec.mainClass=fastio.Demo
cd ..\..
pause
