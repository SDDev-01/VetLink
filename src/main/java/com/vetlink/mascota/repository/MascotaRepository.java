package com.vetlink.mascota.repository;

import com.vetlink.mascota.model.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MascotaRepository extends JpaRepository<Mascota, Integer> {
    List<Mascota> findByTutor_IdUsuarioAndEstado(Integer idTutor, Byte estado);
    List<Mascota> findByTutor_IdUsuario(Integer idTutor);
    List<Mascota> findByNombreContainingIgnoreCaseAndEstado(String nombre, Byte estado);

    /** RF0021/RF0004 (recepcionista) — mascotas que han tenido al menos una cita en esa veterinaria. */
    @Query("select distinct c.mascota from Cita c where c.veterinaria.idVeterinaria = :idVeterinaria")
    List<Mascota> buscarConCitaEnVeterinaria(Integer idVeterinaria);
}
