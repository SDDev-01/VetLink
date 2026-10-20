package com.vetlink.vacuna.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fabricante_vacuna")
@Getter
@Setter
@NoArgsConstructor
public class FabricanteVacuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idFabricante_Vacuna")
    private Integer idFabricanteVacuna;

    @Column(nullable = false, length = 45)
    private String nombre;
}
