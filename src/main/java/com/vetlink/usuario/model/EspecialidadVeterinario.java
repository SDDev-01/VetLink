package com.vetlink.usuario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "especialidad_veterinario")
@Getter
@Setter
@NoArgsConstructor
public class EspecialidadVeterinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEspecialidadVeterinario;

    @Column(nullable = false, length = 45)
    private String nombreEspecialidad;

    @Column(length = 100)
    private String descripcion;
}
