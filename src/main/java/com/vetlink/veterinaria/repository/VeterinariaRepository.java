package com.vetlink.veterinaria.repository;

import com.vetlink.veterinaria.model.Veterinaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VeterinariaRepository extends JpaRepository<Veterinaria, Integer> {
    List<Veterinaria> findByEstado(Byte estado);
    List<Veterinaria> findByNombreContainingIgnoreCaseAndEstado(String nombre, Byte estado);
}
