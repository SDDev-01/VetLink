package com.vetlink.historial.repository;

import com.vetlink.historial.model.HistorialClinico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /** Registros de una mascota hechos en una clínica (lo que puede consultar la recepción). */
    @Query("""
           select h from HistorialClinico h
           where h.mascota.idMascota = :idMascota and h.veterinaria.idVeterinaria = :idVeterinaria
           order by h.fechaRegistro desc nulls last, h.numeroHistorial desc
           """)
    List<HistorialClinico> listarPorMascotaYClinica(@Param("idMascota") Integer idMascota,
                                                    @Param("idVeterinaria") Integer idVeterinaria);

    Optional<HistorialClinico> findByCita_IdCita(Integer idCita);

    long countByVeterinario_IdUsuarioAndFechaRegistroAfter(Integer idVeterinario, LocalDateTime desde);
}
