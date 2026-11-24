package com.vetlink.usuario.repository;

import com.vetlink.usuario.model.Veterinario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VeterinarioRepository extends JpaRepository<Veterinario, Integer> {
    List<Veterinario> findByVeterinaria_IdVeterinaria(Integer idVeterinaria);

    /**
     * Bloqueo pesimista (SELECT ... FOR UPDATE) de la fila del veterinario: serializa todas las
     * operaciones que reservan horario de un mismo veterinario, así dos reservas simultáneas
     * nunca validan el solapamiento a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Veterinario v where v.idUsuario = :id")
    Optional<Veterinario> bloquear(@Param("id") Integer id);
}
