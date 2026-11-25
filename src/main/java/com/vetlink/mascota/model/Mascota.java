package com.vetlink.mascota.model;

import com.vetlink.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "mascota")
@Getter
@Setter
@NoArgsConstructor
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMascota;

    @Column(nullable = false, length = 45)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idEspecie", nullable = false)
    private Especie especie;

    /** 'H' o 'M' (enum nativo del SQL). */
    @Column(nullable = false, length = 1)
    private String genero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idRaza", nullable = false)
    private Raza raza;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    /** 1 = registro activo, 0 = dado de baja (registros antiguos). */
    @Column(nullable = false)
    private Byte estado = 1;

    /** Situación de vida: ACTIVA, PERDIDA o FALLECIDA (ver {@link SituacionMascota}). */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SituacionMascota situacion = SituacionMascota.ACTIVA;

    @Column(length = 500)
    private String foto;

    @Column(nullable = false)
    private Double peso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Tutor_idUsuario", nullable = false)
    private Usuario tutor;

    /**
     * VL-22: dirección para mostrar la foto. Las fotos antiguas que son URL se usan tal cual;
     * las subidas se sirven por /mascotas/{id}/foto (con control de acceso). null = sin foto.
     */
    @Transient
    public String getFotoUrl() {
        if (foto == null || foto.isBlank()) return null;
        if (foto.regionMatches(true, 0, "http", 0, 4)) return foto;
        return "/mascotas/" + idMascota + "/foto";
    }

    @Transient
    public int getEdad() {
        if (fechaNacimiento == null) return 0;
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    @Transient
    public String getEdadTexto() {
        if (fechaNacimiento == null) return "";
        Period p = Period.between(fechaNacimiento, LocalDate.now());
        if (p.getYears() > 0) return p.getYears() + (p.getYears() == 1 ? " año" : " años");
        if (p.getMonths() > 0) return p.getMonths() + (p.getMonths() == 1 ? " mes" : " meses");
        return Math.max(p.getDays(), 0) + " días";
    }

    /** ¿Se le pueden agendar citas? Registro activo y no fallecida. */
    @Transient
    public boolean isAgendable() {
        return estado != null && estado == 1 && situacion != SituacionMascota.FALLECIDA;
    }

    @Transient
    public SituacionMascota getSituacionSegura() {
        return situacion == null ? SituacionMascota.ACTIVA : situacion;
    }
}
