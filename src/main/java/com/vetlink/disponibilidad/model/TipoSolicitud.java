package com.vetlink.disponibilidad.model;

public enum TipoSolicitud {
    HORARIO("Horario laboral"),
    AUSENCIA("Ausencia / novedad"),
    SERVICIOS("Servicios que atiende");

    private final String etiqueta;

    TipoSolicitud(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
