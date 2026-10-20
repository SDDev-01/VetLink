package com.vetlink.veterinaria.repository;

import com.vetlink.veterinaria.model.TelefonoVeterinaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelefonoVeterinariaRepository extends JpaRepository<TelefonoVeterinaria, Integer> {
    List<TelefonoVeterinaria> findByVeterinaria_IdVeterinaria(Integer idVeterinaria);
}
