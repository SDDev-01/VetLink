package com.vetlink.cita.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

/** Datos de un cupo que recepción pone a disposición de los dueños. */
@Getter @Setter
public class OfertarCitaForm {
    @NotNull @FutureOrPresent private LocalDate fecha;
    @NotNull private LocalTime hora;
    @NotNull private Integer idVeterinario;
    @NotNull private Integer idServicio;
}
