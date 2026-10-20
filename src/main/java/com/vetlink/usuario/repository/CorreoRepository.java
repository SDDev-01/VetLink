package com.vetlink.usuario.repository;

import com.vetlink.usuario.model.Correo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorreoRepository extends JpaRepository<Correo, Integer> {
    List<Correo> findByUsuario_IdUsuario(Integer idUsuario);
    boolean existsByCorreoIgnoreCase(String correo);
}
