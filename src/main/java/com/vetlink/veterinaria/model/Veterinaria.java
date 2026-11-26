package com.vetlink.veterinaria.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "veterinaria")
@Getter
@Setter
@NoArgsConstructor
public class Veterinaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idVeterinaria;

    @Column(nullable = false, length = 45, unique = true)
    private String nit;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String logo = "default-logo.png";

    @Column(columnDefinition = "LONGTEXT")
    private String descripcion;

    @Column(length = 50)
    private String paginaWeb;

    @Column(nullable = false)
    private Byte estado = 1;

    /** Fecha de aprobación (V10). En las clínicas anteriores a V10 puede ser null. */
    private java.time.LocalDateTime aprobadaEn;

    /** true = aprobada automáticamente al completar el registro (V10). */
    @Column(nullable = false)
    private Boolean aprobacionAutomatica = false;

    public boolean isActiva() {
        return estado != null && estado == 1;
    }
}
