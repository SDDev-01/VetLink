package com.vetlink.veterinaria.repository;

import com.vetlink.veterinaria.model.HorarioVeterinaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioVeterinariaRepository extends JpaRepository<HorarioVeterinaria, Integer> {
    List<HorarioVeterinaria> findByVeterinaria_IdVeterinaria(Integer idVeterinaria);
}
