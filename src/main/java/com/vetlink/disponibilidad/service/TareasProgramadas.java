package com.vetlink.disponibilidad.service;

import com.vetlink.cita.service.CitaService;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tareas periódicas. Se desactivan con vetlink.reglas.tareas-habilitadas=false (perfil de pruebas).
 * Todas son idempotentes: si una ejecución se repite, no duplica cupos ni notificaciones.
 */
@Component
@ConditionalOnProperty(prefix = "vetlink.reglas", name = "tareas-habilitadas", havingValue = "true", matchIfMissing = true)
public class TareasProgramadas {

    private static final Logger log = LoggerFactory.getLogger(TareasProgramadas.class);

    private final DisponibilidadService disponibilidad;
    private final CitaService citas;
    private final VeterinariaRepository veterinarias;

    public TareasProgramadas(DisponibilidadService disponibilidad, CitaService citas, VeterinariaRepository veterinarias) {
        this.disponibilidad = disponibilidad;
        this.citas = citas;
        this.veterinarias = veterinarias;
    }

    /** Generación de agenda para todas las clínicas activas: al arrancar (tras 1 min) y cada noche. */
    @Scheduled(cron = "${vetlink.reglas.cron-generacion:0 15 0 * * *}", zone = "${vetlink.reglas.zona-horaria:America/Bogota}")
    public void generarAgendas() {
        for (Veterinaria v : veterinarias.findByEstado((byte) 1)) {
            try {
                var r = disponibilidad.generarAgendaClinica(v.getIdVeterinaria());
                if (r.cuposCreados() > 0) {
                    log.info("Agenda generada para '{}': {} cupos nuevos ({} – {})", v.getNombre(), r.cuposCreados(),
                            r.desde(), r.hasta());
                }
            } catch (RuntimeException e) {
                log.warn("No se pudo generar la agenda de la clínica {}: {}", v.getIdVeterinaria(), e.getMessage());
            }
        }
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT6H")
    public void generarAgendasPeriodicamente() {
        generarAgendas();
    }

    @Scheduled(initialDelayString = "PT30S", fixedDelayString = "PT5M")
    public void expirarReservas() {
        try {
            int n = citas.expirarPendientes();
            if (n > 0) log.info("{} reservas pendientes expiraron sin confirmación", n);
        } catch (RuntimeException e) {
            log.warn("Fallo al expirar reservas pendientes: {}", e.getMessage());
        }
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT30M")
    public void recordatorios() {
        try {
            citas.enviarRecordatorios();
        } catch (RuntimeException e) {
            log.warn("Fallo al enviar recordatorios: {}", e.getMessage());
        }
    }
}
