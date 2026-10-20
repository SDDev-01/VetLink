package com.vetlink.resena.repository;

import com.vetlink.resena.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena, Integer> {

    List<Resena> findByVeterinaria_IdVeterinariaOrderByFechaPublicacionDesc(Integer idVeterinaria);

    List<Resena> findByTutor_IdUsuarioOrderByFechaPublicacionDesc(Integer idTutor);

    @Query("select avg(r.calificacion.valor) from Resena r where r.veterinaria.idVeterinaria = :idVeterinaria")
    Double promedioDeVeterinaria(@Param("idVeterinaria") Integer idVeterinaria);

    @Query("""
           select count(c) > 0 from Cita c
           where c.veterinaria.idVeterinaria = :idVeterinaria
             and c.mascota.tutor.idUsuario = :idTutor
             and c.estado <> com.vetlink.cita.model.EstadoCita.CANCELADA
           """)
    boolean tutorTuvoCitaEnVeterinaria(@Param("idVeterinaria") Integer idVeterinaria, @Param("idTutor") Integer idTutor);
}
