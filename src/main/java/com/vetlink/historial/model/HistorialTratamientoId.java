package com.vetlink.historial.model;

import java.io.Serializable;
import java.util.Objects;

public class HistorialTratamientoId implements Serializable {

    private Integer tratamiento;
    private Integer historial;

    public HistorialTratamientoId() {}

    public HistorialTratamientoId(Integer tratamiento, Integer historial) {
        this.tratamiento = tratamiento;
        this.historial = historial;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HistorialTratamientoId)) return false;
        HistorialTratamientoId that = (HistorialTratamientoId) o;
        return Objects.equals(tratamiento, that.tratamiento) && Objects.equals(historial, that.historial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tratamiento, historial);
    }
}
