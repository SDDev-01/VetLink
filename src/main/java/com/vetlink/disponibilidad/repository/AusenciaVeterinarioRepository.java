package com.vetlink.disponibilidad.repository;

import com.vetlink.disponibilidad.model.AusenciaVeterinario;
import com.vetlink.disponibilidad.model.EstadoAprobacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AusenciaVeterinarioRepository extends JpaRepository<AusenciaVeterinario, Integer> {

    @Query("""
           select a from AusenciaVeterinario a
           where a.veterinario.idUsuario = :idVeterinario and a.estado = :estado
             and a.inicio < :hasta and a.fin > :desde
           order by a.inicio
           """)
    List<AusenciaVeterinario> enIntervalo(@Param("idVeterinario") Integer idVeterinario,
                                         @Param("estado") EstadoAprobacion estado,
                                         @Param("desde") LocalDateTime desde,
                                         @Param("hasta") LocalDateTime hasta);

    List<AusenciaVeterinario> findBySolicitud_IdSolicitud(Integer idSolicitud);

    List<AusenciaVeterinario> findByVeterinario_IdUsuarioAndFinAfterOrderByInicioAsc(Integer idVeterinario,
                                                                                  LocalDateTime desde);
}
