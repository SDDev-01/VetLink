package com.vetlink.historial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medicamento")
@Getter
@Setter
@NoArgsConstructor
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMedicamento;

    @Column(nullable = false, length = 45)
    private String nombreMedicamento;

    @Column(nullable = false, length = 45)
    private String dosis;

    @Column(nullable = false, length = 45)
    private String fabricante;
}
