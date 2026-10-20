package com.vetlink.cita.model;

import com.vetlink.mascota.model.Mascota;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.Servicio;
import com.vetlink.veterinaria.model.Veterinaria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
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
}
