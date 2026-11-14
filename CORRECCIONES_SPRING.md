# Correcciones VetLink (Spring Boot) — instrucciones para el agente de código

## Cómo usar este archivo

Pon este archivo en la raíz del repositorio de VetLink (junto a `pom.xml`) y dale al agente este mensaje:

> Lee `CORRECCIONES_SPRING.md` completo antes de tocar código. Ejecuta las tareas fase por fase, en el orden indicado. Al terminar cada fase, compila, corre las pruebas y dame el reporte que pide la sección "Reporte por fase". No pases a la siguiente fase hasta que yo te lo confirme.

---

## Contexto del proyecto

- VetLink es una aplicación web en **Spring Boot 4 / Java 21**, con **Spring MVC + Thymeleaf** (renderizado en servidor), **Spring Security** con login por formulario y sesión, **Spring Data JPA** y **MySQL** (base `mydb`).
- Código en `src/main/java/com/vetlink/`, organizado por módulo: `usuario`, `mascota`, `cita`, `historial`, `vacuna`, `veterinaria`, `resena`, `chat`, `emergencia`, `ubicacion`, `comun`, `security`, `pattern`.
- Plantillas en `src/main/resources/templates/`; CSS y JS en `src/main/resources/static/`.
- Roles: `TUTOR` (dueño de mascota), `VETERINARIO` y `RECEPCIONISTA`. El rol se calcula en `Usuario.getRol()` según la subtabla que exista (`tutor`, `veterinario`, `recepcionista`). La recepcionista con `esPrincipal = true` es la administradora de su clínica.
- El usuario de la sesión se obtiene con `@AuthenticationPrincipal VetlinkUserPrincipal principal` y `principal.getUsuario()`.
- Las columnas de la base están en camelCase literal (`idMascota`, `numeroDocumento`) y se usa `PhysicalNamingStrategyStandardImpl`. No cambies esa estrategia.
- Migraciones SQL manuales en `database/migrations/` (V2, V3 y V4 ya existen).

## Reglas obligatorias para el agente

1. **Una tarea = un commit**, con mensaje `fix(VL-XX): descripción corta`, donde XX es el número de tarea de este archivo.
2. **No hagas refactors fuera de lo que pide cada tarea.** No renombres paquetes, clases ni rutas existentes salvo que la tarea lo diga.
3. **No cambies el esquema con Hibernate.** Todo cambio de base de datos va en un archivo SQL nuevo: `database/migrations/V5_correcciones.sql` (créalo en la Fase 0 y agrégale sentencias a medida que avances). Cada sentencia lleva un comentario con su número de tarea. Nunca borres filas de datos existentes.
4. **Las verificaciones de permisos van en el servidor** (controlador o servicio), nunca solo en la plantilla.
5. Si una tarea no se puede hacer como está descrita (por ejemplo, el método indicado no existe o se llama distinto), **detente y explícalo en el reporte**. No inventes una solución distinta sin avisar.
6. Los números de línea son de referencia y pueden haber cambiado. Ubica el código por **archivo y nombre de método**.
7. Al usar `RedirectAttributes`, importa `org.springframework.web.servlet.mvc.support.RedirectAttributes`.
8. Mensajes al usuario en español. Para los errores de permisos usa `org.springframework.security.access.AccessDeniedException` (Spring ya la muestra como página 403).
9. Después de cada tarea, `./mvnw -q compile` debe pasar. Al final de cada fase, `./mvnw test` debe pasar.

## Pruebas automáticas

- En la **Fase 0** prepara la infraestructura de pruebas:
  - Agrega `com.h2database:h2` con scope `test` al `pom.xml`.
  - Crea `src/test/resources/application-test.properties` con una base H2 en modo MySQL (`jdbc:h2:mem:vetlink;MODE=MySQL;DATABASE_TO_LOWER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE`), `spring.jpa.hibernate.ddl-auto=create-drop` (**solo en pruebas**) y la misma `physical-strategy` del proyecto.
  - Crea una clase base `src/test/java/com/vetlink/support/PruebaWebBase.java` con `@SpringBootTest`, `@AutoConfigureMockMvc` y `@ActiveProfiles("test")`, y métodos de ayuda para crear un dueño, una clínica, una recepcionista (principal o no), un veterinario, una mascota y una cita. Para autenticar en `MockMvc` usa `SecurityMockMvcRequestPostProcessors.user(new VetlinkUserPrincipal(usuario, correo))`.
  - Si H2 no logra crear el esquema a partir de las entidades, prueba con Testcontainers MySQL. Si tampoco funciona, deja escritas las pruebas, anótalo en el reporte y sigue con la verificación manual descrita en cada tarea.
- **Cada tarea de seguridad (Fase 1) necesita al menos una prueba** que haga el ataque descrito en "Verificación" y espere `403` (o redirección a `/403`). Nombre sugerido de las clases: `VL01RegistroRolTest`, `VL02RecepcionMascotaTest`, etc.
- Envía siempre el token CSRF en las peticiones `POST` de las pruebas (`.with(csrf())`).

## Reporte por fase

Al terminar cada fase, responde con:

1. Una tabla con las columnas: Tarea · Archivos modificados · Prueba creada · Resultado (hecho / bloqueado y por qué).
2. Salida resumida de `./mvnw test` (cuántas pruebas pasan o fallan).
3. Lista de sentencias agregadas a `V5_correcciones.sql`.
4. Cualquier decisión que tomaste y que no estaba en estas instrucciones.

---

# FASE 0 — Preparación

### VL-00 · Preparar migración y pruebas

- Crea `database/migrations/V5_correcciones.sql` con un encabezado que diga que se ejecuta una sola vez después de V4, y que es necesario hacer un respaldo de `mydb` antes de aplicarlo.
- Prepara la infraestructura de pruebas descrita arriba.
- Crea una prueba trivial que arranque el contexto y haga `GET /` esperando `200`.
- **Verificación:** `./mvnw test` pasa.

---

# FASE 1 — Seguridad (críticos)

### VL-01 · El registro público permite crear cuentas de recepcionista o veterinario en cualquier clínica

**Problema:** `POST /registro` acepta el campo `rol` que envía el navegador. Si alguien cambia el campo oculto a `RECEPCIONISTA` y agrega `idVeterinariaRecepcion=1`, queda como recepcionista de la clínica 1. Con `rol=VETERINARIO` e `idVeterinaria=1` queda como veterinario de esa clínica.

**Dónde:** `usuario/controller/AuthController.java`, método `registrar`. El servicio `UsuarioServiceImpl.registrar` también lo usa el panel de administración, así que **no cambies el servicio**: la restricción va en el controlador público.

**Cambio requerido:** al inicio de `AuthController.registrar`:

```java
String rol = form.getRol() == null ? "" : form.getRol().trim().toUpperCase();
boolean esDuenio = rol.equals("TUTOR") || rol.equals("DUEÑO");
boolean esClinicaNueva = (rol.equals("VETERINARIA") || rol.equals("RECEPCIONISTA"))
        && form.getNuevaVeterinaria() != null;
if (!esDuenio && !esClinicaNueva) {
    throw new AccessDeniedException("Tipo de registro no permitido");
}
form.setIdVeterinaria(null);
form.setIdVeterinariaRecepcion(null);
```

**Verificación:**
- Prueba: `POST /registro` con `rol=RECEPCIONISTA`, sin `nuevaVeterinaria` y con `idVeterinariaRecepcion=<id de una clínica existente>` → 403, y la tabla `recepcionista` sigue con las mismas filas.
- Prueba: lo mismo con `rol=VETERINARIO` → 403.
- Prueba: el registro de un dueño normal sigue funcionando (redirige a `/login?registrado`).

### VL-02 · Una recepcionista puede ver y editar cualquier mascota de la plataforma

**Problema:** `GET` y `POST /recepcion/mascotas/{id}/editar` no comprueban que la mascota tenga relación con la clínica de la recepcionista. El `POST` usa `mascotaService.actualizarSinVerificarTutor(form)`.

**Dónde:** `mascota/controller/RecepcionMascotaController.java`, métodos `editarForm` y `editar`.

**Cambio requerido:**
1. En `cita/repository/CitaRepository.java` agrega:
   ```java
   boolean existsByMascota_IdMascotaAndVeterinaria_IdVeterinaria(Integer idMascota, Integer idVeterinaria);
   ```
2. Inyecta `CitaRepository` en `RecepcionMascotaController` y crea un método privado:
   ```java
   private void verificarPacienteDeMiClinica(Integer idMascota, VetlinkUserPrincipal principal) {
       Integer miClinica = principal.getUsuario().getRecepcionista().getVeterinaria().getIdVeterinaria();
       if (!citaRepository.existsByMascota_IdMascotaAndVeterinaria_IdVeterinaria(idMascota, miClinica)) {
           throw new AccessDeniedException("Esta mascota no es paciente de tu clínica");
       }
   }
   ```
3. Llámalo al inicio de `editarForm` y de `editar`, antes de cargar o guardar nada.

**Verificación:**
- Prueba: una recepcionista de la clínica A hace `GET /recepcion/mascotas/{id}/editar` con una mascota sin citas en A → 403.
- Prueba: el mismo `POST` → 403, y la mascota no cambia.
- Prueba: con una mascota que tiene cita en A → 200.

### VL-03 · El carnet de vacunas está abierto y cualquier veterinario puede registrar vacunas a cualquier mascota

**Problema:**
- `/carnet/**` no tiene regla en `SecurityConfig` (cae en `anyRequest().authenticated()`). Cualquier usuario con sesión, incluido un dueño, ve el carnet de cualquier mascota en `GET /carnet/{idMascota}`.
- `POST /carnet/{idMascota}/nueva` deja que cualquier veterinario registre vacunas a mascotas que nunca atendió.
- En el `catch`, el mensaje de la excepción (que puede traer SQL interno) se pega a la URL.
- El servicio no valida la especie de la vacuna, el vencimiento del lote ni la fecha de aplicación.

**Dónde:** `vacuna/controller/CarnetVacunaController.java`, `vacuna/service/impl/CarnetVacunaServiceImpl.java` (método `registrar`), `security/SecurityConfig.java` y `historial/controller/HistorialClinicoController.java` (método privado `verificarAccesoPaciente`).

**Cambio requerido:**
1. Crea `src/main/java/com/vetlink/security/AccesoPacienteService.java` (un `@Service`) con el método `void verificar(Mascota mascota, VetlinkUserPrincipal principal)`. Su lógica es exactamente la de `HistorialClinicoController.verificarAccesoPaciente`, con dos mejoras:
   - Usa consultas `exists` en vez de cargar todas las citas. Agrega a `CitaRepository` el método `boolean existsByMascota_IdMascotaAndVeterinario_IdUsuario(Integer idMascota, Integer idVeterinario)` y reutiliza el de VL-02 para recepción.
   - Si el rol es `null`, lanza `AccessDeniedException`.
2. Reemplaza el método privado de `HistorialClinicoController` por llamadas a `AccesoPacienteService.verificar(...)`.
3. En `CarnetVacunaController`:
   - `ver(...)`: llama `accesoPacienteService.verificar(mascota, principal)` antes de agregar el carnet al modelo.
   - `registrar(...)`: si el rol no es `VETERINARIO` → `AccessDeniedException`. Luego `accesoPacienteService.verificar(...)`.
   - `catalogoPorEspecie(...)`: también llama a la verificación.
   - En el `catch` de `registrar`, usa `RedirectAttributes.addFlashAttribute("errorVacuna", mensaje)`. Si la excepción es `IllegalArgumentException` o `IllegalStateException`, el mensaje es el de la excepción; si es cualquier otra, usa "No se pudo registrar la vacuna". Redirige a `/historial/paciente/{idMascota}` sin parámetros en la URL. Ajusta `historial/paciente.html` para leer `errorVacuna` del modelo (flash) en lugar de `param.errorVacuna`.
4. En `SecurityConfig`, antes de `anyRequest()`, agrega `.requestMatchers(HttpMethod.POST, "/carnet/**").hasRole("VETERINARIO")`.
5. En `CarnetVacunaServiceImpl.registrar`, antes de guardar:
   - Si `!vacuna.getEspecie().getIdEspecie().equals(mascota.getEspecie().getIdEspecie())` → `IllegalArgumentException("La vacuna no corresponde a la especie de la mascota")`.
   - Si `lote.getFechaVencimiento().isBefore(fechaAplicacion)` → `IllegalArgumentException("El lote está vencido para esa fecha")`.
   - Si `fechaAplicacion.isAfter(LocalDate.now())` → `IllegalArgumentException("La fecha de aplicación no puede ser futura")`.

**Verificación:**
- Prueba: un dueño hace `GET /carnet/{id}` con una mascota ajena → 403.
- Prueba: un veterinario sin citas con la mascota hace `POST /carnet/{id}/nueva` → 403.
- Prueba: el veterinario que sí atendió la mascota registra una vacuna válida → redirige y se crea la fila.
- Prueba: una vacuna de otra especie → no se crea la fila y aparece el mensaje.
- Prueba: el historial clínico sigue funcionando igual para el dueño y para el veterinario autorizado.

### VL-04 · Un dueño puede cambiar el estado de cualquier emergencia, y recepción puede saltarse pasos

**Problema:** `POST /emergencias/{id}/estado` solo valida al veterinario no asignado y a la recepcionista de otra clínica. Cualquier otro rol, incluido el dueño, cambia el estado de cualquier emergencia. Tampoco hay reglas de qué estado puede seguir a cuál.

**Dónde:** `emergencia/controller/EmergenciaController.java`, método `estado`.

**Cambio requerido:** reescribe el método con estas reglas, en este orden:
1. Si la emergencia está `ATENDIDA` o `CANCELADA` → `IllegalStateException("La emergencia ya está cerrada")`.
2. Rol `TUTOR`: solo puede pasar a `CANCELADA`, solo si la mascota es suya y solo si la emergencia está `PENDIENTE`. En cualquier otro caso → `AccessDeniedException`.
3. Rol `RECEPCIONISTA`: primero `deMiClinica(id, p)`. Puede pasar a `ASIGNADA` (exige que la emergencia tenga veterinario) o a `CANCELADA`. **No puede** marcarla `ATENDIDA`.
4. Rol `VETERINARIO`: solo si es el veterinario asignado. Solo puede pasar de `ASIGNADA` a `ATENDIDA`.
5. Cualquier otra combinación → `AccessDeniedException("Cambio de estado no permitido")`.

Reformatea el controlador completo a un método por línea legible (hoy está comprimido en líneas muy largas), sin cambiar rutas ni nombres de plantilla. Si el `<select>` de estado en `emergencias/lista.html` ofrece opciones que el rol no puede usar, muestra solo las permitidas para cada rol.

**Verificación:**
- Prueba: un dueño envía `estado=ATENDIDA` a la emergencia de otro dueño → 403 y el estado no cambia.
- Prueba: un dueño cancela su propia emergencia pendiente → funciona.
- Prueba: una recepcionista intenta `ATENDIDA` → 403.
- Prueba: el veterinario asignado marca `ATENDIDA` una emergencia `ASIGNADA` → funciona.
- Prueba: intentar cambiar una emergencia `ATENDIDA` → error.

### VL-05 · Veterinarios y recepcionistas ven las citas de cualquier mascota

**Problema:** `GET /citas/mascota/{idMascota}` y `GET /citas/agendar/{idMascota}` usan `verificarAccesoTutor`, que solo bloquea cuando el usuario es `TUTOR` y la mascota no es suya. Para los demás roles deja pasar. `SecurityConfig` solo protege `/citas/mias/**`.

**Dónde:** `cita/controller/CitaController.java` (método `verificarAccesoTutor` y las dos rutas) y `security/SecurityConfig.java`.

**Cambio requerido:**
1. En `SecurityConfig`, antes de `anyRequest()`, agrega `.requestMatchers("/citas/mascota/**", "/citas/agendar/**").hasRole("TUTOR")`. Revisa que esto no rompa el `POST /citas/agendar` del dueño (que también es `TUTOR`).
2. En `verificarAccesoTutor`, quita la condición del rol: si la mascota no es del usuario de la sesión → `AccessDeniedException`.

**Verificación:**
- Prueba: un veterinario hace `GET /citas/mascota/{id}` → 403.
- Prueba: una recepcionista hace `GET /citas/agendar/{id}` → 403.
- Prueba: el dueño sigue viendo las citas de su mascota → 200.

### VL-06 · Recepción ve todas las mascotas y dueños de la plataforma, y puede crear mascotas a nombre de cualquier usuario

**Problema:**
- `RecepcionCitaController` llena el desplegable de mascotas con `mascotaRepository.findAll()`: las mascotas de toda la plataforma.
- `RecepcionMascotaController` llena el desplegable de dueños con `tutorRepository.findAll()`: todos los dueños del sistema.
- El `POST /recepcion/mascotas/nueva` recibe `idTutor` del navegador y no valida que sea un dueño. Se puede crear una mascota a nombre de un veterinario.
- `CitaServiceImpl.agendarComoRecepcionista` no valida la mascota.

**Dónde:** `cita/controller/RecepcionCitaController.java` (métodos `agendarForm` y `agendar`), `mascota/controller/RecepcionMascotaController.java` (métodos `nuevaForm`, `crear` y `cargarCatalogos`), `cita/service/impl/CitaServiceImpl.java` (método `agendarComoRecepcionista`) y las plantillas `citas/recepcion-agendar.html` y `recepcion/mascotas/formulario.html`.

**Cambio requerido:**
1. En `usuario/repository/TutorRepository.java` agrega un método para buscar **un** dueño por número de documento exacto y otro por correo exacto (el correo vive en la tabla `correo`; usa una consulta `@Query` con join si hace falta). Devuelven `Optional<Tutor>`.
2. En las dos pantallas de recepción, reemplaza el desplegable de dueños o de mascotas por:
   - Un formulario `GET` con un campo de texto `buscarDuenio` (documento o correo) y un botón "Buscar".
   - Si encuentra al dueño: muestra su nombre y, en la pantalla de agendar, un desplegable **solo con sus mascotas activas** (`estado = 1`). En la pantalla de registrar mascota, un campo oculto `idTutor` con su id.
   - Si no lo encuentra: el mensaje "No hay un dueño registrado con ese documento o correo".
3. En `RecepcionMascotaController.crear`: valida con `tutorRepository.existsById(idTutor)`; si no existe → `IllegalArgumentException("Dueño no válido")`, que se muestra en el formulario.
4. En `CitaServiceImpl.agendarComoRecepcionista`: si la mascota no está activa → `IllegalStateException("La mascota no está activa")`.
5. Quita `tutorRepository.findAll()` y `mascotaRepository.findAll()` de esos controladores.

**Verificación:**
- Prueba: `GET /citas/recepcion/agendar` no contiene en el HTML el nombre de una mascota de otra clínica que no se buscó.
- Prueba: `POST /recepcion/mascotas/nueva` con el `idTutor` de un veterinario → no crea la mascota y muestra el error.
- Prueba: buscar un dueño por documento muestra solo sus mascotas activas.

### VL-07 · El chat lista a todos los usuarios y permite escribirle a cualquiera

**Problema:** `ChatController.inicio` llena `contactos` con `usuarios.findAll()`: los 61 usuarios con su rol, incluidos los que no tienen rol (se ve "null"). `POST /chat/nuevo/{idContacto}` no valida a quién se le escribe. Las conversaciones existentes sí están protegidas por participante.

**Dónde:** `chat/controller/ChatController.java`.

**Cambio requerido:**
1. Crea `chat/service/ContactosChatService.java` con `List<Usuario> contactosPermitidos(Usuario yo)` y `boolean puedeEscribir(Usuario yo, Usuario otro)`, con estas reglas:
   - `TUTOR`: puede escribirle al personal (veterinarios y recepcionistas) de las clínicas donde alguna de sus mascotas tiene o tuvo una cita no cancelada.
   - `RECEPCIONISTA`: puede escribirle a los dueños de mascotas con cita en su clínica y al personal de su misma clínica.
   - `VETERINARIO`: puede escribirle a los dueños de mascotas que tienen o tuvieron cita con él y al personal de su misma clínica.
   - Nunca a usuarios sin rol, ni a usuarios con `estado = 0`, ni a sí mismo.
   - Usa consultas JPQL para obtener estos conjuntos. No cargues todos los usuarios y filtres en memoria.
2. En `inicio(...)`, usa `contactosPermitidos(...)` en vez de `findAll()`.
3. En `abrir(...)`, si `!puedeEscribir(yo, contacto)` → `AccessDeniedException("No puedes iniciar una conversación con este usuario")`.
4. En la plantilla `chat/lista.html`, si el rol de un contacto es `null` no lo muestres (protección extra), y muestra el rol con un texto legible ("Veterinario", "Recepción", "Dueño").

**Verificación:**
- Prueba: un dueño sin citas ve la lista de contactos vacía.
- Prueba: `POST /chat/nuevo/{id}` de un dueño a otro dueño → 403.
- Prueba: un dueño con cita en la clínica A ve a la recepcionista de A y puede abrir el chat.

### VL-08 · Quien registra una clínica nueva no puede administrarla

**Problema:** al registrar una clínica desde "Soy Veterinaria", la recepcionista que se crea queda con `esPrincipal = false` (el valor por defecto en `Recepcionista`), así que no puede entrar a `/veterinarias/admin`. Solo la migración V2 marcó principales, y solo para las clínicas que ya existían.

**Dónde:** `usuario/service/impl/UsuarioServiceImpl.java`, método `registrar`, rama `RECEPCIONISTA` cuando `form.getNuevaVeterinaria() != null`. Revisa también `pattern/factory/UsuarioFactory.crearRecepcionista`.

**Cambio requerido:** cuando se registra una clínica nueva, la recepcionista creada debe quedar con `esPrincipal = true`. Las recepcionistas creadas desde el panel de administración siguen con `false`.

**Verificación:**
- Prueba: registrar una clínica nueva → la recepcionista creada tiene `esPrincipal = true` y `GET /veterinarias/admin` con esa cuenta → 200.
- Prueba: una recepcionista creada desde el panel (`POST /veterinarias/admin/recepcionistas`) queda con `esPrincipal = false`.

**Al terminar la Fase 1:** entrega el reporte y espera confirmación.

---

# FASE 2 — Funcionalidad y reglas de negocio

### VL-09 · Una clínica recién registrada aparece en el directorio sin verificación

**Problema:** `VeterinariaRegistroServiceImpl` guarda la clínica nueva con `estado = 1`, así que aparece al instante en el directorio y en el formulario de emergencias.

**Dónde:** `veterinaria/service/impl/VeterinariaRegistroServiceImpl.java` (donde hace `veterinaria.setEstado((byte) 1)`).

**Cambio requerido:**
1. Guarda la clínica nueva con `estado = 0`.
2. Revisa que el directorio, el formulario de emergencias, el formulario de agendar y cualquier otra lista pública filtren por `estado = 1` (`findByEstado((byte) 1)`); corrige los que no lo hagan.
3. Revisa que la recepcionista principal de una clínica con `estado = 0` sí pueda iniciar sesión y entrar a su panel para completar la información.
4. En el dashboard de recepción, si la clínica tiene `estado = 0`, muestra un aviso: "Tu clínica está pendiente de verificación. Aún no aparece en el directorio."
5. Agrega a `V5_correcciones.sql`, comentado, el ejemplo de cómo aprobar una clínica: `-- UPDATE veterinaria SET estado = 1 WHERE idVeterinaria = ?;`

**Verificación:**
- Prueba: registrar una clínica → `estado = 0` y no aparece en `GET /directorio`.
- Prueba: su recepcionista principal entra al dashboard y ve el aviso.

### VL-10 · Nadie puede dejar reseñas de 1 o 2 estrellas, se puede reseñar sin haber sido atendido y no hay límite

**Problema:**
- `ResenaServiceImpl.crear` acepta valores de 1 a 5, pero `buscarPorValor` busca en la tabla `calificacion`, que solo tiene los valores 5, 4 y 3. Con 1 o 2 lanza "Calificación inválida".
- La consulta `ResenaRepository.tutorTuvoCitaEnVeterinaria` acepta cualquier cita no cancelada, incluidas las AGENDADAS a futuro.
- Un dueño puede dejar reseñas ilimitadas a la misma clínica.

**Dónde:** `resena/service/impl/ResenaServiceImpl.java` y `resena/repository/ResenaRepository.java`.

**Cambio requerido:**
1. En `V5_correcciones.sql`:
   ```sql
   -- VL-10
   INSERT INTO calificacion (idCalificacion, valor, descripcion) VALUES (4, 2, 'Malo'), (5, 1, 'Muy malo');
   ```
   Verifica antes que los ids 4 y 5 no existan; si existen, usa los siguientes libres.
2. En la consulta `tutorTuvoCitaEnVeterinaria`, cambia la condición a `c.estado = com.vetlink.cita.model.EstadoCita.ATENDIDA`.
3. Agrega a `ResenaRepository` el método `boolean existsByTutor_IdUsuarioAndVeterinaria_IdVeterinaria(Integer idTutor, Integer idVeterinaria)`, y en `crear` lanza `IllegalStateException("Ya dejaste una reseña para esta veterinaria")` si ya existe.
4. En el formulario de reseña, cambia el selector de calificación por 5 estrellas clicables (radio buttons con CSS; sin librerías nuevas), con valores del 1 al 5.

**Verificación:**
- Prueba: reseña de 1 estrella con una cita ATENDIDA → se guarda.
- Prueba: reseña con solo una cita AGENDADA futura → error.
- Prueba: una segunda reseña a la misma clínica → error.

### VL-11 · Cuando un dueño cancela, la hora se pierde para siempre

**Problema:** al cancelar, la cita pasa de AGENDADA a CANCELADA y el cupo no vuelve a estar disponible. Esa hora ya nadie la puede reservar.

**Dónde:** `cita/service/impl/CitaServiceImpl.java`, método `cancelarInterno`.

**Cambio requerido:** después de marcar la cita como CANCELADA, si `fecha + hora` es posterior al momento actual, crea un cupo nuevo (`new Cita()`) con el mismo veterinario, servicio, clínica, fecha y hora, en estado `DISPONIBLE` y sin mascota. La cita cancelada se conserva como historia. Todo dentro de la misma transacción.

**Verificación:**
- Prueba: ofertar un cupo, reservarlo y cancelarlo → existe un cupo DISPONIBLE nuevo con la misma fecha, hora, veterinario y servicio, y la cita original sigue CANCELADA.
- Prueba: al cancelar no se duplica el cupo si ya existe otro DISPONIBLE idéntico.

### VL-12 · El historial clínico muestra solo el año de la consulta

**Problema:** `historial_clinico.fechaConsulta` es un entero y se guarda solo el año (`LocalDate.now().getYear()`). Las pantallas muestran "AÑO 2025". La fecha completa está en `fechaRegistro`, que es NULL en los registros antiguos.

**Dónde:** `historial/service/impl/HistorialClinicoServiceImpl.java` (método `crear`) y todas las plantillas que muestran `fechaConsulta` (búscalas con `grep -rn "fechaConsulta" src/main/resources/templates`).

**Cambio requerido:**
1. En las plantillas: si `fechaRegistro` no es null, muestra `${#temporals.format(h.fechaRegistro, 'dd/MM/yyyy HH:mm')}`. Si es null, muestra "Año " + `fechaConsulta` con la nota "(fecha aproximada)".
2. Ordena los historiales por `fechaRegistro` descendente, dejando los NULL al final, en las consultas del repositorio que los listan.
3. No cambies el tipo de la columna `fechaConsulta` ni borres datos.

**Verificación:**
- Manual: el historial de una mascota muestra fecha y hora en los registros nuevos y "Año 2025 (fecha aproximada)" en los antiguos.
- Prueba: el orden de la lista es del más reciente al más antiguo.

### VL-13 · "Próxima cita" del dashboard del dueño muestra citas pasadas

**Problema:** `DashboardController` toma la primera cita AGENDADA ordenada por fecha, sin descartar las pasadas ni las de mascotas eliminadas. En la demo, el 10 de noviembre de 2026 muestra como próxima una cita del 15 de abril de 2026.

**Dónde:** `usuario/controller/DashboardController.java` (donde llama a `findByMascota_Tutor_IdUsuarioAndEstadoOrderByFechaAscHoraAsc`) y `cita/repository/CitaRepository.java`.

**Cambio requerido:**
1. Agrega en `CitaRepository` una consulta `@Query` que devuelva las citas AGENDADAS del dueño, de mascotas con `estado = 1`, con `fecha > hoy` o (`fecha = hoy` y `hora >= ahora`), ordenadas por fecha y hora ascendentes.
2. Úsala en el dashboard para "Próxima cita", para las notificaciones y para el calendario de próximas.
3. Revisa que "Vacunas pendientes" no cuente mascotas con `estado = 0`; corrige la consulta `pendientesDeTutorHasta` si las cuenta.

**Verificación:**
- Prueba: un dueño con una cita AGENDADA pasada y otra futura → `proximaCita` es la futura.
- Prueba: la cita futura de una mascota con `estado = 0` no aparece.

### VL-14 · No existe el domingo y cada día admite un solo horario

**Problema:** la tabla `dia_semana` tiene 6 filas (lunes a sábado). El registro de clínica no ofrece el domingo, y `CitaController.horariosDisponibles` nunca encuentra horario un domingo. Además, el método toma el primer horario del día con `findFirst()`, así que una clínica que abre de 8 a 12 y de 14 a 18 no se puede representar. Hay horarios inválidos guardados, por ejemplo de 20:34 a 00:23.

**Dónde:** datos de `dia_semana` y `horario_veterinaria`; `cita/controller/CitaController.java` (métodos `horariosDisponibles` y `nombreDiaEs`); el registro de clínica (`VeterinariaRegistroServiceImpl` y `templates/auth/registro.html`).

**Cambio requerido:**
1. En `V5_correcciones.sql`:
   ```sql
   -- VL-14
   INSERT INTO dia_semana (idDiaSemana, diaSemana) VALUES (7, 'Domingo');
   ```
   Verifica antes que el id 7 esté libre.
2. Valida en el registro de clínica y en el panel de administración que la hora de apertura sea menor que la de cierre. Agrega a `V5_correcciones.sql` una consulta comentada que liste los horarios inválidos existentes: `-- SELECT * FROM horario_veterinaria WHERE horaApertura >= horaCierre;`.
3. En `horariosDisponibles`, en vez de `findFirst()`, toma **todos** los horarios activos del día y devuelve una lista de rangos `{apertura, cierre}`. Cambia el `record HorariosDisponiblesResponse` para incluir esa lista, conservando los campos actuales para no romper el JS. Actualiza el JS del formulario de citas que consume esta respuesta para dibujar los rangos.
4. En el registro de clínica, permite agregar más de un rango por día (botón "Agregar otro horario").

**Verificación:**
- Prueba: una clínica con horario el domingo → `horariosDisponibles` un domingo devuelve `cerrado = false`.
- Prueba: una clínica con dos rangos el lunes → la respuesta trae los dos.
- Prueba: registrar un horario de 18:00 a 08:00 → error de validación.

### VL-15 · Se pueden ofertar y reservar cupos en horas que ya pasaron hoy

**Problema:** `ValidadorFechaFutura` compara solo la fecha y `OfertarCitaForm` usa `@FutureOrPresent` sobre la fecha. A las 3 p. m. se puede ofertar y reservar un cupo de las 8 a. m. del mismo día.

**Dónde:** `pattern/strategy/ValidadorFechaFutura.java`, `cita/service/impl/CitaServiceImpl.java` (métodos `ofertarCupo` y `reagendarComoRecepcionista`).

**Cambio requerido:**
1. En `ValidadorFechaFutura`:
   ```java
   if (form.getFecha() != null && form.getHora() != null
           && LocalDateTime.of(form.getFecha(), form.getHora()).isBefore(LocalDateTime.now())) {
       throw new IllegalStateException("No se permiten citas en horas pasadas.");
   }
   ```
2. Aplica la misma comprobación de fecha y hora en `ofertarCupo` y en `reagendarComoRecepcionista`.
3. En `CitaController.horariosDisponibles`, si la fecha es hoy, no devuelvas cupos cuya hora ya pasó.

**Verificación:**
- Prueba: ofertar un cupo hoy a una hora pasada → error.
- Prueba: reservar un cupo de hoy cuya hora ya pasó → error.
- Prueba: un cupo de mañana → funciona.

### VL-16 · Se puede agendar cita a una mascota eliminada

**Problema:** "eliminar" una mascota solo le pone `estado = 0`, pero `CitaServiceImpl.agendar` no revisa el estado.

**Dónde:** `cita/service/impl/CitaServiceImpl.java` (método `agendar`) y `cita/controller/CitaController.java` (método `agendarForm`, ruta `/agendar/{idMascota}`).

**Cambio requerido:**
1. En `agendar`, después de cargar la mascota: si `estado != 1` → `IllegalStateException("La mascota no está activa")`.
2. En `agendarForm`, si la mascota no está activa, redirige a `/mascotas` con un mensaje flash.

**Verificación:**
- Prueba: agendar para una mascota con `estado = 0` → error y no se crea la cita.

### VL-17 · "Próxima cita sugerida" es obligatoria en cada historia y acepta fechas pasadas

**Problema:** `HistorialClinicoForm.proximaCita` tiene `@NotNull` y la columna es NOT NULL. El veterinario tiene que inventar una fecha aunque no haga falta control, y puede poner una fecha pasada.

**Dónde:** `historial/dto/HistorialClinicoForm.java`, `historial/model/HistorialClinico.java` y las plantillas `historial/formulario.html` y `historial/atender-cita.html`.

**Cambio requerido:**
1. En el formulario: quita `@NotNull` y agrega `@FutureOrPresent(message = "La próxima cita no puede ser una fecha pasada")`.
2. En la entidad: `@Column(nullable = true)`.
3. En `V5_correcciones.sql`:
   ```sql
   -- VL-17
   ALTER TABLE historial_clinico MODIFY proximaCita DATE NULL;
   ```
4. En las plantillas, quita el `required` del campo y muestra "Sin control programado" cuando sea null.

**Verificación:**
- Prueba: guardar una historia sin próxima cita → se guarda.
- Prueba: con una próxima cita pasada → error de validación.

### VL-18 · Se puede atender una cita futura y la atención no es una sola transacción

**Problema:**
- `AgendaController.atender` permite atender cualquier cita AGENDADA, sin importar la fecha.
- Crear la historia (`historiales.crear(...)`) y marcar la cita como ATENDIDA (`citas.save(c)`) son dos guardados separados: si el segundo falla, queda una historia y la cita sigue AGENDADA.

**Dónde:** `cita/controller/AgendaController.java` (métodos `atenderForm` y `atender`).

**Cambio requerido:**
1. Crea `cita/service/AtencionService.java` con un método `@Transactional` `void atender(Integer idCita, HistorialClinicoForm form, Integer idVeterinario)` que:
   - valide que el veterinario es el de la cita;
   - valide que la cita está AGENDADA;
   - valide que la fecha de la cita es hoy (si no → `IllegalStateException("Solo se pueden atender citas del día de hoy")`);
   - valide que la mascota del formulario es la de la cita;
   - cree la historia y marque la cita ATENDIDA.
2. `AgendaController.atender` solo llama al servicio y maneja el error mostrando el mensaje en el formulario.
3. En `atenderForm`, si la cita no es de hoy, muestra el detalle con el mensaje en vez del formulario.
4. Reformatea `AgendaController` a métodos legibles (hoy está comprimido en líneas muy largas), sin cambiar rutas.

**Verificación:**
- Prueba: atender una cita de mañana → error y no se crea la historia.
- Prueba: atender una cita de hoy → se crea la historia y la cita queda ATENDIDA.
- Prueba: si se fuerza un error al guardar la cita (por ejemplo, con un mock), no queda historia creada.

**Al terminar la Fase 2:** entrega el reporte y espera confirmación.

---

# FASE 3 — Calidad y robustez

### VL-19 · La historia clínica se puede borrar y editar sin límite

**Problema:** el autor puede eliminar una historia durante 20 minutos y editarla para siempre. Un registro médico no debe desaparecer ni reescribirse sin rastro.

**Dónde:** `historial/controller/HistorialClinicoController.java` (rutas `/{id}/editar` y `/{id}/eliminar`), `historial/service/impl/HistorialClinicoServiceImpl.java` y la plantilla `historial/paciente.html`.

**Cambio requerido:**
1. Elimina la ruta `POST /historial/{id}/eliminar`, el método `eliminar` del servicio y el botón en la plantilla. También el cálculo de `minutosParaEliminar`.
2. Permite editar solo al autor y solo durante las 24 horas siguientes a `fechaRegistro`. Pasado ese plazo → `AccessDeniedException("La historia ya no se puede editar")`, y el botón Editar no aparece.
3. Para registros con `fechaRegistro` NULL (antiguos), no se permite editar.

**Verificación:**
- Prueba: `POST /historial/{id}/eliminar` → 404 o 405.
- Prueba: editar una historia de hace 2 días → 403.
- Prueba: el autor edita una historia de hace 1 hora → funciona.

### VL-20 · El perfil permite contraseñas de 1 carácter, no pide la contraseña actual, a veces no guarda el teléfono y no deja cambiar el correo

**Dónde:** `usuario/dto/PerfilForm.java`, `usuario/service/impl/UsuarioServiceImpl.java` (método `actualizarPerfil`), `usuario/controller/PerfilController.java` y `templates/perfil/ver.html`.

**Cambio requerido:**
1. Agrega a `PerfilForm` el campo `passwordActual`.
2. Si `nuevaPassword` no está vacía: debe tener al menos 8 caracteres, y `passwordActual` debe coincidir (`passwordEncoder.matches`). Si no, error de validación en el campo correspondiente.
3. Si el usuario no tiene fila en `telefono`, créala con el número escrito.
4. Agrega el campo `correo` al formulario. Si cambia, valida formato, valida que no esté en uso por otra cuenta (`correoRepository.existsByCorreoIgnoreCase`) y actualiza la fila de `correo`. Como el correo es el usuario de login, después de cambiarlo cierra la sesión y redirige a `/login?correoActualizado`, con un mensaje en la pantalla de login: "Tu correo se actualizó. Inicia sesión con el nuevo correo."
5. Si cambia la contraseña, también cierra la sesión y pide iniciar de nuevo.

**Verificación:**
- Prueba: nueva contraseña de 4 caracteres → error.
- Prueba: nueva contraseña con una contraseña actual incorrecta → error.
- Prueba: cambiar el correo a uno usado por otra cuenta → error.
- Prueba: un usuario sin teléfono guarda uno y queda la fila creada.

### VL-21 · Errores que terminan en página 500 o con mensajes rotos en la URL

**Problema:**
- `POST /veterinarias/admin/recepcionistas` llama a `usuarioService.registrar(form)` sin `try/catch`: un correo o documento repetido termina en error 500.
- `actualizarInfo` hace `redirect:/veterinarias/admin?error=" + e.getMessage()` sin codificar el texto: tildes, espacios o `&` cortan el mensaje.
- Cuando `registrarRecepcionista` tiene errores de validación, redirige sin mostrar qué campo falló.

**Dónde:** `veterinaria/controller/VeterinariaAdminController.java` y `templates/veterinarias/admin.html`.

**Cambio requerido:**
1. Usa `RedirectAttributes.addFlashAttribute("error", mensaje)` en todos los redirects con error de este controlador; ajusta la plantilla para leer `error` del modelo en vez de `param.error`.
2. Envuelve `usuarioService.registrar(form)` de `registrarRecepcionista` en un `try/catch`, igual que `registrarVeterinario`.
3. Si hay errores de validación al registrar una recepcionista, vuelve a la vista con los errores por campo, igual que hace `registrarVeterinario`.
4. Busca en todo el proyecto otros `"redirect:...?" + e.getMessage()` (`grep -rn 'getMessage()' src/main/java | grep redirect`) y aplica el mismo cambio.

**Verificación:**
- Prueba: registrar una recepcionista con un correo repetido → 200 con el mensaje de error, no 500.
- Manual: un error con tildes se muestra completo.

### VL-22 · La foto de la mascota es una URL escrita a mano

**Problema:** `MascotaForm.foto` es un texto donde el dueño pega una URL. Si no pone nada, las plantillas muestran una imagen rota.

**Dónde:** `mascota/dto/MascotaForm.java`, `mascota/service/impl/MascotaServiceImpl.java`, `mascota/controller/MascotaController.java` y las plantillas `mascotas/formulario.html`, `mascotas/detalle.html`, `mascotas/lista.html`, `mascotas/carnet.html` y `historial/paciente.html`.

**Cambio requerido:**
1. Mínimo obligatorio: en todas las plantillas que muestran la foto, si `foto` está vacía, muestra un ícono de huella (Font Awesome, que el proyecto ya usa) en lugar de `<img>`.
2. Subida real:
   - Cambia el campo del formulario por `<input type="file" accept="image/jpeg,image/png">` y recibe un `MultipartFile` en el controlador.
   - Valida tipo (`image/jpeg` o `image/png`) y tamaño máximo de 5 MB.
   - Guarda el archivo en `uploads/mascotas/{idMascota}/{uuid}.{ext}` y la ruta relativa en la columna `foto`.
   - Crea `GET /mascotas/{id}/foto`, que devuelve la imagen solo a quien tenga acceso a la mascota (usa `AccesoPacienteService` de VL-03).
   - Las plantillas usan esa ruta.
   - Las fotos antiguas que ya son URLs (empiezan por `http`) se siguen mostrando tal cual.

**Verificación:**
- Prueba: subir un `.pdf` como foto → error.
- Prueba: subir un PNG de menos de 5 MB → se guarda y `GET /mascotas/{id}/foto` lo devuelve al dueño.
- Prueba: otro dueño pide esa foto → 403.
- Manual: una mascota sin foto muestra el ícono, no una imagen rota.

### VL-23 · Reagendar desde recepción ignora los cupos y no valida fechas

**Problema:** `reagendarComoRecepcionista` cambia la fecha y hora de la cita libremente:
- No valida fechas pasadas.
- No valida que la mascota esté libre a esa hora.
- No usa los cupos ofertados.
- No libera la hora original.
- Si se mueve a una hora que tenía un cupo DISPONIBLE, ese cupo queda visible pero falla al reservarlo.

**Dónde:** `cita/service/impl/CitaServiceImpl.java` (método `reagendarComoRecepcionista`), `cita/controller/RecepcionCitaController.java` (método `reagendar`) y la plantilla donde se reagenda.

**Cambio requerido:** reagendar pasa a ser "mover la cita a otro cupo DISPONIBLE", en una sola transacción:
1. El método recibe `idCita` e `idCupoNuevo` (en lugar de fecha y hora libres).
2. Valida que el cupo nuevo sea de la misma clínica, esté DISPONIBLE y su fecha y hora sean futuras.
3. Valida que la mascota no tenga otra cita AGENDADA a esa fecha y hora.
4. Pasa el cupo nuevo a AGENDADA con la mascota de la cita.
5. Devuelve la cita original a DISPONIBLE y le quita la mascota.
6. En la interfaz, al pulsar "Reagendar" se muestran los cupos DISPONIBLES futuros del mismo servicio en la clínica, para elegir uno.

**Verificación:**
- Prueba: reagendar a un cupo DISPONIBLE futuro → la cita queda en el nuevo horario y el horario viejo vuelve a estar disponible.
- Prueba: reagendar a un cupo pasado o de otra clínica → error.
- Prueba: reagendar a una hora en que la mascota ya tiene otra cita → error.

### VL-24 · Hibernate modifica el esquema solo y hay inconsistencias en la base

**Problema:**
- `application.properties` tiene `spring.jpa.hibernate.ddl-auto=update`: Hibernate altera tablas al arrancar.
- `emergencia.idMascota` no tiene llave foránea, y su tipo (`int`) no coincide con `mascota.idMascota` (`int unsigned`).
- Hay tablas con charset `utf8` y otras con `utf8mb4`.

**Dónde:** `src/main/resources/application.properties` y `database/migrations/V5_correcciones.sql`.

**Cambio requerido:**
1. En `V5_correcciones.sql`:
   ```sql
   -- VL-24
   ALTER TABLE emergencia MODIFY idMascota INT(10) UNSIGNED NULL;
   ALTER TABLE emergencia ADD CONSTRAINT fk_emergencia_mascota FOREIGN KEY (idMascota) REFERENCES mascota(idMascota);
   ```
   Antes del `ADD CONSTRAINT`, incluye una consulta comentada para detectar emergencias que apunten a mascotas inexistentes, y avisa en el reporte si encuentras alguna en los datos de `mydb`.
2. Agrega también, comentadas y con la advertencia "ejecutar solo con respaldo", las sentencias `ALTER TABLE ... CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;` para las tablas que están en `utf8`.
3. Cambia `spring.jpa.hibernate.ddl-auto=update` por `validate` en `application.properties`. Arranca la aplicación contra una base `mydb` con V2, V3, V4 y V5 aplicadas. Si la validación falla, **no vuelvas a poner `update`**: corrige la entidad o agrega la sentencia SQL que falte, y anótalo en el reporte.
4. Cambia `spring.jpa.show-sql=true` por `false`.

**Verificación:**
- Manual: la aplicación arranca con `validate` sin errores.
- La prueba de contexto sigue pasando (el perfil de pruebas mantiene `create-drop`).

### VL-25 · Consultas que cargan tablas completas en memoria

**Problema:** varias verificaciones cargan todas las citas o todos los usuarios para encontrar uno:
- `AgendaController.autorizada` y `citasPara`.
- `CitaController.veterinariosDe` (`usuarioRepository.findAll()`).
- `EmergenciaController.listar` (veterinarios con `usuarios.findAll()`).
- `HistorialClinicoController.misPacientes`.

**Cambio requerido:**
1. `AgendaController.autorizada`: carga la cita por id y comprueba directamente que `cita.getVeterinario()` sea el usuario (rol veterinario) o que `cita.getVeterinaria()` sea la clínica de la recepcionista.
2. `veterinariosDe` y `EmergenciaController.listar`: usa `veterinarioRepository.findByVeterinaria_IdVeterinaria(id)`, que ya existe.
3. `misPacientes`: crea consultas JPQL `select distinct c.mascota from Cita c where ...` en lugar de recorrer todas las citas.
4. Si VL-03 ya creó los métodos `exists`, reutilízalos.

**Verificación:** las pruebas de permisos de la Fase 1 siguen pasando, y las pantallas de agenda, pacientes y emergencias muestran los mismos datos que antes.

### VL-26 · Restos de pruebas en el repositorio y una ruta pública sin uso

**Cambio requerido:**
1. En `SecurityConfig`, quita `"/uploads/**"` de la lista de `permitAll()`.
2. Elimina del repositorio los archivos de `uploads/historial/2/` y la carpeta `.tmp/`.
3. Agrega `uploads/` y `.tmp/` al `.gitignore`.
4. Revisa que `application.properties` no contenga contraseñas reales; si tiene, cámbialas por `${DB_PASSWORD:}` y documenta la variable en el README.

**Verificación:** `git status` ya no muestra esos archivos y `GET /uploads/...` sin sesión redirige al login.

### VL-27 · Datos de prueba que se ven mal en una demo

**Problema:**
- El usuario 4 (Carlos) no tiene rol y no puede iniciar sesión.
- Hay emergencias con motivos como "morir" y "no rspira".
- Hay clínicas llamadas "vetlolita" o "clinica don jose".
- Una clínica se llama "VetLink", igual que la plataforma.

**Cambio requerido:** crea `database/migrations/V6_datos_demo.sql` (opcional, separado de V5) con sentencias `UPDATE`, sin `DELETE`, que:
1. Den al usuario 4 una fila en `tutor` con una dirección de ejemplo.
2. Corrijan los textos de emergencias, historiales y descripciones de clínicas que tengan errores de ortografía o textos inapropiados para una demo. Lista en el reporte cada cambio, con el valor anterior y el nuevo.
3. Dejen un `UPDATE` comentado para renombrar la clínica "VetLink", con un nombre a elegir por el equipo.

No ejecutes este archivo; solo créalo.

### VL-28 · La portada muestra cifras y promesas que no son ciertas

**Problema:** `index.html` muestra "10K+ mascotas atendidas", "500+ veterinarios", "50+ clínicas asociadas", "4.9/5", "Soporte 24/7", "Cancelación gratuita" y "Sin tarjeta de crédito".

**Dónde:** `templates/index.html` y `comun/controller/PaginasPublicasController.java`.

**Cambio requerido:**
1. En `PaginasPublicasController`, para la ruta `/`, calcula con los repositorios: número de clínicas con `estado = 1`, número de veterinarios, número de mascotas con `estado = 1` y el promedio general de reseñas (redondeado a un decimal). Pásalos al modelo.
2. En `index.html`, muestra esas cifras reales en la franja de estadísticas.
3. Quita los textos "Soporte 24/7", "Cancelación gratuita" y "Sin tarjeta de crédito". Puedes reemplazarlos por afirmaciones verdaderas del sistema, por ejemplo "Historial clínico en un solo lugar", "Agenda en línea con las clínicas" y "Reseñas de dueños verificados".

**Verificación:** `GET /` muestra las cifras de la base de datos (con los datos de prueba: 10 clínicas, 10 veterinarios y las mascotas activas).

**Al terminar la Fase 3:** entrega el reporte final, que incluya además:
- La lista completa de archivos SQL nuevos y el orden en que deben ejecutarse.
- Cómo correr todas las pruebas.
- Cualquier tarea que haya quedado bloqueada y por qué.

---

# RESULTADOS DE LA EJECUCIÓN

> Ejecutado el 2026-11-13 en la rama `fix/monton-de-cositas`. Un commit por tarea (`fix(VL-XX): ...`), 29 commits en total.
> Resultado final de `./mvnw clean test`: **112 pruebas, 0 fallos, 0 errores, 0 omitidas — BUILD SUCCESS**.
> Ninguna tarea quedó bloqueada; la única parte que no se pudo hacer tal como estaba descrita es la validación de horarios en el panel de administración (VL-14, ver abajo).

## Resumen por tarea

| Tarea | Commit | Archivos principales | Prueba creada | Resultado |
|---|---|---|---|---|
| VL-00 | `a824be3` | `pom.xml` (H2 test), `application-test.properties`, `PruebaWebBase`, `V5_correcciones.sql` | `VL00ContextoTest` (2) | Hecho |
| VL-01 | `8a770cb` | `AuthController` | `VL01RegistroRolTest` (3) | Hecho |
| VL-02 | `52e7738` | `CitaRepository`, `RecepcionMascotaController` | `VL02RecepcionMascotaTest` (3) | Hecho |
| VL-03 | `5fb6b80` | `AccesoPacienteService` (nuevo), `CitaRepository`, `HistorialClinicoController`, `CarnetVacunaController`, `CarnetVacunaServiceImpl`, `SecurityConfig`, `historial/paciente.html` | `VL03CarnetVacunaTest` (7) | Hecho |
| VL-04 | `920d63b` | `EmergenciaController` (reescrito legible), `emergencias/lista.html` | `VL04EmergenciaEstadoTest` (8) | Hecho |
| VL-05 | `b5d23d5` | `SecurityConfig`, `CitaController` | `VL05CitasMascotaTest` (5) | Hecho |
| VL-06 | `28801ec` | `TutorRepository`, `RecepcionCitaController`, `RecepcionMascotaController`, `CitaServiceImpl`, `citas/recepcion-agendar.html`, `recepcion/mascotas/formulario.html` | `VL06RecepcionDueniosTest` (6) | Hecho |
| VL-07 | `a91a0fa` | `ContactosChatService` (nuevo), `ChatController`, `chat/lista.html` | `VL07ChatContactosTest` (6) | Hecho |
| VL-08 | `c2062e6` | `UsuarioServiceImpl`, `VeterinariaAdminController` | `VL08ClinicaNuevaPrincipalTest` (2) | Hecho |
| VL-09 | `044a087` | `VeterinariaRegistroServiceImpl`, `VeterinariaController`, `EmergenciaController`, `dashboard/recepcionista.html` | `VL09ClinicaPendienteTest` (3) | Hecho |
| VL-10 | `3524e9c` | `ResenaRepository`, `ResenaServiceImpl`, `veterinarias/detalle.html`, `app.css` | `VL10ResenasTest` (4) | Hecho |
| VL-11 | `85fcc07` | `CitaServiceImpl.cancelarInterno` | `VL11CancelarLiberaCupoTest` (2) | Hecho |
| VL-12 | `a9a9f6c` | `HistorialClinicoRepository`, `HistorialClinicoServiceImpl`, 4 plantillas | `VL12FechaHistorialTest` (2) | Hecho |
| VL-13 | `9ea7958` | `CitaRepository`, `CarnetVacunaRepository`, `DashboardController` | `VL13ProximaCitaTest` (3) | Hecho |
| VL-14 | `a40e1f2` | `CitaController`, `VeterinariaRegistroServiceImpl`, `auth/registro.html`, `registro.js`, `registro_style.css`, `citas/recepcion-agendar.html` | `VL14HorariosTest` (4) | Hecho, salvo la validación en el panel de administración (no tiene pantalla de horarios) |
| VL-15 | `d621bfc` | `ValidadorFechaFutura`, `CitaServiceImpl`, `CitaController` | `VL15HorasPasadasTest` (3) | Hecho |
| VL-16 | `b396fd5` | `CitaServiceImpl.agendar`, `CitaController.agendarForm`, `mascotas/lista.html` | `VL16MascotaInactivaTest` (2) | Hecho |
| VL-17 | `dd3a6cd` | `HistorialClinicoForm`, `HistorialClinico`, `atender-cita.html`, `formulario.html`, `lista.html`, `mascotas/detalle.html` | `VL17ProximaCitaOpcionalTest` (3) | Hecho |
| VL-18 | `6ef6d4b` | `AtencionService` (nuevo), `AgendaController` (reescrito legible), `detalle-profesional.html`, `atender-cita.html` | `VL18AtenderCitaTest` (4), `VL18AtencionTransaccionTest` (1, con mock) | Hecho |
| VL-19 | `217d6bf` | `HistorialClinicoController`, `HistorialClinicoService(Impl)`, `historial/paciente.html` | `VL19HistoriaInmutableTest` (6) | Hecho |
| VL-20 | `7a5ae71` | `PerfilForm`, `PerfilController`, `UsuarioService(Impl)`, `perfil/ver.html`, `auth/login.html` | `VL20PerfilTest` (6) | Hecho |
| VL-21 | `c65cb78` | `VeterinariaAdminController`, `veterinarias/admin.html` | `VL21ErroresAdminTest` (4) | Hecho (además se corrigió un bug grave, ver decisiones) |
| VL-22 | `06f003e` | `FotoMascotaService` (nuevo), `MascotaController`, `Mascota`, `MascotaServiceImpl`, `SecurityConfig`, 4 plantillas | `VL22FotoMascotaTest` (5) | Hecho |
| VL-23 | `dadf3db` | `CitaService(Impl)`, `CitaRepository`, `RecepcionCitaController`, `citas/reagendar.html` (nueva), `citas/reporte.html` | `VL23ReagendarTest` (5) | Hecho |
| VL-24 | `08f5c7f` | `application.properties`, `V5_correcciones.sql` | Arranque manual contra MySQL 8.4 | Hecho: arranca con `validate` sin errores |
| VL-25 | `d37d5ee` | `AgendaController`, `CitaController`, `EmergenciaController`, `HistorialClinicoController`, `MascotaRepository` | `VL25ConsultasDirectasTest` (4) | Hecho |
| VL-26 | `a5d0101` | `SecurityConfig`, `.gitignore`; borrados `uploads/historial/2/*` y `.tmp/` | `VL26UploadsPrivadosTest` (1) | Hecho |
| VL-27 | `75a1415` | `V6_datos_demo.sql` (nuevo) | Ensayo en transacción con rollback | Hecho |
| VL-28 | `9295570` | `PaginasPublicasController`, `VeterinariaRepository`, `MascotaRepository`, `ResenaRepository`, `index.html` | `VL28PortadaRealTest` (2) | Hecho: portada muestra 10 clínicas, 10 veterinarios, 56 mascotas y 3,8/5, igual que la base |

## Sentencias de `V5_correcciones.sql`

- VL-09 (comentada): `-- UPDATE veterinaria SET estado = 1 WHERE idVeterinaria = ?;`
- VL-10: `INSERT INTO calificacion (idCalificacion, valor, descripcion) VALUES (4, 2, 'Malo'), (5, 1, 'Muy malo');` — ids 4 y 5 verificados libres en `mydb`.
- VL-14: `INSERT INTO dia_semana (idDiaSemana, diaSemana) VALUES (7, 'Domingo');` — id 7 verificado libre.
- VL-14 (comentada): `-- SELECT * FROM horario_veterinaria WHERE horaApertura >= horaCierre;` — en `mydb` existe uno: id 5 (20:34 a 00:23) de la clínica 5.
- VL-17: `ALTER TABLE historial_clinico MODIFY proximaCita DATE NULL;`
- VL-24: consulta comentada de emergencias huérfanas (en `mydb` no hay ninguna: las 20 emergencias apuntan a mascotas existentes), `ALTER TABLE emergencia MODIFY idMascota INT(10) UNSIGNED NULL;` y `ALTER TABLE emergencia ADD CONSTRAINT fk_emergencia_mascota ...`.
- VL-24 (comentadas, "ejecutar solo con respaldo"): 36 `ALTER TABLE ... CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;` para las tablas que siguen en `utf8`. Probadas en una copia desechable: funcionan.

## Archivos SQL y orden de ejecución

Hacer respaldo de `mydb` antes de empezar.

1. `database/mydb.sql`
2. `V2`, `V3` y `V4` **con `mysql --force`**. El `mydb.sql` actual es un volcado tomado después de que Hibernate `update` alterara el esquema, así que ya trae casi todo lo de V2–V4 (`esPrincipal`, `conversacion`, `mensaje_chat`, `emergencia`, `nombreArchivo`…). Sin `--force` cada archivo se detiene en el primer "ya existe" y no aplica lo que falta.
3. `V5_correcciones.sql` (corre sin errores).
4. Opcional: `V6_datos_demo.sql` (no se ejecutó en la base real; solo se ensayó en una transacción revertida sobre una copia desechable).

## Cómo correr las pruebas

```bash
./mvnw test
```

No necesitan MySQL: usan H2 en memoria con el perfil `test` (`src/test/resources/application-test.properties`, con `create-drop` solo ahí). Cada contexto de Spring usa su propia base (`jdbc:h2:mem:vetlink-${random.uuid}`).

## Decisiones que no estaban en las instrucciones

**Fase 0**
- El perfil de pruebas usa `H2Dialect` en lugar de `MySQLDialect` (la URL sí está en modo MySQL) y `DB_CLOSE_DELAY=-1`.
- `VetlinkApplicationTests` ahora usa `@ActiveProfiles("test")`; sin eso `./mvnw test` exigía un MySQL encendido.
- `PruebaWebBase` es `@Transactional`: cada prueba se revierte al final.

**Fase 1**
- VL-01: quien se registra es anónimo y Spring convierte un `AccessDeniedException` de un anónimo en redirección a `/login`. Se agregó un `@ExceptionHandler` solo en `AuthController` que responde 403 con `error/403`.
- VL-03: la recepcionista ya no puede registrar vacunas (solo `VETERINARIO`); el formulario le sigue apareciendo pero el servidor responde 403.
- VL-04: el `IllegalStateException` (emergencia cerrada, falta veterinario) vuelve a `/emergencias` con el mensaje en vez de un 500. Cada rol ve solo los cambios de estado que el servidor le permite.
- VL-06: la búsqueda usa correo si el texto tiene `@` (sin distinguir mayúsculas) y documento exacto si no.
- VL-07: las consultas JPQL están en `ContactosChatService` (con `EntityManager`). Recepción y veterinario ven a los dueños con cita aunque esté cancelada (el texto solo excluye canceladas para el dueño).
- VL-08: en `POST /veterinarias/admin/recepcionistas` se fuerza `nuevaVeterinaria = null`, para que desde el panel no se pueda crear una clínica y quedar como principal.

**Fase 2**
- VL-09: una clínica pendiente tampoco se ve en `/directorio/{id}` ni puede recibir emergencias.
- VL-11: para no duplicar, se revisa si el veterinario ya tiene un cupo DISPONIBLE a esa fecha y hora (cualquier servicio).
- VL-13: el calendario muestra las próximas citas de la nueva consulta más el historial; una AGENDADA pasada ya no se pinta como próxima.
- VL-14: el panel de administración no tiene pantalla de horarios, así que la validación apertura < cierre quedó en el servicio de registro. También se rechazan rangos que se cruzan el mismo día, y `horariosDisponibles` ignora los horarios inválidos guardados. Los rangos extra se envían como más entradas `horarios[N]`; el JS renumera los índices al quitar uno. Si el registro falla y se vuelve a mostrar el formulario, los rangos extra se pierden. El botón "Agregar otro horario" solo se revisó con `node --check`; hay que probarlo a mano en el navegador.
- VL-15: el filtro de horas pasadas de hoy aplica a la lista de cupos ofertados; en el modo de rangos de recepción las horas pasadas se dibujan, pero el servidor las rechaza.
- VL-17: editar una historia cuya próxima cita ya quedó en el pasado pide corregirla.
- VL-18: si la cita no está AGENDADA, el formulario también muestra el detalle con el mensaje (antes era 500). Otro veterinario recibe 403.

**Fase 3**
- VL-19: las historias antiguas (sin `fechaRegistro`) no se editan; al editar, la mascota de la historia no cambia.
- VL-20: cambiar solo la contraseña también cierra la sesión (aviso en el login).
- VL-21: **bug grave que ya existía**: registrar personal desde el panel nunca funcionaba. El formulario no envía `rol` ni `confirmarPassword` (obligatorios en `RegistroUsuarioForm`) y el controlador los fijaba después de validar. Ahora se ignoran los errores de esos dos campos. Solo el formulario de recepcionistas muestra errores por campo; el de veterinarios no los tenía.
- VL-22: además del Content-Type se revisa la firma del archivo (un PDF renombrado a PNG se rechaza). El servicio ya no guarda la foto escrita en el formulario (antes, editar desde recepción borraba la foto). `GET /mascotas/{id}/foto` se abrió a cualquier usuario autenticado en `SecurityConfig` y el acceso real lo decide `AccesoPacienteService`. `mascotas/carnet.html` no muestra la foto, así que ahí no hubo cambio.
- VL-23: si no hay cupos, la página de reagendar ofrece un botón para ofertar uno.
- VL-25: se quitó la prueba antigua que buscaba el texto `c.getMascota() != null` en el controlador; el comportamiento lo cubre `VL25ConsultasDirectasTest`.
- VL-26: `application.properties` no tiene contraseñas reales (`password=` vacío), así que no se cambió por `${DB_PASSWORD:}`.
- VL-27: el usuario 4 recibe su fila en `tutor` con un `INSERT` protegido contra duplicados (no se puede crear con `UPDATE`). Los textos se corrigen por valor exacto (`WHERE BINARY col = '...'`): 45 cambios, 268 filas. Los nombres de las clínicas 1 (VetLink), 2 (Vetlink2), 5 (clinica don jose) y 6 (vetlolita) quedan como `UPDATE` comentados.
- VL-28: el promedio se muestra con coma (3,8/5) y "Sin reseñas" si no hay ninguna. En la portada siguen "Emergencias 24/7" y "Disponible 24/7", que la tarea no mencionaba; revisar si se mantienen.

**Entorno**
- El equipo no tenía JDK: se compiló y probó con `eclipse-temurin:21-jdk` en podman. Para VL-24 se usó un MySQL 8.4 temporal (puerto 3399, sin volumen), ya eliminado; no se tocó el MySQL general ni `compose.yaml`.

## Cambios de VL-27 (valor anterior → nuevo, filas)

| Columna | Antes | Después | Filas |
|---|---|---|---|
| historial_clinico.motivoConsulta | Caida de pelo | Caída de pelo | 10 |
| historial_clinico.motivoConsulta | Chequeo prequirurgico | Chequeo prequirúrgico | 9 |
| historial_clinico.motivoConsulta | Control de vacunacion | Control de vacunación | 6 |
| historial_clinico.motivoConsulta | Control geriatrico | Control geriátrico | 5 |
| historial_clinico.motivoConsulta | Deshidratacion | Deshidratación | 9 |
| historial_clinico.motivoConsulta | diarrea | Diarrea | 1 |
| historial_clinico.motivoConsulta | Infeccion de oido | Infección de oído | 8 |
| historial_clinico.motivoConsulta | Inflamacion en pata | Inflamación en pata | 5 |
| historial_clinico.motivoConsulta | Perdida de apetito | Pérdida de apetito | 13 |
| historial_clinico.motivoConsulta | Picazon en la piel | Picazón en la piel | 8 |
| historial_clinico.motivoConsulta | Reaccion alergica leve | Reacción alérgica leve | 6 |
| historial_clinico.motivoConsulta | Revision dental | Revisión dental | 7 |
| historial_clinico.motivoConsulta | Revision post quirurgica | Revisión posquirúrgica | 16 |
| historial_clinico.motivoConsulta | Sangrado en encias | Sangrado en encías | 3 |
| historial_clinico.motivoConsulta | Secrecion nasal | Secreción nasal | 5 |
| historial_clinico.motivoConsulta | Vomito | Vómito | 3 |
| historial_clinico.diagnostico | Dermatitis alergica | Dermatitis alérgica | 14 |
| historial_clinico.diagnostico | diarrea | Diarrea aguda | 1 |
| historial_clinico.diagnostico | Estres digestivo | Estrés digestivo | 8 |
| historial_clinico.diagnostico | Infeccion respiratoria | Infección respiratoria | 16 |
| historial_clinico.diagnostico | Infeccion urinaria | Infección urinaria | 12 |
| historial_clinico.diagnostico | Reaccion alergica a picadura | Reacción alérgica a picadura | 9 |
| historial_clinico.diagnostico | Sindrome vestibular leve | Síndrome vestibular leve | 8 |
| historial_clinico.observaciones | Buena evolucion clinica | Buena evolución clínica | 9 |
| historial_clinico.observaciones | Mejoria notable respecto a consulta anterior | Mejoría notable respecto a la consulta anterior | 24 |
| historial_clinico.observaciones | no coma basura | Evitar que consuma desechos o alimentos en mal estado | 1 |
| historial_clinico.observaciones | Se sugiere realizar examenes de laboratorio de control | Se sugiere realizar exámenes de laboratorio de control | 21 |
| emergencia.motivo | morir | Paro respiratorio | 1 |
| emergencia.motivo | dificultad respiratoria | Dificultad respiratoria | 1 |
| emergencia.motivo | Reaccion alergica grave | Reacción alérgica grave | 2 |
| emergencia.motivo | Vomito severo | Vómito severo | 1 |
| emergencia.detalle | no rspira | No respira; se inició reanimación | 1 |
| emergencia.detalle | tienen vomito | Presenta vómito | 1 |
| emergencia.detalle | Llego en estado critico | Llegó en estado crítico | 5 |
| emergencia.detalle | Necesito hospitalizacion | Necesitó hospitalización | 3 |
| emergencia.detalle | Requirio atencion inmediata | Requirió atención inmediata | 4 |
| emergencia.detalle | Se traslado a la sala de urgencias | Se trasladó a la sala de urgencias | 4 |
| veterinaria.descripcion | La mejor veterinaria de Bogota | La mejor veterinaria de Bogotá | 1 |
| veterinaria.descripcion | --- | Clínica veterinaria para perros y gatos | 1 |
| veterinaria.descripcion | clinica de prueba de don jose | Clínica veterinaria de barrio con atención personalizada | 1 |
| veterinaria.descripcion | vet de prueba | Atención veterinaria general para perros y gatos | 1 |
| veterinaria.descripcion | Atencion veterinaria de confianza | Atención veterinaria de confianza | 1 |
| veterinaria.descripcion | Clinica veterinaria integral 24 horas | Clínica veterinaria integral 24 horas | 1 |
| veterinaria.descripcion | Cuidado veterinario con mas de 10 anios de experiencia | Cuidado veterinario con más de 10 años de experiencia | 1 |
| veterinaria.descripcion | Servicio veterinario rapido y confiable | Servicio veterinario rápido y confiable | 1 |
