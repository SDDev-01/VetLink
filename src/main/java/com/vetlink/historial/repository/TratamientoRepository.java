package com.vetlink.historial.repository;

import com.vetlink.historial.model.Tratamiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TratamientoRepository extends JpaRepository<Tratamiento, Integer> {
}
