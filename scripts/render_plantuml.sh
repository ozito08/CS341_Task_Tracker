#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
mkdir -p docs/diagrams
if command -v plantuml >/dev/null 2>&1;then
  CMD=(plantuml)
elif [[ -f "${PLANTUML_JAR:-}" ]];then
  CMD=(java -jar "$PLANTUML_JAR")
else
  echo 'PlantUML CLI not installed. Set PLANTUML_JAR to the downloaded PlantUML .jar path.' >&2
  echo 'PlantUML sources are in docs/plantuml/*.puml; current PNG/SVG are fallback Graphviz previews.' >&2
  exit 2
fi
"${CMD[@]}" -charset UTF-8 -tsvg -o ../diagrams docs/plantuml/*.puml
"${CMD[@]}" -charset UTF-8 -tpng -o ../diagrams docs/plantuml/*.puml
printf 'PlantUML rendering complete. Recompile the LaTeX PDFs to use updated images.\n'
