package com.vetlink.cita.controller;

import com.vetlink.cita.dto.FiltroCitaForm;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import com.vetlink.cita.service.CitaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Reporte de citas con filtros multicriterio: veterinaria, veterinario,
 * estado y rango de fechas, en cualquier combinacion. Solo veterinarios y
 * recepcionistas lo pueden consultar (ver SecurityConfig).
 */
@Controller
@RequestMapping("/reportes/citas")
public class ReporteController {

    private final CitaService citaService;
    private final VeterinariaRepository veterinariaRepository;

    public ReporteController(CitaService citaService, VeterinariaRepository veterinariaRepository) {
        this.citaService = citaService;
        this.veterinariaRepository = veterinariaRepository;
    }

    @GetMapping
    public String reporte(@ModelAttribute("filtro") FiltroCitaForm filtro, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        // Nunca confiar en el idVeterinaria enviado por el navegador. El
        // filtro se acota en servidor según la relación del usuario.
        if (principal.getUsuario().getRol() == com.vetlink.usuario.model.Rol.RECEPCIONISTA) {
            filtro.setIdVeterinaria(principal.getUsuario().getRecepcionista().getVeterinaria().getIdVeterinaria());
            filtro.setIdVeterinario(null);
        } else if (principal.getUsuario().getRol() == com.vetlink.usuario.model.Rol.VETERINARIO) {
            filtro.setIdVeterinario(principal.getUsuario().getIdUsuario());
            filtro.setIdVeterinaria(null);
        }
        model.addAttribute("citas", citaService.buscarConFiltros(filtro));
        model.addAttribute("veterinarias", principal.getUsuario().getRol() == com.vetlink.usuario.model.Rol.RECEPCIONISTA
                ? java.util.List.of(principal.getUsuario().getRecepcionista().getVeterinaria())
                : java.util.List.of());
        model.addAttribute("puedeCambiarVeterinaria", false);
        model.addAttribute("estados", EstadoCita.values());
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "citas");
        model.addAttribute("navItems", switch (principal.getUsuario().getRol()) {
            case VETERINARIO -> DashboardController.navVeterinario();
            case RECEPCIONISTA -> DashboardController.navRecepcionista();
            case TUTOR -> DashboardController.navTutor();
        });
        return "citas/reporte";
    }
}
