package com.vetlink.vacuna.controller;

import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.vacuna.repository.LoteVacunaRepository;
import com.vetlink.vacuna.repository.VacunaRepository;
import com.vetlink.vacuna.service.CarnetVacunaService;
import com.vetlink.mascota.service.MascotaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/carnet")
public class CarnetVacunaController {

    private final CarnetVacunaService carnetVacunaService;
    private final MascotaService mascotaService;
    private final VacunaRepository vacunaRepository;
    private final LoteVacunaRepository loteVacunaRepository;

    public CarnetVacunaController(CarnetVacunaService carnetVacunaService, MascotaService mascotaService,
                                   VacunaRepository vacunaRepository, LoteVacunaRepository loteVacunaRepository) {
        this.carnetVacunaService = carnetVacunaService;
        this.mascotaService = mascotaService;
        this.vacunaRepository = vacunaRepository;
        this.loteVacunaRepository = loteVacunaRepository;
    }

    @GetMapping("/{idMascota}")
    public String ver(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "mascotas");
        model.addAttribute("navItems", switch (principal.getUsuario().getRol()) {
            case TUTOR -> DashboardController.navTutor();
            case VETERINARIO -> DashboardController.navVeterinario();
            case RECEPCIONISTA -> DashboardController.navRecepcionista();
        });
        var mascota = mascotaService.buscarPorId(idMascota);
        model.addAttribute("mascota", mascota);
        model.addAttribute("carnet", carnetVacunaService.listarPorMascota(idMascota));
        return "mascotas/carnet";
    }

    /** RF0046 — registrar una vacuna aplicada (VETERINARIO/RECEPCIONISTA, verificado por SecurityConfig). */
    @PostMapping("/{idMascota}/nueva")
    public String registrar(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                             @RequestParam Integer idVacuna, @RequestParam Integer idLote, @RequestParam String dosis,
                             @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fechaAplicacion,
                             @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate proximaDosis,
                             @RequestParam(required = false) String observaciones) {
        try {
            carnetVacunaService.registrar(idMascota, idVacuna, idLote, dosis, fechaAplicacion, proximaDosis,
                    observaciones, principal.getUsuario().getIdUsuario());
        } catch (RuntimeException e) {
            return "redirect:/historial/paciente/" + idMascota + "?errorVacuna=" + e.getMessage();
        }
        return "redirect:/historial/paciente/" + idMascota + "?vacunaRegistrada";
    }

    @GetMapping("/{idMascota}/catalogo")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> catalogoPorEspecie(@PathVariable Integer idMascota) {
        var mascota = mascotaService.buscarPorId(idMascota);
        return vacunaRepository.findByEspecie_IdEspecie(mascota.getEspecie().getIdEspecie()).stream()
                .map(v -> java.util.Map.<String, Object>of("id", v.getIdVacuna(), "nombre", v.getNombreVacuna()))
                .toList();
    }
}
