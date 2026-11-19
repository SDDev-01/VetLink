package com.vetlink.pasaporte.service;

/** El pasaporte (o la mascota o el usuario con que se pidió) no existe. */
public class PasaporteNoEncontradoException extends RuntimeException {

    public PasaporteNoEncontradoException(Throwable causa) {
        super("El pasaporte no existe", causa);
    }
}
