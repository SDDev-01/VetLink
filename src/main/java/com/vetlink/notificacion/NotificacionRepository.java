package com.vetlink.notificacion;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<NotificacionUsuario, Long> {

    Optional<NotificacionUsuario> findByIdUsuarioAndClaveUnica(Integer idUsuario, String claveUnica);

    long countByIdUsuarioAndLeidaFalse(Integer idUsuario);

    List<NotificacionUsuario> findByIdUsuarioOrderByCreadaEnDesc(Integer idUsuario, Pageable pageable);

    @Modifying
    @Query("update NotificacionUsuario n set n.leida = true where n.idUsuario = :idUsuario and n.leida = false")
    int marcarTodasLeidas(@Param("idUsuario") Integer idUsuario);
}
