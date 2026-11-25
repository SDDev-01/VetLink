package com.vetlink.emergencia.repository;

import com.vetlink.emergencia.model.Emergencia;
import com.vetlink.emergencia.model.EstadoEmergencia;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmergenciaRepository extends JpaRepository<Emergencia, Integer> {
    List<Emergencia> findByMascota_Tutor_IdUsuarioOrderByCreadaEnDesc(Integer id);

    /**
     * DUE-07: «Mis solicitudes» del dueño = las que ÉL envió (se conservan aunque la mascota se
     * transfiera). Las previas a V9 sin solicitante se atribuyen al dueño actual de la mascota.
     */
    @Query("""
           select e from Emergencia e
             left join e.solicitante s
             left join e.mascota m
             left join m.tutor t
           where s.idUsuario = :id or (s is null and t.idUsuario = :id)
           order by e.creadaEn desc
           """)
    List<Emergencia> deSolicitante(@Param("id") Integer idUsuario);

    /** Bloqueo de fila: serializa el cierre de una emergencia (reintentos o envíos simultáneos). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Emergencia e where e.idEmergencia = :id")
    Optional<Emergencia> bloquear(@Param("id") Integer id);

    @Query("""
           select count(e) > 0 from Emergencia e
           where e.mascota.idMascota = :idMascota and e.estado in :estados
           """)
    boolean existeAbiertaParaMascota(@Param("idMascota") Integer idMascota,
                                     @Param("estados") Collection<EstadoEmergencia> estados);

    List<Emergencia> findByVeterinaria_IdVeterinariaOrderByCreadaEnDesc(Integer id);

    List<Emergencia> findByVeterinario_IdUsuarioOrderByCreadaEnDesc(Integer id);

    boolean existsByMascota_IdMascotaAndVeterinario_IdUsuario(Integer idMascota, Integer idVeterinario);

    boolean existsByVeterinario_IdUsuarioAndEstado(Integer idVeterinario, EstadoEmergencia estado);

    long countByVeterinaria_IdVeterinariaAndEstadoIn(Integer idVeterinaria, Collection<EstadoEmergencia> estados);

    List<Emergencia> findByVeterinario_IdUsuarioAndEstado(Integer idVeterinario, EstadoEmergencia estado);
}
