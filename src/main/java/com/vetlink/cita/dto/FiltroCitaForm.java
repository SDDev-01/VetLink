package com.vetlink.cita.dto;

import com.vetlink.cita.model.EstadoCita;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Filtros opcionales para el reporte de citas: consultas multicriterio. */
@Getter
@Setter
public class FiltroCitaForm {
    private Integer idVeterinaria;
    private EstadoCita estado;
    private LocalDate desde;
    private LocalDate hasta;
    private Integer idVeterinario;
}
