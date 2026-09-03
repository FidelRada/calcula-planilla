#!/usr/bin/env bash
# Despliegue Blue-Green. Cubre los ocho pasos del enunciado, en orden.
#
# Se ejecuta desde fuera de appserver (la VM cicd o el anfitrion) y
# actua por SSH. NO reconstruye nada: descarga el artefacto de la
# Release y verifica que es byte a byte el mismo.
set -euo pipefail

VERSION="${1:?uso: deploy.sh <version>    ej: deploy.sh 1.1.0}"
REPO="${REPO:-FidelRada/calcula-planilla}"
APPSERVER="${APPSERVER:-osboxes@192.168.100.171}"
LLAVE="${LLAVE:-$HOME/.ssh/id_lab5}"
SSH=(ssh -i "$LLAVE" -o BatchMode=yes -o StrictHostKeyChecking=no "$APPSERVER")
R=/opt/calcula

# --- 1. identificar la version y el color destino ---------------------
ACTIVO=$("${SSH[@]}" "cat $R/color-activo 2>/dev/null || echo blue")
if [ "$ACTIVO" = "blue" ]; then DESTINO=green; PUERTO=8081
else                            DESTINO=blue;  PUERTO=8080; fi
echo "==> v$VERSION | activo=$ACTIVO | destino=$DESTINO (:$PUERTO)"

# --- 2. obtener el artefacto DE LA RELEASE ----------------------------
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
gh release download "v$VERSION" -R "$REPO" -D "$TMP" --clobber
JAR=$(ls "$TMP"/calcula-api-*.jar)
SHA_ORIGEN=$(sha256sum "$JAR" | cut -d' ' -f1)
echo "==> sha256 en la Release: ${SHA_ORIGEN:0:16}..."

# --- 3. preparar el ambiente y verificar integridad -------------------
"${SSH[@]}" "mkdir -p $R/releases/$VERSION"
scp -q -i "$LLAVE" -o StrictHostKeyChecking=no "$JAR" "$APPSERVER:$R/releases/$VERSION/calcula-api.jar"
scp -q -i "$LLAVE" -o StrictHostKeyChecking=no "$TMP/calcula-web.tgz" "$APPSERVER:$R/releases/$VERSION/"
"${SSH[@]}" "mkdir -p $R/releases/$VERSION/web && tar -xzf $R/releases/$VERSION/calcula-web.tgz -C $R/releases/$VERSION/web"

SHA_DESTINO=$("${SSH[@]}" "sha256sum $R/releases/$VERSION/calcula-api.jar | cut -d' ' -f1")
if [ "$SHA_ORIGEN" != "$SHA_DESTINO" ]; then
    echo "ERROR: el artefacto desplegado NO es el de la Release" >&2
    exit 1
fi
echo "==> integridad verificada: el jar desplegado es el de la Release"

# --- 4. detener la version anterior EN EL COLOR DESTINO ---------------
# Nunca se toca el color activo: ahi sigue el trafico real.
"${SSH[@]}" "sudo systemctl stop calcula-api@$DESTINO" || true

# --- 5. instalar la nueva version -------------------------------------
"${SSH[@]}" "sudo ln -sfn $R/releases/$VERSION $R/current-$DESTINO && \
             sudo ln -sfn $R/releases/$VERSION/web $R/current-web-$DESTINO"

# --- 6. iniciar la aplicacion -----------------------------------------
"${SSH[@]}" "sudo systemctl start calcula-api@$DESTINO"

# --- 7. verificar ANTES de conmutar -----------------------------------
# Si algo falla aqui, nadie vio la version rota.
if ! "${SSH[@]}" "$R/scripts/health-check.sh $PUERTO" \
|| ! "${SSH[@]}" "$R/scripts/smoke-test.sh  $PUERTO"; then
    echo "==> verificacion FALLIDA: abortando sin tocar el trafico" >&2
    "${SSH[@]}" "sudo systemctl stop calcula-api@$DESTINO" || true
    exit 1
fi

# --- 8. conmutar el trafico e informar --------------------------------
"${SSH[@]}" "$R/scripts/switch-traffic.sh $DESTINO"
echo "==> OK: v$VERSION activa en $DESTINO"
"${SSH[@]}" "$R/scripts/traffic-test.sh 20"
