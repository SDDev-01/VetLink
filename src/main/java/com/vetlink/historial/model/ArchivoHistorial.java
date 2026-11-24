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

    private Long tamanoBytes;

    private java.time.LocalDateTime subidoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idUsuario")
    private com.vetlink.usuario.model.Usuario subidoPor;

    @Transient
    public boolean isImagen() {
        return tipoArchivo != null && tipoArchivo.startsWith("image/");
    }

    @Transient
    public boolean isPdf() {
        return "application/pdf".equals(tipoArchivo);
    }

    @Transient
    public String getIcono() {
        if (isPdf()) return "fa-file-pdf";
        if (isImagen()) return "fa-file-image";
        return "fa-file";
    }
}
