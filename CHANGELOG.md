# CHANGELOG · VetLink V10 (revisión integral)

Fecha: 2026-11-25 · Rama: `Arreglos_Claude` · Historial anterior: `VETLINK_CHANGELOG.md` (V9).

## Resumen

Se atendieron los 15 requerimientos funcionales y visuales y la revisión de calidad. Los cambios están en Spring (Java, Thymeleaf, JS, CSS) y en la base de datos (migración **V10**, solo aditiva). FastAPI no se modificó: sus PDF ya incluían el logo de la clínica emisora y la marca de agua de VetLink.

| Verificación | Resultado |
|---|---|
| `./mvnw test` (H2) | **238 pruebas, 0 fallos**: las 223 que ya existían más 15 nuevas (`src/test/java/com/vetlink/v10/V10RequerimientosTest.java`) |
| Arranque real (Docker) | Spring arrancó con `ddl-auto=validate` después de aplicar V10 |
| Directorio (`/directorio?buscar=1`) | Antes 4,2–7,2 s; ahora 0,7–1,0 s (medido con curl a través de Nginx, con la JVM ya caliente) |
| Revisión visual | Capturas con Edge sin interfaz a 1366 px y 390 px de: Mis citas, detalle con propuesta, solicitudes de recepción, agendamiento de recepción y panel del dueño |
| Consola JS | Sin errores al cargar `/registro` ni `/directorio` |
| FastAPI (pytest) | **No se ejecutó**, porque no hubo cambios en `docs-service/` |

## Errores corregidos (con su causa)

| # | Error | Causa | Corrección |
|---|---|---|---|
| 1 | El estado tapaba el servicio y la hora se partía | La hora tenía una columna fija de 78 px y el estado usaba `nowrap` en una columna `auto`. En el panel del dueño era un flex sin espacio | Columnas flexibles, hora sin partir, título en dos líneas y *container queries* que bajan el estado a otra fila. La mascota y el servicio van en líneas separadas |
| 6 | La geolocalización tardaba o se quedaba cargando | (a) Mientras el aviso de permiso sigue abierto, el navegador no empieza a contar su `timeout`. (b) `enableHighAccuracy` estaba activado de entrada. (c) En el servidor, `cuposReservables` consultaba las ausencias **una vez por cada cupo** | Límite propio de 12 s, precisión normal primero y GPS como respaldo, caché de 10 min en la sesión del navegador, consulta previa del permiso, indicador de carga y bloqueo de clics repetidos. En el servidor, las ausencias se cargan una vez por veterinario y el directorio se detiene en el primer cupo |
| 7 | «Max» aparecía en el historial clínico | La carpeta del dueño usaba `findByTutor_IdUsuario` sin filtrar por `estado`, así que aparecían mascotas dadas de baja (estado 0), como «Max» (id 1) del seed `mydb.sql` | Se listan solo mascotas con estado 1 (las mismas de «Mis mascotas», incluidas las fallecidas). Se agregó un estado vacío informativo. No se borró ningún registro |
| 9 | Recepción no podía reprogramar | «Citas por reprogramar» mostraba las citas del veterinario **en todas las clínicas**. El veterinario 54 cambió de clínica y conservaba citas en otras, y al abrir una de esas la seguridad respondía 403 (en el log de Nginx: `GET /citas/recepcion/278/reagendar 403`) | Nuevo `citasAfectadasDeClinica`, que solo devuelve citas de la clínica (incluidas las de veterinarios que ya no pertenecen a ella). La restricción de seguridad se mantiene |
| 14 | Error 500 al agregar «Ecografia» | La comparación de duplicados ignoraba mayúsculas pero no tildes. MySQL (`utf8mb4_0900_ai_ci`) sí las ignora, y el `INSERT` violaba `nombreServicio_UNIQUE` | `Textos.claveSinTildes` y un nuevo `CatalogoServiciosService` como único punto para buscar y crear servicios |
| — | La cuadrícula del horario no mostraba el día | En SpEL, `mapa[d]` toma `d` como clave literal | Se cambió por `mapa.get(d)` |

## Funcionalidades

- **2. Mis citas:** pestañas «Mis citas» e «Historial de citas» con los mismos componentes de «Mis mascotas». Filtros por mascota, estado, veterinaria, servicio y rango de fechas, con opción para limpiarlos. Las opciones salen de las citas reales del dueño. Las citas que piden respuesta aparecen primero.
- **3. Solicitudes pendientes:** tarjeta con avatar, tipo, fecha de envío, estado y observaciones. El horario se ve en una cuadrícula semanal con inicio y fin por día. Las ausencias se ven como período Desde → Hasta.
- **4. Agendamiento (recepción):** el dueño encontrado aparece en su propia tarjeta (antes desalineaba la columna «Mascota»), la cuadrícula está alineada con campos de 46 px y la ayuda del veterinario va debajo.
- **5. Mascota fallecida:** solo lectura. `Mascota.exigirEditable()` bloquea en el backend la edición del dueño, la foto, el peso de recepción y las vacunas nuevas. Se ocultan las acciones de edición y se muestra un aviso. El historial, las citas y los documentos se conservan.
- **8. Día de ausencia:** «Hasta» se completa con la fecha de «Desde» (hasta las 11:59 p. m.) mientras el usuario no lo cambie a mano. No se permite una fecha final anterior a la inicial, ni en el navegador ni en el servidor.
- **10. Respuesta a una reprogramación:** la cita nueva queda CONFIRMADA (el horario sigue reservado) con `respuestaDuenio = PENDIENTE`. El dueño recibe una notificación con la mascota, la clínica, el servicio, la fecha anterior, la nueva y el estado. Puede:
  - **Confirmar.** Antes se revalida que el veterinario siga disponible y sin cruces.
  - **Elegir otro horario.** Solo del mismo servicio y la misma clínica, sin horarios ocupados, bloqueados ni fuera de jornada. Usa bloqueo pesimista y la nueva queda PENDIENTE hasta que la clínica la confirme.
  - **Cancelar.** El horario se libera.

  Cada respuesta queda en `cita_historial_estado` y en la auditoría, y se notifica a recepción y al veterinario con una clave única (sin duplicados). Solo el dueño puede responder: otros usuarios reciben 403.
- **11. Desbloquear agendamiento:** en «Disponibilidad del equipo» se ve el estado (habilitado o bloqueado), el período y el motivo, con la acción «Desbloquear agendamiento» y su confirmación. Solo la puede usar recepción de la misma clínica. El bloqueo queda como `ANULADO` con quién y cuándo lo desbloqueó (`desbloqueadaPor`, `desbloqueadaEn`), y se regeneran los cupos respetando el horario, las ausencias y las citas existentes.
- **12. Registro de veterinarias:**
  - Validación al escribir y al salir de cada campo, con borde verde o rojo.
  - Detección de duplicados en vivo (NIT, nombre comercial, correos, documento, teléfono) con `GET /api/registro/disponible`.
  - Contraseña de **mínimo 8 caracteres, con mayúscula y símbolo**: los requisitos se marcan mientras se escribe, se puede mostrar u ocultar, y el backend aplica la misma regla.
  - Reglas del backend alineadas con el formulario: NIT de 9 a 11 dígitos, teléfono, longitudes, URL de la página web y dirección.
- **13. Identidad visual:** el documento clínico muestra el logo de la clínica **del registro** (o una imagen predeterminada), sin deformarlo, y el logo de VetLink como marca de agua al 6 %, también al imprimir. Los PDF de FastAPI ya lo hacían.
- **14. Selector de servicios** (registro y «Mi veterinaria»): opciones del catálogo de la BD, buscador, teclado y selección múltiple. No se repiten los servicios ya elegidos y se pueden quitar. La opción «Otros» valida el nombre, evita duplicados y reutiliza el servicio si ya existe en el catálogo.
- **15. Aprobación automática:** la clínica queda activa al completar el registro, con `aprobadaEn` y `aprobacionAutomatica`. No recibe permisos de administración (no existe ese rol). La suspensión sigue funcionando con `estado = 0`, que la saca del directorio, bloquea las reservas y muestra un aviso a su recepción.

## Base de datos

- **Migración nueva:** `database/migrations/V10_reprogramacion_desbloqueo_aprobacion.sql`. Es idempotente y no borra nada.
  - `cita.respuestaDuenio VARCHAR(20) NULL`, `cita.respuestaDuenioEn DATETIME NULL`
  - `ausencia_veterinario.desbloqueadaPor INT NULL` (FK a `usuario`), `ausencia_veterinario.desbloqueadaEn DATETIME NULL`
  - `veterinaria.aprobadaEn DATETIME NULL`, `veterinaria.aprobacionAutomatica TINYINT(1) NOT NULL DEFAULT 0`
- `EstadoAprobacion.ANULADO` usa la columna `estado VARCHAR(20)` que ya existe. No cambia el esquema.
- Registrada en `database/init/00-init.sh` para instalaciones nuevas. Para bases existentes: `bash database/aplicar_V10.sh`, que hace un respaldo en `respaldos/` antes de aplicar.
- **Se aplicó a la base local** con ese script (respaldo `respaldos/vetlink-antes-de-V10-20261125-234310.sql`).
- No hacen falta GRANT nuevos: FastAPI ya tiene `SELECT` sobre `veterinaria` a nivel de tabla.

## Archivos

- **Nuevos:**
  - `cita/model/RespuestaDuenio.java`
  - `veterinaria/service/CatalogoServiciosService.java`
  - `templates/citas/modificar-reprogramacion.html`
  - `static/css/vl-v10.css`
  - la migración V10 y `aplicar_V10.sh`
  - `V10RequerimientosTest.java`
  - `TASKS.md`
- **Modificados:** controladores, servicios y modelos de citas, disponibilidad, mascotas, historial, veterinarias, usuarios y seguridad, además de las plantillas y los JS correspondientes. La lista completa está en `git status`.
- **Pruebas actualizadas por cambios de comportamiento pedidos:**
  - `VL09` ahora espera aprobación automática en vez de pendiente.
  - `VL30`: el dueño puede pasar CONFIRMADA → REPROGRAMADA, solo para responder a una propuesta.
  - `VL08`, `VL14` y `PruebaWebBase` usan contraseña segura, NIT válido y al menos un servicio.
- **Dependencias agregadas:** ninguna.

## Pendientes y decisiones

- La contraseña reforzada solo se exige al **registrar veterinarias**, como pide el requerimiento. El registro de dueños sigue con mínimo 6 caracteres.
- No hay rol de administrador ni pantalla para suspender clínicas. Se sigue haciendo con `estado = 0` en la base.
- Tampoco existe verificación de correo que conservar.
- `/api/registro/disponible` es público (lo usa el registro) y permite saber si un correo, documento o NIT ya está registrado. Responde solo sí o no, sin datos de la cuenta. Conviene agregar un límite de peticiones.
- Una propuesta de reprogramación sin respuesta no vence: el horario queda reservado hasta que el dueño responda o la clínica cancele.
- La interacción en navegador del selector de servicios y de la validación del registro no se probó de punta a punta, solo la carga sin errores de consola. El backend sí está cubierto por pruebas.
- En móvil, el panel de filtros de «Mis citas» ocupa bastante altura. Una mejora posible es plegarlo.
- No se hizo commit ni push.

## Cómo ejecutar

```bash
bash database/aplicar_V10.sh               # solo bases existentes (ya aplicado en la local)
docker compose up -d --build spring        # recompilar Spring
sh ./mvnw -B test                          # pruebas (238)
```
