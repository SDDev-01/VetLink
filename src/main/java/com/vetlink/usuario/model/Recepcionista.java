package com.vetlink.usuario.model;

import com.vetlink.veterinaria.model.Veterinaria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Extension 1-a-1 de Usuario para el rol Recepcionista. */
@Entity
@Table(name = "recepcionista")
@Getter
@Setter
@NoArgsConstructor
public class Recepcionista {

    @Id
    @Column(name = "idUsuario")
    private Integer idUsuario;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "idUsuario")
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDate fechaContratacion;

    /** Enum nativo del SQL: 'MAÑANA','TARDE','NOCHE' */
    @Column(nullable = false, length = 10)
    private String turno;

    /** Solo esta persona puede cambiar la configuración y el equipo de la clínica. */
    @Column(name = "esPrincipal", nullable = false)
    private Boolean esPrincipal = false;

    // Cambias FetchType.LAZY por FetchType.EAGER aquí:
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "idVeterinaria", nullable = false)
    private Veterinaria veterinaria;
}
