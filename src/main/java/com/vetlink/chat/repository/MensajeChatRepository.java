package com.vetlink.chat.repository;

import com.vetlink.chat.model.MensajeChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MensajeChatRepository extends JpaRepository<MensajeChat, Integer> {
    List<MensajeChat> findByConversacion_IdConversacionOrderByEnviadoEnAsc(Integer idConversacion);

    @Query("""
           select count(m) from MensajeChat m
           where m.conversacion.idConversacion = :conv and m.remitente.idUsuario <> :yo and m.leidoEn is null
           """)
    long noLeidos(@Param("conv") Integer idConversacion, @Param("yo") Integer yo);

    @Modifying
    @Query("""
           update MensajeChat m set m.leidoEn = :ahora
           where m.conversacion.idConversacion = :conv and m.remitente.idUsuario <> :yo and m.leidoEn is null
           """)
    int marcarLeidos(@Param("conv") Integer idConversacion, @Param("yo") Integer yo, @Param("ahora") LocalDateTime ahora);

    MensajeChat findFirstByConversacion_IdConversacionOrderByEnviadoEnDesc(Integer idConversacion);
}
