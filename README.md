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

El `.env` guarda las contraseñas. **No se sube a GitHub** (está en `.gitignore`); cada integrante crea el suyo. Desde la carpeta `VetLink`, en **Git Bash**:

```bash
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
EOF
```

O en **PowerShell**:

```powershell
function New-HexSecret([int]$Bytes) {
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $b = New-Object byte[] $Bytes; $rng.GetBytes($b); ([System.BitConverter]::ToString($b) -replace '-', '').ToLowerInvariant() }
    finally { $rng.Dispose() }
}
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
"@
[System.IO.File]::WriteAllText((Join-Path $PWD ".env"), $envContent, (New-Object System.Text.UTF8Encoding($false)))
```

Si prefieres llenarlo a mano, copia `.env.example` como `.env`; ahí está explicada cada variable.

| Variable | Para qué | Obligatoria |
|---|---|---|
| `MYSQL_DATABASE` | Nombre de la base (letras, números y `_`) | Sí |
| `MYSQL_ROOT_PASSWORD` | Contraseña de root. Solo la usan MySQL y el servicio `migrate`; las aplicaciones nunca usan root | Sí |
| `MYSQL_SPRING_PASSWORD`, `MYSQL_FAST_PASSWORD` | Usuarios `vetlink_spring` y `vetlink_fast`. **64 caracteres hexadecimales** (`openssl rand -hex 32`) | Sí |
| `INTERNAL_TOKEN` | Token interno Spring/Nginx → FastAPI | Sí |
| `PUBLIC_BASE_URL` | URL pública de los enlaces y del QR (ver [QR accesible desde Internet](#10-qr-accesible-desde-internet)) | Sí |
| `IP` | IP local del equipo: con `PUBLIC_BASE_URL=http://localhost`, el QR usa `http://IP` | No |
| `NGINX_HTTP_PORT` | Puerto público (80; con Podman rootless, 8081) | No |
| `MYSQL_PORT` | Puerto de MySQL en `127.0.0.1` solo con `compose.debug.yaml` (3307) | No |
| `RUN_DEMO_SEED` | `true` carga los datos de demostración (V6 y pasaportes DEMO) | No (`false`) |
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

- `mydb.sql` trae los catálogos (especies, razas, servicios, ubicaciones…) y los datos base del proyecto.
- Con `RUN_DEMO_SEED=true` se aplican además las migraciones de tipo `demo` (`V6_datos_demo.sql` y `V7_pasaporte_demo.sql`). Se registran en el historial como las demás; si después cambias a `true`, se aplican en la siguiente ejecución. Están escritas para no duplicar filas.

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
| **502 Bad Gateway** | Spring o FastAPI todavía están arrancando. Espera y recarga; si sigue, `docker compose logs spring` |
| "El servicio de pasaportes no está disponible" | FastAPI está caído: `docker compose logs fastapi`. El resto de la aplicación sigue funcionando |
| `the input device is not a TTY` en Git Bash | Agrega `-T` a `docker compose run`/`exec` (los scripts ya lo hacen) |
| Errores raros en los `.sh` en Windows | Clonaste con finales de línea CRLF. Vuelve a clonar (el repositorio obliga a usar LF en los `.sh`) |

## 13. Estructura del repositorio

```
src/                          Aplicación Spring Boot (y sus pruebas en src/test)
docs-service/                 Servicio FastAPI: pasaporte, QR y reportes PDF
database/mydb.sql             Esquema base y datos iniciales (V1)
database/migrations/          Migraciones V5…V11 y datos demo
database/migraciones.txt      Orden de las migraciones (fuente de verdad)
database/permisos_fastapi.sql Permisos mínimos de vetlink_fast
database/verificar_consistencia.sql  Comprobaciones de integridad (solo lectura)
scripts/migrate.sh            Crea o actualiza la base (lo usa el servicio migrate)
scripts/reset-db.sh           Reinicio destructivo de la base de desarrollo
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
