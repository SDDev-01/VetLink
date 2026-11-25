package com.vetlink.disponibilidad.controller;

import com.vetlink.disponibilidad.model.TipoAusencia;
import com.vetlink.disponibilidad.service.SolicitudDisponibilidadService;
import com.vetlink.disponibilidad.service.SolicitudDisponibilidadService.Franja;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.usuario.model.Usuario;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/** «Mi disponibilidad» del VETERINARIO: todo cambio queda pendiente de aprobación de recepción. */
@Controller
@RequestMapping("/disponibilidad")
public class DisponibilidadController {

    private final SolicitudDisponibilidadService solicitudes;

    public DisponibilidadController(SolicitudDisponibilidadService solicitudes) {
        this.solicitudes = solicitudes;
    }

    @GetMapping
    public String ver(@AuthenticationPrincipal VetlinkUserPrincipal p, Model m) {
        Usuario vet = veterinario(p);
        Integer id = vet.getIdUsuario();
        m.addAttribute("usuario", vet);
        m.addAttribute("seccionActiva", "disponibilidad");
        m.addAttribute("navItems", DashboardController.navVeterinario());
        m.addAttribute("veterinaria", vet.getVeterinario().getVeterinaria());
        var horario = solicitudes.horarioAprobado(id);
        m.addAttribute("horario", horario);
        // VET-01: el selector semanal parte del horario aprobado, agrupado por día (1 = lunes … 7 = domingo).
        java.util.Map<Integer, List<com.vetlink.disponibilidad.model.HorarioVeterinario>> porDia = new java.util.TreeMap<>();
        for (int d = 1; d <= 7; d++) porDia.put(d, new ArrayList<>());
        horario.forEach(h -> porDia.get(h.getDiaSemana()).add(h));
        porDia.values().forEach(l -> l.sort(java.util.Comparator.comparing(com.vetlink.disponibilidad.model.HorarioVeterinario::getHoraInicio)));
        m.addAttribute("horarioPorDia", porDia);
        m.addAttribute("nombresDia", List.of("", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"));
        m.addAttribute("servicios", solicitudes.serviciosAprobados(id));
        m.addAttribute("ausencias", solicitudes.ausenciasProximas(id));
        m.addAttribute("solicitudes", solicitudes.deVeterinario(id));
        m.addAttribute("serviciosClinica", solicitudes.serviciosDeClinica(vet.getVeterinario().getVeterinaria().getIdVeterinaria())
                .stream().map(sv -> sv.getServicio()).toList());
        m.addAttribute("idsServiciosAprobados", solicitudes.serviciosAprobados(id).stream()
                .map(s -> s.getServicio().getIdServicio()).toList());
        m.addAttribute("tiposAusencia", Arrays.stream(TipoAusencia.values()).filter(t -> t != TipoAusencia.BLOQUEO).toList());
        m.addAttribute("solicitudSrv", solicitudes);
        return "disponibilidad/veterinario";
    }

    @PostMapping("/horario")
    public String horario(@AuthenticationPrincipal VetlinkUserPrincipal p,
                          @RequestParam(name = "dia", required = false) List<Integer> dias,
                          @RequestParam(name = "inicio", required = false) List<String> inicios,
                          @RequestParam(name = "fin", required = false) List<String> fines,
                          @RequestParam(required = false) String descripcion, RedirectAttributes ra) {
        List<Franja> franjas = new ArrayList<>();
        int n = dias == null ? 0 : dias.size();
        for (int i = 0; i < n; i++) {
            franjas.add(new Franja(dias.get(i), hora(inicios, i), hora(fines, i)));
        }
        return ejecutar(ra, "Solicitud de horario enviada a recepción.",
                () -> solicitudes.solicitarHorario(veterinario(p), franjas, descripcion));
    }

    @PostMapping("/ausencia")
    public String ausencia(@AuthenticationPrincipal VetlinkUserPrincipal p, @RequestParam(required = false) TipoAusencia tipo,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
                           @RequestParam(required = false) String motivo, RedirectAttributes ra) {
        return ejecutar(ra, "Novedad registrada y enviada a recepción para aprobación.",
                () -> solicitudes.solicitarAusencia(veterinario(p), tipo, inicio, fin, motivo));
    }

    @PostMapping("/servicios")
    public String servicios(@AuthenticationPrincipal VetlinkUserPrincipal p,
                            @RequestParam(name = "servicios", required = false) List<Integer> servicios,
                            @RequestParam(required = false) String descripcion, RedirectAttributes ra) {
        return ejecutar(ra, "Solicitud de servicios enviada a recepción.",
                () -> solicitudes.solicitarServicios(veterinario(p), servicios, descripcion));
    }

    private static LocalTime hora(List<String> lista, int i) {
        if (lista == null || i >= lista.size() || lista.get(i) == null || lista.get(i).isBlank()) return null;
        try {
            return LocalTime.parse(lista.get(i));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String ejecutar(RedirectAttributes ra, String ok, Supplier<?> accion) {
        try {
            accion.get();
            ra.addFlashAttribute("mensajeOk", ok);
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorGeneral", e.getMessage());
        }
        return "redirect:/disponibilidad";
    }

    private static Usuario veterinario(VetlinkUserPrincipal p) {
        Usuario u = p.getUsuario();
        if (u.getVeterinario() == null || u.getVeterinario().getVeterinaria() == null) {
            throw new AccessDeniedException("Solo los veterinarios con clínica registran disponibilidad");
        }
        return u;
    }
}
