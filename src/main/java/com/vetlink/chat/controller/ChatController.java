package com.vetlink.chat.controller;

import com.vetlink.chat.model.Conversacion;
import com.vetlink.chat.model.MensajeChat;
import com.vetlink.chat.repository.ConversacionRepository;
import com.vetlink.chat.repository.MensajeChatRepository;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.usuario.repository.UsuarioRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Controller @RequestMapping("/chat")
public class ChatController {
    private final ConversacionRepository conversaciones; private final MensajeChatRepository mensajes;
    private final UsuarioRepository usuarios;
    public ChatController(ConversacionRepository conversaciones, MensajeChatRepository mensajes, UsuarioRepository usuarios) {
        this.conversaciones = conversaciones; this.mensajes = mensajes; this.usuarios = usuarios;
    }
    @GetMapping
    public String inicio(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        shell(model, principal); Integer id = principal.getUsuario().getIdUsuario();
        model.addAttribute("conversaciones", conversaciones.findByUsuarioA_IdUsuarioOrUsuarioB_IdUsuarioOrderByCreadaEnDesc(id, id));
        model.addAttribute("contactos", usuarios.findAll().stream().filter(u -> !u.getIdUsuario().equals(id)).toList());
        return "chat/lista";
    }
    @PostMapping("/nuevo/{idContacto}")
    public String abrir(@PathVariable Integer idContacto, @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        Integer yo = principal.getUsuario().getIdUsuario();
        if (yo.equals(idContacto)) throw new IllegalArgumentException("No puedes iniciar un chat contigo mismo");
        Conversacion c = conversaciones.findByUsuarioA_IdUsuarioAndUsuarioB_IdUsuario(yo, idContacto)
            .or(() -> conversaciones.findByUsuarioA_IdUsuarioAndUsuarioB_IdUsuario(idContacto, yo)).orElseGet(() -> {
                Conversacion nueva = new Conversacion(); nueva.setUsuarioA(principal.getUsuario());
                nueva.setUsuarioB(usuarios.findById(idContacto).orElseThrow()); nueva.setCreadaEn(LocalDateTime.now()); return conversaciones.save(nueva); });
        return "redirect:/chat/" + c.getIdConversacion();
    }
    @GetMapping("/{id}")
    public String ver(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        Conversacion c = autorizada(id, principal); shell(model, principal); Integer yo=principal.getUsuario().getIdUsuario();
        model.addAttribute("conversaciones", conversaciones.findByUsuarioA_IdUsuarioOrUsuarioB_IdUsuarioOrderByCreadaEnDesc(yo,yo));
        model.addAttribute("conversacion", c); model.addAttribute("mensajes", mensajes.findByConversacion_IdConversacionOrderByEnviadoEnAsc(id));
        model.addAttribute("contacto", c.getUsuarioA().getIdUsuario().equals(yo) ? c.getUsuarioB() : c.getUsuarioA()); return "chat/detalle";
    }
    @PostMapping("/{id}/mensajes")
    public String enviar(@PathVariable Integer id, @RequestParam @NotBlank String contenido, @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        Conversacion c=autorizada(id,principal); MensajeChat m=new MensajeChat(); m.setConversacion(c); m.setRemitente(principal.getUsuario()); m.setContenido(contenido.trim()); m.setEnviadoEn(LocalDateTime.now()); mensajes.save(m); return "redirect:/chat/"+id;
    }
    private Conversacion autorizada(Integer id, VetlinkUserPrincipal p) { Conversacion c=conversaciones.findById(id).orElseThrow(); Integer yo=p.getUsuario().getIdUsuario(); if(!c.getUsuarioA().getIdUsuario().equals(yo)&&!c.getUsuarioB().getIdUsuario().equals(yo)) throw new org.springframework.security.access.AccessDeniedException("No tienes acceso a esta conversación"); return c; }
    private void shell(Model m, VetlinkUserPrincipal p) { m.addAttribute("usuario",p.getUsuario());m.addAttribute("seccionActiva","chat");m.addAttribute("navItems",switch(p.getUsuario().getRol()){case TUTOR->DashboardController.navTutor();case VETERINARIO->DashboardController.navVeterinario();case RECEPCIONISTA->DashboardController.navRecepcionista();}); }
}
