package com.vetlink.historial.dto;

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

    @NotNull(message = "La proxima cita sugerida es obligatoria")
    private LocalDate proximaCita;
}
