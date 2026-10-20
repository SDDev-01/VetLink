package com.vetlink.usuario.repository;

import com.vetlink.usuario.model.Telefono;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelefonoRepository extends JpaRepository<Telefono, Integer> {
    List<Telefono> findByUsuario_IdUsuario(Integer idUsuario);
}
