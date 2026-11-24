package com.vetlink.disponibilidad.repository;

import com.vetlink.disponibilidad.model.EstadoAprobacion;
import com.vetlink.disponibilidad.model.HorarioVeterinario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioVeterinarioRepository extends JpaRepository<HorarioVeterinario, Integer> {
    List<HorarioVeterinario> findByVeterinario_IdUsuarioAndEstadoOrderByDiaSemanaAscHoraInicioAsc(Integer idVeterinario,
                                                                                                EstadoAprobacion estado);

    List<HorarioVeterinario> findBySolicitud_IdSolicitudOrderByDiaSemanaAscHoraInicioAsc(Integer idSolicitud);

    boolean existsByVeterinario_IdUsuarioAndEstado(Integer idVeterinario, EstadoAprobacion estado);
}
