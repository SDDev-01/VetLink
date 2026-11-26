package com.vetlink.disponibilidad.model;

import com.vetlink.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Intervalo en el que el veterinario no atiende (permiso, incapacidad, vacaciones, bloqueo…). */
@Entity
@Table(name = "ausencia_veterinario")
@Getter
@Setter
@NoArgsConstructor
public class AusenciaVeterinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAusencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idVeterinario", nullable = false)
    private Usuario veterinario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoAusencia tipo;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Column(length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoAprobacion estado = EstadoAprobacion.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idSolicitud")
    private SolicitudDisponibilidad solicitud;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    /** Desbloqueo de agenda (V10): quién lo hizo y cuándo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "desbloqueadaPor")
    private Usuario desbloqueadaPor;

    private LocalDateTime desbloqueadaEn;

    public boolean cubre(LocalDateTime desde, LocalDateTime hasta) {
        return inicio.isBefore(hasta) && desde.isBefore(fin);
    }
}
