# =====================================================================
# scripts/actualizar-ip.ps1 — pone en .env la IPv4 local actual (variable IP) y reinicia
# Spring y FastAPI para que los QR y enlaces del pasaporte usen http://IP.
# Úsalo cada vez que cambies de red (otra Wi-Fi, punto de acceso del celular, etc.).
#
#   powershell -ExecutionPolicy Bypass -File scripts\actualizar-ip.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\actualizar-ip.ps1 -Ip 192.168.1.50
#   powershell -ExecutionPolicy Bypass -File scripts\actualizar-ip.ps1 -SinReiniciar
#
# Solo toca la línea IP= del .env (la agrega si no existe); no muestra ni cambia las contraseñas.
# =====================================================================
param(
    [string]$Ip = "",
    [switch]$SinReiniciar
)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $raiz '.env'
if (-not (Test-Path $envPath)) { throw "No existe .env. Créalo primero (README, sección 3)." }

if (-not $Ip) {
    # Adaptador activo con puerta de enlace: descarta los virtuales de Docker, WSL y Hyper-V.
    $Ip = Get-NetIPConfiguration |
        Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up' } |
        Select-Object -First 1 -ExpandProperty IPv4Address |
        Select-Object -First 1 -ExpandProperty IPAddress
}
if (-not $Ip) { throw "No se pudo detectar la IPv4. Pásala a mano: -Ip 192.168.x.x" }
if ($Ip -notmatch '^(\d{1,3}\.){3}\d{1,3}$') { throw "'$Ip' no es una IPv4 válida." }

$texto = [System.IO.File]::ReadAllText($envPath)
$salto = if ($texto -match "`r`n") { "`r`n" } else { "`n" }
$lineas = [System.Collections.Generic.List[string]]($texto -split "\r?\n")
if ($lineas.Count -gt 0 -and $lineas[$lineas.Count - 1] -eq '') { $lineas.RemoveAt($lineas.Count - 1) }

$anterior = ($lineas | Where-Object { $_ -like 'IP=*' } | Select-Object -First 1)
$anterior = if ($anterior) { $anterior.Substring(3) } else { '' }
$i = $lineas.FindIndex([Predicate[string]] { param($l) $l -like 'IP=*' })
if ($i -ge 0) {
    $lineas[$i] = "IP=$Ip"
    for ($j = $lineas.Count - 1; $j -gt $i; $j--) { if ($lineas[$j] -like 'IP=*') { $lineas.RemoveAt($j) } }
} else {
    $lineas.Add("IP=$Ip")
}
# UTF-8 sin BOM (Docker Compose no acepta el BOM al inicio del .env).
[System.IO.File]::WriteAllText($envPath, (($lineas -join $salto) + $salto), (New-Object System.Text.UTF8Encoding($false)))
Write-Host "[ip] IP en .env: $(if ($anterior) { $anterior } else { '(vacía)' }) -> $Ip"

$corriendo = $false
Push-Location $raiz
try {
    if (-not $SinReiniciar) { $corriendo = [bool](docker compose ps -q spring 2>$null) }
    if ($corriendo) {
        Write-Host "[ip] Reiniciando Spring y FastAPI para que tomen la nueva IP..."
        docker compose up -d spring fastapi
        Write-Host "[ip] Listo. Los QR nuevos usarán http://$Ip (Spring tarda ~30 s en arrancar)."
    } else {
        Write-Host "[ip] Listo. Al levantar VetLink (docker compose up -d) los QR usarán http://$Ip."
    }
} finally { Pop-Location }
Write-Host "[ip] Desde el celular, conectado a la MISMA red, prueba primero: http://$Ip/login"
