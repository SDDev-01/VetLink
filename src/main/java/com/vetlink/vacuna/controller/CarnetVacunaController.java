package com.vetlink.vacuna.controller;

import com.vetlink.security.AccesoPacienteService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.model.Rol;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.vacuna.repository.LoteVacunaRepository;
import com.vetlink.vacuna.repository.VacunaRepository;
import com.vetlink.vacuna.service.CarnetVacunaService;
import com.vetlink.mascota.service.MascotaService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/carnet")
public class CarnetVacunaController {

    private final CarnetVacunaService carnetVacunaService;
    private final MascotaService mascotaService;
    private final VacunaRepository vacunaRepository;
    private final LoteVacunaRepository loteVacunaRepository;
    private final AccesoPacienteService accesoPacienteService;
    private final com.vetlink.cita.repository.CitaRepository citaRepository;
    private final com.vetlink.cita.service.AtencionService atencionService;

    public CarnetVacunaController(CarnetVacunaService carnetVacunaService, MascotaService mascotaService,
                                   VacunaRepository vacunaRepository, LoteVacunaRepository loteVacunaRepository,
                                   AccesoPacienteService accesoPacienteService,
                                   com.vetlink.cita.repository.CitaRepository citaRepository,
                                   com.vetlink.cita.service.AtencionService atencionService) {
        this.citaRepository = citaRepository;
        this.atencionService = atencionService;
        this.carnetVacunaService = carnetVacunaService;
        this.mascotaService = mascotaService;
        this.vacunaRepository = vacunaRepository;
        this.loteVacunaRepository = loteVacunaRepository;
        this.accesoPacienteService = accesoPacienteService;
    }

    @GetMapping("/{idMascota}")
    public String ver(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "mascotas");
        model.addAttribute("navItems", DashboardController.navPara(principal.getUsuario()));
        var mascota = mascotaService.buscarPorId(idMascota);
        accesoPacienteService.verificar(mascota, principal);
        model.addAttribute("mascota", mascota);
        var usuario = principal.getUsuario();
        model.addAttribute("carnet", carnetVacunaService.listarPorMascota(idMascota).stream()
                .filter(c -> usuario.getRol() != Rol.RECEPCIONISTA || (c.getVeterinaria() != null
                        && c.getVeterinaria().getIdVeterinaria().equals(usuario.getRecepcionista().getVeterinaria().getIdVeterinaria())))
                .toList());
        return "mascotas/carnet";
    }

    /** RF0046 — registrar una vacuna aplicada. Solo el VETERINARIO que atiende o atendió a la mascota (VL-03). */
    @PostMapping("/{idMascota}/nueva")
    public String registrar(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                             @RequestParam Integer idVacuna, @RequestParam Integer idLote, @RequestParam String dosis,
                             @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fechaAplicacion,
                             @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate proximaDosis,
                             @RequestParam(required = false) String observaciones,
                             RedirectAttributes redirectAttributes) {
        if (principal.getUsuario().getRol() != Rol.VETERINARIO) {
            throw new AccessDeniedException("Solo un veterinario puede registrar vacunas");
        }
        accesoPacienteService.verificar(mascotaService.buscarPorId(idMascota), principal);
        // Las vacunas solo se registran durante una cita de vacunación EN ATENCIÓN del propio veterinario.
        var cita = citaRepository.findByVeterinario_IdUsuarioAndEstado(principal.getUsuario().getIdUsuario(),
                        com.vetlink.cita.model.EstadoCita.EN_ATENCION).stream()
                .filter(c -> c.getMascota() != null && c.getMascota().getIdMascota().equals(idMascota))
                .findFirst();
        if (cita.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorVacuna",
                    "Las vacunas se registran durante una cita de vacunación en atención.");
            return "redirect:/historial/paciente/" + idMascota;
        }
        try {
            atencionService.registrarVacuna(cita.get().getIdCita(), idVacuna, idLote, dosis, proximaDosis, observaciones,
                    principal.getUsuario().getIdUsuario());
        } catch (RuntimeException e) {
            String mensaje = (e instanceof IllegalArgumentException || e instanceof IllegalStateException)
                    ? e.getMessage()
                    : "No se pudo registrar la vacuna";
            redirectAttributes.addFlashAttribute("errorVacuna", mensaje);
            return "redirect:/historial/paciente/" + idMascota;
        }
        return "redirect:/historial/paciente/" + idMascota + "?vacunaRegistrada";
    }

    @GetMapping("/{idMascota}/catalogo")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> catalogoPorEspecie(@PathVariable Integer idMascota,
                                                                         @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        var mascota = mascotaService.buscarPorId(idMascota);
        accesoPacienteService.verificar(mascota, principal);
        return vacunaRepository.findByEspecie_IdEspecie(mascota.getEspecie().getIdEspecie()).stream()
                .map(v -> java.util.Map.<String, Object>of("id", v.getIdVacuna(), "nombre", v.getNombreVacuna()))
                .toList();
    }
}
