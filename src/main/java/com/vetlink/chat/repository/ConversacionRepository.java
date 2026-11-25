package com.vetlink.chat.repository;

import com.vetlink.chat.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversacionRepository extends JpaRepository<Conversacion, Integer> {
    List<Conversacion> findByUsuarioA_IdUsuarioOrUsuarioB_IdUsuarioOrderByCreadaEnDesc(Integer a, Integer b);

    Optional<Conversacion> findByUsuarioA_IdUsuarioAndUsuarioB_IdUsuario(Integer a, Integer b);

    /** Solo las conversaciones en las que participa el usuario, por último mensaje. */
    @Query("""
           select c from Conversacion c
           where c.usuarioA.idUsuario = :yo or c.usuarioB.idUsuario = :yo
           order by coalesce(c.ultimoMensajeEn, c.creadaEn) desc
           """)
    List<Conversacion> bandeja(@Param("yo") Integer yo);
}
