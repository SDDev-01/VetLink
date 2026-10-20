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

    public boolean isActiva() {
        return estado != null && estado == 1;
    }
}
