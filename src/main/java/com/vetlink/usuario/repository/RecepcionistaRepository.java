package com.vetlink.usuario.repository;

import com.vetlink.usuario.model.Recepcionista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecepcionistaRepository extends JpaRepository<Recepcionista, Integer> {
    List<Recepcionista> findByVeterinaria_IdVeterinaria(Integer idVeterinaria);
}
