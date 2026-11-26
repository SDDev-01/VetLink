package com.vetlink.cita.controller;

import com.vetlink.cita.dto.AgendarCitaForm;
import com.vetlink.cita.dto.CitaCalendarDTO;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.model.TransicionCita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.cita.service.CitaEstadoService;
import com.vetlink.cita.service.CitaService;
import com.vetlink.comun.web.FormatoVista;
import com.vetlink.disponibilidad.service.DisponibilidadService;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.service.MascotaService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.usuario.model.Rol;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.VeterinarioRepository;
import com.vetlink.veterinaria.model.HorarioVeterinaria;
import com.vetlink.veterinaria.repository.HorarioVeterinariaRepository;
import com.vetlink.veterinaria.repository.ServicioVeterinariaRepository;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import com.vetlink.veterinaria.service.VeterinariaInfoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/** Agendamiento y consulta de citas para el TUTOR, más la API de disponibilidad del asistente. */
@Controller
@RequestMapping("/citas")
public class CitaController {

    private final CitaService citaService;
    private final MascotaService mascotaService;
    private final VeterinariaRepository veterinariaRepository;
    private final HorarioVeterinariaRepository horarioVeterinariaRepository;
    private final CitaRepository citaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ServicioVeterinariaRepository servicioVeterinariaRepository;
    private final DisponibilidadService disponibilidad;
    private final CitaEstadoService estados;
    private final VeterinariaInfoService infoVeterinaria;
    private final Clock reloj;

    public CitaController(CitaService citaService, MascotaService mascotaService, VeterinariaRepository veterinariaRepository,
                          HorarioVeterinariaRepository horarioVeterinariaRepository, CitaRepository citaRepository,
                          VeterinarioRepository veterinarioRepository,
                          ServicioVeterinariaRepository servicioVeterinariaRepository, DisponibilidadService disponibilidad,
                          CitaEstadoService estados, VeterinariaInfoService infoVeterinaria, Clock reloj) {
        this.citaService = citaService;
        this.mascotaService = mascotaService;
        this.veterinariaRepository = veterinariaRepository;
        this.horarioVeterinariaRepository = horarioVeterinariaRepository;
        this.citaRepository = citaRepository;
        this.veterinarioRepository = veterinarioRepository;
        this.servicioVeterinariaRepository = servicioVeterinariaRepository;
        this.disponibilidad = disponibilidad;
        this.estados = estados;
        this.infoVeterinaria = infoVeterinaria;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------
    // Listados
    // ------------------------------------------------------------------

    /** Opción de un filtro de «Mis citas» (solo valores que existen en las citas del dueño). */
    public record OpcionFiltro(String valor, String etiqueta) {}

    /**
     * «Mis citas» con dos pestañas: las activas (próximas, pendientes y las que piden una
     * respuesta del dueño) y el historial. Los filtros se aplican en el servidor sobre las
     * citas del propio dueño; las opciones salen de sus citas reales.
     */
    @GetMapping("/mias")
    public String misCitas(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                           @RequestParam(required = false, defaultValue = "activas") String tab,
                           @RequestParam(required = false) Integer idMascota,
                           @RequestParam(required = false) Integer idVeterinaria,
                           @RequestParam(required = false) Integer idServicio,
                           @RequestParam(required = false) String estado,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                           Model model) {
        cargarShell(model, principal);
        List<Cita> citas = citaService.listarPorTutor(principal.getUsuario().getIdUsuario()).stream()
                .filter(c -> c.getMascota() != null).toList();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        java.util.function.Predicate<Cita> activa = c ->
                (EstadoCita.RESERVAS_VIGENTES.contains(c.getEstado()) || c.getEstado() == EstadoCita.EN_ATENCION)
                        && !c.getFin().isBefore(ahora);
        boolean historial = "historial".equals(tab);
        EstadoCita estadoFiltro = null;
        if (estado != null && !estado.isBlank()) {
            try {
                estadoFiltro = EstadoCita.valueOf(estado.trim());
            } catch (IllegalArgumentException e) {
                model.addAttribute("errorGeneral", "El estado seleccionado no es válido");
            }
        }
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            model.addAttribute("errorGeneral", "La fecha final no puede ser anterior a la inicial");
            hasta = null;
        }
        final EstadoCita ef = estadoFiltro;
        final LocalDate d1 = desde, d2 = hasta;
        java.util.function.Predicate<Cita> pasaFiltros = c ->
                (idMascota == null || c.getMascota().getIdMascota().equals(idMascota))
                        && (idVeterinaria == null || c.getVeterinaria().getIdVeterinaria().equals(idVeterinaria))
                        && (idServicio == null || c.getServicio().getIdServicio().equals(idServicio))
                        && (ef == null || c.getEstado() == ef)
                        && (d1 == null || !c.getFecha().isBefore(d1))
                        && (d2 == null || !c.getFecha().isAfter(d2));

        List<Cita> activas = citas.stream().filter(activa).filter(pasaFiltros)
                .sorted(Comparator.comparing((Cita c) -> !c.isEsperandoRespuestaDuenio())
                        .thenComparing(c -> c.getEstado() != EstadoCita.PENDIENTE)
                        .thenComparing(Cita::getInicio))
                .toList();
        List<Cita> pasadas = citas.stream().filter(activa.negate()).filter(pasaFiltros)
                .sorted(Comparator.comparing(Cita::getInicio).reversed())
                .toList();

        model.addAttribute("tab", historial ? "historial" : "activas");
        model.addAttribute("proximas", activas);
        model.addAttribute("pasadas", pasadas);
        model.addAttribute("requierenAccion", activas.stream().filter(Cita::isEsperandoRespuestaDuenio).count());
        // Opciones de filtro tomadas de las citas reales del dueño.
        List<Cita> base = citas.stream().filter(historial ? activa.negate() : activa).toList();
        model.addAttribute("opMascotas", opciones(citas, c -> c.getMascota().getIdMascota(), c -> c.getMascota().getNombre()));
        model.addAttribute("opVeterinarias", opciones(citas, c -> c.getVeterinaria().getIdVeterinaria(), c -> c.getVeterinaria().getNombre()));
        model.addAttribute("opServicios", opciones(citas, c -> c.getServicio().getIdServicio(), c -> c.getServicio().getNombreServicio()));
        model.addAttribute("opEstados", base.stream().map(Cita::getEstado).distinct().sorted()
                .map(e -> new OpcionFiltro(e.name(), e.getEtiqueta())).toList());
        model.addAttribute("fMascota", idMascota);
        model.addAttribute("fVeterinaria", idVeterinaria);
        model.addAttribute("fServicio", idServicio);
        model.addAttribute("fEstado", ef == null ? null : ef.name());
        model.addAttribute("fDesde", desde);
        model.addAttribute("fHasta", hasta);
        model.addAttribute("hayFiltros", idMascota != null || idVeterinaria != null || idServicio != null || ef != null
                || desde != null || hasta != null);
        model.addAttribute("citasJson", citas.stream().map(CitaCalendarDTO::de).toList());
        return "citas/mias";
    }

    private static List<OpcionFiltro> opciones(List<Cita> citas, java.util.function.Function<Cita, Integer> id,
                                               java.util.function.Function<Cita, String> nombre) {
        Map<Integer, String> m = new java.util.LinkedHashMap<>();
        citas.forEach(c -> m.putIfAbsent(id.apply(c), nombre.apply(c)));
        return m.entrySet().stream()
                .map(e -> new OpcionFiltro(String.valueOf(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(OpcionFiltro::etiqueta, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @GetMapping("/mascota/{idMascota}")
    public String listarPorMascota(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                                   Model model) {
        cargarShell(model, principal);
        var mascota = mascotaService.buscarPorId(idMascota);
        verificarAccesoTutor(mascota, principal);
        model.addAttribute("mascota", mascota);
        model.addAttribute("citas", citaService.listarPorMascota(idMascota));
        return "citas/lista";
    }

    @GetMapping("/{id:\\d+}")
    public String detalle(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        Cita cita = citaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        // REC-06: el personal (recepción/veterinario) gestiona la cita desde la vista profesional,
        // que aplica sus propios permisos (misma clínica / cita asignada).
        if (principal.getUsuario().getRol() != Rol.TUTOR) {
            return "redirect:/agenda/" + id;
        }
        if (cita.getMascota() == null || !cita.getMascota().getTutor().getIdUsuario()
                .equals(principal.getUsuario().getIdUsuario())) {
            throw new AccessDeniedException("No tienes permiso sobre esta cita");
        }
        cargarShell(model, principal);
        boolean futura = cita.getInicio().isAfter(LocalDateTime.now(reloj));
        model.addAttribute("cita", cita);
        model.addAttribute("direccion", infoVeterinaria.direccion(cita.getVeterinaria().getIdVeterinaria()));
        model.addAttribute("telefonos", infoVeterinaria.telefonos(cita.getVeterinaria().getIdVeterinaria()));
        model.addAttribute("historialEstados", estados.historialDe(id));
        model.addAttribute("puedeCancelar", futura
                && TransicionCita.permitida(cita.getEstado(), EstadoCita.CANCELADA, TransicionCita.Actor.TUTOR));
        boolean esperaRespuesta = futura && cita.isEsperandoRespuestaDuenio();
        model.addAttribute("esperaRespuesta", esperaRespuesta);
        model.addAttribute("citaAnterior", cita.getCitaOrigen());
        model.addAttribute("puedeReprogramar", futura && !esperaRespuesta && EstadoCita.RESERVAS_VIGENTES.contains(cita.getEstado()));
        model.addAttribute("reprogramadaA", citaRepository.findByCitaOrigen_IdCita(id));
        return "citas/detalle";
    }

    // ------------------------------------------------------------------
    // Asistente de agendamiento
    // ------------------------------------------------------------------

    @GetMapping("/agendar/{idMascota}")
    public String agendarForm(@PathVariable Integer idMascota, @RequestParam(required = false) Integer idVeterinaria,
                              @RequestParam(required = false) Integer idServicio,
                              @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model,
                              RedirectAttributes redirectAttributes) {
        var mascota = mascotaService.buscarPorId(idMascota);
        verificarAccesoTutor(mascota, principal);
        if (!mascota.isAgendable()) {
            redirectAttributes.addFlashAttribute("errorMascota", "La mascota no está activa; no se le pueden agendar citas.");
            return "redirect:/mascotas";
        }
        AgendarCitaForm form = new AgendarCitaForm();
        form.setIdMascota(idMascota);
        prepararAsistente(model, principal, form, mascota, idVeterinaria, idServicio);
        return "citas/formulario";
    }

    /** Punto de entrada desde el dashboard o el directorio (la mascota se elige en el paso 1). */
    @GetMapping("/agendar")
    public String agendarDirecto(@RequestParam(required = false) Integer idVeterinaria,
                                 @RequestParam(required = false) Integer idServicio,
                                 @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        prepararAsistente(model, principal, new AgendarCitaForm(), null, idVeterinaria, idServicio);
        return "citas/formulario";
    }

    @PostMapping("/agendar")
    public String agendar(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                          @Valid @ModelAttribute("form") AgendarCitaForm form, BindingResult result, Model model,
                          RedirectAttributes redirect) {
        Mascota mascota = form.getIdMascota() == null ? null : mascotaService.buscarPorId(form.getIdMascota());
        if (mascota != null) verificarAccesoTutor(mascota, principal);
        if (result.hasErrors()) {
            prepararAsistente(model, principal, form, mascota, form.getIdVeterinaria(), form.getIdServicio());
            return "citas/formulario";
        }
        try {
            Cita cita = citaService.agendar(form, principal.getUsuario().getIdUsuario());
            redirect.addFlashAttribute("citaReservada", cita.getIdCita());
        } catch (RuntimeException e) {
            model.addAttribute("errorGeneral", e.getMessage());
            prepararAsistente(model, principal, form, mascota, form.getIdVeterinaria(), form.getIdServicio());
            return "citas/formulario";
        }
        return "redirect:/citas/mascota/" + form.getIdMascota() + "?agendada";
    }

    private void prepararAsistente(Model model, VetlinkUserPrincipal principal, AgendarCitaForm form, Mascota mascota,
                                   Integer idVeterinaria, Integer idServicio) {
        cargarShell(model, principal);
        var mascotas = mascotaService.listarActivasPorTutor(principal.getUsuario().getIdUsuario()).stream()
                .filter(Mascota::isAgendable).toList();
        model.addAttribute("mascotas", mascotas);
        model.addAttribute("mascota", mascota);
        model.addAttribute("titulo", mascota == null ? "Agendar cita" : "Agendar cita para " + mascota.getNombre());
        model.addAttribute("veterinarias", veterinariaRepository.findByEstado((byte) 1).stream()
                .map(v -> Map.of("id", v.getIdVeterinaria(), "nombre", v.getNombre(),
                        "direccion", infoVeterinaria.direccion(v.getIdVeterinaria()),
                        "logo", VeterinariaInfoService.logoUrl(v)))
                .toList());
        if (form.getIdVeterinaria() == null && idVeterinaria != null && veterinariaRepository.existsById(idVeterinaria)) {
            form.setIdVeterinaria(idVeterinaria);
        }
        if (form.getIdServicio() == null && idServicio != null) form.setIdServicio(idServicio);
        model.addAttribute("form", form);
    }

    // ------------------------------------------------------------------
    // API JSON del asistente (solo devuelve disponibilidad real)
    // ------------------------------------------------------------------

    public record ServicioOption(Integer id, String nombre, Integer duracion, Double precio, String descripcion) {}

    public record VeterinarioOption(Integer id, String nombre, String especialidad) {
        public VeterinarioOption(Integer id, String nombre) {
            this(id, nombre, null);
        }
    }

    public record DiaDisponible(String fecha, int cupos) {}

    public record HoraDisponible(Integer idCupo, String hora, String etiqueta, Integer duracion, Integer idVeterinario,
                                 String veterinario) {}

    @GetMapping("/api/servicios")
    @ResponseBody
    public List<ServicioOption> serviciosDe(@RequestParam Integer idVeterinaria) {
        return servicioVeterinariaRepository.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .filter(sv -> !sv.getServicio().isEmergencia())
                .map(sv -> new ServicioOption(sv.getServicio().getIdServicio(), sv.getServicio().getNombreServicio(),
                        sv.getDuracionAprox(), sv.getPrecio(), sv.getServicio().getDescripcion()))
                .sorted(Comparator.comparing(ServicioOption::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @GetMapping("/api/veterinarios")
    @ResponseBody
    public List<VeterinarioOption> veterinariosCompatibles(@RequestParam Integer idVeterinaria,
                                                           @RequestParam Integer idServicio) {
        return disponibilidad.veterinariosCompatibles(idVeterinaria, idServicio).stream()
                .map(u -> new VeterinarioOption(u.getIdUsuario(), u.getNombre() + " " + u.getApellido(),
                        u.getVeterinario().getEspecialidad() == null ? null
                                : u.getVeterinario().getEspecialidad().getNombreEspecialidad()))
                .toList();
    }

    /** Días del mes con al menos un cupo reservable (para pintar el mini calendario). */
    @GetMapping("/api/disponibilidad/dias")
    @ResponseBody
    public List<DiaDisponible> diasDisponibles(@RequestParam Integer idVeterinaria, @RequestParam Integer idServicio,
                                               @RequestParam(required = false) Integer idVeterinario,
                                               @RequestParam(required = false) Integer idMascota,
                                               @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
                                               @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        Integer mascotaFiltro = mascotaParaFiltro(idMascota, principal);
        Map<LocalDate, Long> porDia = disponibilidad.cuposReservables(idVeterinaria, idServicio, idVeterinario,
                        mes.atDay(1), mes.atEndOfMonth(), mascotaFiltro).stream()
                .collect(Collectors.groupingBy(Cita::getFecha, TreeMap::new, Collectors.counting()));
        return porDia.entrySet().stream().map(e -> new DiaDisponible(e.getKey().toString(), e.getValue().intValue())).toList();
    }

    /** Horas que realmente se pueden reservar en un día. */
    @GetMapping("/api/disponibilidad/horas")
    @ResponseBody
    public List<HoraDisponible> horasDisponibles(@RequestParam Integer idVeterinaria, @RequestParam Integer idServicio,
                                                 @RequestParam(required = false) Integer idVeterinario,
                                                 @RequestParam(required = false) Integer idMascota,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                                 @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        Integer mascotaFiltro = mascotaParaFiltro(idMascota, principal);
        return disponibilidad.cuposReservables(idVeterinaria, idServicio, idVeterinario, fecha, fecha, mascotaFiltro).stream()
                .map(c -> new HoraDisponible(c.getIdCita(),
                        String.format("%02d:%02d", c.getHora().getHour(), c.getHora().getMinute()),
                        FormatoVista.horaTexto(c.getHora()), c.getDuracionMin(), c.getVeterinario().getIdUsuario(),
                        c.getVeterinario().getNombre() + " " + c.getVeterinario().getApellido()))
                .toList();
    }

    /** Celda de la grilla de horarios del día (REC-04): libre = seleccionable; el resto, deshabilitada con su motivo. */
    public record CeldaHorario(Integer idCupo, String hora, String etiqueta, Integer duracion, Integer idVeterinario,
                               String veterinario, boolean libre, String motivo) {}

    /**
     * REC-02 / REC-04: todos los horarios del día para el servicio (y veterinario, si se eligió).
     * Solo los que {@code DisponibilidadService.cuposReservables} acepta (horario aprobado, sin
     * ausencias/bloqueos, sin cruces con citas o reservas pendientes, con anticipación mínima)
     * salen como libres. Los demás se devuelven deshabilitados con el motivo. El servidor vuelve
     * a comprobar todo al confirmar (bloqueo de fila del cupo + validación de solapamientos).
     */
    @GetMapping("/api/disponibilidad/grilla")
    @ResponseBody
    public List<CeldaHorario> grillaDelDia(@RequestParam Integer idVeterinaria, @RequestParam Integer idServicio,
                                           @RequestParam(required = false) Integer idVeterinario,
                                           @RequestParam(required = false) Integer idMascota,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                           @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        Integer mascotaFiltro = mascotaParaFiltro(idMascota, principal);
        var libres = disponibilidad.cuposReservables(idVeterinaria, idServicio, idVeterinario, fecha, fecha, mascotaFiltro)
                .stream().map(Cita::getIdCita).collect(Collectors.toSet());
        List<CeldaHorario> celdas = new java.util.ArrayList<>();
        java.util.Set<String> vistos = new java.util.HashSet<>();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        for (Cita c : citaRepository.cuposEntre(idVeterinaria, idServicio, idVeterinario, fecha, fecha)) {
            boolean libre = libres.contains(c.getIdCita());
            String motivo = libre ? null : (c.getInicio().isBefore(ahora) ? "Hora pasada" : "No disponible");
            celdas.add(celda(c, libre, motivo));
            vistos.add(c.getVeterinario().getIdUsuario() + "|" + c.getHora());
        }
        // Horarios ya tomados (reservas pendientes, confirmadas o en atención) de los veterinarios consultados.
        var vets = idVeterinario != null ? List.of(idVeterinario)
                : disponibilidad.veterinariosCompatibles(idVeterinaria, idServicio).stream().map(Usuario::getIdUsuario).toList();
        for (Integer idVet : vets) {
            for (Cita c : citaRepository.deVeterinarioEnFechaConEstados(idVet, fecha, EstadoCita.OCUPAN_HORARIO)) {
                if (!c.getVeterinaria().getIdVeterinaria().equals(idVeterinaria)) continue;
                if (!vistos.add(idVet + "|" + c.getHora())) continue;
                celdas.add(celda(c, false, c.getEstado() == EstadoCita.PENDIENTE ? "Reserva pendiente" : "Reservado"));
            }
        }
        celdas.sort(Comparator.comparing(CeldaHorario::hora).thenComparing(CeldaHorario::veterinario));
        return celdas;
    }

    private static CeldaHorario celda(Cita c, boolean libre, String motivo) {
        return new CeldaHorario(libre ? c.getIdCita() : null,
                String.format("%02d:%02d", c.getHora().getHour(), c.getHora().getMinute()),
                FormatoVista.horaTexto(c.getHora()), c.getDuracionMin(), c.getVeterinario().getIdUsuario(),
                c.getVeterinario().getNombre() + " " + c.getVeterinario().getApellido(), libre, motivo);
    }

    private Integer mascotaParaFiltro(Integer idMascota, VetlinkUserPrincipal principal) {
        if (idMascota == null) return null;
        Mascota m = mascotaService.buscarPorId(idMascota);
        Usuario u = principal.getUsuario();
        if (u.getRol() == Rol.TUTOR && !m.getTutor().getIdUsuario().equals(u.getIdUsuario())) {
            throw new AccessDeniedException("No tienes permiso sobre esta mascota");
        }
        return idMascota;
    }

    // ------------------------------------------------------------------
    // Cancelar y solicitar reprogramación
    // ------------------------------------------------------------------

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                           @RequestParam(required = false) Integer idMascota,
                           @RequestParam(required = false, defaultValue = "mascota") String origen,
                           RedirectAttributes redirect) {
        try {
            citaService.cancelar(id, principal.getUsuario().getIdUsuario());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("errorGeneral", e.getMessage());
            return destino(origen, id, idMascota, "");
        }
        return destino(origen, id, idMascota, "?cancelada");
    }

    @PostMapping("/{id}/reprogramacion")
    public String solicitarReprogramacion(@PathVariable Integer id, @RequestParam(required = false) String motivo,
                                          @AuthenticationPrincipal VetlinkUserPrincipal principal,
                                          RedirectAttributes redirect) {
        try {
            citaService.solicitarReprogramacion(id, principal.getUsuario().getIdUsuario(), motivo);
            redirect.addFlashAttribute("mensajeOk", "Enviamos tu solicitud de reprogramación a la clínica.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("errorGeneral", e.getMessage());
        }
        return "redirect:/citas/" + id;
    }

    // ------------------------------------------------------------------
    // Respuesta del dueño a una reprogramación de la clínica
    // ------------------------------------------------------------------

    @PostMapping("/{id}/reprogramacion/confirmar")
    public String confirmarReprogramacion(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                                          RedirectAttributes redirect) {
        exigirTutor(principal);
        try {
            citaService.confirmarReprogramacion(id, principal.getUsuario().getIdUsuario());
            redirect.addFlashAttribute("mensajeOk", "¡Listo! Confirmaste el nuevo horario. La clínica ya recibió tu respuesta.");
        } catch (SecurityException e) {
            throw new AccessDeniedException(e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("errorGeneral", e.getMessage());
        }
        return "redirect:/citas/" + id;
    }

    @GetMapping("/{id}/reprogramacion/modificar")
    public String modificarReprogramacionForm(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                                              Model model, RedirectAttributes redirect) {
        exigirTutor(principal);
        Cita cita = citaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        List<Cita> cupos;
        try {
            cupos = citaService.cuposParaModificarReprogramacion(id, principal.getUsuario().getIdUsuario());
        } catch (SecurityException e) {
            throw new AccessDeniedException(e.getMessage());
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("errorGeneral", e.getMessage());
            return "redirect:/citas/" + id;
        }
        cargarShell(model, principal);
        model.addAttribute("cita", cita);
        // Agrupados por día para elegir primero la fecha y luego la hora.
        model.addAttribute("cuposPorDia", cupos.stream().collect(Collectors.groupingBy(Cita::getFecha, TreeMap::new,
                Collectors.toList())));
        return "citas/modificar-reprogramacion";
    }

    @PostMapping("/{id}/reprogramacion/modificar")
    public String modificarReprogramacion(@PathVariable Integer id, @RequestParam(required = false) Integer idCupoNuevo,
                                          @AuthenticationPrincipal VetlinkUserPrincipal principal, RedirectAttributes redirect) {
        exigirTutor(principal);
        try {
            Cita nueva = citaService.modificarReprogramacion(id, idCupoNuevo, principal.getUsuario().getIdUsuario());
            redirect.addFlashAttribute("mensajeOk", "Enviamos el nuevo horario a la clínica. Te avisaremos cuando lo confirme.");
            return "redirect:/citas/" + nueva.getIdCita();
        } catch (SecurityException e) {
            throw new AccessDeniedException(e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("errorGeneral", e.getMessage());
            return "redirect:/citas/" + id + "/reprogramacion/modificar";
        }
    }

    private static void exigirTutor(VetlinkUserPrincipal principal) {
        if (principal.getUsuario().getRol() != Rol.TUTOR) {
            throw new AccessDeniedException("Solo el dueño de la mascota puede responder a una reprogramación");
        }
    }

    private static String destino(String origen, Integer idCita, Integer idMascota, String sufijo) {
        if ("mias".equals(origen)) return "redirect:/citas/mias" + sufijo;
        if ("detalle".equals(origen) || idMascota == null) return "redirect:/citas/" + idCita + sufijo;
        return "redirect:/citas/mascota/" + idMascota + sufijo;
    }

    // ------------------------------------------------------------------
    // Compatibilidad: horario general de la clínica (usado por recepción)
    // ------------------------------------------------------------------

    @GetMapping("/horarios-disponibles")
    @ResponseBody
    public HorariosDisponiblesResponse horariosDisponibles(@RequestParam Integer idVeterinaria,
                                                           @RequestParam(required = false) Integer idVeterinario,
                                                           @RequestParam(required = false) Integer idServicio,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        if (idVeterinario != null && idServicio != null) {
            List<String> cupos = disponibilidad.cuposReservables(idVeterinaria, idServicio, idVeterinario, fecha, fecha, null)
                    .stream().map(c -> hhmm(c.getHora())).toList();
            return new HorariosDisponiblesResponse(null, null, cupos, cupos.isEmpty(), List.of());
        }
        String nombreDia = nombreDiaEs(fecha.getDayOfWeek());
        List<HorarioVeterinaria> horariosDia = horarioVeterinariaRepository.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .filter(h -> h.getEstado() == 1 && h.getDiaSemana().getDiaSemana().equalsIgnoreCase(nombreDia))
                .filter(h -> h.getHoraApertura().isBefore(h.getHoraCierre()))
                .sorted(Comparator.comparing(HorarioVeterinaria::getHoraApertura))
                .toList();
        if (horariosDia.isEmpty()) {
            return new HorariosDisponiblesResponse(null, null, List.of(), true, List.of());
        }
        List<RangoHorario> rangos = horariosDia.stream()
                .map(h -> new RangoHorario(hhmm(h.getHoraApertura()), hhmm(h.getHoraCierre()))).toList();
        List<String> ocupadas = idVeterinario == null ? List.of()
                : citaRepository.deVeterinarioEnFechaConEstados(idVeterinario, fecha, EstadoCita.OCUPAN_HORARIO).stream()
                .map(c -> hhmm(c.getHora())).toList();
        return new HorariosDisponiblesResponse(rangos.get(0).apertura(),
                rangos.stream().map(RangoHorario::cierre).max(String::compareTo).orElseThrow(), ocupadas, false, rangos);
    }

    private static String hhmm(java.time.LocalTime hora) {
        return String.format("%02d:%02d", hora.getHour(), hora.getMinute());
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

    public record HorariosDisponiblesResponse(String apertura, String cierre, List<String> ocupadas, boolean cerrado,
                                              List<RangoHorario> rangos) {}

    public record RangoHorario(String apertura, String cierre) {}

    /** Veterinarios de una veterinaria (compatibilidad con formularios anteriores). */
    @GetMapping("/veterinarios/{idVeterinaria}")
    @ResponseBody
    public List<VeterinarioOption> veterinariosDe(@PathVariable Integer idVeterinaria) {
        return veterinarioRepository.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .map(v -> v.getUsuario())
                .filter(Usuario::isActivo)
                .map(u -> new VeterinarioOption(u.getIdUsuario(), u.getNombre() + " " + u.getApellido()))
                .toList();
    }

    private void cargarShell(Model model, VetlinkUserPrincipal principal) {
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "citas");
        model.addAttribute("navItems", DashboardController.navPara(principal.getUsuario()));
    }

    private void verificarAccesoTutor(Mascota mascota, VetlinkUserPrincipal principal) {
        if (!mascota.getTutor().getIdUsuario().equals(principal.getUsuario().getIdUsuario())) {
            throw new AccessDeniedException("No tienes permiso para consultar esta mascota");
        }
    }
}
