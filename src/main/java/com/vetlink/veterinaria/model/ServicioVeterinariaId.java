package com.vetlink.veterinaria.model;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de servicio_veterinaria (idServicio, idVeterinaria). */
public class ServicioVeterinariaId implements Serializable {

    private Integer servicio;
    private Integer veterinaria;

    public ServicioVeterinariaId() {}

    public ServicioVeterinariaId(Integer servicio, Integer veterinaria) {
        this.servicio = servicio;
        this.veterinaria = veterinaria;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServicioVeterinariaId)) return false;
        ServicioVeterinariaId that = (ServicioVeterinariaId) o;
        return Objects.equals(servicio, that.servicio) && Objects.equals(veterinaria, that.veterinaria);
    }

    @Override
    public int hashCode() {
        return Objects.hash(servicio, veterinaria);
    }
}
