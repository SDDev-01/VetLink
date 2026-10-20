package com.vetlink.mascota.controller;

import com.vetlink.cita.service.CitaService;
import com.vetlink.historial.service.HistorialClinicoService;
import com.vetlink.mascota.dto.MascotaForm;
import com.vetlink.mascota.repository.EspecieRepository;
import com.vetlink.mascota.repository.RazaRepository;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.mascota.service.MascotaService;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.vacuna.service.CarnetVacunaService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/** CRUD completo de mascotas para el rol TUTOR. */
@Controller
@RequestMapping("/mascotas")
public class MascotaController {

    private final MascotaService mascotaService;
    private final EspecieRepository especieRepository;
    private final RazaRepository razaRepository;
    private final CitaService citaService;
    private final HistorialClinicoService historialClinicoService;
    private final CarnetVacunaService carnetVacunaService;

    public MascotaController(MascotaService mascotaService, EspecieRepository especieRepository, RazaRepository razaRepository,
                              CitaService citaService, HistorialClinicoService historialClinicoService,
                              CarnetVacunaService carnetVacunaService) {
        this.mascotaService = mascotaService;
        this.especieRepository = especieRepository;
        this.razaRepository = razaRepository;
        this.citaService = citaService;
        this.historialClinicoService = historialClinicoService;
        this.carnetVacunaService = carnetVacunaService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        model.addAttribute("mascotas", mascotaService.listarActivasPorTutor(principal.getUsuario().getIdUsuario()));
        return "mascotas/lista";
    }

    @GetMapping("/nueva")
    public String nuevaForm(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        cargarCatalogos(model);
        model.addAttribute("form", new MascotaForm());
        return "mascotas/formulario";
    }

    @PostMapping("/nueva")
    public String crear(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                         @Valid @ModelAttribute("form") MascotaForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            cargarShell(model, principal);
            cargarCatalogos(model);
            return "mascotas/formulario";
        }
        try {
            mascotaService.crear(form, principal.getUsuario().getIdUsuario());
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorGeneral", e.getMessage());
            cargarShell(model, principal);
            cargarCatalogos(model);
            return "mascotas/formulario";
        }
        return "redirect:/mascotas?creada";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        var mascota = mascotaService.buscarPorId(id);
        verificarPropietario(mascota, principal);
        MascotaForm form = new MascotaForm();
        form.setIdMascota(mascota.getIdMascota());
        form.setNombre(mascota.getNombre());
        form.setIdEspecie(mascota.getEspecie().getIdEspecie());
        form.setIdRaza(mascota.getRaza().getIdRaza());
        form.setGenero(mascota.getGenero());
        form.setFechaNacimiento(mascota.getFechaNacimiento());
        form.setPeso(mascota.getPeso());
        form.setFoto(mascota.getFoto());
        cargarShell(model, principal);
        cargarCatalogos(model);
        model.addAttribute("form", form);
        return "mascotas/formulario";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                              @Valid @ModelAttribute("form") MascotaForm form, BindingResult result, Model model) {
        form.setIdMascota(id);
        if (result.hasErrors()) {
            cargarShell(model, principal);
            cargarCatalogos(model);
            return "mascotas/formulario";
        }
        try {
            mascotaService.actualizar(form, principal.getUsuario().getIdUsuario());
        } catch (RuntimeException e) {
            model.addAttribute("errorGeneral", e.getMessage());
            cargarShell(model, principal);
            cargarCatalogos(model);
            return "mascotas/formulario";
        }
        return "redirect:/mascotas?actualizada";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        mascotaService.eliminar(id, principal.getUsuario().getIdUsuario());
        return "redirect:/mascotas?eliminada";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        var mascota = mascotaService.buscarPorId(id);
        verificarPropietario(mascota, principal);
        var citas = citaService.listarPorMascota(id);
        model.addAttribute("mascota", mascota);
        model.addAttribute("citas", citas);
        model.addAttribute("proximasCitas", citas.stream()
                .filter(c -> c.getEstado() == com.vetlink.cita.model.EstadoCita.AGENDADA)
                .toList());
        model.addAttribute("historial", historialClinicoService.listarPorMascota(id));
        model.addAttribute("carnet", carnetVacunaService.listarPorMascota(id));
        return "mascotas/detalle";
    }

    private void cargarCatalogos(Model model) {
        model.addAttribute("especies", especieRepository.findAll());
        model.addAttribute("razas", razaRepository.findAll());
    }

    private void cargarShell(Model model, VetlinkUserPrincipal principal) {
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "mascotas");
        model.addAttribute("navItems", DashboardController.navTutor());
    }

    private void verificarPropietario(com.vetlink.mascota.model.Mascota mascota,
                                      VetlinkUserPrincipal principal) {
        if (!mascota.getTutor().getIdUsuario().equals(principal.getUsuario().getIdUsuario())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso sobre esta mascota");
        }
    }
}
