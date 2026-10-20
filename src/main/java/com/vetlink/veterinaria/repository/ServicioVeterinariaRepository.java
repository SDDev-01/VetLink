package com.vetlink.veterinaria.repository;

import com.vetlink.veterinaria.model.ServicioVeterinaria;
import com.vetlink.veterinaria.model.ServicioVeterinariaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicioVeterinariaRepository extends JpaRepository<ServicioVeterinaria, ServicioVeterinariaId> {
    List<ServicioVeterinaria> findByVeterinaria_IdVeterinaria(Integer idVeterinaria);
}
