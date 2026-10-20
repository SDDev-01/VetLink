package com.vetlink.historial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "archivo_historial")
@Getter
@Setter
@NoArgsConstructor
public class ArchivoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idArchivo;

    @Column(nullable = false, length = 255)
    private String tipoArchivo;

    @Column(length = 255)
    private String nombreArchivo;

    @Column(length = 500)
    private String rutaArchivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "numeroHistorial", nullable = false)
    private HistorialClinico historial;
}
