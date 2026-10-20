package com.vetlink.historial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "historial_medicamento")
@IdClass(HistorialMedicamentoId.class)
@Getter
@Setter
@NoArgsConstructor
public class HistorialMedicamento {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Historial_Clinico_numeroHistorial", nullable = false)
    private HistorialClinico historial;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Medicamento_idMedicamento", nullable = false)
    private Medicamento medicamento;

    @Column(nullable = false, length = 100)
    private String cantidad;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String instrucciones;
}
