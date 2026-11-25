package com.vetlink.historial.model;

import com.vetlink.mascota.model.Mascota;
import com.vetlink.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "historial_clinico")
@Getter
@Setter
@NoArgsConstructor
public class HistorialClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer numeroHistorial;

    @Column(nullable = false)
    private Integer fechaConsulta;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String motivoConsulta;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String diagnostico;

    @Column(columnDefinition = "LONGTEXT")
    private String observaciones;

    /** VL-17: opcional; null = sin control programado. */
    @Column(nullable = true)
    private LocalDate proximaCita;

    /** Marca de tiempo de creación, usada para la regla de negocio "solo se puede
     *  eliminar dentro de los primeros 20 minutos". Nullable a propósito: los
     *  registros creados ANTES de este cambio no la tienen, y se tratan como
     *  "fuera de ventana de edición de tiempo" pero conservando la regla de autoría.
     *  Requiere ejecutar una vez en tu base de datos:
     *    ALTER TABLE historial_clinico ADD COLUMN fechaRegistro DATETIME NULL; */
    @Column(nullable = true)
    private java.time.LocalDateTime fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Veterinario_idUsuario", nullable = false)
    private Usuario veterinario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idMascota", nullable = false)
    private Mascota mascota;

    /** Cita durante la que se registró (única: una cita produce como máximo un registro clínico). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCita", unique = true)
    private com.vetlink.cita.model.Cita cita;

    /**
     * Emergencia atendida con este registro (V9). Única: guardar el registro cierra la
     * emergencia (pasa a ATENDIDA) en la misma transacción.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idEmergencia", unique = true)
    private com.vetlink.emergencia.model.Emergencia emergencia;

    /** Establecimiento responsable del registro. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idVeterinaria")
    private com.vetlink.veterinaria.model.Veterinaria veterinaria;

    /** CONSULTA, CONTROL, PROCEDIMIENTO, LABORATORIO, VACUNACION, URGENCIA (ver TipoRegistroClinico). */
    @Column(length = 30)
    private String tipoRegistro;

    @Column(columnDefinition = "TEXT")
    private String antecedentes;

    private Double temperatura;
    private Integer frecuenciaCardiaca;
    private Integer frecuenciaRespiratoria;
    private Double pesoKg;

    @Column(columnDefinition = "TEXT")
    private String evaluacionClinica;

    @Column(columnDefinition = "TEXT")
    private String hallazgos;

    @Column(columnDefinition = "TEXT")
    private String tratamiento;

    @Column(columnDefinition = "TEXT")
    private String medicamentosIndicados;

    @Column(columnDefinition = "TEXT")
    private String recomendaciones;

    @Transient
    public TipoRegistroClinico getTipo() {
        return TipoRegistroClinico.desde(tipoRegistro);
    }

    @OneToMany(mappedBy = "historial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistorialMedicamento> medicamentos = new ArrayList<>();

    @OneToMany(mappedBy = "historial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistorialTratamiento> tratamientos = new ArrayList<>();

    @OneToMany(mappedBy = "historial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArchivoHistorial> archivos = new ArrayList<>();
}
