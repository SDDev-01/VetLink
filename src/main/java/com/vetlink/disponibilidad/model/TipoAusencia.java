package com.vetlink.disponibilidad.model;

public enum TipoAusencia {
    DESCANSO("Descanso"),
    PERMISO("Permiso"),
    INCAPACIDAD("Incapacidad"),
    VACACIONES("Vacaciones"),
    AUSENCIA("Día de ausencia"),
    /** Bloqueo de agenda decidido por recepción (no lo solicita el veterinario). */
    BLOQUEO("Bloqueo de agenda");

    private final String etiqueta;

    TipoAusencia(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
