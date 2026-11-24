package com.vetlink.auditoria;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Registro inmutable de una acción sensible (quién, qué, sobre qué y cuándo). */
@Entity
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAuditoria;

    private Integer idUsuario;

    @Column(nullable = false, length = 60)
    private String accion;

    @Column(nullable = false, length = 40)
    private String entidad;

    @Column(length = 40)
    private String idEntidad;

    @Column(length = 1000)
    private String detalle;

    @Column(length = 45)
    private String ip;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
