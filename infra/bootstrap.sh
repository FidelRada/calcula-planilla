#!/usr/bin/env bash
# Prepara appserver desde cero. Idempotente: se puede repetir.
set -euo pipefail

echo "==> paquetes"
sudo apt-get update -qq
sudo apt-get install -y -qq nginx curl

echo "==> arbol de directorios"
sudo mkdir -p /opt/calcula/releases /opt/calcula/scripts /etc/calcula
sudo chown -R osboxes:osboxes /opt/calcula

echo "==> unidad systemd y entornos"
sudo cp calcula-api@.service /etc/systemd/system/
sudo cp blue.env green.env  /etc/calcula/
sudo systemctl daemon-reload

echo "==> nginx"
sudo cp calcula.conf /etc/nginx/conf.d/
sudo rm -f /etc/nginx/sites-enabled/default
# Arranca apuntando a BLUE.
echo "server 127.0.0.1:8080;" | sudo tee /etc/nginx/conf.d/calcula-activo.inc >/dev/null
echo "blue" | sudo tee /opt/calcula/color-activo >/dev/null
sudo nginx -t
sudo systemctl enable --now nginx
sudo systemctl reload nginx

echo "==> listo"
