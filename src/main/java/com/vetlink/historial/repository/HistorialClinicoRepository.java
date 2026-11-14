package com.vetlink.historial.repository;

import com.vetlink.historial.model.HistorialClinico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistorialClinicoRepository extends JpaRepository<HistorialClinico, Integer> {

    /** VL-12: del más reciente al más antiguo; los registros antiguos sin fechaRegistro van al final. */
    @Query("""
           select h from HistorialClinico h
           where h.mascota.idMascota = :idMascota
           order by h.fechaRegistro desc nulls last, h.numeroHistorial desc
           """)
    List<HistorialClinico> listarPorMascotaRecientes(@Param("idMascota") Integer idMascota);

    /** VL-12: mismo orden que listarPorMascotaRecientes, para todas las mascotas de un dueño. */
    @Query("""
           select h from HistorialClinico h
           where h.mascota.tutor.idUsuario = :idTutor
           order by h.fechaRegistro desc nulls last, h.numeroHistorial desc
           """)
    List<HistorialClinico> listarPorTutorRecientes(@Param("idTutor") Integer idTutor);
}
