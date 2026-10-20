package com.vetlink.vacuna.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "lote_vacuna")
@Getter
@Setter
@NoArgsConstructor
public class LoteVacuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idLote;

    @Column(nullable = false, length = 45)
    private String numeroLote;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;
}
