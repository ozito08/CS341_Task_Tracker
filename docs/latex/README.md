# Plain English LaTeX documents

Use XeLaTeX (`bash ../../scripts/render_latex.sh`) to build:

- `Project_Report_EN.tex` -> `../Project_Report_EN.pdf`
- `Test_Report_EN.tex` -> `../Test_Report_EN.pdf`
- `User_Guide_EN.tex` -> `../User_Guide_EN.pdf`
- `UML_Diagrams_EN.tex` -> `../UML_Diagrams_EN.pdf`
- `Presentation_EN.tex` -> `../Presentation_EN.pdf` (Beamer)
- `Gap_Audit_EN.tex` -> `../Gap_Audit_EN.pdf`

The shared `style.tex` defines restrained academic formatting, with no colorful callouts. Diagram source files live in `../plantuml/*.puml`; `../diagrams/*` contains the generated black-and-white PlantUML PNG and SVG renders.
