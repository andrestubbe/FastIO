@echo off
echo ðŸš€ Running Hero Demo...
cd examples\target
call mvn -q compile exec:java -Dexec.mainClass=fastio.Demo
cd ..\..
pause
