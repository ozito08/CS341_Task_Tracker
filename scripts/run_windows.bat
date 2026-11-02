@echo off
setlocal
cd /d "%~dp0.."
if errorlevel 1 (
 echo Could not change to the project directory.
 exit /b 1
)
where java >nul 2>&1
if errorlevel 1 (
 echo Java was not found in PATH. Install a JDK 11 or newer and retry.
 exit /b 1
)
where mvn >nul 2>&1
if errorlevel 1 (
 echo Maven was not found in PATH. Install Maven 3.8 or newer and retry.
 exit /b 1
)
call mvn -B clean verify
if errorlevel 1 (
 echo Maven verification failed. Review the output above.
 exit /b 1
)
if not exist "target\task-tracker-1.0.0.jar" (
 echo The executable JAR was not created.
 exit /b 1
)
echo Starting the packaged Task Tracker. Close the application window to return.
java -jar target\task-tracker-1.0.0.jar
set "APP_EXIT=%ERRORLEVEL%"
if not "%APP_EXIT%"=="0" (
 echo The application exited with code %APP_EXIT%.
 exit /b %APP_EXIT%
)
exit /b 0
