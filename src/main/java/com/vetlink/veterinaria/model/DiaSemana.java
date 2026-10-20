package com.vetlink.veterinaria.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dia_semana")
@Getter
@Setter
@NoArgsConstructor
public class DiaSemana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDiaSemana;

    @Column(nullable = false, length = 45)
    private String diaSemana;
}
