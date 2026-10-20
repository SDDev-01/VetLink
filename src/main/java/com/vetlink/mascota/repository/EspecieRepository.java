package com.vetlink.mascota.repository;

import com.vetlink.mascota.model.Especie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspecieRepository extends JpaRepository<Especie, Integer> {
}
