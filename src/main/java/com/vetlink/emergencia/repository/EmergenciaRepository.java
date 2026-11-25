package com.vetlink.emergencia.repository;

import com.vetlink.emergencia.model.Emergencia;
import com.vetlink.emergencia.model.EstadoEmergencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EmergenciaRepository extends JpaRepository<Emergencia, Integer> {
    List<Emergencia> findByMascota_Tutor_IdUsuarioOrderByCreadaEnDesc(Integer id);

    List<Emergencia> findByVeterinaria_IdVeterinariaOrderByCreadaEnDesc(Integer id);

    List<Emergencia> findByVeterinario_IdUsuarioOrderByCreadaEnDesc(Integer id);

    boolean existsByMascota_IdMascotaAndVeterinario_IdUsuario(Integer idMascota, Integer idVeterinario);

    boolean existsByVeterinario_IdUsuarioAndEstado(Integer idVeterinario, EstadoEmergencia estado);

    long countByVeterinaria_IdVeterinariaAndEstadoIn(Integer idVeterinaria, Collection<EstadoEmergencia> estados);

    List<Emergencia> findByVeterinario_IdUsuarioAndEstado(Integer idVeterinario, EstadoEmergencia estado);
}
