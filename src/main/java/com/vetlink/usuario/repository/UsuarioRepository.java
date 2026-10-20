package com.vetlink.usuario.repository;

import com.vetlink.usuario.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByNumeroDocumento(String numeroDocumento);

    /** Autenticacion por correo: el correo vive en la tabla `correo` (1-N), no en `usuario`. */
    @Query("select u from Usuario u join Correo c on c.usuario = u where lower(c.correo) = lower(:correo)")
    Optional<Usuario> findByCorreo(@Param("correo") String correo);

    boolean existsByNumeroDocumento(String numeroDocumento);
}
