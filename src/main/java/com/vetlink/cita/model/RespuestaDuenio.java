package com.vetlink.cita.model;

/**
 * Respuesta del dueño a una cita que la clínica reprogramó. Mientras está PENDIENTE la
 * cita conserva su horario (estado CONFIRMADA) y el dueño puede confirmar, elegir otro
 * horario del mismo servicio o cancelar.
 */
public enum RespuestaDuenio {
    PENDIENTE("Esperando tu confirmación"),
    CONFIRMADA("Confirmada por el dueño"),
    MODIFICADA("El dueño eligió otro horario"),
    CANCELADA("Cancelada por el dueño");

    private final String etiqueta;

    RespuestaDuenio(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
