# Contrato Spring ↔ FastAPI (servicio `docs-service`)

> **BORRADOR — pendiente de aprobación por P2, P3 y P4.** Hasta que el equipo lo
> apruebe en la reunión de contrato, cualquier punto puede cambiar. Cuando se
> apruebe, quitar este aviso y anotar la fecha.

Este documento fija cómo se comunican Spring Boot y FastAPI para el **pasaporte
de salud compartible**: rutas, cuerpos JSON, códigos de error, tablas y columnas
que FastAPI lee. Si el código y este documento no coinciden, se corrige uno de los
dos en el mismo PR.

## 1. Reglas generales

- FastAPI **no** implementa roles ni sesiones. Los permisos (quién puede crear o
  revocar un pasaporte) viven solo en Spring.
- FastAPI escribe **solo** en `pasaporte` y `pasaporte_acceso`. Lee las tablas de
  Spring listadas en la sección 5, y nada más.
- Spring **no** tiene entidades JPA para `pasaporte` ni `pasaporte_acceso`.
- FastAPI **no** crea tablas al arrancar. Las tablas salen de
  `database/migrations/V7_pasaporte.sql`.
- Fechas en JSON: ISO 8601 **sin zona**, en hora de Colombia (`America/Bogota`),
  por ejemplo `"2026-11-19T10:15:00"`. Las dos conexiones a MySQL usan esa zona.
- Nombres de campos JSON en camelCase.

## 2. Rutas internas (solo Spring → FastAPI)

Solo son alcanzables dentro de la red de Compose (`http://fastapi:8000`). Nginx
responde 404 a cualquier `/internal/...` que llegue desde fuera.

Todas exigen el encabezado `X-Internal-Token` con el valor de la variable
`INTERNAL_TOKEN` (la comparten Spring y FastAPI).

| Situación | Respuesta |
|---|---|
| Falta `X-Internal-Token` o su valor no coincide | `401` `{"detail": "..."}` |

### 2.1 Crear pasaporte

`POST /internal/pasaportes`

```json
{ "idMascota": 2, "idUsuario": 8, "duracionHoras": 24 }
```

- `duracionHoras` solo admite `24`, `168` (7 días) o `720` (30 días).
- `idUsuario` es quien crea el pasaporte. Spring ya verificó que es el dueño de la
  mascota; FastAPI no lo vuelve a comprobar.
- El token se genera con `secrets.token_urlsafe(32)` (43 caracteres).

| Resultado | Código | Cuerpo |
|---|---|---|
| Creado | `201` | `{"token": "...", "url": "http://.../p/<token>", "expiraEn": "2026-11-20T10:15:00"}` |
| Mascota o usuario inexistente | `404` | `{"detail": "..."}` |
| Duración no permitida o cuerpo inválido | `422` | `{"detail": [...]}` |

### 2.2 Listar pasaportes de una mascota

`GET /internal/pasaportes?idMascota={id}`

`200` con una lista, del más reciente al más antiguo (lista vacía si no hay):

```json
[
  {
    "token": "...",
    "url": "http://.../p/<token>",
    "creadoEn": "2026-11-19T10:15:00",
    "expiraEn": "2026-11-20T10:15:00",
    "revocadoEn": null,
    "estado": "ACTIVO",
    "aperturas": 3
  }
]
```

- `estado`: `ACTIVO`, `VENCIDO` o `REVOCADO`, calculado por FastAPI en el momento
  de la consulta. Spring lo muestra tal cual.
- `aperturas`: cantidad de filas en `pasaporte_acceso` (para el opcional "lista
  de aperturas visible para el dueño").
- Falta `idMascota` o no es un entero → `422`.

### 2.3 Revocar

`POST /internal/pasaportes/{token}/revocar` (sin cuerpo)

| Resultado | Código |
|---|---|
| Revocado (o ya estaba revocado: se conserva la fecha original) | `204` |
| El token no existe | `404` |

Spring debe comprobar antes que el token pertenece a una mascota del usuario
(por ejemplo, que aparece en el listado de esa mascota).

## 3. Rutas públicas (a través de Nginx)

| Ruta | Respuesta |
|---|---|
| `GET /p/{token}` | `200` página HTML del pasaporte. `404` si el token no existe, venció o fue revocado (la misma página de error en los tres casos, para no revelar cuál). Cada apertura válida crea una fila en `pasaporte_acceso`. |
| `GET /p/{token}/qr.png` | `200` `image/png` con un QR que apunta a `PUBLIC_BASE_URL/p/{token}`. `404` en los mismos casos que arriba. No registra apertura. |
| `GET /health` | `200` `{"status": "ok"}`. No se publica por Nginx; lo usan Compose y Spring. |

La página pública **no** muestra teléfono, documento, dirección ni correo del
dueño. Tampoco su nombre.

Para registrar la IP de la apertura, FastAPI usa `X-Real-IP` y, si falta, el
primer valor de `X-Forwarded-For`. Nginx los fija con `$remote_addr`, así que el
cliente no puede falsificarlos.

## 4. Tablas y dueños

| Tabla | Escribe | Lee |
|---|---|---|
| Todas las tablas actuales de `mydb` | Spring (`vetlink_spring`) | Spring; FastAPI solo las de la sección 5 |
| `pasaporte` | FastAPI (`vetlink_fast`) | FastAPI |
| `pasaporte_acceso` | FastAPI (`vetlink_fast`) | FastAPI |

Permisos de `vetlink_fast` (los aplica `scripts/migrate.sh` en cada arranque, desde
`database/permisos_fastapi.sql` y `database/migrations/V8_permisos_fastapi.sql`):
`SELECT, INSERT, UPDATE` sobre `pasaporte` y `pasaporte_acceso`; `SELECT` sobre
las tablas y columnas de la sección 5. Sin `DELETE` ni `DROP`.

## 5. Columnas de Spring que lee FastAPI

Cualquier PR que cambie estas columnas lo revisa P3, y la prueba de integración
de `docs-service` las consulta todas.

| Tabla | Columnas |
|---|---|
| `mascota` | `idMascota`, `nombre`, `idEspecie`, `idRaza`, `genero`, `fechaNacimiento`, `peso`; reportes PDF: también `situacion`, `estado` y `Tutor_idUsuario` (acceso del dueño: solo su mascota con `estado = 1`) |
| `especie` | `idEspecie`, `nombreEspecie` |
| `raza` | `idRaza`, `nombreRaza` |
| `carnet_vacuna` | `idCarnet`, `idMascota`, `idVacuna`, `fechaAplicacion`, `proximaDosis`, `dosis` |
| `vacuna` | `idVacuna`, `nombreVacuna` |
| `historial_clinico` | `numeroHistorial`, `idMascota`, `Veterinario_idUsuario`, `fechaConsulta`, `fechaRegistro`, `motivoConsulta`, `diagnostico` |
| `historial_medicamento` | `Historial_Clinico_numeroHistorial`, `Medicamento_idMedicamento`, `cantidad` |
| `medicamento` | `idMedicamento`, `nombreMedicamento` |
| `usuario` | `idUsuario`, `nombre`, `apellido` (solo para mostrar el nombre del veterinario; permiso por columna) |
| `veterinaria` | ninguna por ahora (el plan le da `SELECT`; ver nota) |

Notas:

- `usuario.idUsuario` hace falta para unir `historial_clinico.Veterinario_idUsuario`
  con el nombre del veterinario. No es un dato personal del dueño. El plan solo
  mencionaba `nombre` y `apellido`; **pendiente de aprobar**.
- `historial_clinico` no tiene la clínica (solo el veterinario), y llegar a la
  clínica exige leer `veterinario`, que no está en la lista. Por eso la página no
  muestra la clínica y `veterinaria` queda sin uso. **Decidir** si se quita su
  permiso o si se agrega `veterinario.idVeterinaria`.

## 6. Variables de entorno

| Variable | Servicio | Uso |
|---|---|---|
| `DB_HOST` (`mysql`), `DB_PORT` (`3306`), `DB_NAME` (`mydb`) | FastAPI | Conexión a MySQL |
| `DB_USER` (`vetlink_fast`), `DB_PASSWORD` | FastAPI | Usuario de permisos limitados |
| `INTERNAL_TOKEN` | FastAPI y Spring | Valor de `X-Internal-Token` |
| `PUBLIC_BASE_URL` | FastAPI | Dirección pública que va en `url` y en el QR (para la demo con celular, la IP del portátil) |
| `VETLINK_DOCS_BASE_URL` (`http://fastapi:8000`) | Spring | `vetlink.docs.base-url` |
| `VETLINK_DOCS_INTERNAL_TOKEN` | Spring | `vetlink.docs.internal-token` (mismo valor que `INTERNAL_TOKEN`) |

## 7. Comportamiento de Spring ante fallos

- Tiempo máximo de conexión y de lectura: 2 segundos.
- Error de red o tiempo agotado → `ServicioNoDisponibleException`; la pantalla
  muestra "El servicio de pasaportes no está disponible" (nunca un 500).
- `404` de FastAPI → el pasaporte o la mascota no existen.
