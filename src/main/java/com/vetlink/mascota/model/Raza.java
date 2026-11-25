package com.vetlink.mascota.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "raza", uniqueConstraints = @UniqueConstraint(name = "uk_raza_nombre_especie",
        columnNames = {"nombreRaza", "idEspecie"}))
@Getter
@Setter
@NoArgsConstructor
public class Raza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idRaza;

    @Column(nullable = false, length = 45)
    private String nombreRaza;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idEspecie", nullable = false)
    private Especie especie;

    /** Registrada por un dueño con la opción «Otra raza» (V9). */
    @Column(nullable = false)
    private Boolean personalizada = false;
}
