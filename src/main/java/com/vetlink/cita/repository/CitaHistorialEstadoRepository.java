package com.vetlink.cita.repository;

import com.vetlink.cita.model.CitaHistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CitaHistorialEstadoRepository extends JpaRepository<CitaHistorialEstado, Integer> {
    List<CitaHistorialEstado> findByCita_IdCitaOrderByFechaAscIdHistorialEstadoAsc(Integer idCita);
}
