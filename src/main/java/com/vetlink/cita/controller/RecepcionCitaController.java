package com.vetlink.cita.controller;

import com.vetlink.cita.dto.AgendarCitaForm;
import com.vetlink.cita.service.CitaService;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.ServicioRepository;
import com.vetlink.usuario.repository.VeterinarioRepository;
import com.vetlink.veterinaria.repository.ServicioVeterinariaRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

/** RF0018/RF0020/RF0021/RF0022 para el rol RECEPCIONISTA sobre las citas de SU
 *  propia veterinaria (verificado siempre en CitaService, no solo aquí). */
@Controller
@RequestMapping("/citas/recepcion")
public class RecepcionCitaController {

    private final CitaService citaService;
    private final MascotaRepository mascotaRepository;
    private final ServicioRepository servicioRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ServicioVeterinariaRepository servicioVeterinariaRepository;

    public RecepcionCitaController(CitaService citaService, MascotaRepository mascotaRepository,
                                    ServicioRepository servicioRepository, VeterinarioRepository veterinarioRepository,
                                    ServicioVeterinariaRepository servicioVeterinariaRepository) {
        this.citaService = citaService;
        this.mascotaRepository = mascotaRepository;
        this.servicioRepository = servicioRepository;
        this.veterinarioRepository = veterinarioRepository;
        this.servicioVeterinariaRepository = servicioVeterinariaRepository;
    }

    @GetMapping("/ofertar")
    public String ofertarForm(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        Veterinaria veterinaria = miVeterinaria(principal);
        cargarShell(model, principal);
        model.addAttribute("veterinaria", veterinaria);
        model.addAttribute("veterinarios", veterinarioRepository.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()));
        model.addAttribute("servicios", servicioVeterinariaRepository.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()));
        model.addAttribute("form", new com.vetlink.cita.dto.OfertarCitaForm());
        return "citas/ofertar";
    }

    @PostMapping("/ofertar")
    public String ofertar(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                          @Valid @ModelAttribute("form") com.vetlink.cita.dto.OfertarCitaForm form,
                          BindingResult result, Model model) {
        Veterinaria veterinaria = miVeterinaria(principal);
        if (result.hasErrors()) return ofertarForm(principal, model);
        try {
            citaService.ofertarCupo(form, veterinaria.getIdVeterinaria());
        } catch (RuntimeException e) {
            cargarShell(model, principal);
            model.addAttribute("veterinaria", veterinaria);
            model.addAttribute("veterinarios", veterinarioRepository.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()));
            model.addAttribute("servicios", servicioVeterinariaRepository.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()));
            model.addAttribute("errorGeneral", e.getMessage());
            return "citas/ofertar";
        }
        return "redirect:/reportes/citas?cupoOfertado";
    }

    @GetMapping("/agendar")
    public String agendarForm(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        Veterinaria veterinaria = miVeterinaria(principal);
        cargarShell(model, principal);
        model.addAttribute("veterinaria", veterinaria);
        model.addAttribute("servicios", servicioRepository.findAll());
        model.addAttribute("mascotas", mascotaRepository.findAll());
        AgendarCitaForm form = new AgendarCitaForm();
        form.setIdVeterinaria(veterinaria.getIdVeterinaria());
        model.addAttribute("form", form);
        return "citas/recepcion-agendar";
    }

    @PostMapping("/agendar")
    public String agendar(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                           @Valid @ModelAttribute("form") AgendarCitaForm form, BindingResult result, Model model) {
        Veterinaria veterinaria = miVeterinaria(principal);
        form.setIdVeterinaria(veterinaria.getIdVeterinaria());

        if (result.hasErrors()) {
            cargarShell(model, principal);
            model.addAttribute("veterinaria", veterinaria);
            model.addAttribute("servicios", servicioRepository.findAll());
            model.addAttribute("mascotas", mascotaRepository.findAll());
            return "citas/recepcion-agendar";
        }
        try {
            citaService.agendarComoRecepcionista(form, veterinaria.getIdVeterinaria());
        } catch (RuntimeException e) {
            cargarShell(model, principal);
            model.addAttribute("veterinaria", veterinaria);
            model.addAttribute("servicios", servicioRepository.findAll());
            model.addAttribute("mascotas", mascotaRepository.findAll());
            model.addAttribute("errorGeneral", e.getMessage());
            return "citas/recepcion-agendar";
        }
        return "redirect:/reportes/citas?agendada";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        Veterinaria veterinaria = miVeterinaria(principal);
        citaService.cancelarComoRecepcionista(id, veterinaria.getIdVeterinaria());
        return "redirect:/reportes/citas?cancelada";
    }

    @PostMapping("/{id}/reagendar")
    public String reagendar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                             @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fecha,
                             @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.TIME) LocalTime hora) {
        Veterinaria veterinaria = miVeterinaria(principal);
        citaService.reagendarComoRecepcionista(id, fecha, hora, veterinaria.getIdVeterinaria());
        return "redirect:/reportes/citas?reagendada";
    }

    private Veterinaria miVeterinaria(VetlinkUserPrincipal principal) {
        if (principal.getUsuario().getRecepcionista() == null) {
            throw new org.springframework.security.access.AccessDeniedException("No autorizado");
        }
        return principal.getUsuario().getRecepcionista().getVeterinaria();
    }

    private void cargarShell(Model model, VetlinkUserPrincipal principal) {
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "citas");
        model.addAttribute("navItems", DashboardController.navRecepcionista());
    }
}
