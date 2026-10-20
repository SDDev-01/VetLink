package com.vetlink.cita.controller;

import com.vetlink.cita.dto.AgendarCitaForm;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.veterinaria.model.HorarioVeterinaria;
import com.vetlink.veterinaria.repository.HorarioVeterinariaRepository;
import com.vetlink.veterinaria.repository.ServicioRepository;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.cita.service.CitaService;
import com.vetlink.mascota.service.MascotaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** Agendamiento de citas para el TUTOR. Los reportes filtrados estan en ReporteController. */
@Controller
@RequestMapping("/citas")
public class CitaController {

    private final CitaService citaService;
    private final MascotaService mascotaService;
    private final VeterinariaRepository veterinariaRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final HorarioVeterinariaRepository horarioVeterinariaRepository;
    private final CitaRepository citaRepository;

    public CitaController(CitaService citaService, MascotaService mascotaService, VeterinariaRepository veterinariaRepository,
                           ServicioRepository servicioRepository, UsuarioRepository usuarioRepository,
                           HorarioVeterinariaRepository horarioVeterinariaRepository, CitaRepository citaRepository) {
        this.citaService = citaService;
        this.mascotaService = mascotaService;
        this.veterinariaRepository = veterinariaRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.horarioVeterinariaRepository = horarioVeterinariaRepository;
        this.citaRepository = citaRepository;
    }

    @GetMapping("/mias")
    public String misCitas(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        List<Cita> citas = citaService.listarPorTutor(principal.getUsuario().getIdUsuario());
        model.addAttribute("citas", citas);
        model.addAttribute("citasJson", citas.stream()
                .map(c -> new com.vetlink.cita.dto.CitaCalendarDTO(c.getIdCita(), c.getFecha().toString(), c.getHora().toString(),
                        c.getEstado().name(), c.getMascota().getNombre(), c.getServicio().getNombreServicio()))
                .toList());
        return "citas/mias";
    }

    @GetMapping("/mascota/{idMascota}")
    public String listarPorMascota(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        var mascota = mascotaService.buscarPorId(idMascota);
        verificarAccesoTutor(mascota, principal);
        model.addAttribute("mascota", mascota);
        model.addAttribute("citas", citaService.listarPorMascota(idMascota));
        return "citas/lista";
    }

    @GetMapping("/agendar/{idMascota}")
    public String agendarForm(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        var mascota = mascotaService.buscarPorId(idMascota);
        verificarAccesoTutor(mascota, principal);
        model.addAttribute("mascota", mascota);
        model.addAttribute("veterinarias", veterinariaRepository.findByEstado((byte) 1));
        model.addAttribute("servicios", servicioRepository.findAll());
        AgendarCitaForm form = new AgendarCitaForm();
        form.setIdMascota(idMascota);
        model.addAttribute("form", form);
        return "citas/formulario";
    }

    /** Punto de entrada directo desde el dashboard o el directorio. */
    @GetMapping("/agendar")
    public String agendarDirecto(@RequestParam(required = false) Integer idVeterinaria,
                                 @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        var mascotas = mascotaService.listarActivasPorTutor(principal.getUsuario().getIdUsuario());
        model.addAttribute("mascotas", mascotas);
        model.addAttribute("mascota", mascotas.isEmpty() ? null : mascotas.get(0));
        model.addAttribute("veterinarias", veterinariaRepository.findByEstado((byte) 1));
        model.addAttribute("servicios", servicioRepository.findAll());
        AgendarCitaForm form = new AgendarCitaForm();
        if (idVeterinaria != null && veterinariaRepository.existsById(idVeterinaria)) form.setIdVeterinaria(idVeterinaria);
        model.addAttribute("form", form);
        return "citas/formulario";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        Cita cita = citaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        if (cita.getMascota() == null || !cita.getMascota().getTutor().getIdUsuario()
                .equals(principal.getUsuario().getIdUsuario())) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes permiso sobre esta cita");
        }
        cargarShell(model, principal);
        model.addAttribute("cita", cita);
        return "citas/detalle";
    }

    @PostMapping("/agendar")
    public String agendar(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                           @Valid @ModelAttribute("form") AgendarCitaForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            cargarShell(model, principal);
            model.addAttribute("veterinarias", veterinariaRepository.findByEstado((byte) 1));
            model.addAttribute("servicios", servicioRepository.findAll());
            model.addAttribute("mascota", mascotaService.buscarPorId(form.getIdMascota()));
            return "citas/formulario";
        }
        try {
            citaService.agendar(form, principal.getUsuario().getIdUsuario());
        } catch (RuntimeException e) {
            model.addAttribute("errorGeneral", e.getMessage());
            cargarShell(model, principal);
            model.addAttribute("veterinarias", veterinariaRepository.findByEstado((byte) 1));
            model.addAttribute("servicios", servicioRepository.findAll());
            model.addAttribute("mascota", mascotaService.buscarPorId(form.getIdMascota()));
            return "citas/formulario";
        }
        return "redirect:/citas/mascota/" + form.getIdMascota() + "?agendada";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                            @RequestParam Integer idMascota,
                            @RequestParam(required = false, defaultValue = "mascota") String origen, Model model) {
        try {
            citaService.cancelar(id, principal.getUsuario().getIdUsuario());
        } catch (RuntimeException e) {
            if ("mias".equals(origen)) {
                cargarShell(model, principal);
                model.addAttribute("citas", citaService.listarPorTutor(principal.getUsuario().getIdUsuario()));
                model.addAttribute("errorGeneral", e.getMessage());
                return "citas/mias";
            }
            cargarShell(model, principal);
            var mascota = mascotaService.buscarPorId(idMascota);
            verificarAccesoTutor(mascota, principal);
            model.addAttribute("mascota", mascota);
            model.addAttribute("citas", citaService.listarPorMascota(idMascota));
            model.addAttribute("errorGeneral", e.getMessage());
            return "citas/lista";
        }
        return "mias".equals(origen) ? "redirect:/citas/mias?cancelada" : "redirect:/citas/mascota/" + idMascota + "?cancelada";
    }

    /** RF0028/RF0029 (adaptado): horario general del día + horas ya ocupadas, para pintar
     *  botones de horario disponible en el asistente de agendamiento, sin recargar la página. */
    @GetMapping("/horarios-disponibles")
    @ResponseBody
    public HorariosDisponiblesResponse horariosDisponibles(@RequestParam Integer idVeterinaria,
                                                            @RequestParam(required = false) Integer idVeterinario,
                                                            @RequestParam(required = false) Integer idServicio,
                                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        if (idVeterinario != null && idServicio != null) {
            List<String> cupos = citaRepository.findByVeterinaria_IdVeterinariaAndEstadoAndFechaGreaterThanEqualOrderByFechaAscHoraAsc(
                            idVeterinaria, EstadoCita.DISPONIBLE, fecha).stream()
                    .filter(c -> c.getFecha().equals(fecha))
                    .filter(c -> c.getVeterinario().getIdUsuario().equals(idVeterinario))
                    .filter(c -> c.getServicio().getIdServicio().equals(idServicio))
                    .map(c -> c.getHora().toString().substring(0, 5)).toList();
            return new HorariosDisponiblesResponse(null, null, cupos, cupos.isEmpty());
        }
        String nombreDia = nombreDiaEs(fecha.getDayOfWeek());

        HorarioVeterinaria horarioDia = horarioVeterinariaRepository.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .filter(h -> h.getEstado() == 1 && h.getDiaSemana().getDiaSemana().equalsIgnoreCase(nombreDia))
                .findFirst().orElse(null);

        if (horarioDia == null) {
            return new HorariosDisponiblesResponse(null, null, List.of(), true);
        }

        List<Cita> citasDelDia = idVeterinario != null
                ? citaRepository.findByVeterinario_IdUsuarioAndFecha(idVeterinario, fecha)
                : List.of();

        List<String> ocupadas = citasDelDia.stream()
                .filter(c -> c.getEstado() == EstadoCita.AGENDADA || c.getEstado() == EstadoCita.PENDIENTE)
                .map(c -> c.getHora().toString().substring(0, 5))
                .toList();

        return new HorariosDisponiblesResponse(
                horarioDia.getHoraApertura().toString().substring(0, 5),
                horarioDia.getHoraCierre().toString().substring(0, 5),
                ocupadas, false
        );
    }

    private String nombreDiaEs(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miercoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sabado";
            case SUNDAY -> "Domingo";
        };
    }

    public record HorariosDisponiblesResponse(String apertura, String cierre, List<String> ocupadas, boolean cerrado) {}

    /** Veterinarios que trabajan en una veterinaria dada, usado por AJAX desde el formulario. */
    @GetMapping("/veterinarios/{idVeterinaria}")
    @ResponseBody
    public List<VeterinarioOption> veterinariosDe(@PathVariable Integer idVeterinaria) {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getVeterinario() != null
                        && u.getVeterinario().getVeterinaria() != null
                        && u.getVeterinario().getVeterinaria().getIdVeterinaria().equals(idVeterinaria))
                .map(u -> new VeterinarioOption(u.getIdUsuario(), u.getNombre() + " " + u.getApellido()))
                .toList();
    }

    private void cargarShell(Model model, VetlinkUserPrincipal principal) {
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "citas");
        model.addAttribute("navItems", switch (principal.getUsuario().getRol()) {
            case TUTOR -> DashboardController.navTutor();
            case VETERINARIO -> DashboardController.navVeterinario();
            case RECEPCIONISTA -> DashboardController.navRecepcionista();
        });
    }

    private void verificarAccesoTutor(com.vetlink.mascota.model.Mascota mascota,
                                      VetlinkUserPrincipal principal) {
        if (principal.getUsuario().getRol() == com.vetlink.usuario.model.Rol.TUTOR
                && !mascota.getTutor().getIdUsuario().equals(principal.getUsuario().getIdUsuario())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para consultar esta mascota");
        }
    }

    public record VeterinarioOption(Integer id, String nombre) {}
}
