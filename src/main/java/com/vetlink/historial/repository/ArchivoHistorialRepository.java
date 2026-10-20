package com.vetlink.historial.repository;

import com.vetlink.historial.model.ArchivoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchivoHistorialRepository extends JpaRepository<ArchivoHistorial, Integer> {
    List<ArchivoHistorial> findByHistorial_NumeroHistorial(Integer numeroHistorial);
}
