package com.vetlink.usuario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Extension 1-a-1 de Usuario para el rol Dueño/Tutor de mascotas. */
@Entity
@Table(name = "tutor")
@Getter
@Setter
@NoArgsConstructor
public class Tutor {

    @Id
    @Column(name = "idUsuario")
    private Integer idUsuario;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "idUsuario")
    private Usuario usuario;

    @Column(nullable = false, length = 45)
    private String direccion;
}
