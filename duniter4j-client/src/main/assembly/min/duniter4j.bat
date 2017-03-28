@echo off

set OLDDIR=%CD%
cd /d %~dp0%

set DUNITER4j_BASEDIR="%CD%"
set JAVA_HOME=%DUNITER4j_BASEDIR%\jre
set JAVA_COMMAND=%JAVA_HOME%\bin\java
set DUNITER4j_CONFIG_DIR=%DUNITER4j_BASEDIR%\config

echo "Running Duniter4j Client..."
echo "  basedir: %DUNITER4j_BASEDIR%"
echo " jre home: %JAVA_HOME%"

:start

call duniter4j\launch.bat --option duniter4j.launch.mode full --option duniter4j.basedir %DUNITER4j_BASEDIR% --option config.path %DUNITER4j_CONFIG_DIR%
if errorlevel 88 goto start

goto quit

:quit
cd %OLDDIR%
