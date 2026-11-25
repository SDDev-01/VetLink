package com.vetlink.notificacion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Notificación persistente dirigida a un único usuario.
 * {@code claveUnica} identifica el hecho notificado (p. ej. "cita-15-confirmada"): con ella
 * el mismo evento nunca genera dos notificaciones para el mismo usuario.
 */
@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class NotificacionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNotificacion;

    @Column(nullable = false)
    private Integer idUsuario;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(length = 300)
    private String enlace;

    @Column(length = 150)
    private String claveUnica;

    @Column(nullable = false)
    private Boolean leida = false;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    /** Icono FontAwesome según el tipo (para la campana). */
    @Transient
    public String getIcono() {
        if (tipo == null) return "fa-bell";
        if (tipo.startsWith("CITA")) return "fa-calendar-check";
        if (tipo.startsWith("TRANSFERENCIA")) return "fa-right-left";
        if (tipo.startsWith("DISPONIBILIDAD")) return "fa-user-clock";
        if (tipo.startsWith("EMERGENCIA")) return "fa-truck-medical";
        if (tipo.startsWith("CHAT")) return "fa-comments";
        if (tipo.startsWith("MASCOTA")) return "fa-paw";
        return "fa-bell";
    }
}
