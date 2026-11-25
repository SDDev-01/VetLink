# Reglas de negocio — VetLink

Este documento describe las reglas que el sistema aplica en el backend. Las pantallas solo
reflejan estas reglas: ocultar un botón nunca reemplaza la validación del servidor.

## 1. Parámetros configurables

Todos viven en `vetlink.reglas.*` (`src/main/resources/application.properties`, clase
`ReglasNegocio`). Se cambian en el archivo o con variables de entorno.

| Parámetro | Valor por defecto | Qué controla |
|---|---|---|
| `zona-horaria` | `America/Bogota` | Reloj del servidor para todas las reglas de tiempo (`Clock` inyectado) |
| `duracion-servicio-por-defecto-min` | 30 | Duración usada si la clínica no definió la del servicio |
| `anticipacion-minima-reserva-min` | 60 | Un cupo solo se puede reservar si empieza al menos 60 min después de ahora |
| `anticipacion-minima-liberacion-min` | 120 | Si una cita se cancela/rechaza con menos anticipación, su horario no vuelve a ofrecerse |
| `margen-inicio-atencion-min` | 15 | Minutos después de la hora programada en los que el veterinario puede iniciar la atención |
| `expiracion-pendiente-horas` | 24 | Plazo máximo para que recepción confirme una reserva |
| `confirmacion-antes-del-inicio-min` | 30 | Una reserva pendiente expira, a más tardar, 30 min antes de su inicio |
| `horizonte-agenda-dias` | 14 | Días hacia adelante para los que se generan cupos |
| `recordatorio-horas-antes` | 24 | Antelación del recordatorio de cita confirmada |
| `tamano-maximo-adjunto-mb` | 10 | Tamaño máximo de un adjunto clínico |
| `tamano-maximo-imagen-mb` | 2 | Tamaño máximo de la imagen de la veterinaria |
| `formatos-adjuntos` | `pdf,jpg,jpeg,png` | Formatos permitidos para adjuntos clínicos |
| `public-base-url` | `PUBLIC_BASE_URL` | URL pública de enlaces y QR |
| `tareas-habilitadas` | `true` | Tareas programadas (generación, expiración, recordatorios) |
| `cron-generacion` | `0 15 0 * * *` | Hora de la generación nocturna de agenda |

Supuestos adoptados (no estaban definidos en el proyecto): los valores anteriores. Las duraciones de
cada servicio se toman de `servicio_veterinaria.duracionAprox` (ya existían).

## 2. Estados de cita

| Estado | Significado | Ocupa horario |
|---|---|---|
| DISPONIBLE | Cupo publicado (todavía no es una cita) | No |
| PENDIENTE | Reserva del tutor, esperando confirmación de recepción | Sí |
| CONFIRMADA | Cita aceptada por la clínica (antes «AGENDADA») | Sí |
| EN_ATENCION | El veterinario inició la atención | Sí |
| ATENDIDA | Atención finalizada con su registro obligatorio | Sí |
| CANCELADA | Cancelada por el tutor o por recepción | No |
| RECHAZADA | Recepción no aceptó la reserva, o expiró sin confirmación | No |
| NO_ASISTIO | La mascota no llegó (vencido el margen de inicio) | No |
| REPROGRAMADA | Se movió a otra cita; la nueva guarda `idCitaOrigen` | No |

### Transiciones permitidas (`TransicionCita`)

| Desde | Hacia | Quién | Condición |
|---|---|---|---|
| DISPONIBLE | PENDIENTE | Tutor | Cupo futuro con la anticipación mínima, sin solapamientos |
| DISPONIBLE | CONFIRMADA | Recepción | Ídem (reserva hecha en la clínica) |
| PENDIENTE | CONFIRMADA | Recepción | La hora aún no pasó |
| PENDIENTE | RECHAZADA | Recepción / Sistema | Sistema: al expirar `expiraEn` o al pasar la hora |
| PENDIENTE / CONFIRMADA | CANCELADA | Tutor / Recepción | Cita futura |
| PENDIENTE / CONFIRMADA | REPROGRAMADA | Recepción | Hay un cupo destino válido |
| CONFIRMADA | EN_ATENCION | Veterinario asignado | Entre la hora programada y hora + margen, según el reloj del servidor; sin otra atención en curso |
| CONFIRMADA | NO_ASISTIO | Recepción / Veterinario | Vencido el margen de inicio |
| EN_ATENCION | ATENDIDA | Veterinario asignado | Registro obligatorio guardado (ver §5) |

Cualquier otra transición se rechaza con un mensaje. Cada cambio queda en `cita_historial_estado`
(estado anterior, nuevo, usuario, fecha y comentario).

## 3. Disponibilidad y generación de cupos

* **Horario del veterinario** (`horario_veterinario`): franjas semanales. Un descanso fijo se modela
  con dos franjas el mismo día. Solo cuentan las APROBADAS.
* **Horario de la clínica** (`horario_veterinaria`): si existe, recorta las franjas del veterinario.
* **Ausencias** (`ausencia_veterinario`): descanso, permiso, incapacidad, vacaciones, ausencia o bloqueo
  de agenda; solo cuentan las APROBADAS.
* **Servicios autorizados** (`veterinario_servicio`): solo los APROBADOS; el veterinario no se los
  autoriza a sí mismo (pasa por solicitud y aprobación de recepción).

**Generación automática** (`DisponibilidadService.generarParaVeterinario`): para cada día del horizonte
recorre las franjas disponibles y crea cupos DISPONIBLES con la duración de cada servicio autorizado
que ofrece la clínica, rotando el servicio (el orden de arranque cambia cada día) para repartir la
oferta. Nunca crea un cupo que se cruce con otro cupo o con una cita que ocupe horario, por eso es
**idempotente**. No genera cupos de urgencias. Se ejecuta cada noche, 1 min después de arrancar y
cada 6 h, al aprobar un cambio de disponibilidad y con el botón «Generar agenda» de recepción.

Supuesto de migración: a cada veterinario existente se le sembró como horario aprobado el horario
válido de su clínica y como servicios autorizados los servicios de su clínica. Recepción puede
ajustarlos.

**Cupos reservables** (lo que ve el tutor en el calendario): DISPONIBLES, futuros con la anticipación
mínima, de un veterinario activo, sin ausencia aprobada y que no se crucen con otra cita del
veterinario ni (si se indica) de la mascota.

### Aprobación de cambios

1. El veterinario envía una solicitud (horario, novedad o servicios) → PENDIENTE.
2. Recepción de su clínica aprueba o rechaza (otra clínica no puede; el veterinario tampoco).
3. Al aprobar: se actualiza la disponibilidad, se retiran los cupos libres que ya no caben (los
   generados se eliminan por ser derivados; los creados a mano quedan CANCELADOS con nota), se generan
   los nuevos y se listan las **citas afectadas** con alternativas de otros veterinarios compatibles.
   Las citas no se eliminan ni se reprograman solas: recepción decide y el tutor recibe un aviso.

## 4. Reservas, solapamiento y concurrencia

* Solapamiento por **duración completa**: dos intervalos `[inicio, inicio + duración)` no pueden
  cruzarse para el mismo veterinario ni para la misma mascota (ej.: 8:00–8:40 y 8:20–9:00 se rechaza).
* Protocolo de cada reserva (transacción `READ_COMMITTED`):
  1. `SELECT … FOR UPDATE` del cupo, del veterinario y de la mascota (orden fijo, sin interbloqueos).
  2. Validación de estado, anticipación, disponibilidad y solapamientos.
  3. Cambio de estado por la máquina de estados.
  4. Índices únicos en MySQL (`uk_cita_veterinario_ocupado`, `uk_cita_mascota_ocupado`, sobre una
     columna generada que solo vale 1 en estados que ocupan horario) como última barrera.
* Si dos personas reservan el mismo cupo a la vez, solo una lo consigue; la otra recibe
  «Esta cita acaba de ser reservada por otro usuario. Por favor, selecciona otro horario disponible.»
  (`ReservaConcurrenteException`; probado con 6 hilos simultáneos).
* Una reserva PENDIENTE bloquea el cupo hasta que se confirme, se rechace, se cancele o expire.

### Liberación de cupos

Al cancelar, rechazar, expirar o reprogramar, el horario vuelve a ofrecerse como un cupo nuevo
solo si: falta al menos la anticipación mínima de liberación, el veterinario sigue disponible y
autorizado para el servicio, y no hay otro cupo o cita en ese intervalo. La cita cerrada se conserva.

## 5. Atención clínica

* El botón «Atender cita» está deshabilitado antes de la hora, habilitado durante el margen y
  deshabilitado después; la verificación se repite en el servidor.
* Solo el veterinario asignado inicia, registra y finaliza. Recepción nunca registra información clínica.
* Mientras la cita está EN_ATENCION se puede:
  * guardar **una** historia clínica ligada a la cita (índice único `uk_historial_cita`);
  * registrar vacunas (solo si el servicio es de vacunación); se asocian a la cita, la clínica, el
    veterinario y la fecha de hoy;
  * adjuntar archivos al registro (PDF/JPG/PNG, validados por firma y tamaño, nombre aleatorio).
* Registro obligatorio para finalizar según `servicio.tipoRegistro`: `CLINICO` → historia;
  `VACUNACION` → al menos una vacuna; `NINGUNO` (peluquería, hotel, guardería, ventas) → nada.
* La historia la puede editar su autor durante 24 h (regla existente VL-19).
* La creación libre de historias o vacunas desde «Pacientes» quedó bloqueada.

## 6. Transferencia y situación de mascotas

* No hay borrado directo: el tutor transfiere o reporta la situación.
* Transferencia: solicitud a un tutor registrado (por documento o correo), nunca a sí mismo ni a
  usuarios inexistentes o inactivos; una sola pendiente por mascota (bloqueo + índice único); no se
  permite con citas pendientes, confirmadas o en atención. La propiedad cambia solo al aceptar. Al
  aceptar se revocan los pasaportes compartidos por el tutor anterior. El historial, las vacunas y las
  citas pasadas siguen con la mascota; el tutor anterior pierde el acceso. Todo queda en
  `transferencia_mascota`, `mascota_evento` y auditoría.
* Situación: ACTIVA → PERDIDA → ACTIVA (encontrada); ACTIVA/PERDIDA → FALLECIDA (irreversible:
  cancela citas futuras, revoca enlaces compartidos y bloquea nuevas citas).

## 7. Permisos

| Recurso | Tutor | Veterinario | Recepción |
|---|---|---|---|
| Mascota (datos) | Las suyas | Pacientes con cita | Con cita en su clínica; solo **edita el peso** (con trazabilidad) |
| Historia clínica / adjuntos / PDF | Sus mascotas | Pacientes con cita confirmada, en atención o atendida, o emergencia asignada | Solo lectura, mascotas con cita autorizada y **solo registros de su clínica** |
| Crear historia / vacuna | No | Solo durante una cita EN_ATENCION propia | No |
| Citas | Reservar, cancelar, pedir reprogramación | Iniciar/finalizar atención, no asistió | Confirmar, rechazar, cancelar, reprogramar, generar agenda, cupo manual |
| Disponibilidad | — | Solicitar | Aprobar/rechazar, bloquear agenda (su clínica) |
| Chat | Veterinarias activas (canal de recepción) y veterinarios que lo atendieron | Tutores de pacientes que atendió | Tutores con citas en su clínica y personal de su clínica |

Todas las comprobaciones se hacen en el servidor con el usuario de la sesión; cambiar un id en la
URL devuelve 403. Las acciones sensibles se registran en `auditoria`.

## 8. Emergencias

Un veterinario está disponible para una urgencia si: está activo, la hora actual cae en su horario
aprobado, no tiene ausencias, no está EN_ATENCION ni en una cita confirmada en curso y no tiene otra
emergencia ASIGNADA. Si la clínica autorizó el servicio de urgencias solo a algunos veterinarios, solo
cuentan esos. La asignación se valida en el servidor. Una solicitud del tutor queda «Pendiente de
asignación» hasta que recepción la asigna: nunca se presenta como atención garantizada.

## 9. Notificaciones

Tabla `notificacion` con `claveUnica` por evento y usuario: el mismo hecho nunca produce dos
notificaciones (se reutiliza la fila). Se notifica: reservas, confirmaciones, rechazos, cancelaciones,
reprogramaciones, recordatorios, transferencias y su resultado, solicitudes de disponibilidad y su
resultado, citas afectadas, emergencias nuevas/asignadas/cerradas y mensajes de chat.

## 10. Código QR y privacidad

El QR usa `PUBLIC_BASE_URL`. Con `localhost` o una IP privada solo abre en la misma red: Spring y
FastAPI lo advierten. Para que abra desde datos móviles hace falta un dominio con HTTPS o un túnel
(ver README). La página pública del pasaporte ya no muestra motivos, diagnósticos ni medicamentos
(salvo `PASAPORTE_DETALLE_CLINICO=true`); muestra identificación, vacunas y fechas de atención.
