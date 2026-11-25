package com.vetlink.historial.model;

/** Tipo de documento clínico (carpeta de expedientes). */
public enum TipoRegistroClinico {
    CONSULTA("Consulta general", "fa-stethoscope"),
    CONTROL("Control veterinario", "fa-clipboard-check"),
    PROCEDIMIENTO("Procedimiento clínico", "fa-syringe"),
    LABORATORIO("Resultados de laboratorio", "fa-flask"),
    VACUNACION("Registro de vacunación", "fa-shield-virus"),
    URGENCIA("Atención de urgencia", "fa-truck-medical");

    private final String etiqueta;
    private final String icono;

    TipoRegistroClinico(String etiqueta, String icono) {
        this.etiqueta = etiqueta;
        this.icono = icono;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getIcono() {
        return icono;
    }

    /** Los registros antiguos (sin tipo) se muestran como consulta general. */
    public static TipoRegistroClinico desde(String valor) {
        if (valor == null || valor.isBlank()) return CONSULTA;
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CONSULTA;
        }
    }
}
