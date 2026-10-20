package com.vetlink.vacuna.model;

import com.vetlink.mascota.model.Mascota;
import com.vetlink.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "carnet_vacuna")
@Getter
@Setter
@NoArgsConstructor
public class CarnetVacuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCarnet;

    @Column(nullable = false)
    private LocalDate fechaAplicacion;

    @Column(nullable = false)
    private LocalDate proximaDosis;

    @Column(nullable = false, length = 45)
    private String dosis;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idLote", nullable = false)
    private LoteVacuna lote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idVacuna", nullable = false)
    private Vacuna vacuna;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idMascota", nullable = false)
    private Mascota mascota;

    /** Clasificación visual de la próxima dosis, usada por la ficha de mascota (pestaña Carnet). */
    @Transient
    public String getEstadoBadgeClase() {
        LocalDate hoy = LocalDate.now();
        if (proximaDosis.isBefore(hoy)) return "vl-badge-danger";
        if (proximaDosis.isBefore(hoy.plusDays(30))) return "vl-badge-warn";
        return "vl-badge-ok";
    }

    @Transient
    public String getEstadoBadgeTexto() {
        LocalDate hoy = LocalDate.now();
        if (proximaDosis.isBefore(hoy)) return "Vencida";
        if (proximaDosis.isBefore(hoy.plusDays(30))) return "Próxima a vencer";
        return "Al día";
    }
}
