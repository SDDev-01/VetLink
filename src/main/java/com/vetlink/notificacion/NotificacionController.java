package com.vetlink.notificacion;

import com.vetlink.security.VetlinkUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    @PostMapping("/{id}/abrir")
    public String abrir(@PathVariable Long id, @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        return "redirect:" + service.abrir(id, principal.getUsuario().getIdUsuario());
    }

    @PostMapping("/leidas")
    public String marcarLeidas(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                               @RequestHeader(value = "Referer", required = false) String referer) {
        service.marcarTodasLeidas(principal.getUsuario().getIdUsuario());
        return "redirect:/dashboard";
    }
}
