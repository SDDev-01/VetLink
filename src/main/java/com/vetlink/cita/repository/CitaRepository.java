package com.vetlink.cita.repository;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CitaRepository extends JpaRepository<Cita, Integer> {

    List<Cita> findByMascota_IdMascotaOrderByFechaDescHoraDesc(Integer idMascota);

    List<Cita> findByMascota_Tutor_IdUsuarioAndEstadoOrderByFechaAscHoraAsc(Integer idTutor, EstadoCita estado);

    /**
     * Citas del dueño, solo de sus mascotas vigentes (estado 1): la misma condición de
     * «Mis mascotas». Las de mascotas dadas de baja no se muestran en ninguna vista del dueño.
     */
    @Query("""
           select c from Cita c
           where c.mascota.tutor.idUsuario = :idTutor and c.mascota.estado = 1
           order by c.fecha desc, c.hora desc
           """)
    List<Cita> deTutorConMascotaVigente(@Param("idTutor") Integer idTutor);

    List<Cita> findByVeterinario_IdUsuarioAndFecha(Integer idVeterinario, LocalDate fecha);

    List<Cita> findByVeterinario_IdUsuarioOrderByFechaDesc(Integer idVeterinario);

    List<Cita> findByVeterinaria_IdVeterinariaAndEstadoAndFechaGreaterThanEqualOrderByFechaAscHoraAsc(
            Integer idVeterinaria, EstadoCita estado, LocalDate fecha);

    List<Cita> findByVeterinaria_IdVeterinariaAndEstadoInAndFechaGreaterThanEqualOrderByFechaAscHoraAsc(
            Integer idVeterinaria, Collection<EstadoCita> estados, LocalDate fecha);

    Optional<Cita> findByVeterinario_IdUsuarioAndServicio_IdServicioAndFechaAndHoraAndEstado(
            Integer idVeterinario, Integer idServicio, LocalDate fecha, LocalTime hora, EstadoCita estado);

    List<Cita> findByCitaOrigen_IdCita(Integer idCitaOrigen);

    /**
     * Filtros multicriterio para reportes y agendas: cualquier combinación de veterinaria,
     * estado y rango de fechas. Los parámetros nulos se ignoran.
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

    /** Citas (no cupos) de una clínica en un rango, para agendas y tableros. */
    @Query("""
           select c from Cita c
           where c.veterinaria.idVeterinaria = :idVeterinaria
             and c.fecha between :desde and :hasta
             and (c.mascota is not null or c.estado = com.vetlink.cita.model.EstadoCita.DISPONIBLE)
           order by c.fecha asc, c.hora asc
           """)
    List<Cita> deClinicaEntre(@Param("idVeterinaria") Integer idVeterinaria,
                              @Param("desde") LocalDate desde,
                              @Param("hasta") LocalDate hasta);

    @Query("""
           select c from Cita c
           where c.veterinario.idUsuario = :idVeterinario
             and c.fecha between :desde and :hasta
             and c.mascota is not null
           order by c.fecha asc, c.hora asc
           """)
    List<Cita> deVeterinarioEntre(@Param("idVeterinario") Integer idVeterinario,
                                  @Param("desde") LocalDate desde,
                                  @Param("hasta") LocalDate hasta);

    @Query("""
           select c from Cita c
           where c.mascota.idMascota = :idMascota
             and c.estado in :estados
             and c.fecha >= current_date
           order by c.fecha asc, c.hora asc
           """)
    List<Cita> proximasDeMascota(@Param("idMascota") Integer idMascota,
                                 @Param("estados") Collection<EstadoCita> estados);

    /** Citas ocupadas (pendientes, confirmadas, en atención, atendidas) de un veterinario en una fecha. */
    @Query("""
           select c from Cita c
           where c.veterinario.idUsuario = :idVeterinario and c.fecha = :fecha and c.estado in :estados
           """)
    List<Cita> deVeterinarioEnFechaConEstados(@Param("idVeterinario") Integer idVeterinario,
                                              @Param("fecha") LocalDate fecha,
                                              @Param("estados") Collection<EstadoCita> estados);

    @Query("""
           select c from Cita c
           where c.mascota.idMascota = :idMascota and c.fecha = :fecha and c.estado in :estados
           """)
    List<Cita> deMascotaEnFechaConEstados(@Param("idMascota") Integer idMascota,
                                          @Param("fecha") LocalDate fecha,
                                          @Param("estados") Collection<EstadoCita> estados);

    /** Cupos y citas de un veterinario en un rango de fechas (para el generador de agenda). */
    @Query("""
           select c from Cita c
           where c.veterinario.idUsuario = :idVeterinario and c.fecha between :desde and :hasta
             and c.estado in :estados
           """)
    List<Cita> deVeterinarioEntreConEstados(@Param("idVeterinario") Integer idVeterinario,
                                            @Param("desde") LocalDate desde,
                                            @Param("hasta") LocalDate hasta,
                                            @Param("estados") Collection<EstadoCita> estados);

    /** Cupos DISPONIBLES de un servicio en una clínica, opcionalmente de un veterinario, en un rango. */
    @Query("""
           select c from Cita c
           where c.veterinaria.idVeterinaria = :idVeterinaria
             and c.servicio.idServicio = :idServicio
             and (:idVeterinario is null or c.veterinario.idUsuario = :idVeterinario)
             and c.estado = com.vetlink.cita.model.EstadoCita.DISPONIBLE
             and c.fecha between :desde and :hasta
           order by c.fecha asc, c.hora asc
           """)
    List<Cita> cuposEntre(@Param("idVeterinaria") Integer idVeterinaria,
                          @Param("idServicio") Integer idServicio,
                          @Param("idVeterinario") Integer idVeterinario,
                          @Param("desde") LocalDate desde,
                          @Param("hasta") LocalDate hasta);

    /** Próximo cupo DISPONIBLE de una clínica (directorio). */
    @Query("""
           select c from Cita c
           where c.veterinaria.idVeterinaria = :idVeterinaria
             and (:idServicio is null or c.servicio.idServicio = :idServicio)
             and c.estado = com.vetlink.cita.model.EstadoCita.DISPONIBLE
             and (c.fecha > :hoy or (c.fecha = :hoy and c.hora > :ahora))
           order by c.fecha asc, c.hora asc
           """)
    List<Cita> cuposFuturosDeClinica(@Param("idVeterinaria") Integer idVeterinaria,
                                     @Param("idServicio") Integer idServicio,
                                     @Param("hoy") LocalDate hoy,
                                     @Param("ahora") LocalTime ahora);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cita c where c.idCita = :idCita")
    Optional<Cita> bloquear(@Param("idCita") Integer idCita);

    @Query("""
           select c from Cita c
           where c.estado = com.vetlink.cita.model.EstadoCita.PENDIENTE
             and ((c.expiraEn is not null and c.expiraEn < :ahora)
                  or c.fecha < :hoy or (c.fecha = :hoy and c.hora < :horaActual))
           """)
    List<Cita> pendientesVencidas(@Param("ahora") LocalDateTime ahora,
                                  @Param("hoy") LocalDate hoy,
                                  @Param("horaActual") LocalTime horaActual);

    @Query("""
           select c from Cita c
           where c.estado = com.vetlink.cita.model.EstadoCita.CONFIRMADA
             and c.fecha between :desde and :hasta
           """)
    List<Cita> confirmadasEntreFechas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    List<Cita> findByVeterinario_IdUsuarioAndEstado(Integer idVeterinario, EstadoCita estado);

    long countByVeterinaria_IdVeterinariaAndEstado(Integer idVeterinaria, EstadoCita estado);

    boolean existsByMascota_IdMascotaAndVeterinaria_IdVeterinaria(Integer idMascota, Integer idVeterinaria);

    boolean existsByMascota_IdMascotaAndVeterinaria_IdVeterinariaAndEstadoIn(Integer idMascota, Integer idVeterinaria,
                                                                            Collection<EstadoCita> estados);

    boolean existsByMascota_IdMascotaAndVeterinario_IdUsuarioAndEstadoIn(Integer idMascota, Integer idVeterinario,
                                                                        Collection<EstadoCita> estados);

    boolean existsByMascota_IdMascotaAndVeterinario_IdUsuario(Integer idMascota, Integer idVeterinario);

    boolean existsByMascota_IdMascotaAndEstadoIn(Integer idMascota, Collection<EstadoCita> estados);

    boolean existsByVeterinario_IdUsuarioAndFechaAndHoraAndEstado(Integer idVeterinario, LocalDate fecha, LocalTime hora,
                                                                 EstadoCita estado);

    /** Próximas citas vigentes del dueño (pendientes o confirmadas, de mascotas activas). */
    @Query("""
           select c from Cita c
           where c.mascota.tutor.idUsuario = :idTutor
             and c.mascota.estado = 1
             and c.estado in (com.vetlink.cita.model.EstadoCita.PENDIENTE, com.vetlink.cita.model.EstadoCita.CONFIRMADA)
             and (c.fecha > :hoy or (c.fecha = :hoy and c.hora >= :ahora))
           order by c.fecha asc, c.hora asc
           """)
    List<Cita> proximasAgendadasDeTutor(@Param("idTutor") Integer idTutor,
                                        @Param("hoy") LocalDate hoy,
                                        @Param("ahora") LocalTime ahora);

    /** Cupos DISPONIBLES futuros de un servicio en una clínica, para reprogramar. */
    @Query("""
           select c from Cita c
           where c.veterinaria.idVeterinaria = :idVeterinaria
             and c.servicio.idServicio = :idServicio
             and c.estado = com.vetlink.cita.model.EstadoCita.DISPONIBLE
             and (c.fecha > :hoy or (c.fecha = :hoy and c.hora > :ahora))
           order by c.fecha asc, c.hora asc
           """)
    List<Cita> cuposDisponiblesFuturos(@Param("idVeterinaria") Integer idVeterinaria,
                                       @Param("idServicio") Integer idServicio,
                                       @Param("hoy") LocalDate hoy,
                                       @Param("ahora") LocalTime ahora);
}
