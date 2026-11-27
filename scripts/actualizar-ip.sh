#!/usr/bin/env bash
# =====================================================================
# scripts/actualizar-ip.sh — pone en .env la IPv4 local actual (variable IP) y reinicia
# Spring y FastAPI para que los QR y enlaces del pasaporte usen http://IP.
# Úsalo cada vez que cambies de red (otra Wi-Fi, punto de acceso del celular, etc.).
#
#   bash scripts/actualizar-ip.sh                 detecta la IPv4 y la guarda
#   bash scripts/actualizar-ip.sh 192.168.1.50    usa esa IP (si la detección elige otro adaptador)
#   bash scripts/actualizar-ip.sh --sin-reiniciar solo cambia el .env
#
# Funciona en Git Bash (Windows), Linux y macOS. Solo toca la línea IP= del .env
# (la agrega si no existe); no muestra ni cambia las contraseñas.
# =====================================================================
set -Eeuo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${RAIZ}"

info()  { printf '[ip] %s\n' "$*"; }
error() { printf '[ip] ERROR: %s\n' "$*" >&2; exit 1; }

REINICIAR=1; IP_MANUAL=""
for arg in "$@"; do
    case "${arg}" in
        --sin-reiniciar) REINICIAR=0 ;;
        -h|--help) sed -n '2,13p' "${BASH_SOURCE[0]}"; exit 0 ;;
        *) IP_MANUAL="${arg}" ;;
    esac
done

[[ -f .env ]] || error "No existe .env. Créalo primero (README, sección 3)."

# IPv4 del adaptador con salida a Internet (en Windows: el activo con puerta de enlace,
# lo que descarta los adaptadores virtuales de Docker, WSL y Hyper-V).
detectar_ip() {
    if command -v ip >/dev/null 2>&1; then
        ip -4 route get 1.1.1.1 2>/dev/null | awk '{for (i = 1; i < NF; i++) if ($i == "src") { print $(i + 1); exit }}'
    elif command -v powershell.exe >/dev/null 2>&1; then
        powershell.exe -NoProfile -Command "Get-NetIPConfiguration | Where-Object { \$_.IPv4DefaultGateway -and \$_.NetAdapter.Status -eq 'Up' } | Select-Object -First 1 -ExpandProperty IPv4Address | Select-Object -First 1 -ExpandProperty IPAddress" | tr -d '\r'
    elif command -v ipconfig >/dev/null 2>&1; then
        ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null
    fi
}

ip_nueva="${IP_MANUAL:-$(detectar_ip || true)}"
[[ -n "${ip_nueva}" ]] || error "No se pudo detectar la IPv4. Pásala a mano: bash scripts/actualizar-ip.sh 192.168.x.x"
[[ "${ip_nueva}" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]] || error "«${ip_nueva}» no es una IPv4 válida."

ip_anterior=$(grep -m1 '^IP=' .env | cut -d= -f2- | tr -d '\r' || true)

# Reescribe solo la línea IP= (respeta los finales de línea CRLF si el archivo los tiene).
tmp="$(mktemp)"
awk -v ip="${ip_nueva}" '
    { cr = sub(/\r$/, "") ? "\r" : "" }
    /^IP=/ { if (!hecho) print "IP=" ip cr; hecho = 1; next }
    { print $0 cr; ultimo_cr = cr }
    END { if (!hecho) print "IP=" ip ultimo_cr }
' .env > "${tmp}"
cat "${tmp}" > .env      # cat > conserva los permisos del archivo original
rm -f "${tmp}"

info "IP en .env: ${ip_anterior:-(vacía)} -> ${ip_nueva}"

if (( REINICIAR )) && docker compose ps -q spring >/dev/null 2>&1 && [[ -n "$(docker compose ps -q spring 2>/dev/null)" ]]; then
    info "Reiniciando Spring y FastAPI para que tomen la nueva IP..."
    docker compose up -d spring fastapi
    info "Listo. Los QR nuevos usarán http://${ip_nueva} (Spring tarda ~30 s en arrancar)."
else
    info "Listo. Al levantar VetLink (docker compose up -d) los QR usarán http://${ip_nueva}."
fi
info "Desde el celular, conectado a la MISMA red, prueba primero: http://${ip_nueva}/login"
