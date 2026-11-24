package com.vetlink.disponibilidad.model;

import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.Veterinaria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Unidad de aprobación: el veterinario propone y la recepción aprueba o rechaza. */
@Entity
@Table(name = "solicitud_disponibilidad")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudDisponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idVeterinario", nullable = false)
    private Usuario veterinario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idVeterinaria", nullable = false)
    private Veterinaria veterinaria;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoSolicitud tipo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoAprobacion estado = EstadoAprobacion.PENDIENTE;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisadaPor")
    private Usuario revisadaPor;

    private LocalDateTime revisadaEn;

    @Column(length = 500)
    private String observacionRevision;
}
