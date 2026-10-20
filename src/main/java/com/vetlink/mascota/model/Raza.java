package com.vetlink.mascota.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "raza")
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
}
