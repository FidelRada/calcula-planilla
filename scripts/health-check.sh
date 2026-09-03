#!/usr/bin/env bash
# Readiness de una instancia. Se ejecuta EN appserver.
set -uo pipefail
PUERTO="${1:?uso: health-check.sh <puerto> [intentos]}"
INTENTOS="${2:-30}"

for i in $(seq 1 "$INTENTOS"); do
    ESTADO=$(curl -fsS --max-time 3 "http://localhost:${PUERTO}/actuator/health" 2>/dev/null \
             | grep -o '"status":"[A-Z]*"' | cut -d'"' -f4 || true)
    if [ "$ESTADO" = "UP" ]; then
        echo "OK    health :$PUERTO responde UP (intento $i)"
        exit 0
    fi
    sleep 2
done

echo "FAIL  health :$PUERTO no respondio UP tras $INTENTOS intentos" >&2
exit 1
