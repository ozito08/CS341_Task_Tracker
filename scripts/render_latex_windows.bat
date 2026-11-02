@echo off
setlocal
cd /d "%~dp0..\docs\latex"
if errorlevel 1 (
 echo Could not change to docs\latex.
 exit /b 1
)
for %%N in (Project_Report_EN Test_Report_EN User_Guide_EN UML_Diagrams_EN Presentation_EN Gap_Audit_EN) do (
 xelatex -interaction=nonstopmode -halt-on-error -file-line-error %%N.tex
 if errorlevel 1 exit /b 1
 xelatex -interaction=nonstopmode -halt-on-error -file-line-error %%N.tex
 if errorlevel 1 exit /b 1
 copy /Y %%N.pdf ..\%%N.pdf >nul
 if errorlevel 1 exit /b 1
)
echo All six English PDFs built twice from XeLaTeX.
