package com.vetlink.chat.model;

import com.vetlink.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "conversacion") @Getter @Setter @NoArgsConstructor
public class Conversacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer idConversacion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuarioA", nullable = false) private Usuario usuarioA;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuarioB", nullable = false) private Usuario usuarioB;
    @Column(nullable = false) private LocalDateTime creadaEn;
}
