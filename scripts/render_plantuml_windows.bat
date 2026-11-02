@echo off
setlocal
cd /d "%~dp0.."
if "%PLANTUML_JAR%"=="" (
  echo PlantUML JAR path is missing. Set PLANTUML_JAR=C:\path\to\plantuml.jar first.
  exit /b 2
)
if not exist "%PLANTUML_JAR%" (
  echo PlantUML JAR file not found: "%PLANTUML_JAR%"
  exit /b 2
)
java -jar "%PLANTUML_JAR%" -charset UTF-8 -tsvg -o ..\diagrams docs\plantuml\*.puml
if errorlevel 1 exit /b 1
java -jar "%PLANTUML_JAR%" -charset UTF-8 -tpng -o ..\diagrams docs\plantuml\*.puml
if errorlevel 1 exit /b 1
echo PlantUML files rendered. Run scripts\render_latex_windows.bat to refresh PDFs.
