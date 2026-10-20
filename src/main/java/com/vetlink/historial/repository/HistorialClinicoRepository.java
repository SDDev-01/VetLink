package com.vetlink.historial.repository;

import com.vetlink.historial.model.HistorialClinico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialClinicoRepository extends JpaRepository<HistorialClinico, Integer> {
    List<HistorialClinico> findByMascota_IdMascotaOrderByNumeroHistorialDesc(Integer idMascota);
    List<HistorialClinico> findByMascota_Tutor_IdUsuarioOrderByNumeroHistorialDesc(Integer idTutor);
}
