#!/bin/bash
# Двухступенчатая верификация без Android SDK (нужен любой kotlinc + java).
# Ступень 1: типовая компиляция движков и мозга (ViewModel).
# Ступень 2: живой запуск мозга — 26 поведенческих проверок.
set -e
cd "$(dirname "$0")"
WORK=/tmp/tolmach_verify
mkdir -p "$WORK"
SRC="../app/src/main/java/app/tolmach"
for f in "$SRC"/engine/*.kt "$SRC"/TranslatorViewModel.kt "$SRC"/Phrases.kt; do
    python3 strip_commas.py "$f" > "$WORK/$(basename "$f")"
done
cp smoke/VerifyBrain.kt "$WORK/"
kotlinc stubs/*.kt "$WORK"/*.kt -include-runtime -d "$WORK/brain.jar"
echo "Ступень 1 OK: движки + мозг типов-скомпилированы"
java -jar "$WORK/brain.jar"
