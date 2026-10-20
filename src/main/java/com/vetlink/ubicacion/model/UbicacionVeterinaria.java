package com.vetlink.ubicacion.model;

import com.vetlink.veterinaria.model.Veterinaria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ubicacion_veterinaria")
@Getter
@Setter
@NoArgsConstructor
public class UbicacionVeterinaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idUbicacion_Veterinaria")
    private Integer idUbicacionVeterinaria;

    @Column(nullable = false, length = 45)
    private String calle;

    @Column(nullable = false, length = 45)
    private String numero;

    @Column(nullable = false)
    private Integer estrato;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idVeterinaria", nullable = false, unique = true)
    private Veterinaria veterinaria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idBarrio", nullable = false)
    private Barrio barrio;
}
