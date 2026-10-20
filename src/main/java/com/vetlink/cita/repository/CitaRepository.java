package com.vetlink.cita.repository;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Integer> {

    List<Cita> findByMascota_IdMascotaOrderByFechaDescHoraDesc(Integer idMascota);

    List<Cita> findByMascota_Tutor_IdUsuarioAndEstadoOrderByFechaAscHoraAsc(Integer idTutor, EstadoCita estado);

    List<Cita> findByMascota_Tutor_IdUsuarioOrderByFechaDescHoraDesc(Integer idTutor);

    List<Cita> findByVeterinario_IdUsuarioAndFecha(Integer idVeterinario, LocalDate fecha);

    List<Cita> findByVeterinario_IdUsuarioOrderByFechaDesc(Integer idVeterinario);

    List<Cita> findByVeterinaria_IdVeterinariaAndEstadoAndFechaGreaterThanEqualOrderByFechaAscHoraAsc(
            Integer idVeterinaria, EstadoCita estado, LocalDate fecha);

    java.util.Optional<Cita> findByVeterinario_IdUsuarioAndServicio_IdServicioAndFechaAndHoraAndEstado(
            Integer idVeterinario, Integer idServicio, LocalDate fecha, LocalTime hora, EstadoCita estado);

    /**
     * Consulta personalizada con filtros multicriterio para el modulo de
     * reportes: cualquier combinacion de veterinaria, estado y rango de
     * fechas. Los parametros nulos se ignoran gracias al patron
     * ":param IS NULL OR campo = :param".
     */
    @Query("""
           select c from Cita c
           where (:idVeterinaria is null or c.veterinaria.idVeterinaria = :idVeterinaria)
             and (:estado is null or c.estado = :estado)
             and (:desde is null or c.fecha >= :desde)
             and (:hasta is null or c.fecha <= :hasta)
             and (:idVeterinario is null or c.veterinario.idUsuario = :idVeterinario)
           order by c.fecha desc, c.hora desc
           """)
    List<Cita> buscarConFiltros(@Param("idVeterinaria") Integer idVeterinaria,
                                 @Param("estado") EstadoCita estado,
                                 @Param("desde") LocalDate desde,
                                 @Param("hasta") LocalDate hasta,
                                 @Param("idVeterinario") Integer idVeterinario);

    @Query("""
           select c from Cita c
           where c.mascota.idMascota = :idMascota
             and c.estado = com.vetlink.cita.model.EstadoCita.AGENDADA
             and c.fecha >= current_date
           order by c.fecha asc, c.hora asc
           """)
    List<Cita> proximasCitasDeMascota(@Param("idMascota") Integer idMascota);

    boolean existsByMascota_IdMascotaAndFechaAndHoraAndEstado(Integer idMascota, LocalDate fecha, LocalTime hora, EstadoCita estado);

    boolean existsByVeterinario_IdUsuarioAndFechaAndHoraAndEstado(Integer idVeterinario, LocalDate fecha, LocalTime hora, EstadoCita estado);
}
