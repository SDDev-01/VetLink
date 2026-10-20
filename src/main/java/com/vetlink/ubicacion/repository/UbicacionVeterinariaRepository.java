package com.vetlink.ubicacion.repository;

import com.vetlink.ubicacion.model.UbicacionVeterinaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UbicacionVeterinariaRepository extends JpaRepository<UbicacionVeterinaria, Integer> {
    Optional<UbicacionVeterinaria> findByVeterinaria_IdVeterinaria(Integer idVeterinaria);
}
