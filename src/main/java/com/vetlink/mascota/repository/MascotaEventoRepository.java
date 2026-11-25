package com.vetlink.mascota.repository;

import com.vetlink.mascota.model.MascotaEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MascotaEventoRepository extends JpaRepository<MascotaEvento, Integer> {
    List<MascotaEvento> findByMascota_IdMascotaOrderByFechaDesc(Integer idMascota);
}
