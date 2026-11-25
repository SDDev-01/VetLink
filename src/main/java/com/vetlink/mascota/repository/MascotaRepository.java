package com.vetlink.mascota.repository;

import com.vetlink.cita.model.EstadoCita;
import com.vetlink.mascota.model.Mascota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MascotaRepository extends JpaRepository<Mascota, Integer> {
    List<Mascota> findByTutor_IdUsuarioAndEstado(Integer idTutor, Byte estado);
    List<Mascota> findByTutor_IdUsuario(Integer idTutor);
    List<Mascota> findByNombreContainingIgnoreCaseAndEstado(String nombre, Byte estado);
    long countByEstado(Byte estado);

    /** Mascotas que han tenido al menos una cita (no un cupo vacío) en esa veterinaria. */
    @Query("select distinct c.mascota from Cita c where c.veterinaria.idVeterinaria = :idVeterinaria")
    List<Mascota> buscarConCitaEnVeterinaria(@Param("idVeterinaria") Integer idVeterinaria);

    /** Mascotas con cita en la clínica en alguno de los estados dados. */
    @Query("select distinct c.mascota from Cita c where c.veterinaria.idVeterinaria = :idVeterinaria and c.estado in :estados")
    List<Mascota> buscarConCitaEnVeterinariaYEstados(@Param("idVeterinaria") Integer idVeterinaria,
                                                    @Param("estados") Collection<EstadoCita> estados);

    /** Mascotas que tienen o tuvieron alguna cita con el veterinario (los cupos sin mascota no cuentan). */
    @Query("select distinct c.mascota from Cita c where c.veterinario.idUsuario = :idVeterinario")
    List<Mascota> buscarConCitaConVeterinario(@Param("idVeterinario") Integer idVeterinario);

    @Query("select distinct c.mascota from Cita c where c.veterinario.idUsuario = :idVeterinario and c.estado in :estados")
    List<Mascota> buscarConCitaConVeterinarioYEstados(@Param("idVeterinario") Integer idVeterinario,
                                                     @Param("estados") Collection<EstadoCita> estados);

    /** Bloqueo pesimista de la mascota (evita citas simultáneas y transferencias concurrentes). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mascota m where m.idMascota = :id")
    Optional<Mascota> bloquear(@Param("id") Integer id);
}
