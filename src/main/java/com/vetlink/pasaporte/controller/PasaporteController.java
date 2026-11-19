package com.vetlink.pasaporte.controller;

import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.service.MascotaService;
import com.vetlink.pasaporte.model.DuracionPasaporte;
import com.vetlink.pasaporte.model.Pasaporte;
import com.vetlink.pasaporte.service.PasaporteNoEncontradoException;
import com.vetlink.pasaporte.service.PasaporteService;
import com.vetlink.pasaporte.service.ServicioNoDisponibleException;
import com.vetlink.security.AccesoPacienteService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * "Compartir historial": el dueño crea, ve y revoca los pasaportes de salud de su mascota.
 * Solo el dueño (la misma verificación de propietario del resto de /mascotas).
 * No sabe que existe FastAPI: habla con {@link PasaporteService}.
 */
@Controller
@RequestMapping("/mascotas/{idMascota}/pasaporte")
public class PasaporteController {

    static final String VISTA = "mascotas/pasaporte";

    private final MascotaService mascotaService;
    private final AccesoPacienteService accesoPacienteService;
    private final PasaporteService pasaporteService;

    public PasaporteController(MascotaService mascotaService, AccesoPacienteService accesoPacienteService,
                               PasaporteService pasaporteService) {
        this.mascotaService = mascotaService;
        this.accesoPacienteService = accesoPacienteService;
        this.pasaporteService = pasaporteService;
    }

    @GetMapping
    public String ver(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                      Model model) {
        Mascota mascota = mascotaDelDuenio(idMascota, principal);
        List<Pasaporte> pasaportes;
        try {
            pasaportes = pasaporteService.listar(idMascota);
        } catch (ServicioNoDisponibleException e) {
            pasaportes = List.of();
            model.addAttribute("errorServicio", e.getMessage());
        }
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "mascotas");
        model.addAttribute("navItems", DashboardController.navTutor());
        model.addAttribute("mascota", mascota);
        model.addAttribute("pasaportes", pasaportes);
        model.addAttribute("duraciones", DuracionPasaporte.values());
        return VISTA;
    }

    @PostMapping
    public String crear(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                        @RequestParam(value = "horas", required = false) Integer horas,
                        RedirectAttributes redirect) {
        mascotaDelDuenio(idMascota, principal);
        DuracionPasaporte duracion = horas == null ? null : DuracionPasaporte.desdeHoras(horas).orElse(null);
        if (duracion == null) {
            redirect.addFlashAttribute("error", "Elige una duración válida: 24 horas, 7 días o 30 días.");
            return volver(idMascota);
        }
        try {
            Pasaporte creado = pasaporteService.crear(idMascota, principal.getUsuario().getIdUsuario(), duracion);
            redirect.addFlashAttribute("creado", creado);
        } catch (ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (PasaporteNoEncontradoException e) {
            redirect.addFlashAttribute("error", "No se pudo crear el pasaporte de esta mascota.");
        }
        return volver(idMascota);
    }

    @PostMapping("/{token}/revocar")
    public String revocar(@PathVariable Integer idMascota, @PathVariable String token,
                          @AuthenticationPrincipal VetlinkUserPrincipal principal, RedirectAttributes redirect) {
        mascotaDelDuenio(idMascota, principal);
        try {
            boolean esDeLaMascota = pasaporteService.listar(idMascota).stream()
                    .anyMatch(p -> p.token().equals(token));
            if (!esDeLaMascota) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El pasaporte no existe");
            }
            pasaporteService.revocar(token);
            redirect.addFlashAttribute("revocado", true);
        } catch (ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (PasaporteNoEncontradoException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El pasaporte no existe", e);
        }
        return volver(idMascota);
    }

    private Mascota mascotaDelDuenio(Integer idMascota, VetlinkUserPrincipal principal) {
        Mascota mascota = mascotaService.buscarPorId(idMascota);
        accesoPacienteService.verificarPropietario(mascota, principal);
        return mascota;
    }

    private static String volver(Integer idMascota) {
        return "redirect:/mascotas/" + idMascota + "/pasaporte";
    }
}
