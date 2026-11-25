# VetLink: cambios V9 (corrección integral)

Fecha: 2026-11-25 · Rama: `Arreglos_Claude`

## 1. Resumen ejecutivo

Se atendieron los 24 requisitos (DUE-01 a 08, VET-01 a 05, REC-01 a 09, GEN-01 y 02) en las tres capas: frontend (Thymeleaf, JS y CSS), backend (Spring Boot) y base de datos (migración **V9**, no destructiva e idempotente). El servicio de documentos (FastAPI) también recibió cambios: ahora usa la IP local en el QR y pone marca de agua en los reportes PDF.

Verificación realizada:

- **Spring (`./mvnw test`)**: 223 pruebas, 0 fallos. Son las 175 que ya existían más 48 nuevas en `src/test/java/com/vetlink/v9/`.
- **FastAPI (pytest)**, en un contenedor aislado: 43 pruebas pasan y 23 se omiten. Las omitidas son de integración y necesitan una base MySQL de pruebas.
- **Esquema**: Hibernate arrancó con `ddl-auto=validate` contra una **copia** de la base real con V9 aplicada.
- **Navegador real**: Playwright con Microsoft Edge contra esa copia. Se ejecutaron 24 pruebas funcionales de punta a punta y todas pasaron. Además se revisaron 84 combinaciones de página y ancho (360, 768, 1024 y 1440 px) buscando desbordes y errores de consola.

**Tu base de datos real no se modificó.** Las pruebas usaron una copia temporal (`vl_v9test`) que se eliminó al terminar.

## 2 y 3. Errores encontrados y causas

| Requisito | Error | Causa real |
|---|---|---|
| DUE-08 / VET-04 | «El perfil no se guarda» | Sí se guardaba en MySQL. La vista y la barra superior leían el `Usuario` guardado en la **sesión** al iniciar sesión, así que mostraban el dato viejo hasta volver a entrar. Además, un teléfono repetido (columna `UNIQUE`) terminaba en error 500. |
| DUE-07 | Emergencias con mascotas de otros dueños | No se guardaba quién solicitó la emergencia. «Mis solicitudes» se consultaba por el dueño **actual** de la mascota, así que al transferir una mascota sus emergencias pasaban al nuevo dueño. |
| DUE-01 | Especie, sexo y nacimiento editables | El formulario y el servicio aplicaban todos los campos al editar. |
| DUE-02 | Pocas razas; «Mestizo» no podía repetirse entre especies | Había 60 razas y el índice era `UNIQUE(nombreRaza)` global. |
| DUE-04 | El QR apuntaba a localhost | Solo existía `PUBLIC_BASE_URL`. |
| DUE-06 | La geolocalización no funcionaba | Al entrar por IP con HTTP el navegador bloquea la ubicación (contexto no seguro) y no había alternativa. Tampoco se manejaban los códigos de error. |
| VET-02 / REC-07 | Fechas y campos que se salían del contenedor | Los `input` dentro de `.vl-field` no tenían `width:100%`/`min-width:0`. Los `datetime-local` desbordaban la grilla de 2 columnas. |
| VET-03 | Emergencia marcada «atendida» a mano | Existía la transición directa ASIGNADA → ATENDIDA para el veterinario. |
| REC-02 / REC-04 | Se ofrecían horas que luego fallaban | Recepción usaba un endpoint antiguo que calculaba las horas a partir del horario de la clínica. Ignoraba servicio, bloqueos, ausencias y reservas. |
| REC-03 | Botón X fuera de la tarjeta | La celda de acciones se partía y los formularios en línea no tenían el estilo adecuado. |
| REC-06 | Reprogramación fallida | Dos causas identificadas: (1) mover una cita a un horario **cercano del mismo veterinario** chocaba con la propia cita original, que aún «ocupaba» el horario; (2) si el personal abría `/citas/{id}` (vista del dueño) recibía 403. No se pudo reproducir otro bloqueo con los datos de demostración. |
| REC-09 | «Mi veterinaria» daba 403 a recepcionistas no principales | El controlador exigía ser recepcionista principal incluso para consultar. |
| General | Paneles de ~80 px en móvil | Varias vistas fijaban `grid-template-columns` en línea, lo que impedía el colapso responsive. La campana de notificaciones también se salía de la pantalla en móvil. |

## 4. Soluciones implementadas (por requisito)

- **DUE-01**: en la edición, especie, sexo y fecha de nacimiento se muestran como campos de solo lectura. El servidor **ignora** esos valores aunque lleguen en la petición; hay una prueba con una petición manipulada.
- **DUE-02**: el catálogo pasa de 60 a 184 razas, con «Mestizo» por especie. El selector tiene buscador (insensible a tildes), filtro por especie, orden alfabético y la opción «Otra raza». La raza personalizada se normaliza y se reutiliza si ya existe, sin importar mayúsculas o espacios. El alta es atómica (`INSERT IGNORE` + índice único `(nombreRaza, idEspecie)`). No se renombró ni eliminó ninguna raza existente.
- **DUE-03 / REC-09 (nombres)**: regla común `Textos.REGEX_NOMBRE` en el backend y `data-tipo="nombre"` en el frontend. Admite letras, tildes, ñ, espacios y `- ' .`, sin números, y recorta los espacios sobrantes. Se aplica a mascotas, perfiles y alta de personal. Los teléfonos aceptan solo dígitos (7 a 15).
- **DUE-04**: nueva variable `IP`. Spring (`UrlPublica.resolver`) y FastAPI (`resolver_url_publica`) construyen `http://IP[:NGINX_HTTP_PORT]` cuando `PUBLIC_BASE_URL` está vacía o apunta a localhost. Un dominio o túnel público configurado tiene prioridad.
- **DUE-05**: el logo oficial (`resource/1-removebg-preview.png`, copiado a `docs-service/app/static/vetlink-logo.png`) se dibuja centrado en **todas** las páginas del PDF, al 8 % de opacidad incorporada en la imagen, detrás del contenido. Se verificó renderizando el archivo generado.
- **DUE-06**: se comprueba `isSecureContext`. Cada error tiene su mensaje (permiso denegado, ubicación no disponible, tiempo agotado). Hay un **selector manual en el mapa** (clic o arrastrar el marcador) y los resultados se recalculan al fijar la ubicación. No se usan coordenadas ficticias; las 10 clínicas tienen coordenadas reales.
- **DUE-07**: nueva columna `emergencia.idSolicitante`, rellenada para los registros existentes. Las consultas y la autorización van por solicitante. Al crear una emergencia se valida en el servidor la propiedad de la mascota y que no tenga otra emergencia abierta. La lista muestra especie y raza.
- **DUE-08 / VET-04 / GEN-02**: el perfil se lee de la base de datos y, al guardar, se actualiza también el usuario de la sesión. Nueva vista de consulta por tarjetas con botón «Editar perfil» (lápiz), «Guardar cambios» y «Cancelar» (que restaura los valores). Se evitan envíos dobles, se valida el teléfono duplicado sin error 500 y no se puede cambiar rol ni vinculación.
- **VET-01**: selector semanal con una tarjeta por día, activación por casilla, varias franjas, «Copiar lunes a días hábiles» y validación en vivo de franjas inválidas o cruzadas. Es accesible por teclado y adaptable a móvil. Se mantiene el flujo existente de solicitud y aprobación.
- **VET-02**: grilla auto-adaptable, `datetime-local` de ancho completo, validación «fin posterior a inicio» y mensajes visibles.
- **VET-03**: se eliminó el botón y se bloquea `estado=ATENDIDA` en el backend (403). Nueva pantalla «Registrar atención en el historial» (`/emergencias/{id}/atender`). `EmergenciaAtencionService` trabaja en una sola transacción: bloquea la fila, valida veterinario, estado y mascota, crea la historia (`historial_clinico.idEmergencia` único) y cambia el estado a ATENDIDA. Si cualquier paso falla, no se guarda nada. Los reintentos no duplican el registro.
- **VET-05**: el perfil muestra la veterinaria vinculada (nombre, NIT, dirección, teléfonos, correos, web), la licencia, la especialidad, los servicios autorizados y el horario aprobado, o un estado informativo si no hay vinculación. El modelo admite **una** clínica por veterinario.
- **REC-01**: marcas por forma y color. Los cupos libres futuros son una línea amarilla; las reservas pendientes, un punto morado; las citas, un punto azul; las canceladas, un aro rojo. Hay leyenda, etiquetas accesibles y navegación por teclado.
- **REC-02 / REC-04**: se eliminó el campo «Hora». Nuevo endpoint `/citas/api/disponibilidad/grilla`: lo reservable sale activo y lo ocupado, bloqueado o pasado sale gris, deshabilitado y con su motivo. El backend vuelve a validar al confirmar: bloqueo pesimista del cupo, cruces, ausencias y bloqueos. La prueba de concurrencia que ya existía (VL32) sigue pasando.
- **REC-03**: acciones en una fila que no se parte (`vl-td-acciones`), dentro de la tarjeta con desplazamiento propio. Se añadió «Limpiar filtros».
- **REC-05**: tras agendar, redirige a `/agenda?dia=<fecha>` con confirmación. Si falla, no hay redirección ni mensaje de éxito. Se añadió el botón «Reporte de citas».
- **REC-06**: la cita original ya no bloquea el cupo nuevo al reprogramar; el horario anterior se libera solo si la operación termina bien. El personal que abre `/citas/{id}` se redirige a `/agenda/{id}`. Recepción no recibió permisos de administrador.
- **REC-07 / REC-08**: formulario de bloqueo rehecho; tarjetas por profesional con indicador de estado.
- **REC-09**: perfil institucional con edición mediante lápiz (nombre, descripción, web, teléfono y correo). Muestra horario de atención y servicios. Gestión de recepcionistas (alta, consulta, turno), de veterinarios (alta, horario y servicios asignados, aplicados de inmediato y registrados como solicitud aprobada) y de servicios (agregar sin duplicados, editar precio y duración con regeneración de cupos, retirar conservando citas e historial). Todo queda limitado a la clínica de quien opera.
- **GEN-01**: `vl-chat.js` reutilizable. Enter envía y Shift+Enter inserta salto de línea; respeta la composición con IME. Bloquea mensajes vacíos o solo con espacios, evita el doble envío, conserva el texto si el envío falla, ajusta la altura automáticamente y conserva los saltos de línea al mostrarlos.

## 5. Archivos creados

- `database/migrations/V9_mejoras_integrales.sql`
- `database/aplicar_V9.sh`
- `docs-service/app/static/vetlink-logo.png`
- `docs-service/tests/test_v9_qr_y_marca_agua.py`
- `src/main/java/com/vetlink/comun/validacion/Textos.java`
- `src/main/java/com/vetlink/emergencia/service/EmergenciaAtencionService.java`
- `src/main/java/com/vetlink/mascota/service/RazaCatalogoService.java`
- `src/main/java/com/vetlink/veterinaria/service/GestionClinicaService.java`
- `src/main/resources/static/css/vl-v9.css`
- `src/main/resources/static/js/vl-chat.js`
- `src/main/resources/templates/emergencias/atender.html`
- `src/test/java/com/vetlink/v9/` con `DuenioV9Test`, `VeterinarioV9Test`, `EmergenciaAtencionTransaccionTest`, `RecepcionV9Test` y `GeneralV9Test`

## 6. Archivos modificados

La lista completa sale con `git status`. Los principales:

- Controladores: Mascota, RecepcionMascota, Emergencia, Perfil, Cita, RecepcionCita, Disponibilidad, VeterinariaAdmin y Chat.
- Servicios: MascotaServiceImpl, HistorialClinicoServiceImpl, CitaServiceImpl, DisponibilidadService y UsuarioServiceImpl.
- Modelos: Emergencia, Raza e HistorialClinico.
- Vistas: perfil, mascotas, emergencias, disponibilidad, agenda, agendar, reporte, Mi veterinaria, directorio y chat.
- JS: `vl-validate`, `vl-razas`, `vl-calendar-lite` y `registro`.
- Configuración: `compose.yaml`, `.env.example`, `application.properties` y `00-init.sh`.

**Pruebas existentes ajustadas, con su justificación:**

- `VL04EmergenciaEstadoTest`: la prueba que exigía «el veterinario marca atendida» ahora exige 403. VET-03 elimina explícitamente ese comportamiento.
- `PruebaWebBase`: los datos de prueba usaban apellidos y nombres de mascota con dígitos (ahora inválidos) y creaban razas repetidas por especie (ahora lo impide el índice único). No se cambió ninguna aserción.

## 7. Migraciones de base de datos

`V9_mejoras_integrales.sql` es idempotente y no borra filas. Hace lo siguiente:

- Cambia el índice de `raza` de `UNIQUE(nombreRaza)` a `UNIQUE(nombreRaza, idEspecie)` y añade la columna `raza.personalizada`.
- Añade a `emergencia` las columnas `idSolicitante` (FK, rellenada con el dueño actual de cada emergencia existente) y `atendidaEn`.
- Añade `historial_clinico.idEmergencia` (FK, única).
- Inserta las razas que faltaban.

Se probó **dos veces** sobre una copia de la base real.

## 8. Variables de entorno

- `IP`: IP local del equipo. Va en `.env.example` y se pasa a los servicios spring y fastapi.
- `NGINX_HTTP_PORT`: ya existía; ahora también se pasa a spring y fastapi para construir la URL del QR.

## 9. Cambios en permisos

- Todas las recepcionistas pueden **consultar** «Mi veterinaria»; solo la principal edita.
- Las operaciones nuevas de equipo y servicios validan que el recurso pertenece a la clínica de quien opera.
- El veterinario ya no puede cambiar una emergencia a ATENDIDA directamente.
- El tutor solo ve las emergencias que solicitó.
- No se ampliaron otros permisos.

## 10. Nuevas funcionalidades

- Raza personalizada.
- Selector de ubicación en el mapa.
- Atención de emergencias mediante historial clínico.
- Grilla de horarios reales.
- Perfil institucional con gestión de equipo y servicios.
- Selector de horario semanal.
- Perfiles en modo consulta y edición.
- Chat con teclado mejorado.

## 11. Pruebas ejecutadas y resultados reales

| Herramienta | Resultado |
|---|---|
| `./mvnw test` (H2, perfil test) | **223 pruebas, 0 fallos, 0 errores** |
| pytest docs-service (contenedor `python:3.12` del Dockerfile) | **43 pasan, 23 omitidas** (integración: requieren `DB_HOST` con MySQL de pruebas) |
| Validación de esquema (`ddl-auto=validate`, MySQL 8.4 + V9, copia de la base) | Arranque correcto |
| Migración V9 aplicada dos veces sobre la copia | Correcta e idempotente (184 razas, 0 emergencias sin solicitante) |
| E2E Playwright + Edge (copia de la base) | **24/24** (chat, perfil, razas, VET-03, agendamiento, geolocalización con permiso, denegada y por IP) |
| Responsive 360 / 768 / 1024 / 1440 (21 vistas × 4 anchos) | 0 desbordes horizontales. Única alerta: un 404 de la foto de la mascota 63, porque la copia apunta a un archivo que solo existe en el volumen real. |
| Revisión visual del PDF renderizado | La marca de agua aparece en las 7 de 7 páginas |

No hay linter ni comprobación de tipos JS configurados en el proyecto. La compilación Java (`mvnw package`) es correcta.

## 12. Ejecutar el proyecto (base existente, sin perder datos)

```bash
bash database/aplicar_V9.sh                      # respalda en respaldos/ y aplica V9
docker compose up -d --build spring fastapi nginx
```

En una instalación nueva, `00-init.sh` ya aplica V9.

> Importante: si reconstruyes Spring **sin** aplicar V9, no arrancará, porque Hibernate valida el esquema.

## 13. Configurar la IP de los códigos QR

1. Busca la IP del equipo: en Windows con `ipconfig` («Dirección IPv4»); en Linux con `hostname -I`.
2. En `.env` escribe `IP=192.168.1.100` (con tu IP real). Deja `PUBLIC_BASE_URL=http://localhost` o vacía.
3. Ejecuta `docker compose up -d spring fastapi`. Si `NGINX_HTTP_PORT` no es 80, el puerto se añade automáticamente.
4. Permite el puerto en el Firewall de Windows (red privada) y conecta el teléfono a la misma Wi-Fi.
5. En «Compartir historial», el enlace debe verse así: `http://192.168.1.100/p/<token>`.

**Geolocalización por IP:** por HTTP el navegador la bloquea. Opciones:

- Usar «Elegir en el mapa».
- Usar el túnel HTTPS: `docker compose --profile tunel up -d tunel` (ver README).
- Solo para pruebas en Chrome o Edge: activar `chrome://flags/#unsafely-treat-insecure-origin-as-secure` con `http://<IP>`.

## 14. Limitaciones y pendientes reales

- **Dispositivo físico:** no se abrió el QR desde un teléfono. Se verificó la URL generada y que Nginx escucha en todas las interfaces.
- **Integración FastAPI:** las 23 pruebas de integración no se ejecutaron porque requieren un MySQL de pruebas.
- **REC-06:** se corrigieron los dos fallos identificados. Si persiste algún caso concreto con tus datos, se necesitan los pasos exactos para reproducirlo.
- **REC-09:**
  - La dirección y el horario de atención de la clínica se muestran pero no se editan desde aquí.
  - La descripción de un servicio existente no se edita porque el catálogo de servicios es compartido entre clínicas.
  - El «horario laboral» de una recepcionista es su turno, que es el dato que existe en el modelo.
- **Mi veterinaria (veterinario):** el modelo admite una sola veterinaria por profesional.
- **Datos de demostración heredados:** hay franjas aprobadas que se cruzan (lunes de Juan), así que el selector las marca y pide corregirlas antes de enviar. También hay usuarios con dígitos en el nombre (p. ej. «22222»), que deben corregir su perfil para poder guardarlo.

## Matriz de seguimiento

| ID | Rol | Requisito | Estado | Archivos modificados | Prueba realizada | Resultado |
|---|---|---|---|---|---|---|
| DUE-01 | Dueño | Campos no editables | Corregido y verificado | MascotaController, MascotaServiceImpl, MascotaForm, mascotas/formulario | DuenioV9Test (petición manipulada, vista de solo lectura) | OK |
| DUE-02 | Dueño | Catálogo de razas / Otra raza | Corregido y verificado | V9, Raza, RazaRepository, RazaCatalogoService, vl-razas.js | DuenioV9Test (duplicados, orden, otra especie) + E2E | OK |
| DUE-03 | Dueño | Validación del nombre | Corregido y verificado | Textos, MascotaForm, vl-validate.js, registro.js | DuenioV9Test (6 nombres inválidos) | OK |
| DUE-04 | Dueño | IP para el QR | Implementado, pendiente de validación | UrlPublica, ReglasNegocio, config.py, compose, .env.example | Pruebas de la URL en Java y pytest | OK; falta abrirlo desde un teléfono |
| DUE-05 | Dueño | Marca de agua en reportes | Corregido y verificado | pdf.py, static/vetlink-logo.png | pytest (opacidad, todas las páginas) + PDF renderizado | OK |
| DUE-06 | Dueño | Geolocalización | Corregido y verificado | veterinarias/lista.html | E2E: con permiso, denegada, por IP + mapa | OK |
| DUE-07 | Dueño | Emergencias coherentes | Corregido y verificado | V9, Emergencia, EmergenciaRepository, EmergenciaController | DuenioV9Test (ajena, transferencia) | OK |
| DUE-08 | Dueño | Mi perfil persiste | Corregido y verificado | PerfilController, PerfilForm, UsuarioServiceImpl, perfil/ver | DuenioV9Test + E2E (barra superior, recarga) | OK |
| VET-01 | Veterinario | Selector de horario | Corregido y verificado | disponibilidad/veterinario, DisponibilidadController, vl-v9.css | VeterinarioV9Test (válido, cruce, inverso) + responsive | OK |
| VET-02 | Veterinario | Fechas de ausencia | Corregido y verificado | disponibilidad/veterinario, vl-v9.css, vl-validate.js | Responsive en 4 anchos | OK |
| VET-03 | Veterinario | Emergencia cerrada por historial | Corregido y verificado | EmergenciaAtencionService, EmergenciaController, HistorialClinico*, emergencias/* | VeterinarioV9Test, EmergenciaAtencionTransaccionTest (fallo simulado, concurrencia) + E2E | OK |
| VET-04 | Veterinario | Actualizar perfil | Corregido y verificado | PerfilController | VeterinarioV9Test | OK |
| VET-05 | Veterinario | Veterinaria vinculada | Corregido y verificado | PerfilController, perfil/ver | VeterinarioV9Test (con y sin clínica, intento de cambiar la vinculación) | OK |
| REC-01 | Recepcionista | Indicadores del calendario | Corregido y verificado | vl-calendar-lite.js, citas/agenda, vl-v9.css | RecepcionV9Test + responsive | OK |
| REC-02 | Recepcionista | Sin campo Hora | Corregido y verificado | citas/recepcion-agendar, RecepcionCitaController | RecepcionV9Test + E2E | OK |
| REC-03 | Recepcionista | Botones del reporte | Corregido y verificado | citas/reporte, vl-v9.css | Responsive (0 desbordes) | OK |
| REC-04 | Recepcionista | Disponibilidad real | Corregido y verificado | CitaController (grilla), DisponibilidadService | RecepcionV9Test (ocupado, bloqueado, grilla), VL32 concurrencia, E2E | OK |
| REC-05 | Recepcionista | Redirección tras agendar | Corregido y verificado | RecepcionCitaController, recepcion-agendar | RecepcionV9Test + E2E | OK |
| REC-06 | Recepcionista | Reprogramar | Corregido y verificado | CitaServiceImpl, DisponibilidadService, CitaController | RecepcionV9Test (horario cercano, otra clínica, redirección) + VL23 | OK (ver limitaciones) |
| REC-07 | Recepcionista | Bloqueo de agenda | Corregido y verificado | disponibilidad/recepcion | RecepcionV9Test (retira cupos) + responsive | OK |
| REC-08 | Recepcionista | Horarios y servicios aprobados | Corregido y verificado | disponibilidad/recepcion, vl-v9.css | Responsive | OK |
| REC-09 | Recepcionista | Mi veterinaria | Corregido y verificado | VeterinariaAdminController, GestionClinicaService, veterinarias/admin | RecepcionV9Test (servicios, horario, turno, otra clínica, validaciones) | OK (ver limitaciones) |
| GEN-01 | Todos | Chats | Corregido y verificado | vl-chat.js, chat/detalle, ChatController | GeneralV9Test + E2E (Enter, Shift+Enter, vacío, fallo de red) | OK |
| GEN-02 | Todos | Perfiles rediseñados | Corregido y verificado | perfil/ver, PerfilController, vl-v9.css | GeneralV9Test (3 roles) + E2E (cancelar y guardar) | OK |

## Verificación manual sugerida

1. **Dueño**
   - Editar una mascota y comprobar que especie, sexo y fecha aparecen bloqueadas.
   - Registrar una mascota con «Otra raza».
   - Probar el nombre «Max2» y comprobar que se rechaza.
   - Abrir el directorio, pulsar «Usar mi ubicación» y luego «Elegir en el mapa».
   - Descargar un reporte PDF y comprobar la marca de agua.
   - Editar el perfil y comprobar que el cambio aparece en la barra superior al instante.
2. **Veterinario**
   - En «Mi disponibilidad», activar días, agregar franjas y probar un cruce.
   - En una emergencia asignada, usar «Registrar atención en el historial» y comprobar que pasa a «Atendida».
   - En «Mi perfil», revisar la veterinaria vinculada.
3. **Recepción**
   - En el calendario, revisar la leyenda.
   - En «Agendar cita», elegir servicio y fecha, comprobar los cupos grises y verificar que redirige a «Citas de la clínica».
   - Reprogramar una cita.
   - Bloquear la agenda de un veterinario.
   - En «Mi veterinaria» (con la recepcionista principal), editar la información con el lápiz, agregar y editar un servicio y asignarlo a un veterinario.
4. **Chat:** Enter envía, Shift+Enter hace salto de línea y un mensaje vacío muestra un aviso.
