package com.vetlink.usuario.repository;

import com.vetlink.usuario.model.Veterinario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VeterinarioRepository extends JpaRepository<Veterinario, Integer> {
    List<Veterinario> findByVeterinaria_IdVeterinaria(Integer idVeterinaria);
}
