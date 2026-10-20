package com.vetlink.veterinaria.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "servicio_veterinaria")
@IdClass(ServicioVeterinariaId.class)
@Getter
@Setter
@NoArgsConstructor
public class ServicioVeterinaria {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idServicio", nullable = false)
    private Servicio servicio;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idVeterinaria", nullable = false)
    private Veterinaria veterinaria;

    @Column(nullable = false)
    private Double precio;

    @Column(nullable = false)
    private Integer duracionAprox; // minutos
}
