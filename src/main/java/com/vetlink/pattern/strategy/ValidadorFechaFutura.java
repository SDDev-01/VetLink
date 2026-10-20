package com.vetlink.pattern.strategy;

import com.vetlink.cita.dto.AgendarCitaForm;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Replica la regla del trigger tr_cita_fecha_valida: no se permiten citas en el pasado. */
@Component
public class ValidadorFechaFutura implements ValidadorCita {
    @Override
    public void validar(AgendarCitaForm form) {
        if (form.getFecha() != null && form.getFecha().isBefore(LocalDate.now())) {
            throw new IllegalStateException("No se permiten citas en fechas pasadas.");
        }
    }
}
