package com.vetlink.emergencia.model;

import com.vetlink.mascota.model.Mascota;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.Veterinaria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "emergencia")
@Getter
@Setter
@NoArgsConstructor
public class Emergencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEmergencia;

    @ManyToOne
    @JoinColumn(name = "idMascota")
    private Mascota mascota;

    @ManyToOne
    @JoinColumn(name = "idVeterinaria")
    private Veterinaria veterinaria;

    @ManyToOne
    @JoinColumn(name = "idVeterinario")
    private Usuario veterinario;

    /**
     * Dueño que envió la solicitud (V9). Se conserva aunque la mascota se transfiera
     * después: «Mis solicitudes» se consulta por este campo, no por el dueño actual.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idSolicitante")
    private Usuario solicitante;

    @Column(nullable = false)
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEmergencia estado = EstadoEmergencia.PENDIENTE;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    /** Momento en que se guardó el registro clínico que la cerró (V9). */
    private LocalDateTime atendidaEn;

    /** ¿La solicitó este usuario? (los registros previos a V9 sin solicitante usan el dueño de la mascota). */
    @Transient
    public boolean esDeSolicitante(Integer idUsuario) {
        if (idUsuario == null) return false;
        if (solicitante != null) return idUsuario.equals(solicitante.getIdUsuario());
        return mascota != null && mascota.getTutor() != null && idUsuario.equals(mascota.getTutor().getIdUsuario());
    }

    @Transient
    public boolean isAbierta() {
        return estado == EstadoEmergencia.PENDIENTE || estado == EstadoEmergencia.ASIGNADA;
    }
}
