# Cambios realizados

Resumen técnico de la ampliación. Las reglas detalladas están en `REGLAS_NEGOCIO.md`, la API de PDF en
`API_REPORTES.md` y los resultados de pruebas en `PRUEBAS_RESULTADOS.md`.

## Base de datos

`database/migrations/V8_reglas_negocio.sql` (solo agrega; no borra filas):

* `cita`: estados `CONFIRMADA` (antes `AGENDADA`, se renombró en los datos), `EN_ATENCION`, `RECHAZADA`,
  `NO_ASISTIO`, `REPROGRAMADA`; columnas `duracionMin`, `motivo`, `observaciones`, `indicacionesPrevias`,
  `observacionesAdmin`, `creadaEn`, `reservadaEn`, `expiraEn`, `inicioAtencion`, `finAtencion`,
  `idCitaOrigen`, `generadaAuto`, `version`; columna generada `ocupaHorario` con índices únicos por
  veterinario y por mascota.
* Tablas nuevas: `cita_historial_estado`, `solicitud_disponibilidad`, `horario_veterinario`,
  `ausencia_veterinario`, `veterinario_servicio`, `mascota_evento`, `transferencia_mascota`
  (índice único de una pendiente por mascota), `notificacion`, `auditoria`, `reporte_generado`.
* `servicio.tipoRegistro` y `servicio.esEmergencia`; `historial_clinico` con cita, clínica, tipo,
  antecedentes, signos vitales, evaluación, hallazgos, tratamiento, medicamentos y recomendaciones
  (índice único por cita); `carnet_vacuna.idCita/idVeterinaria`; `archivo_historial` con tamaño, fecha
  y autor; `mascota.situacion`; `mensaje_chat.leidoEn`; `conversacion.ultimoMensajeEn/idVeterinaria`.
* Siembra: horario aprobado y servicios autorizados de cada veterinario a partir de su clínica.
* `V8_permisos_fastapi.sql`: permisos mínimos de FastAPI. `database/aplicar_V8.sh`: aplica V8 a una
  base existente con respaldo previo. `00-init.sh` la aplica en instalaciones nuevas.

## Backend (Spring Boot)

Paquetes y clases nuevas:

* `comun.config.ReglasNegocio`, `RelojConfig` (reglas y reloj del servidor); `comun.web.FormatoVista`
  (`@fmt`, horas «2:30 p. m.»), `comun.web.UrlPublica`.
* `cita.model.TransicionCita`, `CitaHistorialEstado`; `cita.service.CitaEstadoService`,
  `ReservaConcurrenteException`; `CitaServiceImpl` reescrito (bloqueos, solapamiento por duración,
  confirmar/rechazar/no asistió/reprogramar con trazabilidad, liberación con margen, expiración,
  recordatorios); `AtencionService` reescrito (iniciar con ventana → registrar → finalizar).
* `disponibilidad.*`: entidades, repositorios, `DisponibilidadService` (franjas, cupos reservables,
  generación idempotente, citas afectadas, alternativas), `SolicitudDisponibilidadService`
  (solicitudes y aprobación), `TareasProgramadas`, controladores del veterinario y de recepción.
* `mascota.service.GestionMascotaService` (transferencias, situación, peso), `MascotaEvento`,
  `TransferenciaMascota`, `SituacionMascota`.
* `historial.service.ArchivoClinicoService` (adjuntos seguros), `ExpedienteService` (carpetas),
  `TipoRegistroClinico`; `HistorialClinicoController` (carpetas, documento, descarga autorizada).
* `notificacion.*` (persistentes, sin duplicados, campana global), `auditoria.*`.
* `emergencia.service.EmergenciaDisponibilidadService`; detalle de emergencia.
* `veterinaria.service.DirectorioService` (filtros, distancia, emergencias), `VeterinariaInfoService`,
  `LogoVeterinariaService`.
* `reporte.ReporteAuthController` (`/internal/auth/reportes` para Nginx).
* `security.AccesoPacienteService` endurecido (relación clínica por estado, recepción por clínica);
  `SecurityConfig` (rutas nuevas, 401 para la subpetición de Nginx).
* `chat`: bandeja propia ordenada por último mensaje, no leídos, contacto con veterinarias, reglas de
  contacto nuevas.

Comportamientos que cambiaron:

* La reserva del tutor queda PENDIENTE (antes quedaba confirmada directamente).
* Reprogramar conserva la cita original como REPROGRAMADA (antes volvía a DISPONIBLE).
* «Atender» ya no crea historia y cierra en un paso; ahora: iniciar, registrar y finalizar.
* No se crean historias ni vacunas fuera de una cita EN_ATENCION.
* Recepción solo edita el peso de la mascota.
* El tutor ya no elimina mascotas; las transfiere o reporta su situación.
* El veterinario ve «Mi disponibilidad» en lugar de «Mi veterinaria».
* El chat del veterinario solo muestra tutores de pacientes atendidos.
* El directorio muestra un formulario de búsqueda antes de los resultados.
* El pasaporte público no muestra diagnósticos ni medicamentos (configurable).

## Frontend (Thymeleaf)

* Nuevos: `citas/formulario.html` (asistente de 7 pasos + mini calendario), `historial/documento.html`,
  `mascotas/transferencias.html`, `mascotas/acciones.html`, `recepcion/mascotas/ficha.html`,
  `disponibilidad/{veterinario,recepcion,resultado}.html`, `emergencias/detalle.html`,
  `chat/fragmentos.html`.
* Rediseñados: dashboards de veterinario y recepción, agenda con tarjetas, detalle de cita (tutor y
  profesional), atención clínica, carpetas de expedientes, directorio, chat, emergencias, mascotas.
* Recursos: `css/vl-ui.css` (estados, tarjetas, carpetas, expediente, calendario, modales, validación),
  `js/vl-validate.js` (validación onBlur accesible en todos los formularios del panel y login),
  `js/vl-confirm.js` (modales propios para acciones sensibles; se eliminaron los `confirm()`),
  `js/vl-minical.js`, `js/vl-razas.js`, `resource/veterinaria-default.svg`. Se eliminó `js/citas-mias.js`.
* Horas sin segundos en toda la interfaz; se corrigió un XSS en la lista de la agenda (antes insertaba
  el nombre de la mascota sin escapar).

## Servicio de reportes (FastAPI)

`docs-service/app/reportes.py`, `reportes_repo.py`, `pdf.py`; `config.py` (validación de URL pública,
entorno, privacidad del pasaporte); `main.py` (router, `lifespan`, health con avisos);
`requirements.txt` + `reportlab==4.2.5`; pruebas `tests/test_reportes.py`, `tests/test_health.py`.

## Infraestructura

* `nginx/nginx.conf.template`: `auth_request` para `/api/reportes/`.
* `compose.yaml`: variables nuevas, uploads de solo lectura en FastAPI, `INTERNAL_TOKEN` en Nginx,
  perfil opcional `tunel` (cloudflared) para probar el QR desde datos móviles.
* `.env.example`: `VETLINK_ENTORNO`, `PASAPORTE_DETALLE_CLINICO` y explicación de `PUBLIC_BASE_URL`.
* `.gitignore`: carpeta `respaldos/`.

## Pruebas

Se ajustaron las pruebas existentes a los comportamientos nuevos y se agregaron las de
`src/test/java/com/vetlink/fase4/` (estados, reservas, solapamiento, concurrencia, disponibilidad,
generación, transferencias, permisos de recepción, autorización de reportes, adjuntos, emergencias,
logo, pantallas por rol).
