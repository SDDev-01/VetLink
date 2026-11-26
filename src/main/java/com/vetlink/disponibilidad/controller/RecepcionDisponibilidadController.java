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
        Map<Integer, Object> bloqueos = new LinkedHashMap<>();
        for (Usuario v : equipo) {
            horarios.put(v.getIdUsuario(), solicitudes.horarioAprobado(v.getIdUsuario()));
            servicios.put(v.getIdUsuario(), solicitudes.serviciosAprobados(v.getIdUsuario()));
            bloqueos.put(v.getIdUsuario(), solicitudes.bloqueosVigentes(v.getIdUsuario()));
        }
        // Solo citas de ESTA clínica (antes se listaban las del veterinario en cualquier clínica
        // y al abrir una ajena la seguridad respondía 403).
        Map<Integer, List<Cita>> afectadas = disponibilidad.citasAfectadasDeClinica(clinica).stream()
                .collect(java.util.stream.Collectors.groupingBy(c -> c.getVeterinario().getIdUsuario(),
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        m.addAttribute("horarios", horarios);
        m.addAttribute("serviciosEquipo", servicios);
        m.addAttribute("bloqueos", bloqueos);
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

    /** Levanta un bloqueo de agenda de un veterinario de la clínica (pide confirmación en la vista). */
    @PostMapping("/bloqueo/{idAusencia}/desbloquear")
    public String desbloquear(@PathVariable Integer idAusencia, @AuthenticationPrincipal VetlinkUserPrincipal p,
                              RedirectAttributes ra) {
        try {
            int creados = solicitudes.desbloquearAgenda(idAusencia, recepcionista(p));
            ra.addFlashAttribute("mensajeOk", "Agendamiento desbloqueado. Se publicaron " + creados
                    + " cupo(s) nuevos dentro del horario laboral del veterinario; las ausencias y citas existentes se respetan.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorGeneral", e.getMessage());
        }
        return "redirect:/recepcion/disponibilidad#equipo";
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

    /** Encabezados de la cuadrícula semanal del horario solicitado (1 = lunes … 7 = domingo). */
    @ModelAttribute("nombresDiaCorto")
    public Map<Integer, String> nombresDiaCorto() {
        return Map.of(1, "Lun", 2, "Mar", 3, "Mié", 4, "Jue", 5, "Vie", 6, "Sáb", 7, "Dom");
    }

    @ModelAttribute("tipoSolicitudClase")
    public Map<String, String> clases() {
        return Map.of("HORARIO", "fa-clock", "AUSENCIA", "fa-calendar-xmark", "SERVICIOS", "fa-briefcase-medical");
    }
}
