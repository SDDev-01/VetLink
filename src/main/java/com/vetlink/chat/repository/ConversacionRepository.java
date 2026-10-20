package com.vetlink.chat.repository;
import com.vetlink.chat.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface ConversacionRepository extends JpaRepository<Conversacion,Integer> {
    List<Conversacion> findByUsuarioA_IdUsuarioOrUsuarioB_IdUsuarioOrderByCreadaEnDesc(Integer a, Integer b);
    Optional<Conversacion> findByUsuarioA_IdUsuarioAndUsuarioB_IdUsuario(Integer a, Integer b);
}
