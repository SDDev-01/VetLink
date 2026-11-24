# Resultados de pruebas

Fecha de ejecución: 24/11/2026. Todas las cifras de este documento salen de ejecuciones reales.

## Resumen

| Suite | Comando | Resultado |
|---|---|---|
| Spring Boot (JUnit, H2) | `./mvnw clean test` | **175 pruebas, 0 fallos, 0 errores** (40 clases) |
| FastAPI (pytest, unitarias + integración contra la MySQL de Compose con V8) | ver `API_REPORTES.md` | **60 pruebas, 0 fallos** |
| Línea base antes de los cambios | `./mvnw test` | 133 pruebas, 0 fallos |
| Migración V8 sobre la base real existente | `bash database/aplicar_V8.sh` | Aplicada, con respaldo en `respaldos/`; sin pérdida de filas |
| Instalación desde cero (`00-init.sh` con V8 y datos demo) | MySQL 8.4 desechable | `Inicialización finalizada`; permisos de Spring y FastAPI creados |
| Arranque de Spring contra MySQL (`ddl-auto=validate`) | `docker compose up -d --build` | Arranca: el esquema V8 coincide con las entidades |
| Comprobaciones extremo a extremo por Nginx (sin sesión) | `curl` | Ver §3 |

## 1. Pruebas existentes ajustadas (comportamiento cambiado a propósito)

| Prueba | Cambio |
|---|---|
| VL05, VL15 | La reserva del dueño ahora queda PENDIENTE (antes confirmada) |
| VL06 | Mensaje de mascota inactiva más descriptivo |
| VL03 | Las vacunas solo se registran durante una cita de vacunación EN_ATENCION |
| VL07 | Nuevas reglas de chat (veterinarias para el tutor; el veterinario solo ve tutores atendidos) |
| VL09 | El directorio muestra resultados tras el formulario de búsqueda |
| VL12 | Hora con formato «2:30 p. m.» |
| VL17, VL18 | Historias creadas desde la cita en atención; flujo iniciar → registrar → finalizar |
| VL23 | Reprogramar conserva la cita original como REPROGRAMADA |
| VL25 | Recepción ve carpetas de solo lectura; emergencias listan solo veterinarios disponibles |
| Todas | Estado `AGENDADA` renombrado a `CONFIRMADA` |

## 2. Pruebas nuevas (`src/test/java/com/vetlink/fase4`)

| Clase | Qué comprueba |
|---|---|
| VL30TransicionCitaTest | Transiciones permitidas por actor; no se salta el flujo clínico; estados cerrados no se reabren |
| VL31ReservasYSolapamientoTest | Reserva PENDIENTE bloquea el cupo y avisa a recepción; confirmar/rechazar; recepción ajena no confirma; cancelación sin margen no libera; solapamiento 8:00–8:40 vs 8:20–9:00; mascota sin citas simultáneas; cupo manual que se cruza; expiración idempotente; anticipación mínima |
| VL32ConcurrenciaReservaTest | 6 hilos reservan el mismo cupo: 1 éxito y 5 con el mensaje «Esta cita acaba de ser reservada por otro usuario…» |
| VL33DisponibilidadYAgendaTest | Generación por horario/servicios/citas existentes, reparto entre servicios, idempotencia; sin servicios no hay cupos; solicitud → aprobación → cupos; ausencia retira cupos y lista citas afectadas sin tocarlas; el veterinario no se autoriza servicios ni aprueba; API de días/horas solo con cupos reservables; mascota ajena → 403 |
| VL34TransferenciaYEstadoMascotaTest | Propiedad cambia solo al aceptar; historial conservado; el anterior dueño pierde acceso; rechazo; a sí mismo, inexistente, duplicada; con citas vigentes; perdida/encontrada/fallecida (cancela citas, bloquea agenda); no hay borrado directo |
| VL35PermisosRecepcionReportesYAdjuntosTest | Recepción solo actualiza peso (con trazabilidad) y no edita otros datos; no registra historias/vacunas; ve solo documentos de su clínica; autorización de reportes (401 sin sesión, 204 con identidad, 403 ajeno o con id manipulado); adjuntos (firma inválida, extensión no permitida, nombre seguro, descarga autorizada); veterinario en atención o con emergencia no recibe otra; sin «TI» en formularios de personal; imagen de veterinaria validada y publicada |
| VL36PantallasPorRolTest | ~50 pantallas de tutor, veterinario, recepción y públicas renderizan sin errores, sin horas con segundos ni el estado antiguo; «volver» del directorio no acepta URLs externas |

FastAPI (`docs-service/tests`): `test_reportes.py` (PDF válido, sin datos inventados, nombre seguro,
formato de fecha, 401 sin token/rol inválido, 403 recepción sin clínica, descarga del tutor con
registro en bitácora, 403 con id ajeno, registro individual, rango de fechas y 422 por rango
inválido), `test_health.py` (validación de la URL pública), `test_plantilla.py` (el pasaporte
público ya no muestra diagnósticos por defecto).

## 3. Comprobaciones extremo a extremo por Nginx

| Escenario | Resultado |
|---|---|
| `GET /api/reportes/health` | 200 `{"status":"ok"}` |
| PDF sin sesión | 302 → `/login` |
| PDF con `X-Vetlink-*` y `X-Internal-Token` falsificados | 302 → `/login` (Nginx los sobrescribe) |
| `GET /internal/auth/reportes` desde fuera | 404 |
| `/directorio` | Muestra el formulario previo |
| `/directorio?buscar=1` | 10 veterinarias |
| `/directorio?buscar=1&emergencia=true` a las 3:40 a. m. | 0 resultados (ningún horario aprobado cubre esa hora: correcto) |
| Imagen de una veterinaria sin logo | 302 → imagen por defecto |
| `/health` de FastAPI con `PUBLIC_BASE_URL=http://localhost` | `urlPublicaValida: false` con el aviso correspondiente |
| Tarea de generación en la base real | 2.499 cupos en 14 días, sin duplicados (consulta de control con `GROUP BY … HAVING COUNT > 1` vacía) |

## 4. Errores encontrados y corregidos durante la verificación

* Clases de prueba compiladas contra el enum anterior (compilación incremental) → se recompiló desde cero.
* El punto de entrada 401 para Nginx se volvía el predeterminado de toda la aplicación → entrada propia por ruta.
* Las carpetas mostraban una fecha inventada (1 de enero) para registros antiguos sin fecha exacta → ahora «Año N (fecha aproximada)».
* Thymeleaf 3.1 no permite referencias a beans en `th:attr`/`th:data-*` → el texto se calcula con `th:with`.
* Expresión regular del nombre de adjuntos con un rango accidental (dejaba pasar `/` y `<`) → corregida.
* `00-init.sh` quedó con finales CRLF tras editarlo en Windows → convertido a LF (lo exige `.gitattributes`).

## 5. Efectos sobre los datos existentes (base real)

* 65 citas `AGENDADA` → `CONFIRMADA` (mismo significado).
* Al arrancar, la tarea de expiración pasó a `RECHAZADA` 27 reservas `PENDIENTE` cuya hora ya había
  pasado (cada una con su registro en `cita_historial_estado` y notificación al tutor).
* Se sembraron 60 franjas de horario aprobadas y 69 servicios autorizados a partir de las clínicas.
* Respaldo previo: `respaldos/vetlink-antes-de-V8-20261124-033236.sql`.

## 6. Criterios de aceptación

| Criterio | Estado | Evidencia |
|---|---|---|
| Registrar mascota sin raza predeterminada | Implementado | `vl-razas.js` + opción «Seleccione una raza»; validación servidor raza/especie existente |
| Escanear QR desde red externa | **Pendiente de infraestructura** | Código listo y advertencias; requiere dominio HTTPS o túnel (perfil `tunel`) |
| Agendar desde el perfil de una mascota | Verificado | VL36 (`/citas/agendar/{id}`), título «Agendar cita para …» |
| Impedir avanzar en formularios incompletos | Implementado | Asistente: cada paso exige su dato (JS) y el servidor valida el formulario completo |
| Impedir fechas u horas pasadas | Verificado | VL15, VL31 (anticipación), API solo devuelve cupos futuros (VL33) |
| Disponibilidad real de un veterinario | Verificado | VL33 |
| Días disponibles en azul | Implementado | `vl-minical.js` + `vl-ui.css` (con texto accesible, no solo color) |
| Reservar y bloquear el cupo | Verificado | VL31 |
| Confirmar desde recepción | Verificado | VL31 |
| Impedir dos reservas simultáneas | Verificado | VL32 |
| Impedir horarios superpuestos | Verificado | VL31 |
| Liberar cita cancelada | Verificado | VL11, VL31 |
| Transferir mascota / aceptar / rechazar | Verificado | VL34 |
| Perdida, encontrada, fallecida | Verificado | VL34 |
| Historial por carpetas | Verificado | VL36, VL35 |
| Buscar veterinarias por ubicación y servicio | Verificado | VL36, comprobación por Nginx |
| Buscar emergencia disponible | Verificado | VL25, VL35, comprobación por Nginx |
| Impedir contactar tutores no autorizados | Verificado | VL07 |
| Impedir atender antes de la hora / fuera del margen | Verificado | VL18 |
| Registrar vacunas e historiales durante cita activa | Verificado | VL03, VL17, VL18 |
| Cargar y consultar adjuntos | Verificado | VL35 |
| Impedir finalizar sin registro obligatorio | Verificado | VL18 |
| Solicitar disponibilidad, aprobar y rechazar | Verificado | VL33 |
| Actualizar automáticamente la agenda | Verificado | VL33 (regeneración al aprobar) y tarea programada en la base real |
| Impedir asignar emergencias a veterinarios ocupados | Verificado | VL35 |
| Detalle de emergencia | Verificado | VL35, VL36 |
| Recepción no modifica información clínica / solo peso | Verificado | VL35 |
| Cargar y ver imagen de la veterinaria | Verificado | VL35 |
| Descargar historial completo / registro individual en PDF | Verificado | pytest de integración |
| Reportes desde FastAPI mediante Nginx | Verificado parcialmente | Cadena `auth_request` comprobada sin sesión; la descarga con sesión real está cubierta por VL35 (Spring) + pytest (FastAPI), no con un navegador |
| Impedir descargas no autorizadas | Verificado | VL35, pytest, Nginx |
| Validar formularios al salir de cada campo | Implementado | `vl-validate.js` en todas las páginas del panel y en el login; **no hay prueba automatizada de navegador** |
| Interfaces en móviles | **No verificado en dispositivo** | Estilos adaptables escritos |
| Funcionalidades existentes siguen operando | Verificado | Las 133 pruebas originales siguen (ajustadas donde el requisito cambió el comportamiento) |

## 7. Pendiente de validación manual

* Recorrido completo en navegador con una cuenta real de cada rol (no hay contraseñas de demostración
  en claro y no se crearon cuentas en la base local).
* Validación onBlur y modales en distintos navegadores; vista en móviles.
* QR escaneado desde datos móviles, una vez configurado el dominio o el túnel.
