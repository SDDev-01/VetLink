package com.vetlink.veterinaria.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "servicio")
@Getter
@Setter
@NoArgsConstructor
public class Servicio {

    /** Registro que exige el servicio para poder finalizar la atención. */
    public enum TipoRegistro { CLINICO, VACUNACION, NINGUNO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idServicio;

    @Column(nullable = false, length = 45)
    private String nombreServicio;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoRegistro tipoRegistro = TipoRegistro.CLINICO;

    /** Servicio de urgencias: no se agenda como cita ordinaria. */
    @Column(nullable = false)
    private Boolean esEmergencia = false;

    public boolean isEmergencia() {
        return Boolean.TRUE.equals(esEmergencia);
    }
}
