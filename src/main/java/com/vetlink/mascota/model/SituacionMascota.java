package com.vetlink.mascota.model;

public enum SituacionMascota {
    ACTIVA("Activa", "vl-badge-ok"),
    PERDIDA("Perdida", "vl-badge-warn"),
    FALLECIDA("Fallecida", "vl-badge-muted");

    private final String etiqueta;
    private final String clase;

    SituacionMascota(String etiqueta, String clase) {
        this.etiqueta = etiqueta;
        this.clase = clase;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getClase() {
        return clase;
    }
}
