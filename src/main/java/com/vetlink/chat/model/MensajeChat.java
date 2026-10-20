package com.vetlink.chat.model;

import com.vetlink.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "mensaje_chat") @Getter @Setter @NoArgsConstructor
public class MensajeChat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer idMensaje;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "idConversacion", nullable = false) private Conversacion conversacion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "idRemitente", nullable = false) private Usuario remitente;
    @Column(nullable = false, columnDefinition = "TEXT") private String contenido;
    @Column(nullable = false) private LocalDateTime enviadoEn;
}
