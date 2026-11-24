# API de reportes clínicos en PDF

Servicio: `docs-service` (Python 3.12, FastAPI 0.115, Uvicorn, ReportLab 4.2), detrás de Nginx.
Spring Boot sigue siendo el backend principal; FastAPI no tiene usuarios propios.

## Arquitectura y autenticación

```
Navegador ──GET /api/reportes/...──▶ Nginx
                                     │ 1) auth_request /_auth/reportes  ──▶ Spring /internal/auth/reportes
                                     │    (cookie de sesión + X-Original-URI)   · 401 sin sesión
                                     │                                          · 403 sin permiso sobre ESE recurso
                                     │                                          · 204 + X-Vetlink-Usuario/Rol/Veterinaria
                                     │ 2) proxy a FastAPI con X-Internal-Token + identidad
                                     ▼
                                  FastAPI /api/reportes/...  (re-verifica el acceso con SQL y genera el PDF)
```

* La regla de acceso es la misma de toda la aplicación (`AccesoPacienteService`); FastAPI la repite
  como defensa en profundidad (`app/reportes_repo.py`).
* El navegador no puede falsificar la identidad: Nginx sobrescribe `X-Vetlink-*` y `X-Internal-Token`.
* `/internal/` está bloqueado desde fuera (404). Sin sesión, Nginx redirige a `/login`.
* Mascotas ajenas o inexistentes responden igual (403) para no revelar si existen.

## Endpoints

| Método y ruta | Descripción |
|---|---|
| `GET /api/reportes/mascotas/{id}/historial` | Historial clínico completo (registros + vacunas) |
| `GET /api/reportes/mascotas/{id}/historial?fecha_inicio=AAAA-MM-DD&fecha_fin=AAAA-MM-DD` | Historial por rango de fechas (422 si inicio > fin) |
| `GET /api/reportes/mascotas/{id}/vacunas` | Historial de vacunación |
| `GET /api/reportes/historial/{numero}/pdf` | Registro clínico individual |
| `GET /api/reportes/health` | Estado del servicio (sin autenticación) |

Respuestas: `200 application/pdf` con `Content-Disposition: attachment; filename="historial-clinico_max_12_20261124-0930.pdf"`
(nombre ASCII seguro, sin rutas), `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`.
Errores: 401 (sin autenticación válida), 403 (sin permiso), 422 (parámetros inválidos).

Por rol:

* **Tutor**: sus mascotas actuales.
* **Veterinario**: pacientes con cita confirmada/en atención/atendida con él o emergencia asignada.
* **Recepción**: pacientes con cita autorizada en su clínica; el PDF incluye solo registros y vacunas
  de su clínica (se indica en el documento).

## Contenido del PDF

Encabezado en cada página (logo, nombre, NIT, dirección y teléfono de la veterinaria emisora; título y
fecha de generación), pie con «Página X de Y» y nota de confidencialidad. Secciones: identificación del
paciente (nombre, especie, raza, sexo, edad, peso, situación, tutor — solo el nombre), registros
clínicos (tipo, fecha, establecimiento responsable de cada registro, profesional y registro
profesional, motivo, antecedentes, signos vitales, evaluación, hallazgos, diagnóstico, tratamiento,
medicamentos, recomendaciones, próximo control y referencias a adjuntos) e historial de vacunación.
Los campos vacíos se omiten: nunca se inventa información. El logo se lee del volumen de uploads
montado en solo lectura.

Cada descarga se registra en `reporte_generado` (usuario, rol, tipo, mascota, registro, parámetros,
IP, fecha).

## Configuración

Variables de FastAPI (en `compose.yaml`, desde `.env`):

| Variable | Uso |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | MySQL (usuario `vetlink_fast`, permisos mínimos) |
| `INTERNAL_TOKEN` | Token compartido Nginx/Spring → FastAPI |
| `UPLOADS_DIR` | Carpeta de uploads (logos), por defecto `/uploads` |
| `PUBLIC_BASE_URL`, `VETLINK_ENTORNO` | URL pública del QR y validación de HTTPS en producción |
| `PASAPORTE_DETALLE_CLINICO` | `false` por defecto (privacidad del pasaporte público) |

Permisos MySQL de `vetlink_fast`: `database/migrations/V8_permisos_fastapi.sql`.

Nginx: `nginx/nginx.conf.template` (locations `/_auth/reportes`, `/api/reportes/health` y
`/api/reportes/`). El contenedor de Nginx recibe `INTERNAL_TOKEN` para inyectarlo.

## Ejecución

```bash
docker compose up -d --build            # levanta mysql, spring, fastapi y nginx
curl http://localhost/api/reportes/health
```

Desarrollo sin Docker (desde `docs-service/`):

```bash
pip install -r requirements-dev.txt
uvicorn app.main:app --reload --port 8000
```

## Pruebas

```bash
# Unitarias (sin base):
cd docs-service && python -m pytest -q
# Con integración contra la MySQL de Compose:
docker run --rm --network vetlink_vetlink_net -v "$PWD/docs-service":/srv -w /srv \
  -e DB_HOST=mysql -e DB_NAME=vetlink -e DB_USER=vetlink_fast -e DB_PASSWORD=... -e INTERNAL_TOKEN=... \
  python:3.12-slim sh -c "pip install -q -r requirements-dev.txt && python -m pytest -q"
```

## Producción

* Publicar Nginx detrás de HTTPS (certificado del dominio o un proxy/túnel con TLS).
* No exponer los puertos 8000/8080 fuera de la red de Compose.
* Generar `INTERNAL_TOKEN` con `openssl rand -hex 32`; no se versiona.
