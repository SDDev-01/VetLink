package com.vetlink.pasaporte.service;

/** El servicio de pasaportes no respondió a tiempo o devolvió un error. */
public class ServicioNoDisponibleException extends RuntimeException {

    public static final String MENSAJE = "El servicio de pasaportes no está disponible";

    public ServicioNoDisponibleException(Throwable causa) {
        super(MENSAJE, causa);
    }
}
