package com.vetlink.veterinaria.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "correo_veterinaria")
@Getter
@Setter
@NoArgsConstructor
public class CorreoVeterinaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCorreo_Veterinaria")
    private Integer idCorreoVeterinaria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idVeterinaria", nullable = false)
    private Veterinaria veterinaria;

    @Column(nullable = false, length = 45)
    private String correo;
}
