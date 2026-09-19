#!/usr/bin/env bash
# Lightweight structure / sanity check for MPorT Tech Pro
set -euo pipefail
cd "$(dirname "$0")/.."

echo "== Package tree =="
find app/src/main/java/com/mporttech/pro -type d | sort

echo
echo "== Kotlin files =="
KT_COUNT=$(find app/src/main/java -name '*.kt' | wc -l | tr -d ' ')
echo "Count: ${KT_COUNT}"
if [[ "${KT_COUNT}" -lt 50 ]]; then
  echo "WARN: expected more Kotlin sources (got ${KT_COUNT})"
fi

echo
echo "== Required entry points =="
REQUIRED=(
  "app/src/main/java/com/mporttech/pro/MainActivity.kt"
  "app/src/main/java/com/mporttech/pro/MPorTTechApplication.kt"
  "app/src/main/AndroidManifest.xml"
  "app/build.gradle.kts"
  "settings.gradle.kts"
  "build.gradle.kts"
)
MISSING=0
for f in "${REQUIRED[@]}"; do
  if [[ -f "$f" ]]; then
    echo "OK  $f"
  else
    echo "MISS $f"
    MISSING=$((MISSING + 1))
  fi
done

echo
echo "== Hilt / DI modules =="
for f in AppModule NetworkModule RepositoryModule DatabaseModule; do
  path=$(find app/src/main/java -name "${f}.kt" | head -1)
  if [[ -n "$path" ]]; then
    echo "OK  $path"
  else
    echo "MISS ${f}.kt"
    MISSING=$((MISSING + 1))
  fi
done

echo
echo "== Navigation screens referenced =="
for name in DashboardScreen LoginScreen AppNavigation NetworkScannerScreen PingScreen; do
  if find app/src/main/java -name "*.kt" -print0 | xargs -0 grep -l "fun ${name}" >/dev/null 2>&1; then
    echo "OK  ${name}"
  else
    echo "MISS ${name}"
    MISSING=$((MISSING + 1))
  fi
done

echo
echo "== Gradle wrapper =="
if [[ -f gradlew ]]; then
  echo "OK  gradlew present"
else
  echo "WARN gradlew missing — CI downloads Gradle manually; local builds need system Gradle"
fi

echo
if [[ "$MISSING" -eq 0 ]]; then
  echo "check-project: PASS"
  exit 0
else
  echo "check-project: FAIL (${MISSING} missing items)"
  exit 1
fi
