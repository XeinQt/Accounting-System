@echo off
cd /d "%~dp0"
echo Compiling HashGen.java...
javac HashGen.java
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Running HashGen...
java HashGen > output.txt 2>&1
type output.txt
echo.
echo Checking for generated file...
if exist admin_insert_hashed.sql (
    echo File created successfully!
    echo.
    echo Contents:
    type admin_insert_hashed.sql
) else (
    echo File was not created. Check output.txt for errors.
    type output.txt
)
pause




