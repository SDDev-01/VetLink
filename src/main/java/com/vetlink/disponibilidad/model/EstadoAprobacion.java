package com.vetlink.disponibilidad.model;

/** Estado de una solicitud de disponibilidad y de cada elemento que propone. */
public enum EstadoAprobacion {
    PENDIENTE("Pendiente de aprobación"),
    APROBADO("Aprobado"),
    RECHAZADO("Rechazado"),
    /** Elemento aprobado que dejó de regir porque se aprobó otro que lo sustituye. */
    REEMPLAZADO("Reemplazado"),
    /** Bloqueo de agenda levantado por recepción antes de terminar (se conserva como historia). */
    ANULADO("Desbloqueado");

    private final String etiqueta;

    EstadoAprobacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
