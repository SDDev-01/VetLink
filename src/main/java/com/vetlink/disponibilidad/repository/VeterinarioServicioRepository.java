package com.vetlink.disponibilidad.repository;

import com.vetlink.disponibilidad.model.EstadoAprobacion;
import com.vetlink.disponibilidad.model.VeterinarioServicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VeterinarioServicioRepository extends JpaRepository<VeterinarioServicio, Integer> {
    List<VeterinarioServicio> findByVeterinario_IdUsuarioAndEstado(Integer idVeterinario, EstadoAprobacion estado);

    boolean existsByVeterinario_IdUsuarioAndServicio_IdServicioAndEstado(Integer idVeterinario, Integer idServicio,
                                                                        EstadoAprobacion estado);

    List<VeterinarioServicio> findBySolicitud_IdSolicitud(Integer idSolicitud);
}
