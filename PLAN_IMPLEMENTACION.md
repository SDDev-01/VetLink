# Plan de implementación — VetLink (corrección, optimización y ampliación)

Archivo de seguimiento. Si la sesión se interrumpe, retomar desde la primera tarea `[ ]` de la fase en curso.

Leyenda de estado: `[x]` hecho y verificado · `[~]` hecho parcialmente (ver nota) · `[ ]` pendiente.
Prioridad: **P1** crítica (integridad, seguridad, concurrencia) · **P2** funcional · **P3** UX/estética.

## Fase 1 — Auditoría (hecha)

Arquitectura encontrada:

| Pieza | Tecnología | Notas |
|---|---|---|
| App principal | Spring Boot 4.1 / Java 21 / Thymeleaf / Spring Security (sesión) / JPA (validate) | `src/` |
| BD | MySQL 8.4, esquema en `database/mydb.sql` + migraciones `database/migrations/V*.sql` aplicadas por `database/init/00-init.sh` | Hibernate **valida**, no crea: toda columna nueva requiere migración |
| Pasaporte/QR | FastAPI (docs-service) + PyMySQL + qrcode | Usuario MySQL `vetlink_fast` con permisos mínimos |
| Proxy | Nginx (`nginx/nginx.conf.template`) | `/p/` → FastAPI, resto → Spring, `/internal` bloqueado |

Errores detectados en la auditoría:

- [x] Cupos solapados ya existentes en la base real (vet 53 y 54: dos filas a la misma hora). No hay restricción ni bloqueo.
- [x] `CitaServiceImpl` solo compara hora exacta, nunca la duración → solapamientos posibles.
- [x] Reserva no atómica (leer DISPONIBLE + guardar): dos usuarios pueden reservar el mismo cupo.
- [x] Reagendar devolvía la cita original a DISPONIBLE y perdía la trazabilidad.
- [x] Cualquier veterinario con *alguna* cita podía crear historias y vacunas libremente desde Pacientes.
- [x] Adjuntos clínicos se guardaban en `uploads/` relativo (fuera del volumen Docker) y sin validar tipo.
- [x] La recepcionista podía editar todos los datos de la mascota.
- [x] El formulario de mascota preselecciona la primera raza (Labrador) y lista razas de todas las especies.
- [x] El chat del veterinario mostraba dueños con citas no atendidas; la lista de conversaciones no se ordenaba por último mensaje ni mostraba no leídos.
- [x] Agenda: nombre de mascota insertado en `innerHTML` sin escapar (XSS almacenado).
- [x] Horas mostradas como `14:30` / `14:30:00` sin formato homogéneo.
- [x] El logo de la veterinaria era solo un texto.
- [x] QR: la URL sale de `PUBLIC_BASE_URL`; con `http://localhost` o IP privada no funciona fuera de la LAN.

## Fase 2 — Backend y reglas de negocio

| # | Tarea | Módulo | Rol | Prio | Estado |
|---|---|---|---|---|---|
| 2.1 | Migración `V8_reglas_negocio.sql` (columnas nuevas, tablas de disponibilidad, transferencias, notificaciones, auditoría, índices únicos) | BD | — | P1 | [x] |
| 2.2 | Reglas configurables centralizadas (`vetlink.reglas.*`) | config | — | P1 | [x] |
| 2.3 | Máquina de estados de citas (`TransicionCita`) + historial de estados | citas | todos | P1 | [x] |
| 2.4 | Disponibilidad por veterinario (horario, ausencias, servicios autorizados) | disponibilidad | vet/recep | P1 | [x] |
| 2.5 | Generación automática e idempotente de cupos + tarea programada | disponibilidad | recep | P1 | [x] |
| 2.6 | Reserva atómica (UPDATE condicional) + bloqueo pesimista por veterinario + índice único de horario ocupado | citas | tutor/recep | P1 | [x] |
| 2.7 | Control de solapamiento por duración (veterinario y mascota) | citas | todos | P1 | [x] |
| 2.8 | Confirmar / rechazar / cancelar / reprogramar con trazabilidad; liberación con margen | citas | recep/tutor | P1 | [x] |
| 2.9 | Expiración de reservas pendientes (tarea programada) | citas | sistema | P1 | [x] |
| 2.10 | Transferencia de mascotas (solicitud, aceptar, rechazar, cancelar, trazabilidad) | mascotas | tutor | P1 | [x] |
| 2.11 | Estado de mascota (perdida / encontrada / fallecida) con eventos | mascotas | tutor | P2 | [x] |
| 2.12 | Notificaciones persistentes sin duplicados | notificaciones | todos | P2 | [x] (se ven al cargar cada página; no hay envío en vivo por WebSocket) |
| 2.13 | Auditoría de acciones sensibles | auditoria | todos | P1 | [x] |
| 2.14 | Revisión de permisos (acceso clínico por estado de cita, recepción solo lectura) | seguridad | todos | P1 | [x] |

## Fase 3 — Rol tutor

| # | Tarea | Prio | Estado |
|---|---|---|---|
| 3.1 | Registro de mascota: raza vacía, filtrada por especie, se limpia al cambiar especie | P2 | [x] |
| 3.2 | QR: URL pública validada, advertencia si es localhost/IP privada, HTTPS en producción, sin datos clínicos sensibles | P1 | [~] Implementado y probado localmente; falta configurar un dominio HTTPS o el túnel (infraestructura externa) |
| 3.3 | Asistente de agendamiento en 7 pasos con validación por paso | P2 | [x] |
| 3.4 | Mini calendario (azul disponible / gris no disponible / hoy / seleccionado, accesible) | P2 | [x] |
| 3.5 | Agendar desde la mascota: preseleccionada + título dinámico | P2 | [x] |
| 3.6 | Detalle completo de la cita con acciones según estado | P2 | [x] |
| 3.7 | Reemplazar "eliminar" por transferir / perdida / encontrada / fallecida | P1 | [x] |
| 3.8 | Bandeja de transferencias recibidas (aceptar / rechazar) | P1 | [x] |
| 3.9 | Historial clínico en carpetas por mascota + documento tipo expediente | P2 | [x] |
| 3.10 | Directorio con formulario previo, geolocalización, distancia, filtros persistentes, emergencias | P2 | [x] (la geolocalización del navegador requiere HTTPS fuera de localhost) |
| 3.11 | Chat: solo sus conversaciones, iniciar con veterinarias, no leídos, orden por último mensaje | P1 | [x] |

## Fase 4 — Rol veterinario

| # | Tarea | Prio | Estado |
|---|---|---|---|
| 4.1 | Dashboard clínico (agenda del día, próximo paciente, contadores por estado, emergencias, semana) | P2 | [x] |
| 4.2 | Agenda con tarjetas por cita | P3 | [x] |
| 4.3 | "Atender cita" con ventana horaria del servidor | P1 | [x] |
| 4.4 | Registro clínico / vacuna solo durante una cita EN_ATENCION | P1 | [x] |
| 4.5 | Adjuntos PDF/JPG/PNG validados por firma y tamaño, almacenamiento seguro | P1 | [x] |
| 4.6 | Documento clínico estructurado | P2 | [x] |
| 4.7 | "Mi disponibilidad" (horario, ausencias, servicios) con aprobación | P1 | [x] |
| 4.8 | Chat restringido a dueños de pacientes atendidos | P1 | [x] |
| 4.9 | Finalización solo con registro obligatorio | P1 | [x] |
| 4.10 | Emergencias: veterinario ocupado no disponible; detalle de emergencia | P1 | [x] |

## Fase 5 — Rol recepcionista

| # | Tarea | Prio | Estado |
|---|---|---|---|
| 5.1 | Dashboard operativo | P2 | [x] |
| 5.2 | Calendario con tarjetas + detalle | P3 | [x] |
| 5.3 | Detalle de cita ampliado + historial de estados + acciones | P2 | [x] |
| 5.4 | Generación automática de agenda (botón + tarea programada) | P1 | [x] |
| 5.5 | Confirmar / rechazar reservas pendientes | P1 | [x] |
| 5.6 | Aprobar / rechazar solicitudes de disponibilidad (con citas afectadas y alternativas) | P1 | [x] |
| 5.7 | Mascotas: solo peso editable con trazabilidad | P1 | [x] |
| 5.8 | Historial clínico solo lectura, carpetas, solo mascotas con cita autorizada | P1 | [x] |
| 5.9 | Logo real de la veterinaria (subida, vista previa, validación, imagen por defecto) | P2 | [x] |
| 5.10 | Quitar "TI" en formularios de personal de "Mi veterinaria" + validaciones | P2 | [x] |

## Fase 6 — Servicio de reportes PDF (FastAPI + Nginx)

| # | Tarea | Prio | Estado |
|---|---|---|---|
| 6.1 | Endpoints `/api/reportes/...` con ReportLab | P1 | [x] |
| 6.2 | Autenticación por `auth_request` de Nginx contra Spring + re-verificación en FastAPI | P1 | [x] |
| 6.3 | Registro de reportes generados (`reporte_generado`) | P2 | [x] |
| 6.4 | Botones de descarga en carpetas y documentos | P2 | [x] |
| 6.5 | Pruebas pytest de PDF y permisos | P1 | [x] |

## Fase 7 — Validaciones y diseño

| # | Tarea | Prio | Estado |
|---|---|---|---|
| 7.1 | `vl-validate.js`: validación onBlur global y accesible | P2 | [x] |
| 7.2 | `vl-confirm.js`: modales propios para acciones sensibles | P2 | [x] |
| 7.3 | Formato de hora homogéneo (`h:mm a`) | P3 | [x] |
| 7.4 | Campana de notificaciones en todas las páginas | P2 | [x] |
| 7.5 | Responsive de las vistas nuevas | P3 | [~] Estilos adaptables escritos (grid y media queries); no se probó en dispositivos físicos |

## Fase 8 — Pruebas y documentación

| # | Tarea | Estado |
|---|---|---|
| 8.1 | Pruebas JUnit nuevas (estados, solapamiento, concurrencia, transferencias, atención, permisos) | [x] |
| 8.2 | Ajuste de pruebas existentes a los nuevos estados | [x] |
| 8.3 | Pruebas pytest del servicio de reportes | [x] |
| 8.4 | Migración V8 aplicada sobre la base real en Docker | [x] |
| 8.5 | Documentación: CAMBIOS_REALIZADOS, REGLAS_NEGOCIO, API_REPORTES, PRUEBAS_RESULTADOS, README | [x] |

Los resultados reales de cada ejecución están en `PRUEBAS_RESULTADOS.md`; lo que no se pudo verificar se lista allí como pendiente.

## Pendientes reales

* Dominio con HTTPS (o túnel con nombre) para que el QR y la geolocalización funcionen fuera de la red local.
* Verificación manual con cuentas reales por rol en navegador y en móvil (las pruebas automáticas cubren
  backend y renderizado de todas las pantallas; no hay contraseñas de demostración en claro).
* Actualización en vivo de vistas abiertas (hoy se recalculan al recargar o tras cada acción).
