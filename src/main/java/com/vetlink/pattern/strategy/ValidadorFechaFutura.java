package com.vetlink.pattern.strategy;

import com.vetlink.cita.dto.AgendarCitaForm;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Replica la regla del trigger tr_cita_fecha_valida: no se permiten citas en el pasado (fecha y hora). */
@Component
public class ValidadorFechaFutura implements ValidadorCita {

    private final Clock reloj;

    public ValidadorFechaFutura(Clock reloj) {
        this.reloj = reloj;
    }

    @Override
    public void validar(AgendarCitaForm form) {
        if (form.getFecha() != null && form.getFecha().isBefore(LocalDate.now(reloj))) {
            throw new IllegalStateException("No puedes seleccionar una fecha anterior a la actual.");
        }
        // VL-15: hoy tampoco se permiten horas que ya pasaron.
        if (form.getFecha() != null && form.getHora() != null
                && LocalDateTime.of(form.getFecha(), form.getHora()).isBefore(LocalDateTime.now(reloj))) {
            throw new IllegalStateException("No se permiten citas en horas pasadas.");
        }
    }
}
