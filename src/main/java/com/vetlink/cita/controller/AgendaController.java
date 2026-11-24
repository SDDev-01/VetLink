package com.vetlink.cita.controller;

import com.vetlink.cita.dto.CitaCalendarDTO;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.model.TransicionCita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.cita.service.AtencionService;
import com.vetlink.cita.service.CitaEstadoService;
import com.vetlink.cita.service.CitaService;
import com.vetlink.comun.config.ReglasNegocio;
import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.historial.repository.ArchivoHistorialRepository;
import com.vetlink.historial.service.HistorialClinicoService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.usuario.model.Rol;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.vacuna.repository.CarnetVacunaRepository;
import com.vetlink.vacuna.repository.LoteVacunaRepository;
import com.vetlink.vacuna.repository.VacunaRepository;
import com.vetlink.veterinaria.model.Servicio;
import com.vetlink.veterinaria.service.VeterinariaInfoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Agenda clínica (veterinario y recepción) y flujo de atención. El alcance se calcula con
 * el usuario de sesión, nunca con filtros del navegador.
 */
@Controller
@RequestMapping("/agenda")
public class AgendaController {

    private final CitaRepository citas;
    private final CitaService citaService;
    private final HistorialClinicoService historiales;
    private final AtencionService atencionService;
    private final CitaEstadoService estados;
    private final ArchivoHistorialRepository archivos;
    private final CarnetVacunaRepository carnets;
    private final VacunaRepository vacunas;
    private final LoteVacunaRepository lotes;
    private final VeterinariaInfoService infoVeterinaria;
    private final ReglasNegocio reglas;
    private final Clock reloj;

    public AgendaController(CitaRepository citas, CitaService citaService, HistorialClinicoService historiales,
                            AtencionService atencionService, CitaEstadoService estados, ArchivoHistorialRepository archivos,
                            CarnetVacunaRepository carnets, VacunaRepository vacunas, LoteVacunaRepository lotes,
                            VeterinariaInfoService infoVeterinaria, ReglasNegocio reglas, Clock reloj) {
        this.citas = citas;
        this.citaService = citaService;
        this.historiales = historiales;
        this.atencionService = atencionService;
        this.estados = estados;
        this.archivos = archivos;
        this.carnets = carnets;
        this.vacunas = vacunas;
        this.lotes = lotes;
        this.infoVeterinaria = infoVeterinaria;
        this.reglas = reglas;
        this.reloj = reloj;
    }

    @GetMapping
    public String agenda(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
                         @AuthenticationPrincipal VetlinkUserPrincipal p, Model m) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate desde = hoy.minusMonths(2);
        LocalDate hasta = hoy.plusMonths(3);
        Usuario u = p.getUsuario();
        List<Cita> lista = switch (u.getRol()) {
            case VETERINARIO -> citas.deVeterinarioEntre(u.getIdUsuario(), desde, hasta);
            case RECEPCIONISTA -> citas.deClinicaEntre(clinicaDe(u), desde, hasta);
            default -> throw new AccessDeniedException("Agenda no disponible");
        };
        shell(m, p);
        m.addAttribute("diaInicial", dia == null ? hoy : dia);
        m.addAttribute("citasJson", lista.stream()
                .filter(c -> !(c.getMascota() == null && c.getEstado() != EstadoCita.DISPONIBLE))
                .map(CitaCalendarDTO::de).toList());
        m.addAttribute("pendientes", lista.stream().filter(c -> c.getEstado() == EstadoCita.PENDIENTE).count());
        return "citas/agenda";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p, Model m) {
        Cita c = autorizada(id, p);
        mostrarDetalle(c, p, m);
        return "citas/detalle-profesional";
    }

    // ------------------------------------------------------------------
    // Atención (veterinario asignado)
    // ------------------------------------------------------------------

    @PostMapping("/{id}/iniciar")
    public String iniciar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p,
                          RedirectAttributes ra) {
        autorizadaVeterinario(id, p);
        try {
            atencionService.iniciar(id, p.getUsuario().getIdUsuario());
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorGeneral", e.getMessage());
            return "redirect:/agenda/" + id;
        }
        return "redirect:/agenda/" + id + "/atencion";
    }

    /** Compatibilidad con el enlace anterior "Atender": lleva al flujo nuevo. */
    @GetMapping("/{id}/atender")
    public String atenderAntiguo(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p) {
        Cita c = autorizadaVeterinario(id, p);
        return c.getEstado() == EstadoCita.EN_ATENCION ? "redirect:/agenda/" + id + "/atencion" : "redirect:/agenda/" + id;
    }

    @GetMapping("/{id}/atencion")
    public String atencion(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p, Model m) {
        Cita c = autorizadaVeterinario(id, p);
        if (c.getEstado() != EstadoCita.EN_ATENCION) {
            return "redirect:/agenda/" + id;
        }
        HistorialClinicoForm f = new HistorialClinicoForm();
        f.setIdMascota(c.getMascota().getIdMascota());
        f.setPesoKg(c.getMascota().getPeso());
        f.setMotivoConsulta(c.getMotivo());
        mostrarAtencion(c, p, m, f);
        return "historial/atender-cita";
    }

    @PostMapping("/{id}/historial")
    public String guardarHistorial(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p,
                                   @Valid @ModelAttribute("form") HistorialClinicoForm f, BindingResult r, Model m,
                                   RedirectAttributes ra) {
        Cita c = autorizadaVeterinario(id, p);
        if (r.hasErrors()) {
            mostrarAtencion(c, p, m, f);
            return "historial/atender-cita";
        }
        try {
            atencionService.registrarHistorial(id, f, p.getUsuario().getIdUsuario());
        } catch (IllegalStateException | IllegalArgumentException e) {
            mostrarAtencion(c, p, m, f);
            m.addAttribute("errorGeneral", e.getMessage());
            return "historial/atender-cita";
        }
        ra.addFlashAttribute("mensajeOk", "Registro clínico guardado.");
        return "redirect:/agenda/" + id + "/atencion";
    }

    @PostMapping("/{id}/vacuna")
    public String registrarVacuna(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p,
                                  @RequestParam(required = false) Integer idVacuna, @RequestParam(required = false) Integer idLote,
                                  @RequestParam(required = false) String dosis,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate proximaDosis,
                                  @RequestParam(required = false) String observaciones, RedirectAttributes ra) {
        autorizadaVeterinario(id, p);
        try {
            if (idVacuna == null || idLote == null) throw new IllegalArgumentException("Selecciona la vacuna y el lote");
            atencionService.registrarVacuna(id, idVacuna, idLote, dosis, proximaDosis, observaciones,
                    p.getUsuario().getIdUsuario());
            ra.addFlashAttribute("mensajeOk", "Vacuna registrada en el carnet.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorVacuna", e.getMessage());
        }
        return "redirect:/agenda/" + id + "/atencion";
    }

    @PostMapping("/{id}/archivos")
    public String adjuntar(@PathVariable Integer id, @RequestParam("archivo") MultipartFile archivo,
                           @AuthenticationPrincipal VetlinkUserPrincipal p, RedirectAttributes ra) {
        autorizadaVeterinario(id, p);
        try {
            atencionService.adjuntar(id, archivo, p.getUsuario().getIdUsuario());
            ra.addFlashAttribute("mensajeOk", "Archivo adjuntado.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorArchivo", e.getMessage());
        }
        return "redirect:/agenda/" + id + "/atencion";
    }

    @PostMapping("/{id}/finalizar")
    public String finalizar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p,
                            RedirectAttributes ra) {
        autorizadaVeterinario(id, p);
        try {
            atencionService.finalizar(id, p.getUsuario().getIdUsuario());
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorGeneral", e.getMessage());
            return "redirect:/agenda/" + id + "/atencion";
        }
        return "redirect:/agenda/" + id + "?atendida";
    }

    // ------------------------------------------------------------------
    // Gestión administrativa (recepción) e inasistencia
    // ------------------------------------------------------------------

    @PostMapping("/{id}/confirmar")
    public String confirmar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p, RedirectAttributes ra) {
        exigirRecepcion(p);
        try {
            citaService.confirmar(id, clinicaDe(p.getUsuario()), p.getUsuario());
            ra.addFlashAttribute("mensajeOk", "Cita confirmada. Se notificó al dueño.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorGeneral", e.getMessage());
        }
        return "redirect:/agenda/" + id;
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(@PathVariable Integer id, @RequestParam(required = false) String motivo,
                           @AuthenticationPrincipal VetlinkUserPrincipal p, RedirectAttributes ra) {
        exigirRecepcion(p);
        try {
            citaService.rechazar(id, clinicaDe(p.getUsuario()), p.getUsuario(), motivo);
            ra.addFlashAttribute("mensajeOk", "Solicitud rechazada. El horario volvió a la agenda si había margen.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorGeneral", e.getMessage());
        }
        return "redirect:/agenda/" + id;
    }

    @PostMapping("/{id}/no-asistio")
    public String noAsistio(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p, RedirectAttributes ra) {
        autorizada(id, p);
        try {
            citaService.marcarNoAsistio(id, p.getUsuario());
            ra.addFlashAttribute("mensajeOk", "Se registró la inasistencia.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorGeneral", e.getMessage());
        }
        return "redirect:/agenda/" + id;
    }

    // ------------------------------------------------------------------
    // Vistas
    // ------------------------------------------------------------------

    private void mostrarDetalle(Cita c, VetlinkUserPrincipal p, Model m) {
        shell(m, p);
        Usuario u = p.getUsuario();
        m.addAttribute("cita", c);
        m.addAttribute("direccion", infoVeterinaria.direccion(c.getVeterinaria().getIdVeterinaria()));
        m.addAttribute("historialEstados", estados.historialDe(c.getIdCita()));
        m.addAttribute("historial", c.getMascota() == null ? List.of()
                : historiales.listarPorMascota(c.getMascota().getIdMascota()));
        m.addAttribute("registroCita", historiales.buscarPorCita(c.getIdCita()).orElse(null));
        m.addAttribute("reprogramadaA", citas.findByCitaOrigen_IdCita(c.getIdCita()));
        boolean futura = c.getInicio().isAfter(java.time.LocalDateTime.now(reloj));
        boolean vencidaVentana = java.time.LocalDateTime.now(reloj)
                .isAfter(c.getInicio().plusMinutes(reglas.margenInicioAtencionMin()));
        if (u.getRol() == Rol.VETERINARIO) {
            m.addAttribute("ventana", atencionService.ventana(c, u.getIdUsuario()));
            m.addAttribute("puedeNoAsistio", vencidaVentana
                    && TransicionCita.permitida(c.getEstado(), EstadoCita.NO_ASISTIO, TransicionCita.Actor.VETERINARIO));
        } else {
            TransicionCita.Actor a = TransicionCita.Actor.RECEPCIONISTA;
            m.addAttribute("puedeConfirmar", futura && TransicionCita.permitida(c.getEstado(), EstadoCita.CONFIRMADA, a)
                    && c.getMascota() != null);
            m.addAttribute("puedeRechazar", TransicionCita.permitida(c.getEstado(), EstadoCita.RECHAZADA, a));
            m.addAttribute("puedeCancelar", futura && TransicionCita.permitida(c.getEstado(), EstadoCita.CANCELADA, a));
            m.addAttribute("puedeReprogramar", futura && TransicionCita.permitida(c.getEstado(), EstadoCita.REPROGRAMADA, a));
            m.addAttribute("puedeNoAsistio", vencidaVentana
                    && TransicionCita.permitida(c.getEstado(), EstadoCita.NO_ASISTIO, a));
        }
    }

    private void mostrarAtencion(Cita c, VetlinkUserPrincipal p, Model m, HistorialClinicoForm f) {
        shell(m, p);
        var registro = historiales.buscarPorCita(c.getIdCita()).orElse(null);
        m.addAttribute("cita", c);
        m.addAttribute("form", f);
        m.addAttribute("registro", registro);
        m.addAttribute("archivos", registro == null ? List.of()
                : archivos.findByHistorial_NumeroHistorial(registro.getNumeroHistorial()));
        m.addAttribute("historialPrevio", historiales.listarPorMascota(c.getMascota().getIdMascota()).stream()
                .filter(h -> registro == null || !h.getNumeroHistorial().equals(registro.getNumeroHistorial())).toList());
        m.addAttribute("tipoRegistro", c.getServicio().getTipoRegistro().name());
        m.addAttribute("esVacunacion", c.getServicio().getTipoRegistro() == Servicio.TipoRegistro.VACUNACION);
        m.addAttribute("vacunasCita", carnets.findByCita_IdCita(c.getIdCita()));
        m.addAttribute("vacunasEspecie", vacunas.findByEspecie_IdEspecie(c.getMascota().getEspecie().getIdEspecie()));
        m.addAttribute("lotes", lotes.findAll().stream()
                .filter(l -> l.getFechaVencimiento() == null || !l.getFechaVencimiento().isBefore(LocalDate.now(reloj)))
                .toList());
        m.addAttribute("pendientesFinalizar", atencionService.pendientesParaFinalizar(c));
        m.addAttribute("formatos", String.join(", ", reglas.formatosAdjuntos()).toUpperCase());
        m.addAttribute("tamanoMaximoMb", reglas.tamanoMaximoAdjuntoMb());
    }

    /** VL-25: se carga solo la cita pedida y se compara con el usuario. */
    private Cita autorizada(Integer id, VetlinkUserPrincipal p) {
        Cita c = citas.findById(id).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        var u = p.getUsuario();
        boolean permitido = switch (u.getRol()) {
            case VETERINARIO -> c.getVeterinario().getIdUsuario().equals(u.getIdUsuario());
            case RECEPCIONISTA -> c.getVeterinaria().getIdVeterinaria().equals(clinicaDe(u));
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

    private static void exigirRecepcion(VetlinkUserPrincipal p) {
        if (p.getUsuario().getRol() != Rol.RECEPCIONISTA) {
            throw new AccessDeniedException("Solo recepción gestiona la confirmación de citas");
        }
    }

    private static Integer clinicaDe(Usuario u) {
        if (u.getRecepcionista() == null) throw new AccessDeniedException("No autorizado");
        return u.getRecepcionista().getVeterinaria().getIdVeterinaria();
    }

    private void shell(Model m, VetlinkUserPrincipal p) {
        m.addAttribute("usuario", p.getUsuario());
        m.addAttribute("seccionActiva", "citas");
        m.addAttribute("navItems", DashboardController.navPara(p.getUsuario()));
    }
}
