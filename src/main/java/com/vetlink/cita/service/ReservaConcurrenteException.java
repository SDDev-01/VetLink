package com.vetlink.cita.service;

/** Otro usuario reservó el mismo cupo (o un horario que se cruza) un instante antes. */
public class ReservaConcurrenteException extends IllegalStateException {

    public static final String MENSAJE =
            "Esta cita acaba de ser reservada por otro usuario. Por favor, selecciona otro horario disponible.";

    public ReservaConcurrenteException() {
        super(MENSAJE);
    }

    public ReservaConcurrenteException(Throwable causa) {
        super(MENSAJE, causa);
    }
}
