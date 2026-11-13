package com.vetlink.cita.service;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.historial.service.HistorialClinicoService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * VL-18: atender una cita es una sola operación. Crear la historia clínica y marcar
 * la cita como ATENDIDA van en la misma transacción: si algo falla, no queda nada a medias.
 */
@Service
public class AtencionService {

    private final CitaRepository citas;
    private final HistorialClinicoService historiales;

    public AtencionService(CitaRepository citas, HistorialClinicoService historiales) {
        this.citas = citas;
        this.historiales = historiales;
    }

    @Transactional
    public void atender(Integer idCita, HistorialClinicoForm form, Integer idVeterinario) {
        Cita cita = citas.findById(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        verificarAtendible(cita, idVeterinario);
        if (cita.getMascota() == null || !cita.getMascota().getIdMascota().equals(form.getIdMascota())) {
            throw new AccessDeniedException("La mascota no coincide con la cita");
        }
        historiales.crear(form, idVeterinario);
        cita.setEstado(EstadoCita.ATENDIDA);
        citas.save(cita);
    }

    /**
     * Reglas para poder atender: la cita es del veterinario, está AGENDADA y es de hoy.
     * La usa también el formulario para decidir si se muestra.
     */
    public void verificarAtendible(Cita cita, Integer idVeterinario) {
        if (!cita.getVeterinario().getIdUsuario().equals(idVeterinario)) {
            throw new AccessDeniedException("Esta cita no está asignada a ti");
        }
        if (cita.getEstado() != EstadoCita.AGENDADA) {
            throw new IllegalStateException("Solo se pueden atender citas agendadas");
        }
        if (!cita.getFecha().equals(LocalDate.now())) {
            throw new IllegalStateException("Solo se pueden atender citas del día de hoy");
        }
    }
}
