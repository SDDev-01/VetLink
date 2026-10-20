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

    @Column(nullable = false)
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

    @OneToMany(mappedBy = "historial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistorialMedicamento> medicamentos = new ArrayList<>();

    @OneToMany(mappedBy = "historial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistorialTratamiento> tratamientos = new ArrayList<>();

    @OneToMany(mappedBy = "historial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArchivoHistorial> archivos = new ArrayList<>();
}
