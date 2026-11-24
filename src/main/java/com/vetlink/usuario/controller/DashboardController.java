package com.vetlink.usuario.controller;

import com.vetlink.cita.dto.CitaCalendarDTO;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.comun.model.NavItem;
import com.vetlink.comun.model.Notificacion;
import com.vetlink.comun.web.FormatoVista;
import com.vetlink.disponibilidad.model.EstadoAprobacion;
import com.vetlink.disponibilidad.repository.SolicitudDisponibilidadRepository;
import com.vetlink.disponibilidad.service.DisponibilidadService;
import com.vetlink.emergencia.model.EstadoEmergencia;
import com.vetlink.emergencia.repository.EmergenciaRepository;
import com.vetlink.historial.repository.HistorialClinicoRepository;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.mascota.repository.TransferenciaMascotaRepository;
import com.vetlink.mascota.model.TransferenciaMascota;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.model.Veterinario;
import com.vetlink.usuario.repository.VeterinarioRepository;
import com.vetlink.vacuna.model.CarnetVacuna;
import com.vetlink.vacuna.repository.CarnetVacunaRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    /** Disponibilidad de un veterinario para el tablero de recepción. */
    public record EstadoVeterinario(Usuario usuario, String estado, String clase, int citasHoy, int pendientesHoy) {}

    /** Celda del calendario semanal del veterinario. */
    public record DiaSemanaAgenda(LocalDate fecha, String etiqueta, boolean hoy, List<Cita> citas) {}

    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final CarnetVacunaRepository carnetVacunaRepository;
    private final EmergenciaRepository emergenciaRepository;
    private final HistorialClinicoRepository historialRepository;
    private final SolicitudDisponibilidadRepository solicitudes;
    private final VeterinarioRepository veterinarioRepository;
    private final TransferenciaMascotaRepository transferencias;
    private final DisponibilidadService disponibilidad;
    private final Clock reloj;

    public DashboardController(MascotaRepository mascotaRepository, CitaRepository citaRepository,
                               CarnetVacunaRepository carnetVacunaRepository, EmergenciaRepository emergenciaRepository,
                               HistorialClinicoRepository historialRepository, SolicitudDisponibilidadRepository solicitudes,
                               VeterinarioRepository veterinarioRepository, TransferenciaMascotaRepository transferencias,
                               DisponibilidadService disponibilidad, Clock reloj) {
        this.mascotaRepository = mascotaRepository;
        this.citaRepository = citaRepository;
        this.carnetVacunaRepository = carnetVacunaRepository;
        this.emergenciaRepository = emergenciaRepository;
        this.historialRepository = historialRepository;
        this.solicitudes = solicitudes;
        this.veterinarioRepository = veterinarioRepository;
        this.transferencias = transferencias;
        this.disponibilidad = disponibilidad;
        this.reloj = reloj;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        var usuario = principal.getUsuario();
        model.addAttribute("usuario", usuario);
        model.addAttribute("seccionActiva", "home");
        model.addAttribute("navItems", navPara(usuario));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = ahora.toLocalDate();
        model.addAttribute("saludo", saludo(ahora.toLocalTime()));
        model.addAttribute("hoy", hoy);

        return switch (usuario.getRol()) {
            case TUTOR -> tutor(usuario, model, ahora);
            case VETERINARIO -> veterinario(usuario, model, ahora);
            case RECEPCIONISTA -> recepcionista(usuario, model, ahora);
        };
    }

    private String tutor(Usuario usuario, Model model, LocalDateTime ahora) {
        Integer idTutor = usuario.getIdUsuario();
        LocalDate hoy = ahora.toLocalDate();
        List<Mascota> mascotas = mascotaRepository.findByTutor_IdUsuarioAndEstado(idTutor, (byte) 1);
        List<Cita> proximas = citaRepository.proximasAgendadasDeTutor(idTutor, hoy, ahora.toLocalTime());
        List<Cita> todasCitas = new ArrayList<>(proximas);
        citaRepository.findByMascota_Tutor_IdUsuarioOrderByFechaDescHoraDesc(idTutor).stream()
                .filter(c -> !EstadoCita.RESERVAS_VIGENTES.contains(c.getEstado()))
                .forEach(todasCitas::add);
        var vacunasPendientes = carnetVacunaRepository.pendientesDeTutorHasta(idTutor, hoy.plusDays(30));

        model.addAttribute("mascotas", mascotas);
        model.addAttribute("statMascotas", mascotas.size());
        model.addAttribute("statVacunas", vacunasPendientes.size());
        model.addAttribute("proximaCita", proximas.isEmpty() ? null : proximas.get(0));
        model.addAttribute("todasCitas", todasCitas.stream().map(CitaCalendarDTO::de).toList());
        model.addAttribute("notificaciones", notificacionesTutor(proximas, vacunasPendientes, hoy));
        model.addAttribute("transferenciasPendientes", transferencias
                .findByTutorDestino_IdUsuarioAndEstadoOrderByCreadaEnDesc(idTutor, TransferenciaMascota.Estado.PENDIENTE).size());
        return "dashboard/tutor";
    }

    private String veterinario(Usuario usuario, Model model, LocalDateTime ahora) {
        Integer idVet = usuario.getIdUsuario();
        LocalDate hoy = ahora.toLocalDate();
        List<Cita> hoyCitas = citaRepository.findByVeterinario_IdUsuarioAndFecha(idVet, hoy).stream()
                .filter(c -> c.getMascota() != null)
                .sorted(Comparator.comparing(Cita::getHora)).toList();
        Optional<Cita> enCurso = hoyCitas.stream().filter(c -> c.getEstado() == EstadoCita.EN_ATENCION).findFirst();
        Optional<Cita> proximo = hoyCitas.stream()
                .filter(c -> c.getEstado() == EstadoCita.CONFIRMADA && !c.getFin().isBefore(ahora))
                .findFirst();

        LocalDate lunes = hoy.with(DayOfWeek.MONDAY);
        Map<LocalDate, List<Cita>> semana = citaRepository.deVeterinarioEntre(idVet, lunes, lunes.plusDays(6)).stream()
                .filter(c -> c.getEstado().ocupaHorario() || c.getEstado() == EstadoCita.NO_ASISTIO)
                .collect(Collectors.groupingBy(Cita::getFecha));
        FormatoVista f = new FormatoVista();
        List<DiaSemanaAgenda> dias = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = lunes.plusDays(i);
            dias.add(new DiaSemanaAgenda(d, f.fechaCorta(d), d.equals(hoy),
                    semana.getOrDefault(d, List.of()).stream().sorted(Comparator.comparing(Cita::getHora)).toList()));
        }
        List<Cita> registrosPendientes = citaRepository.findByVeterinario_IdUsuarioAndEstado(idVet, EstadoCita.EN_ATENCION);

        model.addAttribute("citasHoy", hoyCitas);
        model.addAttribute("enCurso", enCurso.orElse(null));
        model.addAttribute("proximoPaciente", proximo.orElse(null));
        model.addAttribute("statPendientes", hoyCitas.stream().filter(c -> c.getEstado() == EstadoCita.CONFIRMADA).count());
        model.addAttribute("statEnAtencion", hoyCitas.stream().filter(c -> c.getEstado() == EstadoCita.EN_ATENCION).count());
        model.addAttribute("statFinalizadas", hoyCitas.stream().filter(c -> c.getEstado() == EstadoCita.ATENDIDA).count());
        model.addAttribute("statPorConfirmar", hoyCitas.stream().filter(c -> c.getEstado() == EstadoCita.PENDIENTE).count());
        model.addAttribute("emergencias", emergenciaRepository.findByVeterinario_IdUsuarioAndEstado(idVet, EstadoEmergencia.ASIGNADA));
        model.addAttribute("registrosPendientes", registrosPendientes);
        model.addAttribute("semana", dias);
        model.addAttribute("disponible", !disponibilidad.ocupadoAhora(idVet)
                && disponibilidad.disponibleEn(usuario, ahora, ahora.plusMinutes(1), true));
        model.addAttribute("ocupado", disponibilidad.ocupadoAhora(idVet));
        model.addAttribute("solicitudesPendientes", solicitudes.findByVeterinario_IdUsuarioOrderByCreadaEnDesc(idVet).stream()
                .filter(s -> s.getEstado() == EstadoAprobacion.PENDIENTE).count());
        model.addAttribute("veterinaria", usuario.getVeterinario() != null ? usuario.getVeterinario().getVeterinaria() : null);
        return "dashboard/veterinario";
    }

    private String recepcionista(Usuario usuario, Model model, LocalDateTime ahora) {
        var veterinaria = usuario.getRecepcionista().getVeterinaria();
        Integer idClinica = veterinaria.getIdVeterinaria();
        LocalDate hoy = ahora.toLocalDate();
        List<Cita> deHoy = citaRepository.deClinicaEntre(idClinica, hoy, hoy).stream()
                .filter(c -> c.getMascota() != null).sorted(Comparator.comparing(Cita::getHora)).toList();
        List<Cita> proximos7 = citaRepository.deClinicaEntre(idClinica, hoy, hoy.plusDays(7));
        List<Cita> pendientes = proximos7.stream()
                .filter(c -> c.getEstado() == EstadoCita.PENDIENTE).sorted(Comparator.comparing(Cita::getInicio)).toList();
        long cupos = proximos7.stream().filter(c -> c.getEstado() == EstadoCita.DISPONIBLE).count();
        long ocupados = proximos7.stream().filter(c -> c.getEstado().ocupaHorario()).count();

        List<EstadoVeterinario> equipo = veterinarioRepository.findByVeterinaria_IdVeterinaria(idClinica).stream()
                .map(Veterinario::getUsuario).filter(Usuario::isActivo)
                .map(u -> {
                    List<Cita> suyas = deHoy.stream().filter(c -> c.getVeterinario().getIdUsuario().equals(u.getIdUsuario())).toList();
                    String estado;
                    String clase;
                    if (disponibilidad.ocupadoAhora(u.getIdUsuario())) {
                        estado = "En atención";
                        clase = "vl-st-atencion";
                    } else if (disponibilidad.disponibleEn(u, ahora, ahora.plusMinutes(1), true)) {
                        estado = "Disponible";
                        clase = "vl-st-confirmada";
                    } else {
                        estado = "Fuera de turno";
                        clase = "vl-st-noasistio";
                    }
                    return new EstadoVeterinario(u, estado, clase, suyas.size(),
                            (int) suyas.stream().filter(c -> c.getEstado() == EstadoCita.CONFIRMADA).count());
                }).toList();

        model.addAttribute("veterinaria", veterinaria);
        model.addAttribute("citasHoy", deHoy);
        model.addAttribute("statCitasHoy", deHoy.size());
        model.addAttribute("statPendientes", pendientes.size());
        model.addAttribute("statConfirmadas", deHoy.stream().filter(c -> c.getEstado() == EstadoCita.CONFIRMADA).count());
        model.addAttribute("statCanceladas", deHoy.stream()
                .filter(c -> c.getEstado() == EstadoCita.CANCELADA || c.getEstado() == EstadoCita.RECHAZADA).count());
        model.addAttribute("statAgendadas", citaRepository.countByVeterinaria_IdVeterinariaAndEstado(idClinica, EstadoCita.CONFIRMADA));
        model.addAttribute("pendientes", pendientes.stream().limit(8).toList());
        model.addAttribute("proximas", proximos7.stream()
                .filter(c -> c.getEstado() == EstadoCita.CONFIRMADA && c.getInicio().isAfter(ahora))
                .sorted(Comparator.comparing(Cita::getInicio)).limit(6).toList());
        model.addAttribute("equipo", equipo);
        model.addAttribute("solicitudesPendientes",
                solicitudes.countByVeterinaria_IdVeterinariaAndEstado(idClinica, EstadoAprobacion.PENDIENTE));
        model.addAttribute("emergenciasPendientes", emergenciaRepository.countByVeterinaria_IdVeterinariaAndEstadoIn(idClinica,
                EnumSet.of(EstadoEmergencia.PENDIENTE)));
        model.addAttribute("ocupacion", cupos + ocupados == 0 ? 0 : Math.round(ocupados * 100.0 / (cupos + ocupados)));
        model.addAttribute("cuposLibres", cupos);
        return "dashboard/recepcionista";
    }

    private static String saludo(LocalTime t) {
        if (t.getHour() < 12) return "Buenos días";
        if (t.getHour() < 19) return "Buenas tardes";
        return "Buenas noches";
    }

    /** Citas vigentes dentro de 3 días + vacunas por vencer dentro de 30 días. */
    private List<Notificacion> notificacionesTutor(List<Cita> proximas, List<CarnetVacuna> vacunasPendientes, LocalDate hoy) {
        List<Notificacion> notifs = new ArrayList<>();
        LocalDate limiteCita = hoy.plusDays(3);
        FormatoVista f = new FormatoVista();
        for (Cita c : proximas) {
            if (!c.getFecha().isAfter(limiteCita)) {
                notifs.add(new Notificacion("fa-calendar-check", "vl-badge-info",
                        c.getMascota().getNombre() + ": cita el " + f.fechaCorta(c.getFecha()) + " a las " + f.hora(c.getHora())
                                + (c.getEstado() == EstadoCita.PENDIENTE ? " (pendiente de confirmación)" : "")));
            }
        }
        for (CarnetVacuna v : vacunasPendientes) {
            boolean vencida = v.getProximaDosis().isBefore(hoy);
            notifs.add(new Notificacion("fa-syringe", vencida ? "vl-badge-danger" : "vl-badge-warn",
                    v.getMascota().getNombre() + ": " + v.getVacuna().getNombreVacuna()
                            + (vencida ? " vencida" : " próxima a vencer") + " (" + f.fechaCorta(v.getProximaDosis()) + ")"));
        }
        return notifs;
    }

    public static List<NavItem> navPara(Usuario u) {
        return switch (u.getRol()) {
            case TUTOR -> navTutor();
            case VETERINARIO -> navVeterinario();
            case RECEPCIONISTA -> navRecepcionista();
        };
    }

    public static List<NavItem> navTutor() {
        return List.of(
                new NavItem("home", "/dashboard", "fas fa-house", "Inicio"),
                new NavItem("mascotas", "/mascotas", "fas fa-dog", "Mis Mascotas"),
                new NavItem("citas", "/citas/mias", "fas fa-calendar-alt", "Mis Citas"),
                new NavItem("historial", "/historial/mias", "fas fa-folder-open", "Historial Clínico"),
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
                new NavItem("disponibilidad", "/disponibilidad", "fas fa-user-clock", "Mi disponibilidad"),
                new NavItem("chat", "/chat", "fas fa-comments", "Chat"),
                new NavItem("emergencias", "/emergencias", "fas fa-truck-medical", "Emergencias"),
                new NavItem("perfil", "/perfil", "fas fa-user-circle", "Mi Perfil")
        );
    }

    public static List<NavItem> navRecepcionista() {
        return List.of(
                new NavItem("home", "/dashboard", "fas fa-house", "Inicio"),
                new NavItem("citas", "/agenda", "fas fa-calendar-alt", "Citas de la Clínica"),
                new NavItem("disponibilidad", "/recepcion/disponibilidad", "fas fa-user-clock", "Disponibilidad del equipo"),
                new NavItem("mascotas", "/recepcion/mascotas", "fas fa-dog", "Mascotas"),
                new NavItem("historial", "/historial/pacientes", "fas fa-folder-open", "Historial Clínico"),
                new NavItem("veterinaria", "/veterinarias/admin", "fas fa-hospital", "Mi Veterinaria"),
                new NavItem("directorio", "/directorio", "fas fa-hospital-user", "Directorio"),
                new NavItem("chat", "/chat", "fas fa-comments", "Chat"),
                new NavItem("emergencias", "/emergencias", "fas fa-truck-medical", "Emergencias"),
                new NavItem("perfil", "/perfil", "fas fa-user-circle", "Mi Perfil")
        );
    }
}
