# Revisión inicial VetLink — V2

## Aplicar antes de iniciar

1. Importe `database/mydb.sql` en MySQL.
2. Ejecute en orden `database/migrations/V2_cupos_y_recepcionista_principal.sql`, `database/migrations/V3_chat_emergencias.sql` y `database/migrations/V4_atencion_y_archivos.sql`.
3. Configure `application.properties` con las credenciales locales y arranque con JDK 21.

## Correcciones incorporadas

- Los cupos `DISPONIBLE` ya no requieren una mascota; recepción puede publicarlos y el dueño solo reserva un cupo existente.
- `GET /citas/agendar` permite comenzar el agendamiento sin pasar antes por Mis mascotas.
- El detalle de una cita del dueño está protegido por propiedad de la mascota.
- Los reportes se fuerzan en servidor a la clínica de recepción o a la agenda del veterinario; los parámetros manipulados no amplían el acceso.
- El expediente clínico recuperó su ruta POST de creación y el acceso de veterinarios/recepción se valida contra una cita relacionada.
- La configuración de veterinaria exige la nueva marca `esPrincipal` también en el backend.
- Chat funcional: conversaciones privadas, mensajes y verificación de participantes.
- Emergencias funcionales: solicitud del dueño, asignación por recepción y actualización del caso por el veterinario asignado.
- Directorio y dashboard enlazan a los flujos reales de agendamiento, chat y emergencias.

## Verificación realizada

`mvn test -DskipTests` finalizó correctamente con JDK 21. La prueba de integración contra MySQL debe ejecutarse después de importar el esquema y aplicar la migración.

## Pendiente para siguientes iteraciones

Chat y emergencias no están representados por tablas en el SQL entregado, por lo que requieren una migración y definición de reglas de negocio antes de poder quedar conectados de extremo a extremo. Los cambios de esta entrega no inventan ese modelo.
