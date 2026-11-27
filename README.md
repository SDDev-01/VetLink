# VetLink

Plataforma web para clínicas veterinarias. Dueños, veterinarios y recepcionistas gestionan mascotas, citas, historia clínica, vacunas, chat y emergencias. El dueño también puede compartir un **pasaporte de salud** con un enlace y un código QR, y descargar reportes clínicos en PDF.

Todo corre con Docker: no necesitas instalar Java, Python ni MySQL.

---

## 1. Tecnologías y servicios

| Servicio | Tecnología | Qué hace | Puerto |
|---|---|---|---|
| `nginx` | Nginx 1.30 | Única entrada desde el navegador. `/p/...` y `/api/reportes/...` van a FastAPI (los reportes, tras validar la sesión con Spring); todo lo demás va a Spring | **`NGINX_HTTP_PORT`** (80 por defecto), el único publicado |
| `spring` | Spring Boot 4.1, Java 21, Thymeleaf, Spring Security, JPA | La aplicación principal | 8080, solo interno |
| `fastapi` | Python 3.12, FastAPI, ReportLab | Pasaporte de salud, QR y reportes PDF | 8000, solo interno |
| `mysql` | MySQL 8.4 | Base de datos compartida | 3306, solo interno |
| `migrate` | MySQL 8.4 (cliente) + `scripts/migrate.sh` | Crea o actualiza la base y **termina**. Corre en cada arranque, antes de Spring y FastAPI | — |

Direcciones (con el puerto por defecto):

| Dirección | Qué es |
|---|---|
| http://localhost | Aplicación |
| http://localhost/p/{token} | Pasaporte público de una mascota |
| http://localhost/api/reportes/health | Estado del servicio de reportes |

## 2. Requisitos

- **Git**. En Windows usa **Git Bash** para todos los comandos de esta guía (salvo donde se indica PowerShell).
- **Docker Desktop** (Windows o Mac) o **Docker Engine** con el plugin Compose (Linux). Comprueba que funciona:
  ```bash
  docker compose version
  ```
- Unos 3 GB libres para las imágenes. También sirve **Podman**; mira [Podman](#podman).

## 3. Variables de entorno (`.env`)

El `.env` guarda las contraseñas. **No se sube a GitHub** (está en `.gitignore`); cada integrante crea el suyo. Los dos scripts detectan solos la **IPv4 local** del equipo (la del adaptador Wi-Fi/Ethernet con salida a Internet) y la guardan en `IP`. Desde la carpeta `VetLink`, en **Git Bash** (Windows) o en la terminal de **Linux/macOS**:

```bash
# IPv4 local: Linux (ip route), Git Bash en Windows (PowerShell) o macOS (ipconfig)
detectar_ip() {
    if command -v ip >/dev/null 2>&1; then
        ip -4 route get 1.1.1.1 2>/dev/null | awk '{for (i = 1; i < NF; i++) if ($i == "src") { print $(i + 1); exit }}'
    elif command -v powershell.exe >/dev/null 2>&1; then
        powershell.exe -NoProfile -Command "Get-NetIPConfiguration | Where-Object { \$_.IPv4DefaultGateway -and \$_.NetAdapter.Status -eq 'Up' } | Select-Object -First 1 -ExpandProperty IPv4Address | Select-Object -First 1 -ExpandProperty IPAddress" | tr -d '\r'
    elif command -v ipconfig >/dev/null 2>&1; then
        ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null
    fi
}
IP_LOCAL=$(detectar_ip)
echo "IPv4 detectada: ${IP_LOCAL:-ninguna (IP quedará vacía)}"

cat > .env <<EOF
MYSQL_DATABASE=vetlink
MYSQL_ROOT_PASSWORD=$(openssl rand -hex 16)
MYSQL_SPRING_PASSWORD=$(openssl rand -hex 32)
MYSQL_FAST_PASSWORD=$(openssl rand -hex 32)
INTERNAL_TOKEN=$(openssl rand -hex 32)
PUBLIC_BASE_URL=http://localhost
NGINX_HTTP_PORT=80
MYSQL_PORT=3307
RUN_DEMO_SEED=true
IP=${IP_LOCAL}
EOF
```

O en **PowerShell**:

```powershell
function New-HexSecret([int]$Bytes) {
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $b = New-Object byte[] $Bytes; $rng.GetBytes($b); ([System.BitConverter]::ToString($b) -replace '-', '').ToLowerInvariant() }
    finally { $rng.Dispose() }
}
# IPv4 local: el adaptador activo con puerta de enlace (descarta los virtuales de Docker/WSL)
$ipLocal = Get-NetIPConfiguration |
    Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up' } |
    Select-Object -First 1 -ExpandProperty IPv4Address |
    Select-Object -First 1 -ExpandProperty IPAddress
Write-Host "IPv4 detectada: $(if ($ipLocal) { $ipLocal } else { 'ninguna (IP quedará vacía)' })"
$envContent = @"
MYSQL_DATABASE=vetlink
MYSQL_ROOT_PASSWORD=$(New-HexSecret 16)
MYSQL_SPRING_PASSWORD=$(New-HexSecret 32)
MYSQL_FAST_PASSWORD=$(New-HexSecret 32)
INTERNAL_TOKEN=$(New-HexSecret 32)
PUBLIC_BASE_URL=http://localhost
NGINX_HTTP_PORT=80
MYSQL_PORT=3307
RUN_DEMO_SEED=true
IP=$ipLocal
"@
[System.IO.File]::WriteAllText((Join-Path $PWD ".env"), $envContent, (New-Object System.Text.UTF8Encoding($false)))
```

**Si cambias de red** (otra Wi-Fi, el punto de acceso del celular, otro router), la IPv4 cambia y los QR dejan de abrir desde el celular. Actualiza solo la línea `IP` con el script (detecta la IP, cambia esa línea y reinicia Spring y FastAPI). **No vuelvas a generar el `.env`**: uno nuevo cambia las contraseñas y MySQL ya no aceptaría las de su volumen.

```bash
bash scripts/actualizar-ip.sh                 # Git Bash (Windows), Linux o macOS
bash scripts/actualizar-ip.sh 192.168.1.50    # si quieres forzar una IP concreta
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts\actualizar-ip.ps1                  # PowerShell
powershell -ExecutionPolicy Bypass -File scripts\actualizar-ip.ps1 -Ip 192.168.1.50 # IP concreta
```

Los pasaportes creados antes del cambio conservan la URL anterior en su QR: genera uno nuevo. Si el celular sigue sin abrirlo:

- El celular y el computador deben estar en la **misma red** (los dos conectados al mismo punto de acceso o Wi-Fi). Prueba primero en el navegador del celular `http://IP/login` con la IP que mostró el script.
- Si **el computador comparte su propio punto de acceso** (Zona con cobertura inalámbrica móvil de Windows) y el celular se conecta a él, el celular llega al computador por `192.168.137.1`: `bash scripts/actualizar-ip.sh 192.168.137.1`.
- Algunos puntos de acceso tienen activado el "aislamiento de clientes" y no dejan que los dispositivos se vean entre sí; en ese caso usa el túnel público ([QR accesible desde Internet](#10-qr-accesible-desde-internet)).

Si prefieres llenarlo a mano, copia `.env.example` como `.env`; ahí está explicada cada variable.

| Variable | Para qué | Obligatoria |
|---|---|---|
| `MYSQL_DATABASE` | Nombre de la base (letras, números y `_`) | Sí |
| `MYSQL_ROOT_PASSWORD` | Contraseña de root. Solo la usan MySQL y el servicio `migrate`; las aplicaciones nunca usan root | Sí |
| `MYSQL_SPRING_PASSWORD`, `MYSQL_FAST_PASSWORD` | Usuarios `vetlink_spring` y `vetlink_fast`. **64 caracteres hexadecimales** (`openssl rand -hex 32`) | Sí |
| `INTERNAL_TOKEN` | Token interno Spring/Nginx → FastAPI | Sí |
| `PUBLIC_BASE_URL` | URL pública de los enlaces y del QR (ver [QR accesible desde Internet](#10-qr-accesible-desde-internet)) | Sí |
| `IP` | IPv4 local del equipo (los scripts de arriba la detectan solos): con `PUBLIC_BASE_URL=http://localhost`, el QR y los enlaces usan `http://IP` para abrirse desde otros dispositivos de la misma red | No |
| `NGINX_HTTP_PORT` | Puerto público (80; con Podman rootless, 8081) | No |
| `MYSQL_PORT` | Puerto de MySQL en `127.0.0.1` solo con `compose.debug.yaml` (3307) | No |
| `RUN_DEMO_SEED` | `true` carga los datos de demostración (V6, pasaportes DEMO y las 20 veterinarias de V13) | No (`false`) |
| `VETLINK_ENTORNO` | `desarrollo` o `produccion` (exige HTTPS en la URL pública) | No |
| `PASAPORTE_DETALLE_CLINICO` | `true` muestra diagnósticos en el pasaporte público | No (`false`) |

`scripts/migrate.sh` valida estas variables antes de tocar la base y se detiene con un mensaje claro si falta alguna o si las contraseñas de aplicación no tienen el formato correcto.

Las reglas de negocio (anticipación, expiración de reservas, horizonte de agenda, tamaños de archivo…) están en `src/main/resources/application.properties` bajo `vetlink.reglas.*`; ver `REGLAS_NEGOCIO.md`.

## 4. Instalación desde cero

```bash
git clone https://github.com/SDDev-01/VetLink.git
cd VetLink
git checkout development
# crea el .env (sección 3)
docker compose up -d --build
```

La primera vez tarda varios minutos (descarga imágenes y compila Spring). El arranque sigue este orden:

1. `mysql` arranca con un volumen vacío y queda `healthy`.
2. `migrate` crea las tablas, carga los datos iniciales, crea los usuarios de aplicación con sus permisos y termina con código 0.
3. `spring` y `fastapi` arrancan **solo si `migrate` terminó bien**.
4. `nginx` publica la aplicación.

Para seguir el progreso:

```bash
docker compose ps -a                   # migrate: "Exited (0)"; mysql y fastapi: "healthy"
docker compose logs migrate            # qué migraciones aplicó
docker compose logs -f spring          # listo cuando aparece "Started VetlinkApplication"
```

Abre **http://localhost**. Mientras Spring termina de arrancar, Nginx puede responder **502**; espera unos segundos y recarga.

## 5. Migraciones de la base de datos

### Cómo funciona

- **Fuente de verdad del esquema:** `database/migraciones.txt` lista en orden los scripts SQL. `database/mydb.sql` es la base (V1) y los archivos de `database/migrations/` la modifican. Hibernate solo **valida** el esquema (`ddl-auto=validate`): nunca crea ni cambia tablas.
- **Historial:** la tabla `vetlink_migracion` guarda qué migraciones se aplicaron, cuándo, cuánto tardaron y el checksum del archivo. Cada migración se ejecuta **una sola vez**.
- **Sin ejecuciones simultáneas:** un bloqueo en la tabla `vetlink_migracion_bloqueo` hace que una segunda ejecución espere a que termine la primera.
- **Errores:** si una migración falla, el script se detiene con código 1, no la registra y no aplica las siguientes. Spring y FastAPI no arrancan.
- **Respaldo automático:** antes de aplicar migraciones sobre una base que ya tiene datos, guarda un respaldo completo en `respaldos/`.
- **Permisos:** en cada ejecución crea o actualiza `vetlink_spring` y `vetlink_fast` (con las contraseñas del `.env`) y les da permisos mínimos. Spring recibe permisos sobre las tablas principales; FastAPI, los de `database/permisos_fastapi.sql` y `database/migrations/V8_permisos_fastapi.sql`.
- **Comprobaciones:** al final ejecuta `database/verificar_consistencia.sql` (registros huérfanos, duplicados, reservas abiertas de mascotas dadas de baja…). Solo avisa; no modifica nada.

### Automática

Ocurre sola en cada `docker compose up`. Si no hay nada pendiente, no cambia nada.

### Manual

```bash
bash scripts/migrate.sh             # aplica las pendientes (inicia MySQL si hace falta)
bash scripts/migrate.sh --estado    # muestra aplicadas, pendientes y omitidas, sin cambiar nada
```

El script se puede ejecutar todas las veces que quieras: una segunda ejecución no aplica nada ni duplica datos. Por dentro lanza el servicio `migrate` con `docker compose run`, así que no necesitas el cliente de MySQL en tu equipo.

### Datos iniciales (seeds)

- `mydb.sql` trae los catálogos (especies, razas, servicios, ubicaciones…) y los datos base del proyecto, con estas cuentas (contraseña **`123456`**):

  | Rol | Correo |
  |---|---|
  | Dueño | `maria@gmail.com` |
  | Veterinario | `juan@gmail.com` |
  | Recepcionista | `laura@gmail.com` |

- Con `RUN_DEMO_SEED=true` se aplican además las migraciones de tipo `demo`. Se registran en el historial como las demás; si después cambias a `true`, se aplican en la siguiente ejecución. Están escritas para no duplicar filas:
  - `V6_datos_demo.sql`: corrige textos de los datos base para la demostración.
  - `V7_pasaporte_demo.sql`: pasaportes de ejemplo (tokens `DEMO-…`).
  - `V13_veterinarias_directorio.sql`: **20 veterinarias más** para el directorio (12 en Bogotá; las demás en Medellín, Cali, Barranquilla, Bucaramanga, Cartagena y Pereira), cada una con ubicación en el mapa, teléfono, correo, horario y servicios con precio. No traen personal, así que aparecen con "Sin cupos publicados por ahora". Busca ciudades, barrios y servicios por nombre, por eso funciona en cualquier base del equipo.
- Los **datos de prueba de 500 registros** (32 cuentas) no son una migración: se cargan a mano con `scripts/semilla-pruebas.sh` (ver [Datos de prueba](#datos-de-prueba-500-registros)).

### Agregar una migración nueva

1. Crea `database/migrations/V<N>_<descripcion>.sql`. De preferencia idempotente (comprobando en `information_schema` antes de cada `ALTER`, como `V10`).
2. Añade una línea **al final** de `database/migraciones.txt` con un id nuevo.
3. Si agregas columnas, actualiza la entidad JPA en el mismo PR: sin la migración, Spring no arranca (`Schema-validation`).
4. Si FastAPI debe leer una tabla o columna nueva, agrega el `GRANT` en `database/permisos_fastapi.sql` y actualiza `docs/contrato-documentos.md`.
5. Prueba con `bash scripts/migrate.sh`.

No edites una migración ya aplicada: el script avisa si su checksum cambió, pero no la vuelve a ejecutar. Para corregirla, crea otra.

## 6. Actualizar una instalación existente

Para traer cambios del equipo **sin perder datos**:

```bash
git pull
docker compose up -d --build
```

`migrate` aplica solo lo pendiente (con respaldo previo en `respaldos/`) y después arrancan Spring y FastAPI con el código nuevo.

**Bases creadas antes de existir el historial** (con el antiguo `database/init/00-init.sh` o los `database/aplicar_V*.sh`): la primera vez, `migrate` las «adopta». Detecta qué migraciones ya tenían, las registra como `ADOPTADA` sin volver a ejecutarlas y aplica solo las que faltan. No hace falta borrar nada. Los scripts `database/aplicar_V8.sh`, `aplicar_V9.sh` y `aplicar_V10.sh` quedan obsoletos.

## 7. Reinicio completo de la base de desarrollo (destructivo)

> **Borra todos los datos de la base**: usuarios, mascotas, citas, historias… Solo para desarrollo. Nunca forma parte del arranque ni de una actualización.

```bash
bash scripts/reset-db.sh                    # pide escribir BORRAR para confirmar
```

El script:

1. Identifica el volumen de MySQL **de este proyecto** por las etiquetas de Compose. No toca volúmenes de otros proyectos.
2. Guarda un respaldo completo en `respaldos/antes-de-reset-<fecha>.sql`. Si no puede respaldar, se detiene sin borrar nada.
3. Detiene los servicios y borra el volumen de MySQL.
4. Ejecuta `docker compose up -d --build`, y `migrate` crea la base desde cero.

Opciones: `--si` (sin preguntar), `--incluir-uploads` (borra también las fotos y adjuntos subidos; por defecto se conservan), `--sin-respaldo` y `--sin-levantar`. Se niega a correr si `.env` tiene `VETLINK_ENTORNO=produccion`.

Evita `docker compose down -v`: borra a la vez la base **y** las fotos y adjuntos (`vetlink_uploads`), sin respaldo.

### Datos de prueba (500 registros)

`scripts/semilla-pruebas.sh` deja la base de desarrollo con **exactamente 500 registros** realistas (contexto colombiano, mayoría en Bogotá) repartidos entre todas las tablas, con 32 cuentas de prueba. **No es una migración**: cada integrante que quiera estos datos debe ejecutarlo en su computador. Si levantas el proyecto en otra máquina, esa base solo tiene las cuentas de `mydb.sql` (sección 5) hasta que corras el script.

> **Destructivo:** vacía **todas** las tablas de la base (también las 20 veterinarias de V13, que no se vuelven a crear solas). No cambia la estructura ni el historial de migraciones. Respalda antes en `respaldos/` y se niega a correr con `VETLINK_ENTORNO=produccion`.

Ejecútalo en **Git Bash** (no en PowerShell ni dentro de un contenedor), con Docker Desktop encendido y desde la carpeta del proyecto:

```bash
bash scripts/semilla-pruebas.sh              # vacía las tablas y carga los 500 registros (pide escribir BORRAR)
bash scripts/semilla-pruebas.sh --si         # igual, sin preguntar
bash scripts/semilla-pruebas.sh --reset --si # además recrea la base desde cero con reset-db.sh (borra el volumen de MySQL)
bash scripts/semilla-pruebas.sh --verificar  # solo comprueba, no cambia nada
```

Al terminar muestra el conteo por tabla y debe decir `OK: exactamente 500`. El script levanta los servicios solo; espera a que Spring muestre "Started VetlinkApplication" (~1 minuto) y entra.

Cuentas de prueba (contraseña **`123456V`**, con V mayúscula):

| Rol | Correos | Cantidad |
|---|---|---|
| Dueño | `1@gmail.com` … `10@gmail.com` | 10 |
| Veterinario | `100@gmail.com` … `110@gmail.com` | 11 |
| Recepcionista | `1000@gmail.com` … `1010@gmail.com` | 11 |

Qué hace, en orden:

1. (Con `--reset`) respalda y recrea la base con `scripts/reset-db.sh` y aplica las migraciones.
2. Detiene Spring y FastAPI para que sus tareas programadas no escriban durante la carga.
3. Vacía las tablas (los ID vuelven a empezar en 1) y carga los datos que genera `database/pruebas/semilla_500.py`. Corre en un contenedor `python:3.12-slim`: no hace falta Python en el equipo.
4. Copia al volumen de uploads los PDF adjuntos de las historias clínicas.
5. Levanta los servicios y ejecuta las comprobaciones de `database/pruebas/verificar_semilla_500.sql`: total exacto, ID desde 1, roles, mascotas con `estado = 1`, relaciones e integridad.

Los datos incluyen 4 clínicas (3 en Bogotá, 1 en Medellín), 16 mascotas, citas pasadas, presentes y futuras (atendidas, confirmadas, pendientes, canceladas…), historias clínicas con diagnóstico y tratamiento, vacunas, emergencias, chat, reseñas, pasaportes y reportes. Las fechas se calculan desde el momento en que corres el script.

El total de 500 es el estado justo después de cargar. La aplicación lo cambia con el uso normal: cada noche genera los cupos del nuevo día de la agenda, las reservas pendientes vencen a las ~24 h, y navegar o generar PDF agrega registros (auditoría, reportes). Para volver a 500, ejecuta el script otra vez.

## 8. Respaldos

- Se crean solos en `respaldos/` (ignorada por Git): antes de cada migración sobre una base con datos y antes de cada reinicio.
- Respaldo manual:
  ```bash
  mkdir -p respaldos
  docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump --single-transaction --routines -uroot "$MYSQL_DATABASE"' > respaldos/manual-$(date +%Y%m%d-%H%M%S).sql
  ```
- **Restaurar un respaldo** (reemplaza las tablas actuales):
  ```bash
  docker compose stop spring fastapi
  docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE"' < respaldos/<archivo>.sql
  docker compose up -d        # migrate aplica lo que le falte a ese respaldo
  ```

## 9. Uso diario

| Quiero… | Comando |
|---|---|
| Encender | `docker compose up -d` |
| Apagar (los datos se conservan) | `docker compose down` |
| Reiniciar un servicio | `docker compose restart spring` (o `fastapi`, `nginx`, `mysql`) |
| Aplicar cambios de código tras un `git pull` | `docker compose up -d --build` |
| Recompilar solo Spring | `docker compose up -d --build spring` |
| Ver el estado | `docker compose ps -a` |
| Ver logs | `docker compose logs -f spring` (o `fastapi`, `mysql`, `nginx`, `migrate`) |
| Ver migraciones pendientes | `bash scripts/migrate.sh --estado` |
| Comprobar la consistencia de los datos | `docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -t "$MYSQL_DATABASE"' < database/verificar_consistencia.sql` |

Apagar, reiniciar o recrear los contenedores **no borra datos**: la base vive en el volumen `mysql_data` y las fotos en `vetlink_uploads`. Al volver a encender, `migrate` comprueba que no haya nada pendiente y termina.

### Depurar con los puertos internos abiertos

Solo para desarrollo, nunca para la demostración. Publica Spring (8080), FastAPI (8000) y MySQL (`MYSQL_PORT`, por defecto 3307) en `127.0.0.1`:

```bash
docker compose -f compose.yaml -f compose.debug.yaml up -d --build
```

Así puedes conectarte a MySQL desde DBeaver o Workbench en `127.0.0.1:3307` y ver la documentación de FastAPI en http://localhost:8000/docs.

### Podman

Los mismos comandos sirven cambiando `docker compose` por `podman compose`. En Linux con Podman *rootless* no se puede publicar el puerto 80: usa `NGINX_HTTP_PORT=8081` y `PUBLIC_BASE_URL=http://localhost:8081`. El arranque ordenado necesita un proveedor de Compose que soporte `condition: service_completed_successfully` (docker-compose v2 lo soporta).

## 10. QR accesible desde Internet

El QR y los enlaces del pasaporte usan `PUBLIC_BASE_URL`. Con `http://localhost` o una IP privada (`192.168.x.x`), el QR **solo abre en este equipo o en la misma Wi-Fi**. La aplicación lo advierte en «Compartir historial» y en `docker compose logs fastapi`.

1. **Producción**: publica Nginx con un dominio y HTTPS, y pon `PUBLIC_BASE_URL=https://tu-dominio` y `VETLINK_ENTORNO=produccion`.
2. **Pruebas sin dominio (túnel HTTPS gratuito)**:
   ```bash
   docker compose --profile tunel up -d tunel
   docker compose logs tunel | grep trycloudflare   # copia la URL https://....trycloudflare.com
   # pon esa URL en PUBLIC_BASE_URL dentro de .env y luego:
   docker compose up -d fastapi spring
   ```
   La URL del túnel rápido cambia cada vez que se reinicia `tunel`; los QR creados con una URL anterior dejan de abrir.
3. **Solo misma Wi-Fi**: `IP=<IP del portátil>` en `.env`.

Por privacidad, la página pública del pasaporte no muestra diagnósticos ni medicamentos (`PASAPORTE_DETALLE_CLINICO=false`).

## 11. Pruebas

GitHub Actions corre las dos suites en cada Pull Request (`.github/workflows/ci.yml`); el job de FastAPI también ejecuta las migraciones **dos veces** para comprobar que repetirlas no falla ni duplica nada. Resultados de la última ejecución: `PRUEBAS_RESULTADOS.md`.

**Spring** (perfil de pruebas con H2, no necesita MySQL):

```bash
# Git Bash en Windows (sin MSYS_NO_PATHCONV, Git Bash convierte /app en una ruta de Windows)
MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd -W)":/app -w /app -e TZ=America/Bogota eclipse-temurin:21-jdk sh ./mvnw -B test
# Linux / macOS
docker run --rm -v "$PWD":/app -w /app -e TZ=America/Bogota eclipse-temurin:21-jdk sh ./mvnw -B test
# Una sola clase
MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd -W)":/app -w /app -e TZ=America/Bogota eclipse-temurin:21-jdk sh ./mvnw -B test -Dtest='VL37MascotasDelDuenioEnTodasLasVistasTest'
```

**FastAPI** (unitarias e integración, contra la base de Compose; necesita los contenedores encendidos). Las pruebas de integración escriben filas en `reporte_generado`:

```bash
MSYS_NO_PATHCONV=1 docker run --rm --network vetlink_vetlink_net --env-file .env -v "$(pwd -W)/docs-service":/app -w /app python:3.12-slim \
  sh -c 'pip install -q -r requirements-dev.txt && DB_HOST=mysql DB_PORT=3306 DB_NAME="$MYSQL_DATABASE" DB_USER=vetlink_fast DB_PASSWORD="$MYSQL_FAST_PASSWORD" DB_ADMIN_USER=root DB_ADMIN_PASSWORD="$MYSQL_ROOT_PASSWORD" python -m pytest -q'
```

(En Linux/macOS usa `"$PWD/docs-service"` en lugar de `"$(pwd -W)/docs-service"`. La red se llama `<carpeta>_vetlink_net`; compruébalo con `docker network ls`).

## 12. Problemas frecuentes

| Síntoma | Solución |
|---|---|
| `port 80 is already allocated` | En `.env`, pon `NGINX_HTTP_PORT=8081` y `PUBLIC_BASE_URL=http://localhost:8081`; luego `docker compose up -d` y abre http://localhost:8081 |
| `service "migrate" didn't complete successfully: exit 1` | Una migración falló o falta una variable. Mira `docker compose logs migrate`: el mensaje dice cuál y qué hacer. En desarrollo, `bash scripts/reset-db.sh` recrea la base |
| `migrate` dice «las contraseñas deben tener 64 caracteres hexadecimales» | Corrige `MYSQL_SPRING_PASSWORD` / `MYSQL_FAST_PASSWORD` en `.env` (`openssl rand -hex 32`) y vuelve a `docker compose up -d`. No hace falta borrar la base: `migrate` actualiza las contraseñas de los usuarios |
| `migrate` espera con «Otra ejecución está migrando» | Hay otra ejecución en curso; termina sola. Un bloqueo abandonado se libera a los 30 minutos |
| Aviso «el archivo cambió después de aplicarse» | Alguien editó una migración ya aplicada. No se vuelve a ejecutar; si el cambio era de esquema, hay que crear una migración nueva |
| Spring se reinicia con `Schema-validation` | Falta una migración para una columna nueva de una entidad. Créala (sección 5) |
| `mysql` nunca queda `healthy` | `docker compose logs mysql`. Revisa `MYSQL_ROOT_PASSWORD` y el espacio en disco |
| En otra computadora no puedo entrar con `1@gmail.com` / `123456V` | Esas cuentas solo existen donde se corrió `scripts/semilla-pruebas.sh`. Entra con las de `mydb.sql` (`maria@`, `juan@`, `laura@gmail.com`, contraseña `123456`) o ejecuta el script en esa computadora ([Datos de prueba](#datos-de-prueba-500-registros)) |
| Ya no aparecen las 20 veterinarias nuevas | Se corrió `scripts/semilla-pruebas.sh`, que vacía todas las tablas. V13 ya está registrada y no se vuelve a aplicar sola |
| **502 Bad Gateway** | Spring o FastAPI todavía están arrancando. Espera y recarga; si sigue, `docker compose logs spring` |
| "El servicio de pasaportes no está disponible" | FastAPI está caído: `docker compose logs fastapi`. El resto de la aplicación sigue funcionando |
| `the input device is not a TTY` en Git Bash | Agrega `-T` a `docker compose run`/`exec` (los scripts ya lo hacen) |
| Errores raros en los `.sh` en Windows | Clonaste con finales de línea CRLF. Vuelve a clonar (el repositorio obliga a usar LF en los `.sh`) |

## 13. Estructura del repositorio

```
src/                          Aplicación Spring Boot (y sus pruebas en src/test)
docs-service/                 Servicio FastAPI: pasaporte, QR y reportes PDF
database/mydb.sql             Esquema base y datos iniciales (V1)
database/migrations/          Migraciones V5…V13 y datos demo (V13: 20 veterinarias)
database/migraciones.txt      Orden de las migraciones (fuente de verdad)
database/permisos_fastapi.sql Permisos mínimos de vetlink_fast
database/verificar_consistencia.sql  Comprobaciones de integridad (solo lectura)
database/pruebas/             Generador y comprobaciones de los datos de prueba (500 registros)
scripts/migrate.sh            Crea o actualiza la base (lo usa el servicio migrate)
scripts/reset-db.sh           Reinicio destructivo de la base de desarrollo
scripts/semilla-pruebas.sh    Carga los datos de prueba (destructivo)
scripts/actualizar-ip.sh/.ps1 Pone en .env la IPv4 actual (QR del pasaporte)
nginx/                        Configuración del proxy inverso
docs/                         Contrato entre Spring y FastAPI
compose.yaml                  Los servicios
.env.example                  Plantilla de variables (sin secretos)
REGLAS_NEGOCIO.md, API_REPORTES.md, PLAN_IMPLEMENTACION.md, CAMBIOS_REALIZADOS.md, PRUEBAS_RESULTADOS.md
```

## 14. Cómo trabajamos con Git

La rama de integración es **`development`**. `main` solo se actualiza para las entregas.

```bash
git checkout development
git pull origin development
git checkout -b feat/mi-tarea          # o fix/..., docs/..., style/...
# ...trabajar...
git add <archivos>
git commit -m "feat: qué hiciste"
git push -u origin feat/mi-tarea
```

En GitHub, abre el Pull Request con **base: `development`**. Otra persona lo revisa y lo fusiona: nadie fusiona su propio PR.

- **Nunca subas el `.env`** ni contraseñas.
- Ramas pequeñas, una por tarea, con mensajes de commit claros.
- Si Git te da un error que no entiendes, pregunta en el grupo antes de forzar algo (`--force`, `reset --hard`).
- (hola gente, es solo para poder subir el commit :v)