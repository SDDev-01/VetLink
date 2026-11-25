package com.vetlink.disponibilidad.model;

import com.vetlink.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Franja semanal de trabajo de un veterinario. Varias franjas el mismo día modelan los
 * descansos fijos (p. ej. 08:00–12:00 y 14:00–18:00 = almuerzo de 12 a 14).
 */
@Entity
@Table(name = "horario_veterinario")
@Getter
@Setter
@NoArgsConstructor
public class HorarioVeterinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idHorarioVeterinario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idVeterinario", nullable = false)
    private Usuario veterinario;

    /** 1 = lunes … 7 = domingo. */
    @Column(nullable = false)
    private Integer diaSemana;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoAprobacion estado = EstadoAprobacion.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idSolicitud")
    private SolicitudDisponibilidad solicitud;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    @Transient
    public String getNombreDia() {
        String n = DayOfWeek.of(diaSemana).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-CO"));
        return n.substring(0, 1).toUpperCase() + n.substring(1);
    }
}
