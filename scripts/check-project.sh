#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
echo "== Package tree =="
find app/src/main/java/com/mporttech/pro -type d | sort
echo "== Kotlin files =="
find app/src/main/java -name '*.kt' | wc -l
