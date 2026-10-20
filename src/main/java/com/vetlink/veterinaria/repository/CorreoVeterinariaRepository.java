package com.vetlink.veterinaria.repository;

import com.vetlink.veterinaria.model.CorreoVeterinaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorreoVeterinariaRepository extends JpaRepository<CorreoVeterinaria, Integer> {
    List<CorreoVeterinaria> findByVeterinaria_IdVeterinaria(Integer idVeterinaria);
}
