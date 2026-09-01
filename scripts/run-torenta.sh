#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="${SCRIPT_DIR}/app"

if [ ! -d "${APP_DIR}" ]; then
  echo "Portable layout not found. Expected app/ next to this script."
  exit 1
fi

APP_JAR="$(ls "${APP_DIR}"/*.jar 2>/dev/null | head -n 1)"
if [ -z "${APP_JAR}" ]; then
  echo "No backend jar found in ${APP_DIR}."
  exit 1
fi

exec "${SCRIPT_DIR}/runtime/bin/java" -jar "${APP_JAR}"
