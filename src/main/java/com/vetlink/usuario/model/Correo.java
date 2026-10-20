package com.vetlink.usuario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Correos electronicos del usuario. Normalizado 1-N tal como en mydb.sql (tabla `correo`). */
@Entity
@Table(name = "correo")
@Getter
@Setter
@NoArgsConstructor
public class Correo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCorreo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 45)
    private String correo;
}
