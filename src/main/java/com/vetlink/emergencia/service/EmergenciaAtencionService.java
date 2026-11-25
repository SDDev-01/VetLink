package com.vetlink.emergencia.service;

import com.vetlink.auditoria.AuditoriaService;
import com.vetlink.emergencia.model.Emergencia;
import com.vetlink.emergencia.model.EstadoEmergencia;
import com.vetlink.emergencia.repository.EmergenciaRepository;
import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.historial.service.HistorialClinicoService;
import com.vetlink.notificacion.NotificacionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * VET-03: una emergencia solo pasa a ATENDIDA cuando el veterinario asignado guarda el
 * registro clínico de esa atención. Ambas cosas ocurren en UNA transacción:
 * <ol>
 *   <li>se bloquea la fila de la emergencia (dos envíos simultáneos se serializan);</li>
 *   <li>se valida veterinario asignado, estado ASIGNADA y que la mascota coincida;</li>
 *   <li>se crea la historia clínica ligada a la emergencia (idEmergencia es UNIQUE);</li>
 *   <li>se cambia la emergencia a ATENDIDA.</li>
 * </ol>
 * Si cualquier paso falla, no se guarda nada: la emergencia sigue ASIGNADA.
 * No existe otra forma de marcarla como atendida.
 */
@Service
public class EmergenciaAtencionService {

    private final EmergenciaRepository emergencias;
    private final HistorialClinicoService historiales;
    private final NotificacionService notificaciones;
    private final AuditoriaService auditoria;
    private final Clock reloj;

    public EmergenciaAtencionService(EmergenciaRepository emergencias, HistorialClinicoService historiales,
                                     NotificacionService notificaciones, AuditoriaService auditoria, Clock reloj) {
        this.emergencias = emergencias;
        this.historiales = historiales;
        this.notificaciones = notificaciones;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    /** Emergencia que el veterinario puede atender ahora, o excepción con el motivo. */
    public Emergencia paraAtender(Integer idEmergencia, Integer idVeterinario) {
        Emergencia e = emergencias.findById(idEmergencia)
                .orElseThrow(() -> new IllegalArgumentException("Emergencia no encontrada"));
        exigirAtendible(e, idVeterinario);
        return e;
    }

    private static void exigirAtendible(Emergencia e, Integer idVeterinario) {
        if (e.getVeterinario() == null || !e.getVeterinario().getIdUsuario().equals(idVeterinario)) {
            throw new AccessDeniedException("Esta emergencia no está asignada a ti");
        }
        if (e.getEstado() == EstadoEmergencia.ATENDIDA) {
            throw new IllegalStateException("Esta emergencia ya fue atendida.");
        }
        if (e.getEstado() != EstadoEmergencia.ASIGNADA) {
            throw new IllegalStateException("Solo se registran atenciones de emergencias asignadas.");
        }
        if (e.getMascota() == null) {
            throw new IllegalStateException("La emergencia no tiene una mascota asociada.");
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public HistorialClinico registrarAtencion(Integer idEmergencia, HistorialClinicoForm form, Integer idVeterinario) {
        Emergencia e = emergencias.bloquear(idEmergencia)
                .orElseThrow(() -> new IllegalArgumentException("Emergencia no encontrada"));
        exigirAtendible(e, idVeterinario);
        HistorialClinico h;
        try {
            h = historiales.crearParaEmergencia(form, e, idVeterinario);
        } catch (DataIntegrityViolationException duplicado) {
            // Otra petición ya cerró esta emergencia con su registro (índice único idEmergencia).
            throw new IllegalStateException("Esta emergencia ya tiene un registro clínico.");
        }
        EstadoEmergencia antes = e.getEstado();
        e.setEstado(EstadoEmergencia.ATENDIDA);
        e.setAtendidaEn(LocalDateTime.now(reloj));
        emergencias.saveAndFlush(e);
        auditoria.registrar(idVeterinario, "HISTORIAL_CREAR", "historial_clinico", h.getNumeroHistorial(),
                "emergencia=" + idEmergencia);
        auditoria.registrar(idVeterinario, "EMERGENCIA_ESTADO", "emergencia", idEmergencia,
                antes + " -> ATENDIDA (registro clínico #" + h.getNumeroHistorial() + ")");
        Integer idDuenio = e.getSolicitante() != null ? e.getSolicitante().getIdUsuario()
                : e.getMascota().getTutor().getIdUsuario();
        notificaciones.notificar(idDuenio, "EMERGENCIA_ATENDIDA", "Emergencia atendida",
                e.getMascota().getNombre() + " fue atendido(a) en " + e.getVeterinaria().getNombre()
                        + ". El registro ya está en su historial clínico.",
                "/emergencias/" + idEmergencia, "emergencia-" + idEmergencia + "-atendida");
        return h;
    }
}
