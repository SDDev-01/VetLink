package com.vetlink.mascota.model;

import com.vetlink.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Bitácora de la mascota: pérdida, hallazgo, fallecimiento, transferencias y cambios de peso
 * hechos por recepción (con valor anterior y nuevo).
 */
@Entity
@Table(name = "mascota_evento")
@Getter
@Setter
@NoArgsConstructor
public class MascotaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEvento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idMascota", nullable = false)
    private Mascota mascota;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(length = 500)
    private String detalle;

    @Column(length = 60)
    private String valorAnterior;

    @Column(length = 60)
    private String valorNuevo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idUsuario")
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
