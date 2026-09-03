#!/usr/bin/env bash
# Cuenta que instancia responde. Es lo que hace observable el
# Blue-Green: sin esto, un despliegue correcto y uno que no conmuto
# se veen exactamente igual.
set -uo pipefail
N="${1:-20}"
URL="${2:-http://localhost/api/instance}"

for _ in $(seq 1 "$N"); do
    curl -s --max-time 3 "$URL" \
        | grep -o '"instance":"[A-Z]*"' | cut -d'"' -f4 || echo ERROR
done | sort | uniq -c | sort -rn
