package com.vetlink.pasaporte.model;

import java.time.LocalDateTime;

/**
 * Pasaporte de salud compartible, tal como lo devuelve el servicio de pasaportes.
 * Al crear uno, solo vienen token, url y expiraEn; el resto llega al listar.
 */
public record Pasaporte(
        String token,
        String url,
        LocalDateTime creadoEn,
        LocalDateTime expiraEn,
        LocalDateTime revocadoEn,
        String estado,
        Integer aperturas) {

    public boolean activo() {
        return "ACTIVO".equals(estado);
    }
}
