package com.vetlink.historial.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HistorialClinicoForm {

    @NotNull(message = "Selecciona una mascota")
    private Integer idMascota;

    @NotBlank(message = "El motivo de consulta es obligatorio")
    private String motivoConsulta;

    @NotBlank(message = "El diagnostico es obligatorio")
    private String diagnostico;

    private String observaciones;

    /** VL-17: opcional (no siempre hace falta control), pero nunca en el pasado. */
    @FutureOrPresent(message = "La próxima cita no puede ser una fecha pasada")
    private LocalDate proximaCita;
}
