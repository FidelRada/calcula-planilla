#!/usr/bin/env bash
# Conmuta el trafico entre colores. Se ejecuta EN appserver.
set -euo pipefail
COLOR="${1:?uso: switch-traffic.sh blue|green}"
case "$COLOR" in
    blue)  PUERTO=8080 ;;
    green) PUERTO=8081 ;;
    *) echo "color invalido: $COLOR" >&2; exit 2 ;;
esac

echo "server 127.0.0.1:${PUERTO};" | sudo tee /etc/nginx/conf.d/calcula-activo.inc >/dev/null
sudo ln -sfn "/opt/calcula/current-web-$COLOR" /opt/calcula/current-web

sudo nginx -t                # nunca recargar una configuracion invalida
sudo nginx -s reload         # reload, no restart: no corta lo que esta en vuelo

echo "$COLOR" | sudo tee /opt/calcula/color-activo >/dev/null
echo "trafico -> $COLOR (:$PUERTO)"
