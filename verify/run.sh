#!/bin/bash
# Полнопроектная верификация без Android SDK (нужен kotlinc + java).
# Ступень 1: типовая компиляция ВСЕГО проекта — движки, мозг, разговорник,
#            тема и весь Compose-интерфейс — против рукописных стабов.
# Ступень 2: живой запуск мозга — 26 поведенческих проверок.
set -e
cd "$(dirname "$0")"
WORK=/tmp/tolmach_verify
rm -rf "$WORK"
mkdir -p "$WORK"
for f in $(find ../app/src/main/java -name '*.kt'); do
    python3 strip_commas.py "$f" > "$WORK/$(basename "$f")"
done
python3 strip_commas.py smoke/VerifyBrain.kt > "$WORK/VerifyBrain.kt"
kotlinc stubs/*.kt "$WORK"/*.kt -include-runtime -d "$WORK/tolmach.jar"
echo "Ступень 1 OK: весь проект (включая интерфейс) типов-скомпилирован"
java -jar "$WORK/tolmach.jar"
