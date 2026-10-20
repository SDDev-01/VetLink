package com.vetlink.usuario.controller;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.cita.dto.CitaCalendarDTO;
import com.vetlink.comun.model.NavItem;
import com.vetlink.comun.model.Notificacion;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.vacuna.model.CarnetVacuna;
import com.vetlink.vacuna.repository.CarnetVacunaRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class DashboardController {

    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final CarnetVacunaRepository carnetVacunaRepository;

    public DashboardController(MascotaRepository mascotaRepository, CitaRepository citaRepository,
                                CarnetVacunaRepository carnetVacunaRepository) {
        this.mascotaRepository = mascotaRepository;
        this.citaRepository = citaRepository;
        this.carnetVacunaRepository = carnetVacunaRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        var usuario = principal.getUsuario();
        model.addAttribute("usuario", usuario);
        model.addAttribute("seccionActiva", "home");

        return switch (usuario.getRol()) {
            case TUTOR -> {
                Integer idTutor = usuario.getIdUsuario();
                List<Mascota> mascotas = mascotaRepository.findByTutor_IdUsuarioAndEstado(idTutor, (byte) 1);
                List<Cita> proximas = citaRepository.findByMascota_Tutor_IdUsuarioAndEstadoOrderByFechaAscHoraAsc(idTutor, EstadoCita.AGENDADA);
                List<Cita> todasCitas = citaRepository.findByMascota_Tutor_IdUsuarioOrderByFechaDescHoraDesc(idTutor);
                var vacunasPendientes = carnetVacunaRepository.pendientesDeTutorHasta(idTutor, LocalDate.now().plusDays(30));

                model.addAttribute("navItems", navTutor());
                model.addAttribute("mascotas", mascotas);
                model.addAttribute("statMascotas", mascotas.size());
                model.addAttribute("statVacunas", vacunasPendientes.size());
                model.addAttribute("proximaCita", proximas.isEmpty() ? null : proximas.get(0));
                model.addAttribute("todasCitas", todasCitas.stream().map(this::aCalendarDTO).toList());
                model.addAttribute("notificaciones", notificacionesTutor(proximas, vacunasPendientes));

                yield "dashboard/tutor";
            }
            case VETERINARIO -> {
                Integer idVet = usuario.getIdUsuario();
                List<Cita> hoy = citaRepository.findByVeterinario_IdUsuarioAndFecha(idVet, LocalDate.now());
                List<Cita> proximas = citaRepository.buscarConFiltros(null, EstadoCita.AGENDADA, LocalDate.now(), null, idVet);

                model.addAttribute("navItems", navVeterinario());
                model.addAttribute("citasHoy", hoy);
                model.addAttribute("statCitasHoy", hoy.size());
                model.addAttribute("statProximas", proximas.size());
                model.addAttribute("veterinaria", usuario.getVeterinario() != null ? usuario.getVeterinario().getVeterinaria() : null);

                yield "dashboard/veterinario";
            }
            case RECEPCIONISTA -> {
                var veterinaria = usuario.getRecepcionista().getVeterinaria();
                List<Cita> hoy = citaRepository.buscarConFiltros(veterinaria.getIdVeterinaria(), null, LocalDate.now(), LocalDate.now(), null);
                List<Cita> agendadas = citaRepository.buscarConFiltros(veterinaria.getIdVeterinaria(), EstadoCita.AGENDADA, null, null, null);

                model.addAttribute("navItems", navRecepcionista());
                model.addAttribute("veterinaria", veterinaria);
                model.addAttribute("citasHoy", hoy);
                model.addAttribute("statCitasHoy", hoy.size());
                model.addAttribute("statAgendadas", agendadas.size());

                yield "dashboard/recepcionista";
            }
        };
    }

    private CitaCalendarDTO aCalendarDTO(Cita c) {
        return new CitaCalendarDTO(c.getIdCita(), c.getFecha().toString(), c.getHora().toString(),
                c.getEstado().name(), c.getMascota().getNombre(), c.getServicio().getNombreServicio());
    }

    /** RF de notificaciones: citas agendadas dentro de 3 días + vacunas por vencer dentro de 30 días. */
    private List<Notificacion> notificacionesTutor(List<Cita> proximasAgendadas, List<CarnetVacuna> vacunasPendientes) {
        List<Notificacion> notifs = new ArrayList<>();
        LocalDate limiteCita = LocalDate.now().plusDays(3);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d 'de' MMMM");

        for (Cita c : proximasAgendadas) {
            if (!c.getFecha().isAfter(limiteCita)) {
                notifs.add(new Notificacion("fa-calendar-check", "vl-badge-info",
                        c.getMascota().getNombre() + ": cita el " + c.getFecha().format(fmt) + " a las " + c.getHora()));
            }
        }
        for (CarnetVacuna v : vacunasPendientes) {
            boolean vencida = v.getProximaDosis().isBefore(LocalDate.now());
            notifs.add(new Notificacion("fa-syringe", vencida ? "vl-badge-danger" : "vl-badge-warn",
                    v.getMascota().getNombre() + ": " + v.getVacuna().getNombreVacuna()
                            + (vencida ? " vencida" : " próxima a vencer") + " (" + v.getProximaDosis().format(fmt) + ")"));
        }
        return notifs;
    }

    public static List<NavItem> navTutor() {
        return List.of(
                new NavItem("home", "/dashboard", "fas fa-house", "Inicio"),
                new NavItem("mascotas", "/mascotas", "fas fa-dog", "Mis Mascotas"),
                new NavItem("citas", "/citas/mias", "fas fa-calendar-alt", "Mis Citas"),
                new NavItem("historial", "/historial/mias", "fas fa-file-medical", "Historial Clínico"),
                new NavItem("directorio", "/directorio", "fas fa-hospital-user", "Buscar Veterinarias"),
                new NavItem("chat", "/chat", "fas fa-comments", "Chat"),
                new NavItem("emergencias", "/emergencias", "fas fa-circle-exclamation", "Emergencias"),
                new NavItem("resenas", "/resenas/mias", "fas fa-star-half-alt", "Reseñas"),
                new NavItem("perfil", "/perfil", "fas fa-user-circle", "Mi Perfil")
        );
    }

    public static List<NavItem> navVeterinario() {
        return List.of(
                new NavItem("home", "/dashboard", "fas fa-house", "Inicio"),
                new NavItem("citas", "/agenda", "fas fa-calendar-alt", "Mi Agenda"),
                new NavItem("pacientes", "/historial/pacientes", "fas fa-dog", "Pacientes"),
                new NavItem("directorio", "/veterinarias/mi-clinica", "fas fa-hospital-user", "Mi veterinaria"),
                new NavItem("chat", "/chat", "fas fa-comments", "Chat"),
                new NavItem("emergencias", "/emergencias", "fas fa-truck-medical", "Emergencias"),
                new NavItem("perfil", "/perfil", "fas fa-user-circle", "Mi Perfil")
        );
    }
    public static List<NavItem> navRecepcionista() {
        return List.of(
                new NavItem("home", "/dashboard", "fas fa-house", "Inicio"),
                new NavItem("citas", "/agenda", "fas fa-calendar-alt", "Citas de la Clínica"),
                new NavItem("mascotas", "/recepcion/mascotas", "fas fa-dog", "Mascotas"),
                new NavItem("historial", "/historial/pacientes", "fas fa-file-medical", "Historial Clínico"),
                new NavItem("veterinaria", "/veterinarias/admin", "fas fa-hospital", "Mi Veterinaria"),
                new NavItem("directorio", "/directorio", "fas fa-hospital-user", "Directorio"),
                new NavItem("chat", "/chat", "fas fa-comments", "Chat"),
                new NavItem("emergencias", "/emergencias", "fas fa-truck-medical", "Emergencias"),
                new NavItem("perfil", "/perfil", "fas fa-user-circle", "Mi Perfil")
        );
    }
}
