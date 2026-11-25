package com.vetlink.historial.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HistorialClinicoForm {

    @NotNull(message = "Selecciona una mascota")
    private Integer idMascota;

    /** CONSULTA, CONTROL, PROCEDIMIENTO, LABORATORIO, URGENCIA. */
    @Pattern(regexp = "^(CONSULTA|CONTROL|PROCEDIMIENTO|LABORATORIO|VACUNACION|URGENCIA)?$",
            message = "Tipo de registro no válido")
    private String tipoRegistro;

    @NotBlank(message = "El motivo de consulta es obligatorio")
    @Size(max = 4000, message = "Máximo 4000 caracteres")
    private String motivoConsulta;

    @Size(max = 4000, message = "Máximo 4000 caracteres")
    private String antecedentes;

    @DecimalMin(value = "30.0", message = "La temperatura debe estar entre 30 y 45 °C")
    @DecimalMax(value = "45.0", message = "La temperatura debe estar entre 30 y 45 °C")
    private Double temperatura;

    @Min(value = 1, message = "Frecuencia cardiaca no válida")
    @Max(value = 400, message = "Frecuencia cardiaca no válida")
    private Integer frecuenciaCardiaca;

    @Min(value = 1, message = "Frecuencia respiratoria no válida")
    @Max(value = 200, message = "Frecuencia respiratoria no válida")
    private Integer frecuenciaRespiratoria;

    @Positive(message = "Ingresa un peso mayor que cero")
    @Max(value = 1500, message = "Peso no válido")
    private Double pesoKg;

    @Size(max = 4000, message = "Máximo 4000 caracteres")
    private String evaluacionClinica;

    @Size(max = 4000, message = "Máximo 4000 caracteres")
    private String hallazgos;

    @NotBlank(message = "El diagnostico es obligatorio")
    @Size(max = 4000, message = "Máximo 4000 caracteres")
    private String diagnostico;

    @Size(max = 4000, message = "Máximo 4000 caracteres")
    private String tratamiento;

    @Size(max = 4000, message = "Máximo 4000 caracteres")
    private String medicamentosIndicados;

    @Size(max = 4000, message = "Máximo 4000 caracteres")
    private String recomendaciones;

    @Size(max = 4000, message = "Máximo 4000 caracteres")
    private String observaciones;

    /** VL-17: opcional (no siempre hace falta control), pero nunca en el pasado. */
    @FutureOrPresent(message = "La próxima cita no puede ser una fecha pasada")
    private LocalDate proximaCita;
}
