package com.vetlink.veterinaria.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "telefono_veterinaria")
@Getter
@Setter
@NoArgsConstructor
public class TelefonoVeterinaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTelefonoVeterinaria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idVeterinaria", nullable = false)
    private Veterinaria veterinaria;

    @Column(nullable = false, length = 45)
    private String numeroTelefono;
}
