@echo off
REM Build script for FastIO native DLL
REM Requires Visual Studio 2019+ with C++ tools

echo Building FastIO native library...

set JAVA_HOME=C:\Program Files\Java\jdk-17
set VC_PATH=C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build

if not exist "%VC_PATH%" (
    set VC_PATH=C:\Program Files\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build
)

if not exist "%VC_PATH%" (
    set VC_PATH=C:\Program Files\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build
)

call "%VC_PATH%\vcvars64.bat"

if not exist "..\..\..\..\..\..\target\native" mkdir "..\..\..\..\..\..\target\native"

cl.exe /O2 /MD /W3 /EHsc /Fe:..\..\..\..\..\..\target\native\fastio.dll ^
    /I "%JAVA_HOME%\include" ^
    /I "%JAVA_HOME%\include\win32" ^
    fastio.cpp ^
    /link /DLL /MACHINE:X64

echo.
echo Build complete: target\native\fastio.dll
pause
