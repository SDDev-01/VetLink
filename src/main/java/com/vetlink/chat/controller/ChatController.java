package com.vetlink.chat.controller;

import com.vetlink.chat.model.Conversacion;
import com.vetlink.chat.model.MensajeChat;
import com.vetlink.chat.repository.ConversacionRepository;
import com.vetlink.chat.repository.MensajeChatRepository;
import com.vetlink.chat.service.ContactosChatService;
import com.vetlink.notificacion.NotificacionService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.usuario.model.Rol;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Chat interno: bandeja con solo las conversaciones propias, no leídos y permisos validados en el servidor. */
@Controller
@RequestMapping("/chat")
public class ChatController {

    /** Fila de la bandeja. */
    public record Bandeja(Conversacion conversacion, Usuario contacto, String titulo, String subtitulo,
                          String ultimo, LocalDateTime fecha, long noLeidos) {}

    private final ConversacionRepository conversaciones;
    private final MensajeChatRepository mensajes;
    private final UsuarioRepository usuarios;
    private final VeterinariaRepository veterinarias;
    private final ContactosChatService contactosChat;
    private final NotificacionService notificaciones;
    private final Clock reloj;

    public ChatController(ConversacionRepository conversaciones, MensajeChatRepository mensajes, UsuarioRepository usuarios,
                          VeterinariaRepository veterinarias, ContactosChatService contactosChat,
                          NotificacionService notificaciones, Clock reloj) {
        this.conversaciones = conversaciones;
        this.mensajes = mensajes;
        this.usuarios = usuarios;
        this.veterinarias = veterinarias;
        this.contactosChat = contactosChat;
        this.notificaciones = notificaciones;
        this.reloj = reloj;
    }

    @GetMapping
    public String inicio(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        shell(model, principal);
        Usuario yo = principal.getUsuario();
        model.addAttribute("bandeja", bandeja(yo));
        model.addAttribute("contactos", contactosChat.contactosPermitidos(yo));
        if (yo.getRol() == Rol.TUTOR) model.addAttribute("veterinarias", contactosChat.veterinariasParaTutor());
        return "chat/lista";
    }

    /** Tutor: abrir (o retomar) la conversación con una veterinaria. */
    @PostMapping("/veterinaria/{idVeterinaria}")
    public String abrirConVeterinaria(@PathVariable Integer idVeterinaria,
                                      @AuthenticationPrincipal VetlinkUserPrincipal principal, RedirectAttributes ra) {
        Usuario yo = principal.getUsuario();
        if (yo.getRol() != Rol.TUTOR) throw new AccessDeniedException("Solo los tutores contactan veterinarias desde aquí");
        var clinica = veterinarias.findById(idVeterinaria).filter(v -> v.isActiva())
                .orElseThrow(() -> new IllegalArgumentException("Veterinaria no encontrada"));
        Usuario responsable = contactosChat.responsableDeClinica(idVeterinaria).orElse(null);
        if (responsable == null) {
            ra.addFlashAttribute("errorGeneral", "Esta veterinaria todavía no tiene personal de recepción para el chat.");
            return "redirect:/chat";
        }
        Conversacion c = buscarOCrear(yo, responsable);
        if (c.getVeterinaria() == null) {
            c.setVeterinaria(clinica);
            conversaciones.save(c);
        }
        return "redirect:/chat/" + c.getIdConversacion();
    }

    @PostMapping("/nuevo/{idContacto}")
    public String abrir(@PathVariable Integer idContacto, @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        Usuario yo = principal.getUsuario();
        if (yo.getIdUsuario().equals(idContacto)) throw new IllegalArgumentException("No puedes iniciar un chat contigo mismo");
        Usuario contacto = usuarios.findById(idContacto)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!contactosChat.puedeEscribir(yo, contacto)) {
            throw new AccessDeniedException("No puedes iniciar una conversación con este usuario");
        }
        return "redirect:/chat/" + buscarOCrear(yo, contacto).getIdConversacion();
    }

    @GetMapping("/{id}")
    @Transactional
    public String ver(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        Usuario yo = principal.getUsuario();
        Conversacion c = autorizada(id, yo);
        mensajes.marcarLeidos(id, yo.getIdUsuario(), LocalDateTime.now(reloj));
        shell(model, principal);
        model.addAttribute("bandeja", bandeja(yo));
        model.addAttribute("conversacion", c);
        model.addAttribute("mensajes", mensajes.findByConversacion_IdConversacionOrderByEnviadoEnAsc(id));
        model.addAttribute("contacto", c.otroParticipante(yo.getIdUsuario()));
        model.addAttribute("tituloChat", titulo(c, yo));
        return "chat/detalle";
    }

    @PostMapping("/{id}/mensajes")
    @Transactional
    public String enviar(@PathVariable Integer id, @RequestParam(required = false) String contenido,
                         @AuthenticationPrincipal VetlinkUserPrincipal principal, RedirectAttributes ra) {
        Usuario yo = principal.getUsuario();
        Conversacion c = autorizada(id, yo);
        String texto = contenido == null ? "" : contenido.trim();
        if (texto.isEmpty()) {
            ra.addFlashAttribute("errorGeneral", "Este campo es obligatorio");
            return "redirect:/chat/" + id;
        }
        if (texto.length() > 2000) texto = texto.substring(0, 2000);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        MensajeChat m = new MensajeChat();
        m.setConversacion(c);
        m.setRemitente(yo);
        m.setContenido(texto);
        m.setEnviadoEn(ahora);
        mensajes.save(m);
        c.setUltimoMensajeEn(ahora);
        conversaciones.save(c);
        Usuario otro = c.otroParticipante(yo.getIdUsuario());
        notificaciones.notificar(otro.getIdUsuario(), "CHAT_MENSAJE", "Nuevo mensaje de " + yo.getNombre(),
                texto.length() > 120 ? texto.substring(0, 119) + "…" : texto, "/chat/" + id, "chat-" + id);
        return "redirect:/chat/" + id;
    }

    private List<Bandeja> bandeja(Usuario yo) {
        return conversaciones.bandeja(yo.getIdUsuario()).stream()
                .filter(c -> puedeVer(c, yo))
                .map(c -> {
                    MensajeChat ultimo = mensajes.findFirstByConversacion_IdConversacionOrderByEnviadoEnDesc(c.getIdConversacion());
                    Usuario otro = c.otroParticipante(yo.getIdUsuario());
                    return new Bandeja(c, otro, titulo(c, yo), subtitulo(c, yo),
                            ultimo == null ? "Sin mensajes todavía" : ultimo.getContenido(),
                            ultimo == null ? c.getCreadaEn() : ultimo.getEnviadoEn(),
                            mensajes.noLeidos(c.getIdConversacion(), yo.getIdUsuario()));
                }).toList();
    }

    private static String titulo(Conversacion c, Usuario yo) {
        Usuario otro = c.otroParticipante(yo.getIdUsuario());
        if (yo.getRol() == Rol.TUTOR && c.getVeterinaria() != null && otro.getRol() == Rol.RECEPCIONISTA) {
            return c.getVeterinaria().getNombre();
        }
        return otro.getNombre() + " " + otro.getApellido();
    }

    private static String subtitulo(Conversacion c, Usuario yo) {
        Usuario otro = c.otroParticipante(yo.getIdUsuario());
        if (otro.getRol() == null) return "";
        return switch (otro.getRol()) {
            case TUTOR -> "Tutor";
            case VETERINARIO -> "Veterinario" + (otro.getVeterinario().getVeterinaria() != null
                    ? " · " + otro.getVeterinario().getVeterinaria().getNombre() : "");
            case RECEPCIONISTA -> "Recepción · " + otro.getRecepcionista().getVeterinaria().getNombre();
        };
    }

    private Conversacion buscarOCrear(Usuario yo, Usuario contacto) {
        return conversaciones.findByUsuarioA_IdUsuarioAndUsuarioB_IdUsuario(yo.getIdUsuario(), contacto.getIdUsuario())
                .or(() -> conversaciones.findByUsuarioA_IdUsuarioAndUsuarioB_IdUsuario(contacto.getIdUsuario(), yo.getIdUsuario()))
                .orElseGet(() -> {
                    Conversacion nueva = new Conversacion();
                    nueva.setUsuarioA(yo);
                    nueva.setUsuarioB(contacto);
                    nueva.setCreadaEn(LocalDateTime.now(reloj));
                    return conversaciones.save(nueva);
                });
    }

    /** Participante y, para el veterinario, relación de atención vigente con el otro usuario. */
    private boolean puedeVer(Conversacion c, Usuario yo) {
        if (!c.participa(yo.getIdUsuario())) return false;
        if (yo.getRol() == Rol.VETERINARIO) {
            return contactosChat.puedeEscribir(yo, c.otroParticipante(yo.getIdUsuario()));
        }
        if (yo.getRol() == Rol.TUTOR) {
            return c.otroParticipante(yo.getIdUsuario()).getRol() != Rol.TUTOR;
        }
        return true;
    }

    private Conversacion autorizada(Integer id, Usuario yo) {
        Conversacion c = conversaciones.findById(id).orElseThrow(() -> new IllegalArgumentException("Conversación no encontrada"));
        if (!puedeVer(c, yo)) throw new AccessDeniedException("No tienes acceso a esta conversación");
        return c;
    }

    private void shell(Model m, VetlinkUserPrincipal p) {
        m.addAttribute("usuario", p.getUsuario());
        m.addAttribute("seccionActiva", "chat");
        m.addAttribute("navItems", DashboardController.navPara(p.getUsuario()));
    }
}
