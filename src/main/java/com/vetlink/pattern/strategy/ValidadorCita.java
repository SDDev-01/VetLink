package com.vetlink.pattern.strategy;

import com.vetlink.cita.dto.AgendarCitaForm;

/**
 * Patron de diseño GoF: STRATEGY.
 *
 * Cada regla de negocio para agendar una cita (que en el SQL original vivia
 * "escondida" dentro de triggers como tr_cita_fecha_valida, tr_mascota_ocupada
 * y tr_veterinario_ocupado) se implementa como una estrategia de validacion
 * independiente. CitaService las ejecuta todas en orden (ver
 * CompositeValidadorCita); agregar una regla nueva no requiere modificar las
 * existentes, solo registrar una implementacion mas de esta interfaz.
 */
public interface ValidadorCita {
    void validar(AgendarCitaForm form);
}
