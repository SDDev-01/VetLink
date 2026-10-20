package com.vetlink.vacuna.model;

import com.vetlink.mascota.model.Especie;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vacuna")
@Getter
@Setter
@NoArgsConstructor
public class Vacuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idVacuna;

    @Column(nullable = false, length = 45)
    private String nombreVacuna;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idEspecie", nullable = false)
    private Especie especie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idFabricanteVacuna", nullable = false)
    private FabricanteVacuna fabricanteVacuna;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idTipoVacuna", nullable = false)
    private TipoVacuna tipoVacuna;
}
