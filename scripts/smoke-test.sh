#!/usr/bin/env bash
# E2E minimo contra el color NUEVO, por su puerto directo y ANTES de
# conmutar. Probar por el :80 no diria nada: ahi todavia responde la
# version vieja.
#
# Los valores esperados son los mismos que fija PlanillaServiceTest.
set -uo pipefail
PUERTO="${1:?uso: smoke-test.sh <puerto>}"
BASE="http://localhost:${PUERTO}"
fallos=0

probar() {   # descripcion, url, campo json, valor esperado
    local obtenido
    obtenido=$(curl -fsS --max-time 5 "$2" 2>/dev/null \
               | grep -o "\"$3\":[0-9.]*" | cut -d: -f2 || echo ERROR)
    if [ "$obtenido" = "$4" ]; then
        echo "OK    $1"
    else
        echo "FAIL  $1 - esperado $4, obtenido $obtenido" >&2
        fallos=$((fallos + 1))
    fi
}

probar "liquidar 8500 / 5 anios" "$BASE/api/planilla/liquidar?salario=8500&anios=5" liquidoPagable 7399.82
probar "liquidar 8500 / 4 anios" "$BASE/api/planilla/liquidar?salario=8500&anios=4" liquidoPagable 7274.52
probar "exencion de RC-IVA"      "$BASE/api/planilla/liquidar?salario=2750&anios=0" liquidoPagable 2400.48
probar "aguinaldo 6 meses"       "$BASE/api/planilla/aguinaldo?salario=8500&anios=5&meses=6" montoAguinaldo 4401.25
probar "aguinaldo sin derecho"   "$BASE/api/planilla/aguinaldo?salario=8500&anios=5&meses=2" montoAguinaldo 0.0

# Una entrada invalida debe dar 400, no 500.
CODIGO=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 \
         "$BASE/api/planilla/liquidar?salario=100&anios=0")
if [ "$CODIGO" = "400" ]; then
    echo "OK    entrada invalida devuelve 400"
else
    echo "FAIL  entrada invalida devolvio $CODIGO" >&2
    fallos=$((fallos + 1))
fi

if curl -fsS --max-time 5 "$BASE/api/instance" 2>/dev/null | grep -q '"version"'; then
    echo "OK    /api/instance expone la version"
else
    echo "FAIL  /api/instance" >&2
    fallos=$((fallos + 1))
fi

[ "$fallos" -eq 0 ] || { echo "smoke FALLIDO: $fallos comprobaciones" >&2; exit 1; }
echo "smoke OK"
