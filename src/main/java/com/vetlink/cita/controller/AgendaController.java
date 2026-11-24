package com.vetlink.cita.controller;

import com.vetlink.cita.dto.CitaCalendarDTO;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.cita.service.AtencionService;
import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.historial.service.HistorialClinicoService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.usuario.model.Rol;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Agenda clínica: el alcance se calcula con el usuario de sesión, nunca con filtros del navegador. */
@Controller
@RequestMapping("/agenda")
public class AgendaController {

    private final CitaRepository citas;
    private final HistorialClinicoService historiales;
    private final AtencionService atencionService;

    public AgendaController(CitaRepository citas, HistorialClinicoService historiales, AtencionService atencionService) {
        this.citas = citas;
        this.historiales = historiales;
        this.atencionService = atencionService;
    }

    @GetMapping
    public String agenda(@AuthenticationPrincipal VetlinkUserPrincipal p, Model m) {
        List<Cita> lista = citasPara(p);
        shell(m, p);
        m.addAttribute("citas", lista);
        m.addAttribute("citasJson", lista.stream()
                .map(c -> new CitaCalendarDTO(c.getIdCita(), c.getFecha().toString(), c.getHora().toString(),
                        c.getEstado().name(),
                        c.getMascota() == null ? "Cupo disponible" : c.getMascota().getNombre(),
                        c.getServicio().getNombreServicio()))
                .toList());
        return "citas/agenda";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p, Model m) {
        Cita c = autorizada(id, p);
        mostrarDetalle(c, p, m);
        return "citas/detalle-profesional";
    }

    @GetMapping("/{id}/atender")
    public String atenderForm(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p, Model m) {
        Cita c = autorizadaVeterinario(id, p);
        // VL-18: si la cita no se puede atender (no es de hoy, no está agendada), se muestra
        // el detalle con el motivo en vez del formulario.
        try {
            atencionService.verificarAtendible(c, p.getUsuario().getIdUsuario());
        } catch (IllegalStateException e) {
            mostrarDetalle(c, p, m);
            m.addAttribute("mensajeAtencion", e.getMessage());
            return "citas/detalle-profesional";
        }
        HistorialClinicoForm f = new HistorialClinicoForm();
        f.setIdMascota(c.getMascota().getIdMascota());
        mostrarFormulario(c, p, m, f);
        return "historial/atender-cita";
    }

    @PostMapping("/{id}/atender")
    public String atender(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p,
                          @Valid @ModelAttribute("form") HistorialClinicoForm f, BindingResult r, Model m) {
        Cita c = autorizadaVeterinario(id, p);
        if (r.hasErrors()) {
            mostrarFormulario(c, p, m, f);
            return "historial/atender-cita";
        }
        try {
            atencionService.atender(id, f, p.getUsuario().getIdUsuario());
        } catch (IllegalStateException | IllegalArgumentException e) {
            mostrarFormulario(c, p, m, f);
            m.addAttribute("errorGeneral", e.getMessage());
            return "historial/atender-cita";
        }
        return "redirect:/agenda/" + id + "?atendida";
    }

    private void mostrarDetalle(Cita c, VetlinkUserPrincipal p, Model m) {
        shell(m, p);
        m.addAttribute("cita", c);
        m.addAttribute("historial", c.getMascota() == null
                ? List.of()
                : historiales.listarPorMascota(c.getMascota().getIdMascota()));
    }

    private void mostrarFormulario(Cita c, VetlinkUserPrincipal p, Model m, HistorialClinicoForm f) {
        shell(m, p);
        m.addAttribute("cita", c);
        m.addAttribute("historialPrevio", historiales.listarPorMascota(c.getMascota().getIdMascota()));
        m.addAttribute("form", f);
    }

    private List<Cita> citasPara(VetlinkUserPrincipal p) {
        return switch (p.getUsuario().getRol()) {
            case VETERINARIO -> citas.findByVeterinario_IdUsuarioOrderByFechaDesc(p.getUsuario().getIdUsuario());
            case RECEPCIONISTA -> citas.buscarConFiltros(
                    p.getUsuario().getRecepcionista().getVeterinaria().getIdVeterinaria(), null, null, null, null);
            default -> throw new AccessDeniedException("Agenda no disponible");
        };
    }

    /** VL-25: se carga solo la cita pedida y se compara con el usuario, sin traer toda la agenda. */
    private Cita autorizada(Integer id, VetlinkUserPrincipal p) {
        Cita c = citas.findById(id).orElseThrow();
        var u = p.getUsuario();
        boolean permitido = switch (u.getRol()) {
            case VETERINARIO -> c.getVeterinario().getIdUsuario().equals(u.getIdUsuario());
            case RECEPCIONISTA -> c.getVeterinaria().getIdVeterinaria()
                    .equals(u.getRecepcionista().getVeterinaria().getIdVeterinaria());
            default -> false;
        };
        if (!permitido) {
            throw new AccessDeniedException("No tienes acceso a esta cita");
        }
        return c;
    }

    private Cita autorizadaVeterinario(Integer id, VetlinkUserPrincipal p) {
        if (p.getUsuario().getRol() != Rol.VETERINARIO) {
            throw new AccessDeniedException("Solo un veterinario puede atender citas");
        }
        return autorizada(id, p);
    }

    private void shell(Model m, VetlinkUserPrincipal p) {
        m.addAttribute("usuario", p.getUsuario());
        m.addAttribute("seccionActiva", "citas");
        m.addAttribute("navItems", p.getUsuario().getRol() == Rol.VETERINARIO
                ? DashboardController.navVeterinario()
                : DashboardController.navRecepcionista());
    }
}
