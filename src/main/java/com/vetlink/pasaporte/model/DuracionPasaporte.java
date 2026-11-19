package com.vetlink.pasaporte.model;

import java.util.Arrays;
import java.util.Optional;

/** Duraciones permitidas para un pasaporte (las mismas que acepta el servicio de pasaportes). */
public enum DuracionPasaporte {
    HORAS_24(24, "24 horas"),
    DIAS_7(168, "7 días"),
    DIAS_30(720, "30 días");

    private final int horas;
    private final String etiqueta;

    DuracionPasaporte(int horas, String etiqueta) {
        this.horas = horas;
        this.etiqueta = etiqueta;
    }

    public int getHoras() {
        return horas;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static Optional<DuracionPasaporte> desdeHoras(int horas) {
        return Arrays.stream(values()).filter(d -> d.horas == horas).findFirst();
    }
}
