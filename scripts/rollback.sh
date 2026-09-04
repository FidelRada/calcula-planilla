#!/usr/bin/env bash
# Vuelve al color anterior. No hay que "recuperar" nada: la version
# anterior nunca se apago, sigue corriendo en el otro color. Esa es
# la propiedad que se compra gastando el doble de memoria.
set -euo pipefail

# La IP de appserver cambia cada vez que el anfitrion se conecta a otra
# WiFi (las VM tienen IP estatica sobre un adaptador puente). Se puede
# sobrescribir sin tocar el codigo:  APPSERVER=osboxes@10.0.0.5 ./deploy.sh 1.1.0
APPSERVER="${APPSERVER:-osboxes@192.168.2.171}"
LLAVE="${LLAVE:-$HOME/.ssh/id_lab5}"
SSH=(ssh -i "$LLAVE" -o BatchMode=yes -o StrictHostKeyChecking=no "$APPSERVER")
R=/opt/calcula

ACTIVO=$("${SSH[@]}" "cat $R/color-activo")
if [ "$ACTIVO" = "blue" ]; then ANTERIOR=green; PUERTO=8081
else                            ANTERIOR=blue;  PUERTO=8080; fi
echo "==> rollback: $ACTIVO -> $ANTERIOR"

# 1. Comprobar que la version anterior sigue sana ANTES de mover trafico.
if ! "${SSH[@]}" "$R/scripts/health-check.sh $PUERTO 5"; then
    echo "ERROR: $ANTERIOR tampoco responde. No se mueve el trafico." >&2
    exit 1
fi

# 2. Restablecer el trafico.
"${SSH[@]}" "$R/scripts/switch-traffic.sh $ANTERIOR"

# 3. Aislar la defectuosa: primero se saco del upstream, ahora se apaga.
"${SSH[@]}" "sudo systemctl stop calcula-api@$ACTIVO" || true

# 4. Verificar. Sin esto el rollback no esta terminado.
"${SSH[@]}" "$R/scripts/traffic-test.sh 20"
echo "==> rollback verificado: trafico en $ANTERIOR"
