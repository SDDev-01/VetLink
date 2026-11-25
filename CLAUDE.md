# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Resumen

VetLink es una plataforma web para clínicas veterinarias: dueños, veterinarios y recepcionistas gestionan mascotas, citas, historia clínica, vacunas, chat, emergencias y un pasaporte de salud con QR. Es un proyecto SENA ADSO de un equipo de 5 personas que trabajan en el mismo repo. Nadie es "el líder" por defecto. Código, commits y documentación van en español.

## Arquitectura (`compose.yaml`)

| Servicio | Tecnología | Puerto |
|---|---|---|
| `mysql` | MySQL 8.4, base compartida | 3306, solo interno |
| `spring` | Spring Boot 4.1.1, Java 21, Maven, Thymeleaf, Spring Security (sesión), JPA | 8080, solo interno |
| `fastapi` | Python 3.12, FastAPI, Uvicorn, PyMySQL, Jinja2, qrcode, ReportLab (PDF) | 8000, solo interno |
| `nginx` | Único punto público (`nginx/nginx.conf.template`) | `${NGINX_HTTP_PORT:-80}` |

Rutas de Nginx:
- `/` → Spring.
- `/p/` → FastAPI (pasaporte público y QR).
- `/api/reportes/` → FastAPI, después de que Spring valide la sesión con `auth_request` en `/internal/auth/reportes`.
- `/internal/...` desde fuera → 404. Nunca debe quedar público.

Spring llama a las rutas `/internal/...` de FastAPI con la cabecera `X-Internal-Token`. Las rutas, los JSON y las columnas que lee FastAPI están en `docs/contrato-documentos.md`. Si el código y el contrato no coinciden, se corrigen los dos en el mismo PR. FastAPI no maneja sesiones ni roles. Si FastAPI se cae, el resto de Spring sigue funcionando.

## Estructura

| Carpeta | Para qué |
|---|---|
| `src/main/java/com/vetlink/` | Spring, organizado por dominio (`cita`, `mascota`, `historial`, `disponibilidad`, `emergencia`, `chat`, `pasaporte`, `veterinaria`, `usuario`…), cada uno con `controller/ model/ repository/ service/ dto/`. Los permisos por URL y rol están en `security/SecurityConfig.java` |
| `src/main/resources/` | `application.properties` (incluye las reglas de negocio `vetlink.reglas.*`) y las plantillas Thymeleaf en `templates/` |
| `src/test/` | Pruebas Spring con H2 (perfil `test`). Las pruebas web extienden `support/PruebaWebBase` |
| `docs-service/` | FastAPI: `app/` (pasaporte, QR, reportes PDF), `tests/` (pytest) y su `Dockerfile` |
| `database/` | `mydb.sql` (esquema base), `migrations/V*.sql`, `init/00-init.sh` (primer arranque), `aplicar_V8.sh` y `aplicar_V9.sh` (para bases que ya existen), `pruebas/` |
| `nginx/` | Plantilla del proxy y su README |
| `docs/` | Contrato Spring ↔ FastAPI |
| `.github/workflows/ci.yml` | Corre las dos suites de pruebas en cada PR |
| `*.md` en la raíz | `REGLAS_NEGOCIO.md`, `PLAN_IMPLEMENTACION.md`, `API_REPORTES.md`, `CAMBIOS_REALIZADOS.md`, `PRUEBAS_RESULTADOS.md`, `VETLINK_CHANGELOG.md` |

## Comandos

No hace falta tener Java, Python ni MySQL instalados. En Windows usa Git Bash (como dice el README) o PowerShell donde se indica. `podman compose` sirve igual que `docker compose`. Con Podman rootless pon `NGINX_HTTP_PORT=8081`.

```bash
docker compose up -d --build          # levantar (necesita un .env; ver README §2)
docker compose ps                     # estado: "Up" no significa "healthy"
docker compose logs -f spring         # o fastapi, mysql, nginx
docker compose up -d --build spring   # recompilar y reiniciar solo Spring
docker compose -f compose.yaml -f compose.debug.yaml up -d --build   # publica 8080, 8000 y MySQL en 127.0.0.1
```

Spring con Maven en contenedor. `mvnw` no tiene bit de ejecución, por eso se usa `sh ./mvnw`:

```bash
# Linux / macOS
docker run --rm -v "$PWD":/app -w /app eclipse-temurin:21-jdk sh ./mvnw -B test
# Git Bash en Windows: sin esta variable, Git Bash convierte /app en una ruta de Windows
MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd -W)":/app -w /app eclipse-temurin:21-jdk sh ./mvnw -B test
# PowerShell
docker run --rm -v "${PWD}:/app" -w /app eclipse-temurin:21-jdk sh ./mvnw -B test
```
Variantes: `sh ./mvnw -B clean package -DskipTests` (solo compilar) y `sh ./mvnw -B test -Dtest='VL30TransicionCitaTest#metodo'` (una prueba).

FastAPI (`docs-service/`, necesita Python 3.12 local, o usar el servicio de Compose):

```bash
cd docs-service
pip install -r requirements-dev.txt
python -m pytest                      # sin DB_HOST se omiten las pruebas marcadas "integracion"
uvicorn app.main:app --reload         # necesita DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, INTERNAL_TOKEN
```
Para las pruebas de integración: levanta solo `mysql` con `compose.debug.yaml` y exporta las variables `DB_*` como lo hace el job `fastapi` de `ci.yml`.

## Reglas de base de datos

- Spring es dueño de las tablas principales. FastAPI es dueño de `pasaporte`, `pasaporte_acceso` y también de `reporte_generado` (la bitácora de reportes). Spring no tiene entidades JPA para esas tres tablas ni permisos sobre ellas.
- Cada servicio usa su propio usuario MySQL con permisos mínimos: `vetlink_spring` y `vetlink_fast`, creados en `00-init.sh`. Las apps nunca usan root. Si FastAPI necesita leer una tabla o columna nueva, hay que agregar el `GRANT` en el init y en la migración.
- Migraciones: scripts SQL versionados en `database/migrations/`, sin Flyway. `00-init.sh` los aplica en orden y solo cuando el volumen de MySQL está vacío. Una migración nueva se agrega a ese script. Para bases que ya existen se hace un `database/aplicar_VN.sh` con respaldo, igual que V8 y V9. No re-ejecutar migraciones a mano.
- `mydb.sql` ya trae casi todo V2–V4, por eso el init no aplica V2–V4. Aplicarlas encima de `mydb.sql` falla.
- Hibernate trabaja con `ddl-auto=validate` en `application.properties` y en Compose. Nunca `update` ni `create`. Toda columna nueva necesita su migración; sin ella Spring no arranca (error `Schema-validation`). La única excepción es el perfil de pruebas: H2 en memoria con `create-drop`.
- Los nombres de columna son camelCase (`PhysicalNamingStrategyStandardImpl`). Todo trabaja en hora `America/Bogota`. En código nuevo usa el `Clock` de `comun/config/RelojConfig`.

## Reglas de trabajo

- Lee los archivos antes de modificarlos. El repo es la fuente de verdad, no la memoria ni la documentación vieja.
- Revisa los logs reales (`docker compose logs <servicio>`) antes de proponer un arreglo.
- Distingue tres estados: el contenedor está arriba (`Up`), el servicio está `healthy` (solo mysql y fastapi tienen healthcheck) y la app funciona de verdad (por ejemplo, Spring muestra "Started VetlinkApplication" y la página responde a través de Nginx; un 502 significa que todavía está arrancando).
- No digas que un test pasó sin haberlo corrido y visto el resultado.
- Haz cambios mínimos y no toques configuración que no tenga relación con la tarea.
- Nunca leas, pidas ni muestres los secretos del `.env`. Eso incluye `docker compose config`, que los imprime.
- Nunca borres volúmenes (`docker compose down -v`) sin explicar antes qué se pierde: toda la base de datos y las fotos y adjuntos subidos (`vetlink_uploads`).
- Avisa si un cambio toca el área de otro integrante: el contrato Spring ↔ FastAPI, las migraciones o los GRANT, Nginx, `compose.yaml`.

## Git

- La rama de integración es `development`. Los PR van con base `development`; `main` solo se usa para entregas. Nadie fusiona su propio PR.
- No sobrescribas trabajo ajeno: nada de `--force` ni `reset --hard` sobre ramas compartidas.
- Revisa el diff (`git diff --staged`) antes de cada commit y di qué archivos toca cada cambio.
- Nunca subas el `.env`.
