package com.vetlink.mascota.model;

import com.vetlink.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Solicitud de transferencia de propiedad de una mascota entre dos tutores registrados. */
@Entity
@Table(name = "transferencia_mascota")
@Getter
@Setter
@NoArgsConstructor
public class TransferenciaMascota {

    public enum Estado {
        PENDIENTE("Pendiente"), ACEPTADA("Aceptada"), RECHAZADA("Rechazada"), CANCELADA("Cancelada");

        private final String etiqueta;

        Estado(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String getEtiqueta() {
            return etiqueta;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTransferencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idMascota", nullable = false)
    private Mascota mascota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idTutorOrigen", nullable = false)
    private Usuario tutorOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idTutorDestino", nullable = false)
    private Usuario tutorDestino;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    @Column(length = 300)
    private String mensaje;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    private LocalDateTime resueltaEn;
}
