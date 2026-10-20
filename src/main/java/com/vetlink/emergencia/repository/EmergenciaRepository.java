package com.vetlink.emergencia.repository;
import com.vetlink.emergencia.model.Emergencia; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface EmergenciaRepository extends JpaRepository<Emergencia,Integer>{ List<Emergencia> findByMascota_Tutor_IdUsuarioOrderByCreadaEnDesc(Integer id); List<Emergencia> findByVeterinaria_IdVeterinariaOrderByCreadaEnDesc(Integer id); List<Emergencia> findByVeterinario_IdUsuarioOrderByCreadaEnDesc(Integer id); }
