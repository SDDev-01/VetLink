package com.vetlink.cita.model;

import com.vetlink.mascota.model.Mascota;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.Servicio;
import com.vetlink.veterinaria.model.Veterinaria;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "cita")
@Getter
@Setter
@NoArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCita;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoCita estado = EstadoCita.DISPONIBLE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Veterinario_idUsuario", nullable = false)
    private Usuario veterinario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Servicio_idServicio", nullable = false)
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Veterinaria_idVeterinaria", nullable = false)
    private Veterinaria veterinaria;

    /** Una cita DISPONIBLE es un cupo ofertado y todavía no tiene mascota. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idMascota")
    private Mascota mascota;

    /** Duración en minutos: el horario ocupado es [hora, hora + duracionMin). */
    @Column(nullable = false)
    private Integer duracionMin = 30;

    @Column(length = 500)
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(columnDefinition = "TEXT")
    private String indicacionesPrevias;

    @Column(columnDefinition = "TEXT")
    private String observacionesAdmin;

    private LocalDateTime creadaEn;
    private LocalDateTime reservadaEn;
    /** Una reserva PENDIENTE no confirmada antes de esta hora se rechaza automáticamente. */
    private LocalDateTime expiraEn;
    private LocalDateTime inicioAtencion;
    private LocalDateTime finAtencion;

    /** Si esta cita nació de una reprogramación, la cita original (que queda REPROGRAMADA). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCitaOrigen")
    private Cita citaOrigen;

    /**
     * Reprogramación hecha por la clínica: la respuesta del dueño (V10). null = la cita no
     * requiere respuesta. Mientras está PENDIENTE el horario sigue reservado.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private RespuestaDuenio respuestaDuenio;

    private LocalDateTime respuestaDuenioEn;

    /** true = cupo creado por el generador automático de agenda. */
    @Column(nullable = false)
    private Boolean generadaAuto = false;

    @Version
    @Column(nullable = false)
    private Integer version = 0;

    @PrePersist
    void antesDeCrear() {
        if (creadaEn == null) creadaEn = LocalDateTime.now();
        if (duracionMin == null || duracionMin <= 0) duracionMin = 30;
        if (generadaAuto == null) generadaAuto = false;
    }

    @Transient
    public LocalDateTime getInicio() {
        return LocalDateTime.of(fecha, hora);
    }

    @Transient
    public LocalDateTime getFin() {
        return getInicio().plusMinutes(duracionMin == null ? 30 : duracionMin);
    }

    @Transient
    public LocalTime getHoraFin() {
        return hora.plusMinutes(duracionMin == null ? 30 : duracionMin);
    }

    /** ¿Se solapa [inicio, fin) con el intervalo dado? */
    public boolean seSolapaCon(LocalDateTime inicio, LocalDateTime fin) {
        return getInicio().isBefore(fin) && inicio.isBefore(getFin());
    }

    /** ¿La clínica la reprogramó y el dueño todavía no responde? */
    @Transient
    public boolean isEsperandoRespuestaDuenio() {
        return respuestaDuenio == RespuestaDuenio.PENDIENTE && estado == EstadoCita.CONFIRMADA;
    }
}
