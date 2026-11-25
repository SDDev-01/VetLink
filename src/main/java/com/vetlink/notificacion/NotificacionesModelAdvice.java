package com.vetlink.notificacion;

import com.vetlink.security.VetlinkUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Pone la campana de notificaciones en el modelo de todas las páginas MVC con sesión
 * (vlNotificaciones y vlNoLeidas), para que el topbar la muestre en cualquier vista.
 */
@ControllerAdvice(annotations = Controller.class)
public class NotificacionesModelAdvice {

    private final NotificacionService service;

    public NotificacionesModelAdvice(NotificacionService service) {
        this.service = service;
    }

    @ModelAttribute
    public void campana(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof VetlinkUserPrincipal p)) {
            return;
        }
        Integer id = p.getUsuario().getIdUsuario();
        try {
            model.addAttribute("vlNotificaciones", service.recientes(id, 8));
            model.addAttribute("vlNoLeidas", service.noLeidas(id));
        } catch (RuntimeException e) {
            // La campana nunca debe tumbar una página.
            model.addAttribute("vlNotificaciones", List.of());
            model.addAttribute("vlNoLeidas", 0L);
        }
    }
}
