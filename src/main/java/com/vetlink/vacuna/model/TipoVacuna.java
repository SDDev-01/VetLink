package com.vetlink.vacuna.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tipo_vacuna")
@Getter
@Setter
@NoArgsConstructor
public class TipoVacuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTipoVacuna;

    @Column(nullable = false, length = 45)
    private String nombreTipo;
}
