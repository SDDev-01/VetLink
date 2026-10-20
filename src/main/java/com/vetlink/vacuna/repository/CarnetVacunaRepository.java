package com.vetlink.vacuna.repository;

import com.vetlink.vacuna.model.CarnetVacuna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface CarnetVacunaRepository extends JpaRepository<CarnetVacuna, Integer> {

    List<CarnetVacuna> findByMascota_IdMascotaOrderByFechaAplicacionDesc(Integer idMascota);

    @Query("""
           select cv from CarnetVacuna cv
           where cv.proximaDosis between current_date and :limite
           order by cv.proximaDosis asc
           """)
    List<CarnetVacuna> pendientesHasta(LocalDate limite);

    @Query("""
           select cv from CarnetVacuna cv
           where cv.mascota.tutor.idUsuario = :idTutor
             and cv.proximaDosis between current_date and :limite
           order by cv.proximaDosis asc
           """)
    List<CarnetVacuna> pendientesDeTutorHasta(Integer idTutor, LocalDate limite);
}
