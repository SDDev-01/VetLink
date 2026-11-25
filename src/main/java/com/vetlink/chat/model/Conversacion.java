package com.vetlink.chat.model;

import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.Veterinaria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversacion")
@Getter
@Setter
@NoArgsConstructor
public class Conversacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idConversacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuarioA", nullable = false)
    private Usuario usuarioA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuarioB", nullable = false)
    private Usuario usuarioB;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    /** Fecha del último mensaje: ordena la bandeja. */
    private LocalDateTime ultimoMensajeEn;

    /** Si la conversación se abrió con una veterinaria (canal de la clínica). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idVeterinaria")
    private Veterinaria veterinaria;

    public Usuario otroParticipante(Integer yo) {
        return usuarioA.getIdUsuario().equals(yo) ? usuarioB : usuarioA;
    }

    public boolean participa(Integer idUsuario) {
        return usuarioA.getIdUsuario().equals(idUsuario) || usuarioB.getIdUsuario().equals(idUsuario);
    }
}
