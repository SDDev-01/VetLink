package com.vetlink.disponibilidad.repository;

import com.vetlink.disponibilidad.model.EstadoAprobacion;
import com.vetlink.disponibilidad.model.SolicitudDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudDisponibilidadRepository extends JpaRepository<SolicitudDisponibilidad, Integer> {
    List<SolicitudDisponibilidad> findByVeterinario_IdUsuarioOrderByCreadaEnDesc(Integer idVeterinario);

    List<SolicitudDisponibilidad> findByVeterinaria_IdVeterinariaAndEstadoOrderByCreadaEnAsc(Integer idVeterinaria,
                                                                                           EstadoAprobacion estado);

    List<SolicitudDisponibilidad> findByVeterinaria_IdVeterinariaOrderByCreadaEnDesc(Integer idVeterinaria);

    long countByVeterinaria_IdVeterinariaAndEstado(Integer idVeterinaria, EstadoAprobacion estado);
}
