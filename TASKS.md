# TASKS · Revisión integral VetLink (2026-11-25)

Estados: **Pendiente** · **En progreso** · **Completado** · **Validado** · **Bloqueado**

Línea base antes de los cambios: `./mvnw test` → 223 pruebas, 0 fallos.

## Diagnóstico (causas raíz encontradas)

| # | Problema | Causa raíz |
|---|---|---|
| 1 | Tarjeta de cita: estado encima del servicio, hora partida | Columna de hora fija de 78 px y columna de estado `auto` con `white-space: nowrap`: en paneles estrechos el título se comprime a 0 y el estado lo tapa |
| 6 | Directorio lento al usar ubicación | `enableHighAccuracy: true` + espera de 15 s en el navegador, y en el servidor `cuposReservables` hace una consulta de ausencias **por cada cupo** (≈7 s por búsqueda medidos en local) |
| 7 | «Max» en el historial clínico | La carpeta del dueño usa `findByTutor_IdUsuario` sin filtrar `estado`: aparecen mascotas dadas de baja (estado 0), como «Max» (id 1) del seed `mydb.sql` |
| 9 | Recepción no puede reprogramar | «Citas por reprogramar» lista las citas del veterinario **en todas las clínicas**; un veterinario que cambió de clínica arrastra citas ajenas y al abrirlas la seguridad responde 403 (log de Nginx: `GET /citas/recepcion/278/reagendar 403`) |
| 14 | Error 500 al agregar «Ecografia» | La comparación de duplicados ignora mayúsculas pero no tildes; MySQL (`utf8mb4_0900_ai_ci`) sí, y el `INSERT` viola `nombreServicio_UNIQUE` |

## Fase 1 · UX/UI
- [x] 1. Tarjetas de citas sin superposición, responsive (incluido el panel del dueño) — **Validado** (capturas 1366/390 px)
- [x] 2. «Mis citas» con pestañas y filtros + limpiar — **Validado** (prueba `misCitasSeparaActivasEHistorialYFiltraPorMascota` + capturas)
- [x] 3. Rediseño de solicitudes pendientes (semana, período, servicios) — **Validado** (capturas)
- [x] 4. Alineación del formulario de agendamiento de recepción — **Validado** (capturas)

## Fase 2 · Funcional
- [x] 5. Mascota fallecida en solo lectura (frontend + backend) — **Validado** (prueba `mascotaFallecidaNoSeEditaNiPorLaApi`)
- [x] 6. Geolocalización del directorio — **Completado** (servidor medido: 7,2 s → 0,8 s; flujo del permiso no probado con un usuario real)
- [x] 7. Historial clínico solo con mascotas vigentes del usuario — **Validado** (prueba `historialDelDuenioNoMuestraMascotasDadasDeBajaNiAjenas`)
- [x] 8. «Día de ausencia»: autocompletar «Hasta» — **Completado** (validación del servidor probada; autocompletado en JS no probado en navegador)
- [x] 9. Reprogramación desde recepción — **Validado** (prueba `citasPorReprogramarSoloMuestraCitasDeLaClinicaYSePuedenReprogramar`)
- [x] 10. Confirmar / modificar / cancelar reprogramación — **Validado** (6 pruebas)
- [x] 11. Desbloquear agendamiento — **Validado** (prueba `recepcionDesbloqueaLaAgendaYQuedaTrazado`)
- [x] 12. Validación inmediata del registro + contraseña segura — **Completado** (backend probado; interacción JS no probada en navegador)

## Fase 3 · Nuevas funcionalidades
- [x] 13. Logo de la clínica + marca de agua VetLink en el documento clínico — **Completado** (los PDF de FastAPI ya lo tenían)
- [x] 14. Selector de servicios con buscador y «Otros» — **Validado** (backend: 2 pruebas; UI sin prueba en navegador)
- [x] 15. Aprobación automática de veterinarias — **Validado** (prueba VL09 actualizada)

## Fase 4 · Calidad
- [x] 16-18. Seguridad, base de datos (V10 aditiva) y rendimiento — **Completado** (ver CHANGELOG.md, «Pendientes»)
- [x] 19. 15 pruebas nuevas + regresión: 238/238 — **Validado**
- [x] CHANGELOG.md — **Completado**
