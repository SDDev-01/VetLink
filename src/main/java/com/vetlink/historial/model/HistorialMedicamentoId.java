package com.vetlink.historial.model;

import java.io.Serializable;
import java.util.Objects;

public class HistorialMedicamentoId implements Serializable {

    private Integer historial;
    private Integer medicamento;

    public HistorialMedicamentoId() {}

    public HistorialMedicamentoId(Integer historial, Integer medicamento) {
        this.historial = historial;
        this.medicamento = medicamento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HistorialMedicamentoId)) return false;
        HistorialMedicamentoId that = (HistorialMedicamentoId) o;
        return Objects.equals(historial, that.historial) && Objects.equals(medicamento, that.medicamento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(historial, medicamento);
    }
}
