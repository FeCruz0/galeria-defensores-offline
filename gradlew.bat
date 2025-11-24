@echo off
rem Gradle wrapper script for Windows

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.

set GRADLE_HOME=%DIRNAME%gradle\wrapper\gradle-wrapper.properties
if not exist "%GRADLE_HOME%" (
    echo "Gradle wrapper properties file not found."
    exit /b 1
)

set WRAPPER_VERSION=
for /f "tokens=2 delims==" %%i in ('findstr /r "^distributionUrl=" "%GRADLE_HOME%"') do (
    set WRAPPER_VERSION=%%i
)

if "%WRAPPER_VERSION%"=="" (
    echo "No Gradle version specified in gradle-wrapper.properties."
    exit /b 1
)

set WRAPPER_VERSION=%WRAPPER_VERSION:~32%
set WRAPPER_VERSION=%WRAPPER_VERSION:/=%

echo "Starting Gradle %WRAPPER_VERSION%..."

java -jar "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" %*