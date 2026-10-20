package com.vetlink.ubicacion.repository;

import com.vetlink.ubicacion.model.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CiudadRepository extends JpaRepository<Ciudad, Integer> {
}
