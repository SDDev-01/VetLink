package com.vetlink.historial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "historial_tratamiento")
@IdClass(HistorialTratamientoId.class)
@Getter
@Setter
@NoArgsConstructor
public class HistorialTratamiento {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Tratamiento_idTratamiento", nullable = false)
    private Tratamiento tratamiento;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Historial_Clinico_numeroHistorial", nullable = false)
    private HistorialClinico historial;

    @Column(nullable = false, length = 45)
    private String duracionTratamiento;

    @Column(name = "intrucciones", nullable = false, columnDefinition = "LONGTEXT")
    private String instrucciones;
}
