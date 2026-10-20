package com.vetlink.usuario.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla supertipo `usuario`. Los datos especificos de cada rol viven en las
 * subtablas 1-a-1 tutor/veterinario/recepcionista (igual que en mydb.sql).
 * getRol() calcula el rol efectivo mirando cual de esas asociaciones existe;
 * lo usa VetlinkUserDetailsService para construir las authorities de Spring
 * Security (gestion dinamica de roles).
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idUsuario;

    @Column(nullable = false, length = 45, unique = true)
    private String numeroDocumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idTipoDocumento", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String apellido;

    @JsonIgnore
    @Column(nullable = false, length = 120)
    private String password; // hash BCrypt

    @Column(nullable = false)
    private Byte estado = 1; // 1 = activo, 0 = inactivo

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Tutor tutor;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Veterinario veterinario;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Recepcionista recepcionista;

    @Transient
    public Rol getRol() {
        if (tutor != null) return Rol.TUTOR;
        if (veterinario != null) return Rol.VETERINARIO;
        if (recepcionista != null) return Rol.RECEPCIONISTA;
        return null;
    }

    public boolean isActivo() {
        return estado != null && estado == 1;
    }
}
