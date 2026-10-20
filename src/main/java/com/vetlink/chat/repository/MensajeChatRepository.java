package com.vetlink.chat.repository;
import com.vetlink.chat.model.MensajeChat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MensajeChatRepository extends JpaRepository<MensajeChat,Integer> {
    List<MensajeChat> findByConversacion_IdConversacionOrderByEnviadoEnAsc(Integer idConversacion);
}
