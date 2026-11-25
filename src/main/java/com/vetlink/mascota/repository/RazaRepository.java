package com.vetlink.mascota.repository;

import com.vetlink.mascota.model.Raza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RazaRepository extends JpaRepository<Raza, Integer> {
    List<Raza> findByEspecie_IdEspecie(Integer idEspecie);

    /**
     * DUE-02: alta atómica de una raza personalizada. Si otra petición la creó a la vez, el
     * índice único (nombreRaza, idEspecie) hace que esta inserción se ignore sin error.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT IGNORE INTO raza (nombreRaza, idEspecie, personalizada) VALUES (:nombre, :idEspecie, 1)",
            nativeQuery = true)
    int insertarSiNoExiste(@Param("nombre") String nombre, @Param("idEspecie") Integer idEspecie);
}
