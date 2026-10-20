package com.vetlink.resena.controller;

import com.vetlink.resena.dto.ResenaForm;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.resena.service.ResenaService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/resenas")
public class ResenaController {

    private final ResenaService resenaService;

    public ResenaController(ResenaService resenaService) {
        this.resenaService = resenaService;
    }

    @GetMapping("/mias")
    public String misResenas(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        model.addAttribute("resenas", resenaService.listarPorTutor(principal.getUsuario().getIdUsuario()));
        model.addAttribute("navItems", com.vetlink.usuario.controller.DashboardController.navTutor());
        model.addAttribute("seccionActiva", "resenas");
        model.addAttribute("usuario", principal.getUsuario());
        return "resenas/mias";
    }

    @PostMapping("/nueva")
    public String crear(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                         @Valid @ModelAttribute("form") ResenaForm form, BindingResult result,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorResena", "Selecciona una calificación válida antes de publicar.");
            return redirigirDetalle(form);
        }
        try {
            resenaService.crear(form, principal.getUsuario().getIdUsuario());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorResena", e.getMessage());
            return redirigirDetalle(form);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorResena", e.getMessage());
            return redirigirDetalle(form);
        }
        return "redirect:/directorio/" + form.getIdVeterinaria() + "?gracias";
    }

    private String redirigirDetalle(ResenaForm form) {
        if (form.getIdVeterinaria() == null) {
            return "redirect:/directorio";
        }
        return "redirect:/directorio/" + form.getIdVeterinaria() + "?error";
    }
}
