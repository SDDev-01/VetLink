# Auditoría contra el encargo de VetLink

## Cerrado en esta revisión

- **Fases 1, 2 y 6:** autenticación por sesión, separación de datos en citas, agendamiento directo del dueño, cupos publicados por clínica, detalle de cita, directorio con datos desde base de datos y diseño común responsive.
- **Fase 3:** los reportes de recepción se acotan en backend a su veterinaria; existe calendario/agenda de clínica, publicación de cupos, detalle profesional y administración de emergencias. La recepcionista principal se valida en backend para ajustes de clínica y equipo; puede crear veterinarios y recepcionistas adicionales.
- **Fase 4:** agenda privada por veterinario, detalle de paciente/expediente, vacunación separada, atención desde cita que registra historia y marca `ATENDIDA`, adjuntos del historial, perfil y emergencias asignadas.
- **Fase 10:** chat y emergencias tienen modelo, tablas, controladores, vistas y autorización por participante/asignación/pertenencia a clínica.

## Migraciones requeridas

Importe el esquema base y ejecute, en orden: `V2_cupos_y_recepcionista_principal.sql`, `V3_chat_emergencias.sql` y `V4_atencion_y_archivos.sql`.

## Verificación

El proyecto compila con JDK 21 mediante `mvn test -DskipTests`. Para las pruebas de extremo a extremo se requiere una instancia MySQL con las tres migraciones aplicadas; la configuración local está en `src/main/resources/application.properties`.

## Notas de diseño y seguridad

La eliminación de cuenta se implementa como baja lógica: deshabilita inicio de sesión pero preserva información clínica. Los adjuntos se almacenan bajo `uploads/historial/` y están limitados a 10 MB; los descarga únicamente alguien autorizado sobre la mascota.
