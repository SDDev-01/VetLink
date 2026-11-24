package com.vetlink.disponibilidad.model;

import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.Servicio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Servicio que un veterinario está autorizado a atender (solo cuenta si está APROBADO). */
@Entity
@Table(name = "veterinario_servicio")
@Getter
@Setter
@NoArgsConstructor
public class VeterinarioServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idVeterinarioServicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idVeterinario", nullable = false)
    private Usuario veterinario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idServicio", nullable = false)
    private Servicio servicio;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoAprobacion estado = EstadoAprobacion.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idSolicitud")
    private SolicitudDisponibilidad solicitud;

    @Column(nullable = false)
    private LocalDateTime creadoEn;
}
