# VetLink

Plataforma para clínicas veterinarias: dueños, veterinarios y recepcionistas gestionan mascotas, citas, historia clínica, vacunas, chat y emergencias. El dueño también puede compartir un **pasaporte de salud** con un enlace y un código QR.

Todo se levanta con un solo comando de Docker. No necesitas instalar Java, Python ni MySQL.

| Contenedor | Qué hace | Puerto |
|---|---|---|
| `nginx` | Única entrada desde el navegador. `/p/...` va a FastAPI y todo lo demás a Spring | **80** (el único publicado) |
| `spring` | La aplicación principal (Spring Boot 4, Java 21, Thymeleaf) | 8080, solo interno |
| `fastapi` | El pasaporte de salud y su QR (Python 3.12) | 8000, solo interno |
| `mysql` | La base `mydb` (MySQL 8.4). Se crea sola la primera vez | 3306, solo interno |

---

## 1. Requisitos

- **Git**. En Windows usa **Git Bash** para todos los comandos de esta guía.
- **Docker Desktop** (Windows o Mac) o **Docker Engine** con el plugin Compose (Linux). Comprueba que funciona:
  ```bash
  docker compose version
  ```
  También sirve **Podman** (`podman compose ...`); mira la sección [Podman](#podman).

## 2. Primera vez

```bash
git clone https://github.com/SDDev-01/VetLink.git
cd VetLink
git checkout development
```

### Crear el archivo `.env`

El `.env` guarda las contraseñas. **No se sube a GitHub** (está en `.gitignore`); cada uno crea el suyo. Pega esto en Git Bash o en la terminal, desde la carpeta `VetLink`:

```bash
cat > .env <<EOF
MYSQL_DATABASE=mydb
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

Si prefieres llenarlo a mano, copia `.env.example` como `.env`. Ahí está explicada cada variable. Las dos contraseñas de aplicación (`MYSQL_SPRING_PASSWORD` y `MYSQL_FAST_PASSWORD`) **deben** tener 64 caracteres hexadecimales; si no, MySQL no arranca.

### Levantar todo

```bash
docker compose up -d --build
```

La primera vez tarda varios minutos: descarga las imágenes, compila Spring y crea la base. Para ver cómo va:

```bash
docker compose ps              # los 4 servicios deben estar "Up" (mysql y fastapi "healthy")
docker compose logs -f spring  # listo cuando aparece "Started VetlinkApplication"
```

Abre **http://localhost** en el navegador. Mientras Spring termina de arrancar, Nginx puede responder **502**; espera unos segundos y recarga.

## 3. Uso diario

| Quiero… | Comando |
|---|---|
| Encender | `docker compose up -d` |
| Apagar (los datos se conservan) | `docker compose down` |
| Ver logs de un servicio | `docker compose logs -f spring` (o `fastapi`, `mysql`, `nginx`) |
| Aplicar cambios de código tras un `git pull` | `docker compose up -d --build` |
| **Borrar la base y crearla de cero** | `docker compose down -v` y luego `docker compose up -d --build` |

Los scripts de `database/` **solo corren cuando el volumen de MySQL está vacío**. Si alguien agrega o cambia un `.sql`, tienes que ejecutar `docker compose down -v`; si no, tu base seguirá con la versión vieja. Ojo: `down -v` también borra las fotos subidas.

## 4. Probar el pasaporte desde un celular

1. Averigua la IP de tu computador en la red Wi-Fi (`ipconfig` en Windows; `ip addr` o `ifconfig` en Linux o Mac), por ejemplo `192.168.1.50`.
2. En `.env`, pon `PUBLIC_BASE_URL=http://192.168.1.50` (agrega `:8081` si cambiaste el puerto).
3. Ejecuta `docker compose up -d` para aplicarlo.
4. Entra como dueño, abre una mascota, pulsa **Compartir historial** y escanea el QR con el celular, que debe estar en la misma red.

Si la red no deja que el celular vea tu computador, abre el enlace del pasaporte en una ventana de incógnito.

## 5. Problemas frecuentes

| Síntoma | Solución |
|---|---|
| `port 80 is already allocated`, o el puerto 80 no se puede usar | En `.env`, pon `NGINX_HTTP_PORT=8081` y `PUBLIC_BASE_URL=http://localhost:8081`, ejecuta `docker compose up -d` y abre http://localhost:8081 |
| `mysql` nunca queda `healthy` | `docker compose logs mysql`. Si dice que las contraseñas deben tener 64 caracteres hexadecimales, corrige el `.env` y ejecuta `docker compose down -v` |
| Spring se reinicia y en los logs aparece `Schema-validation` | Tu base es de una versión anterior: `docker compose down -v` y `docker compose up -d --build` |
| **502 Bad Gateway** | Spring o FastAPI todavía están arrancando. Espera y recarga; si sigue, revisa `docker compose logs spring` |
| "El servicio de pasaportes no está disponible" | FastAPI está caído: `docker compose logs fastapi`. El resto de la aplicación sigue funcionando |
| En Windows, errores raros en `00-init.sh` | Clonaste con finales de línea de Windows. Borra la carpeta y vuelve a clonar (el repositorio ya obliga a usar LF en los `.sh`) |

### Podman

Los mismos comandos sirven cambiando `docker compose` por `podman compose`. En Linux con Podman *rootless* no se puede publicar el puerto 80: usa `NGINX_HTTP_PORT=8081` (como en la tabla anterior).

### Depurar con los puertos internos abiertos

Solo para desarrollo, nunca para la demostración. Publica Spring (8080), FastAPI (8000) y MySQL (`MYSQL_PORT`, por defecto 3307) en `127.0.0.1`:

```bash
docker compose -f compose.yaml -f compose.debug.yaml up -d --build
```

Con eso puedes conectarte a MySQL desde DBeaver o Workbench en `127.0.0.1:3307`, y ver la documentación de FastAPI en http://localhost:8000/docs.

## 6. Pruebas

GitHub Actions corre las dos suites en cada Pull Request. Para correrlas en tu máquina sin instalar Java:

```bash
# Spring (perfil de pruebas con H2, no necesita MySQL)
docker run --rm -v "$PWD":/app -w /app eclipse-temurin:21-jdk sh ./mvnw test
```

Para FastAPI, mira el job `fastapi` en `.github/workflows/ci.yml`: levanta solo `mysql` y corre `pytest` dentro de `docs-service/`.

## 7. Estructura del repositorio

```
src/                    Aplicación Spring Boot
docs-service/           Servicio FastAPI del pasaporte (app/, tests/, Dockerfile)
database/mydb.sql       Esquema y datos base
database/migrations/    Cambios de esquema (V5, V7) y datos de demostración (V6, V7_demo)
database/init/00-init.sh  Crea la base, los usuarios MySQL y sus permisos al primer arranque
nginx/                  Configuración del proxy inverso
docs/                   Contrato entre Spring y FastAPI
compose.yaml            Los cuatro servicios
.env.example            Plantilla de variables (sin secretos)
```

---

## 8. Cómo trabajamos con Git

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

En GitHub, abre el Pull Request con **base: `development`**. Si eliges `main`, el PR mostrará cambios de todo el equipo que no son tuyos. Otra persona lo revisa y lo fusiona: nadie fusiona su propio PR.

- **Nunca subas el `.env`** ni contraseñas.
- Ramas pequeñas, una por tarea, con mensajes de commit claros.
- Si Git te da un error que no entiendes, pregunta en el grupo antes de forzar algo (`--force`, `reset --hard`).
