#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/docs/latex"
for base in Project_Report_EN Test_Report_EN User_Guide_EN UML_Diagrams_EN Presentation_EN Gap_Audit_EN; do
  xelatex -interaction=nonstopmode -halt-on-error "$base.tex" > "/tmp/${base}_xelatex.log"
  xelatex -interaction=nonstopmode -halt-on-error "$base.tex" >> "/tmp/${base}_xelatex.log"
  cp "$base.pdf" "../${base}.pdf"
  echo "Compiled docs/${base}.pdf"
done
