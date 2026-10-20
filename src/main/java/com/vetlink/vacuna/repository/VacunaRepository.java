package com.vetlink.vacuna.repository;

import com.vetlink.vacuna.model.Vacuna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VacunaRepository extends JpaRepository<Vacuna, Integer> {
    List<Vacuna> findByEspecie_IdEspecie(Integer idEspecie);
}
