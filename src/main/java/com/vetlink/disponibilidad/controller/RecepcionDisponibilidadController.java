package com.vetlink.disponibilidad.controller;

import com.vetlink.cita.model.Cita;
import com.vetlink.disponibilidad.service.DisponibilidadService;
import com.vetlink.disponibilidad.service.SolicitudDisponibilidadService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.model.Veterinario;
import com.vetlink.usuario.repository.VeterinarioRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Recepción: revisión de solicitudes de disponibilidad, bloqueos de agenda y citas afectadas. */
@Controller
@RequestMapping("/recepcion/disponibilidad")
public class RecepcionDisponibilidadController {

    private final SolicitudDisponibilidadService solicitudes;
    private final DisponibilidadService disponibilidad;
    private final VeterinarioRepository veterinarios;

    public RecepcionDisponibilidadController(SolicitudDisponibilidadService solicitudes, DisponibilidadService disponibilidad,
                                             VeterinarioRepository veterinarios) {
        this.solicitudes = solicitudes;
        this.disponibilidad = disponibilidad;
        this.veterinarios = veterinarios;
    }

    @GetMapping
    public String panel(@AuthenticationPrincipal VetlinkUserPrincipal p, Model m) {
        Usuario u = recepcionista(p);
        Integer clinica = u.getRecepcionista().getVeterinaria().getIdVeterinaria();
        shell(m, u);
        m.addAttribute("pendientes", solicitudes.pendientesDeClinica(clinica));
        m.addAttribute("historialSolicitudes", solicitudes.historialDeClinica(clinica).stream().limit(30).toList());
        List<Usuario> equipo = veterinarios.findByVeterinaria_IdVeterinaria(clinica).stream()
                .map(Veterinario::getUsuario).filter(Usuario::isActivo).toList();
        m.addAttribute("equipo", equipo);
        Map<Integer, Object> horarios = new LinkedHashMap<>();
        Map<Integer, Object> servicios = new LinkedHashMap<>();
        Map<Integer, List<Cita>> afectadas = new LinkedHashMap<>();
        for (Usuario v : equipo) {
            horarios.put(v.getIdUsuario(), solicitudes.horarioAprobado(v.getIdUsuario()));
            servicios.put(v.getIdUsuario(), solicitudes.serviciosAprobados(v.getIdUsuario()));
            List<Cita> af = disponibilidad.citasAfectadas(v.getIdUsuario());
            if (!af.isEmpty()) afectadas.put(v.getIdUsuario(), af);
        }
        m.addAttribute("horarios", horarios);
        m.addAttribute("serviciosEquipo", servicios);
        m.addAttribute("afectadas", afectadas);
        m.addAttribute("srv", solicitudes);
        return "disponibilidad/recepcion";
    }

    @PostMapping("/{id}/aprobar")
    public String aprobar(@PathVariable Integer id, @RequestParam(required = false) String observacion,
                          @AuthenticationPrincipal VetlinkUserPrincipal p, Model m, RedirectAttributes ra) {
        Usuario u = recepcionista(p);
        try {
            var r = solicitudes.aprobar(id, u, observacion);
            shell(m, u);
            m.addAttribute("resultado", r);
            m.addAttribute("alternativas", r.citasAfectadas().stream()
                    .collect(LinkedHashMap::new, (map, c) -> map.put(c.getIdCita(), disponibilidad.alternativas(c)), Map::putAll));
            return "disponibilidad/resultado";
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorGeneral", e.getMessage());
            return "redirect:/recepcion/disponibilidad";
        }
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(@PathVariable Integer id, @RequestParam(required = false) String observacion,
                           @AuthenticationPrincipal VetlinkUserPrincipal p, RedirectAttributes ra) {
        try {
            solicitudes.rechazar(id, recepcionista(p), observacion);
            ra.addFlashAttribute("mensajeOk", "Solicitud rechazada. Se notificó al veterinario.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorGeneral", e.getMessage());
        }
        return "redirect:/recepcion/disponibilidad";
    }

    @PostMapping("/bloqueo")
    public String bloquear(@RequestParam(required = false) Integer idVeterinario,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
                           @RequestParam(required = false) String motivo,
                           @AuthenticationPrincipal VetlinkUserPrincipal p, Model m, RedirectAttributes ra) {
        Usuario u = recepcionista(p);
        try {
            if (idVeterinario == null) throw new IllegalArgumentException("Selecciona el veterinario");
            var r = solicitudes.bloquearAgenda(u, idVeterinario, inicio, fin, motivo);
            shell(m, u);
            m.addAttribute("resultado", r);
            m.addAttribute("alternativas", r.citasAfectadas().stream()
                    .collect(LinkedHashMap::new, (map, c) -> map.put(c.getIdCita(), disponibilidad.alternativas(c)), Map::putAll));
            return "disponibilidad/resultado";
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorGeneral", e.getMessage());
            return "redirect:/recepcion/disponibilidad";
        }
    }

    private static void shell(Model m, Usuario u) {
        m.addAttribute("usuario", u);
        m.addAttribute("seccionActiva", "disponibilidad");
        m.addAttribute("navItems", DashboardController.navRecepcionista());
    }

    private static Usuario recepcionista(VetlinkUserPrincipal p) {
        if (p.getUsuario().getRecepcionista() == null) throw new AccessDeniedException("No autorizado");
        return p.getUsuario();
    }

    @ModelAttribute("tipoSolicitudClase")
    public Map<String, String> clases() {
        return Map.of("HORARIO", "fa-clock", "AUSENCIA", "fa-calendar-xmark", "SERVICIOS", "fa-briefcase-medical");
    }
}
