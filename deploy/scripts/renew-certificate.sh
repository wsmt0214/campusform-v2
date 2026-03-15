#!/bin/sh
set -eu

PROJECT_DIR="${1:-$HOME/campusform-v2}"

cd "$PROJECT_DIR"

docker run --rm \
  -v "$PROJECT_DIR/certbot/www:/var/www/certbot" \
  -v "$PROJECT_DIR/certbot/conf:/etc/letsencrypt" \
  certbot/certbot renew --webroot -w /var/www/certbot --quiet

docker exec campus-nginx nginx -s reload
