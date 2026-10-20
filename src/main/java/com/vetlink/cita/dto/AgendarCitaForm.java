package com.vetlink.cita.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class AgendarCitaForm {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime hora;

    @NotNull(message = "Selecciona un veterinario")
    private Integer idVeterinario;

    @NotNull(message = "Selecciona un servicio")
    private Integer idServicio;

    @NotNull(message = "Selecciona una veterinaria")
    private Integer idVeterinaria;

    @NotNull(message = "Selecciona una mascota")
    private Integer idMascota;
}
