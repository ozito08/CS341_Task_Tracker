#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
echo '1/3: Java compilation and password hashing smoke check'
bash scripts/offline_smoke.sh
echo '2/3: independent Python SQLite SQL/schema checks (NOT Java JDBC)'
python3 scripts/sqlite_offline_check.py
echo '3/3: real JUnit + Java SQLite JDBC integration tests'
if ! command -v mvn >/dev/null 2>&1; then
  echo 'NOT VERIFIED: Maven is unavailable. Install Maven and run mvn -B clean verify.' >&2
  if [[ "${1:-}" == '--offline-only' ]]; then
    echo 'Offline-only mode requested: return 0 for checks 1/3 and 2/3 only.'
    exit 0
  fi
  exit 2
fi
mvn -B clean verify
