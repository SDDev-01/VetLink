package com.vetlink.ubicacion.repository;

import com.vetlink.ubicacion.model.Localidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocalidadRepository extends JpaRepository<Localidad, Integer> {
    List<Localidad> findByCiudad_IdCiudad(Integer idCiudad);
}
