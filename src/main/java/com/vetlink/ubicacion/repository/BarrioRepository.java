package com.vetlink.ubicacion.repository;

import com.vetlink.ubicacion.model.Barrio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BarrioRepository extends JpaRepository<Barrio, Integer> {
    List<Barrio> findByLocalidad_IdLocalidad(Integer idLocalidad);
}
