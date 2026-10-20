package com.vetlink.historial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tratamiento")
@Getter
@Setter
@NoArgsConstructor
public class Tratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTratamiento;

    @Column(nullable = false, length = 45)
    private String nombreTratamiento;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String descripcion;
}
