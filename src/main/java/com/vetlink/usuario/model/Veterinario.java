package com.vetlink.usuario.model;

import com.vetlink.veterinaria.model.Veterinaria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Extension 1-a-1 de Usuario para el rol Veterinario. */
@Entity
@Table(name = "veterinario")
@Getter
@Setter
@NoArgsConstructor
public class Veterinario {

    @Id
    @Column(name = "idUsuario")
    private Integer idUsuario;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "idUsuario")
    private Usuario usuario;

    @Column(nullable = false, length = 45)
    private String licencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idEspecialidad")
    private EspecialidadVeterinario especialidad;

    // Cambias FetchType.LAZY por FetchType.EAGER aquí:
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idVeterinaria")
    private Veterinaria veterinaria;
}