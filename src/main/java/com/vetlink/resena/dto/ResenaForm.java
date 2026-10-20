package com.vetlink.resena.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResenaForm {

    @NotNull(message = "Selecciona una veterinaria")
    private Integer idVeterinaria;

    @NotNull(message = "Selecciona una calificacion")
    @Min(1) @Max(5)
    private Integer valor;

    private String comentario;
}
