package com.vetlink.mascota.repository;

import com.vetlink.mascota.model.Raza;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RazaRepository extends JpaRepository<Raza, Integer> {
    List<Raza> findByEspecie_IdEspecie(Integer idEspecie);
}
