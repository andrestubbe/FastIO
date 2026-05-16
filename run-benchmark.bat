@echo off
echo Running FastIO Benchmark...
cd examples
mvn exec:java@benchmark
pause
