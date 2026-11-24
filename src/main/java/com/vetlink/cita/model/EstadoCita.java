package com.vetlink.cita.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de una fila de la tabla cita. DISPONIBLE es un cupo publicado (todavía no es
 * una cita); el resto son estados de una cita reservada. Las transiciones permitidas están
 * en {@link TransicionCita}.
 */
public enum EstadoCita {
    DISPONIBLE("Disponible", "vl-st-disponible", "fa-circle-plus"),
    PENDIENTE("Pendiente de confirmación", "vl-st-pendiente", "fa-hourglass-half"),
    CONFIRMADA("Confirmada", "vl-st-confirmada", "fa-circle-check"),
    EN_ATENCION("En atención", "vl-st-atencion", "fa-stethoscope"),
    ATENDIDA("Atendida", "vl-st-atendida", "fa-clipboard-check"),
    CANCELADA("Cancelada", "vl-st-cancelada", "fa-ban"),
    RECHAZADA("Rechazada", "vl-st-rechazada", "fa-circle-xmark"),
    NO_ASISTIO("No asistió", "vl-st-noasistio", "fa-user-slash"),
    REPROGRAMADA("Reprogramada", "vl-st-reprogramada", "fa-calendar-plus");

    /** Estados que bloquean el horario del veterinario y de la mascota. */
    public static final Set<EstadoCita> OCUPAN_HORARIO = EnumSet.of(PENDIENTE, CONFIRMADA, EN_ATENCION, ATENDIDA);

    /** Reservas vigentes (aún pueden cancelarse o reprogramarse). */
    public static final Set<EstadoCita> RESERVAS_VIGENTES = EnumSet.of(PENDIENTE, CONFIRMADA);

    /** Estados que demuestran una relación clínica autorizada con la mascota. */
    public static final Set<EstadoCita> RELACION_CLINICA = EnumSet.of(CONFIRMADA, EN_ATENCION, ATENDIDA);

    private final String etiqueta;
    private final String clase;
    private final String icono;

    EstadoCita(String etiqueta, String clase, String icono) {
        this.etiqueta = etiqueta;
        this.clase = clase;
        this.icono = icono;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getClase() {
        return clase;
    }

    public String getIcono() {
        return icono;
    }

    public boolean ocupaHorario() {
        return OCUPAN_HORARIO.contains(this);
    }
}
