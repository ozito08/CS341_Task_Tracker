#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn clean verify
java -jar target/task-tracker-1.0.0.jar
