package com.vetlink.emergencia.controller;

import com.vetlink.auditoria.AuditoriaService;
import com.vetlink.emergencia.dto.EmergenciaForm;
import com.vetlink.emergencia.model.Emergencia;
import com.vetlink.emergencia.model.EstadoEmergencia;
import com.vetlink.emergencia.repository.EmergenciaRepository;
import com.vetlink.emergencia.service.EmergenciaDisponibilidadService;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.notificacion.NotificacionService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.usuario.model.Recepcionista;
import com.vetlink.usuario.model.Rol;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.model.Veterinario;
import com.vetlink.usuario.repository.RecepcionistaRepository;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.usuario.repository.VeterinarioRepository;
import com.vetlink.usuario.repository.TelefonoRepository;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import com.vetlink.veterinaria.service.VeterinariaInfoService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/emergencias")
public class EmergenciaController {

    private final EmergenciaRepository repo;
    private final MascotaRepository mascotas;
    private final VeterinariaRepository veterinarias;
    private final UsuarioRepository usuarios;
    private final VeterinarioRepository veterinarios;
    private final RecepcionistaRepository recepcionistas;
    private final TelefonoRepository telefonos;
    private final EmergenciaDisponibilidadService disponibilidad;
    private final VeterinariaInfoService infoVeterinaria;
    private final NotificacionService notificaciones;
    private final AuditoriaService auditoria;
    private final Clock reloj;
    private final com.vetlink.emergencia.service.EmergenciaAtencionService atencion;
    private final com.vetlink.historial.service.HistorialClinicoService historiales;

    public EmergenciaController(EmergenciaRepository repo, MascotaRepository mascotas, VeterinariaRepository veterinarias,
                                UsuarioRepository usuarios, VeterinarioRepository veterinarios,
                                RecepcionistaRepository recepcionistas, TelefonoRepository telefonos,
                                EmergenciaDisponibilidadService disponibilidad, VeterinariaInfoService infoVeterinaria,
                                NotificacionService notificaciones, AuditoriaService auditoria, Clock reloj,
                                com.vetlink.emergencia.service.EmergenciaAtencionService atencion,
                                com.vetlink.historial.service.HistorialClinicoService historiales) {
        this.atencion = atencion;
        this.historiales = historiales;
        this.repo = repo;
        this.mascotas = mascotas;
        this.veterinarias = veterinarias;
        this.usuarios = usuarios;
        this.veterinarios = veterinarios;
        this.recepcionistas = recepcionistas;
        this.telefonos = telefonos;
        this.disponibilidad = disponibilidad;
        this.infoVeterinaria = infoVeterinaria;
        this.notificaciones = notificaciones;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Integer idVeterinaria,
                         @AuthenticationPrincipal VetlinkUserPrincipal p, Model m) {
        shell(m, p);
        var u = p.getUsuario();
        switch (u.getRol()) {
            case TUTOR -> {
                // DUE-07: solo las solicitudes que envió este dueño (consulta por solicitante, no filtro visual).
                m.addAttribute("emergencias", repo.deSolicitante(u.getIdUsuario()));
                m.addAttribute("mascotas", mascotas.findByTutor_IdUsuarioAndEstado(u.getIdUsuario(), (byte) 1).stream()
                        .filter(x -> x.isAgendable()).toList());
                Map<Integer, Integer> disponibles = new LinkedHashMap<>();
                var conUrgencias = veterinarias.findByEstado((byte) 1).stream()
                        .filter(v -> disponibilidad.ofreceEmergencias(v.getIdVeterinaria()))
                        .peek(v -> disponibles.put(v.getIdVeterinaria(), disponibilidad.disponiblesAhora(v.getIdVeterinaria()).size()))
                        .toList();
                m.addAttribute("veterinarias", conUrgencias);
                m.addAttribute("disponibles", disponibles);
                if (!m.containsAttribute("form")) {
                    EmergenciaForm f = new EmergenciaForm();
                    f.setIdVeterinaria(idVeterinaria);
                    m.addAttribute("form", f);
                }
            }
            case VETERINARIO -> m.addAttribute("emergencias", repo.findByVeterinario_IdUsuarioOrderByCreadaEnDesc(u.getIdUsuario()));
            case RECEPCIONISTA -> {
                var v = u.getRecepcionista().getVeterinaria();
                m.addAttribute("emergencias", repo.findByVeterinaria_IdVeterinariaOrderByCreadaEnDesc(v.getIdVeterinaria()));
                m.addAttribute("veterinarios", disponibilidad.disponiblesAhora(v.getIdVeterinaria()));
                m.addAttribute("equipoCompleto", veterinarios.findByVeterinaria_IdVeterinaria(v.getIdVeterinaria()).size());
            }
        }
        return "emergencias/lista";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p, Model m) {
        Emergencia e = autorizada(id, p);
        shell(m, p);
        m.addAttribute("e", e);
        m.addAttribute("registroClinico", historiales.buscarPorEmergencia(id).orElse(null));
        m.addAttribute("direccionClinica", infoVeterinaria.direccion(e.getVeterinaria().getIdVeterinaria()));
        m.addAttribute("telefonosClinica", infoVeterinaria.telefonos(e.getVeterinaria().getIdVeterinaria()));
        Usuario u = p.getUsuario();
        // Datos de contacto del tutor: solo para el personal que gestiona o atiende la emergencia.
        if (u.getRol() != Rol.TUTOR && e.getMascota() != null) {
            m.addAttribute("telefonosTutor", telefonos.findByUsuario_IdUsuario(e.getMascota().getTutor().getIdUsuario()));
        }
        if (u.getRol() == Rol.RECEPCIONISTA) {
            m.addAttribute("veterinarios", disponibilidad.disponiblesAhora(e.getVeterinaria().getIdVeterinaria()));
        }
        return "emergencias/detalle";
    }

    @PostMapping
    public String crear(@AuthenticationPrincipal VetlinkUserPrincipal p,
                        @Valid @ModelAttribute("form") EmergenciaForm f, BindingResult r, Model m) {
        if (p.getUsuario().getRol() != Rol.TUTOR) {
            throw new AccessDeniedException("Solo los dueños solicitan emergencias");
        }
        String motivo = f.getMotivo() == null ? "" : f.getMotivo().strip();
        if (!r.hasFieldErrors("motivo") && motivo.length() > 255) {
            r.rejectValue("motivo", "largo", "El motivo no puede superar 255 caracteres");
        }
        if (f.getDetalle() != null && f.getDetalle().length() > 2000) {
            r.rejectValue("detalle", "largo", "Los detalles no pueden superar 2000 caracteres");
        }
        if (r.hasErrors()) {
            return listar(f.getIdVeterinaria(), p, m);
        }
        // DUE-07: la mascota debe ser del dueño autenticado (se valida en el servidor, no en la vista).
        var mascota = mascotas.findById(f.getIdMascota()).orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        if (mascota.getTutor() == null || !mascota.getTutor().getIdUsuario().equals(p.getUsuario().getIdUsuario())) {
            throw new AccessDeniedException("La mascota no te pertenece");
        }
        if (!mascota.isAgendable()) {
            throw new IllegalStateException("No se pueden solicitar emergencias para esta mascota (inactiva o fallecida).");
        }
        var veterinaria = veterinarias.findById(f.getIdVeterinaria())
                .orElseThrow(() -> new IllegalArgumentException("Veterinaria no encontrada"));
        if (!veterinaria.isActiva()) {
            throw new IllegalArgumentException("Veterinaria no encontrada");
        }
        if (repo.existeAbiertaParaMascota(mascota.getIdMascota(),
                java.util.EnumSet.of(EstadoEmergencia.PENDIENTE, EstadoEmergencia.ASIGNADA))) {
            throw new IllegalStateException(mascota.getNombre() + " ya tiene una solicitud de emergencia en curso.");
        }
        Emergencia e = new Emergencia();
        e.setMascota(mascota);
        e.setSolicitante(p.getUsuario());
        e.setVeterinaria(veterinaria);
        e.setMotivo(motivo);
        e.setDetalle(f.getDetalle() == null || f.getDetalle().isBlank() ? null : f.getDetalle().strip());
        e.setCreadaEn(LocalDateTime.now(reloj));
        repo.save(e);
        auditoria.registrar(p.getUsuario().getIdUsuario(), "EMERGENCIA_SOLICITAR", "emergencia", e.getIdEmergencia(), null);
        notificaciones.notificarVarios(recepcionistas.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()).stream()
                        .map(Recepcionista::getUsuario).filter(Usuario::isActivo).map(Usuario::getIdUsuario).toList(),
                "EMERGENCIA_NUEVA", "Nueva solicitud de emergencia", mascota.getNombre() + ": " + e.getMotivo(),
                "/emergencias/" + e.getIdEmergencia(), "emergencia-" + e.getIdEmergencia());
        return "redirect:/emergencias/" + e.getIdEmergencia() + "?creada";
    }

    @PostMapping("/{id}/asignar")
    public String asignar(@PathVariable Integer id, @RequestParam Integer idVeterinario,
                          @AuthenticationPrincipal VetlinkUserPrincipal p) {
        Emergencia e = deMiClinica(id, p);
        if (e.getEstado() != EstadoEmergencia.PENDIENTE && e.getEstado() != EstadoEmergencia.ASIGNADA) {
            throw new IllegalStateException("La emergencia ya está cerrada");
        }
        var v = usuarios.findById(idVeterinario).orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));
        if (v.getVeterinario() == null || v.getVeterinario().getVeterinaria() == null
                || !v.getVeterinario().getVeterinaria().getIdVeterinaria().equals(e.getVeterinaria().getIdVeterinaria())) {
            throw new AccessDeniedException("Veterinario inválido");
        }
        Integer servicio = disponibilidad.servicioEmergencia(e.getVeterinaria().getIdVeterinaria()).orElse(null);
        boolean yaEsElAsignado = e.getVeterinario() != null && e.getVeterinario().getIdUsuario().equals(idVeterinario);
        if (!yaEsElAsignado && !disponibilidad.disponibleAhora(v, servicio)) {
            throw new IllegalStateException("Ese veterinario no está disponible ahora (fuera de turno, en atención o con otra emergencia).");
        }
        e.setVeterinario(v);
        e.setEstado(EstadoEmergencia.ASIGNADA);
        repo.save(e);
        auditoria.registrar(p.getUsuario().getIdUsuario(), "EMERGENCIA_ASIGNAR", "emergencia", id, "veterinario=" + idVeterinario);
        notificaciones.notificar(idVeterinario, "EMERGENCIA_ASIGNADA", "Emergencia asignada",
                (e.getMascota() == null ? "Paciente" : e.getMascota().getNombre()) + ": " + e.getMotivo(),
                "/emergencias/" + id, "emergencia-" + id + "-asignada");
        if (e.getMascota() != null) {
            notificaciones.notificar(idDuenio(e), "EMERGENCIA_ASIGNADA",
                    "Tu emergencia fue asignada", "La clínica asignó a " + v.getNombre() + " " + v.getApellido()
                            + " para atender a " + e.getMascota().getNombre() + ".",
                    "/emergencias/" + id, "emergencia-" + id + "-asignada");
        }
        return "redirect:/emergencias?asignada";
    }

    /**
     * VL-04: cambio de estado con reglas por rol.
     * - Cerrada (ATENDIDA/CANCELADA): no se toca.
     * - TUTOR: solo cancela sus propias emergencias PENDIENTES.
     * - RECEPCIONISTA (de la clínica): ASIGNADA (con veterinario) o CANCELADA; nunca ATENDIDA.
     * - VETERINARIO: nunca. VET-03: la emergencia pasa a ATENDIDA solo al guardar el registro
     *   clínico (POST /emergencias/{id}/atender, EmergenciaAtencionService).
     */
    @PostMapping("/{id}/estado")
    public String estado(@PathVariable Integer id, @RequestParam EstadoEmergencia estado,
                         @AuthenticationPrincipal VetlinkUserPrincipal p) {
        Emergencia e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Emergencia no encontrada"));
        Usuario u = p.getUsuario();
        EstadoEmergencia actual = e.getEstado();
        if (actual == EstadoEmergencia.ATENDIDA || actual == EstadoEmergencia.CANCELADA) {
            throw new IllegalStateException("La emergencia ya está cerrada");
        }
        Rol rol = u.getRol();
        if (estado == EstadoEmergencia.ATENDIDA) {
            throw new AccessDeniedException("La emergencia se marca como atendida automáticamente al guardar el registro clínico.");
        }
        if (rol == Rol.TUTOR) {
            boolean esSuya = e.esDeSolicitante(u.getIdUsuario());
            if (estado != EstadoEmergencia.CANCELADA || !esSuya || actual != EstadoEmergencia.PENDIENTE) {
                throw new AccessDeniedException("Cambio de estado no permitido");
            }
        } else if (rol == Rol.RECEPCIONISTA) {
            deMiClinica(id, p);
            if (estado == EstadoEmergencia.ASIGNADA) {
                if (e.getVeterinario() == null) {
                    throw new IllegalStateException("Primero asigna un veterinario a la emergencia");
                }
            } else if (estado != EstadoEmergencia.CANCELADA) {
                throw new AccessDeniedException("Cambio de estado no permitido");
            }
        } else {
            throw new AccessDeniedException("Cambio de estado no permitido");
        }
        e.setEstado(estado);
        repo.save(e);
        auditoria.registrar(u.getIdUsuario(), "EMERGENCIA_ESTADO", "emergencia", id, actual + " -> " + estado);
        if (e.getMascota() != null && rol != Rol.TUTOR) {
            notificaciones.notificar(idDuenio(e), "EMERGENCIA_" + estado.name(),
                    "Actualización de tu emergencia", e.getMascota().getNombre() + ": solicitud cancelada por la clínica.",
                    "/emergencias/" + id, "emergencia-" + id + "-" + estado.name().toLowerCase());
        }
        return "redirect:/emergencias?actualizada";
    }

    // ------------------------------------------------------------------
    // VET-03: atender = registrar la historia clínica de la emergencia
    // ------------------------------------------------------------------

    @GetMapping("/{id}/atender")
    public String atenderForm(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p, Model m,
                              RedirectAttributes ra) {
        exigirVeterinario(p);
        var existente = historiales.buscarPorEmergencia(id);
        if (existente.isPresent()) {
            ra.addFlashAttribute("mensajeOk", "Esta emergencia ya fue atendida y tiene su registro clínico.");
            return "redirect:/historial/documento/" + existente.get().getNumeroHistorial();
        }
        Emergencia e = atencion.paraAtender(id, p.getUsuario().getIdUsuario());
        if (!m.containsAttribute("form")) {
            var f = new com.vetlink.historial.dto.HistorialClinicoForm();
            f.setIdMascota(e.getMascota().getIdMascota());
            f.setTipoRegistro("URGENCIA");
            f.setMotivoConsulta(e.getMotivo());
            f.setPesoKg(e.getMascota().getPeso());
            m.addAttribute("form", f);
        }
        shell(m, p);
        m.addAttribute("e", e);
        return "emergencias/atender";
    }

    @PostMapping("/{id}/atender")
    public String atender(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal p,
                          @Valid @ModelAttribute("form") com.vetlink.historial.dto.HistorialClinicoForm form,
                          BindingResult r, Model m, RedirectAttributes ra) {
        exigirVeterinario(p);
        var existente = historiales.buscarPorEmergencia(id);
        if (existente.isPresent()) {
            // Reintento (doble clic, recarga): no se crea un segundo registro.
            ra.addFlashAttribute("mensajeOk", "Esta emergencia ya fue atendida y tiene su registro clínico.");
            return "redirect:/historial/documento/" + existente.get().getNumeroHistorial();
        }
        Emergencia e = atencion.paraAtender(id, p.getUsuario().getIdUsuario());
        if (r.hasErrors()) {
            shell(m, p);
            m.addAttribute("e", e);
            return "emergencias/atender";
        }
        try {
            atencion.registrarAtencion(id, form, p.getUsuario().getIdUsuario());
        } catch (IllegalArgumentException ex) {
            shell(m, p);
            m.addAttribute("e", e);
            m.addAttribute("errorGeneral", ex.getMessage());
            return "emergencias/atender";
        }
        ra.addFlashAttribute("mensajeOk", "Registro clínico guardado. La emergencia quedó como «Atendida».");
        return "redirect:/emergencias/" + id;
    }

    private static void exigirVeterinario(VetlinkUserPrincipal p) {
        if (p.getUsuario().getRol() != Rol.VETERINARIO) {
            throw new AccessDeniedException("Solo el veterinario asignado registra la atención");
        }
    }

    /** Dueño a notificar: quien solicitó (o el dueño actual en registros previos a V9). */
    private static Integer idDuenio(Emergencia e) {
        return e.getSolicitante() != null ? e.getSolicitante().getIdUsuario() : e.getMascota().getTutor().getIdUsuario();
    }

    /** Errores de negocio (emergencia cerrada, veterinario ocupado): vuelve a la lista con el mensaje. */
    @ExceptionHandler(IllegalStateException.class)
    public String errorDeEstado(IllegalStateException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:/emergencias";
    }

    private Emergencia autorizada(Integer id, VetlinkUserPrincipal p) {
        Emergencia e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Emergencia no encontrada"));
        Usuario u = p.getUsuario();
        boolean ok = switch (u.getRol()) {
            case TUTOR -> e.esDeSolicitante(u.getIdUsuario());
            case VETERINARIO -> e.getVeterinario() != null && e.getVeterinario().getIdUsuario().equals(u.getIdUsuario());
            case RECEPCIONISTA -> e.getVeterinaria().getIdVeterinaria()
                    .equals(u.getRecepcionista().getVeterinaria().getIdVeterinaria());
        };
        if (!ok) throw new AccessDeniedException("No tienes acceso a esta emergencia");
        return e;
    }

    private Emergencia deMiClinica(Integer id, VetlinkUserPrincipal p) {
        Emergencia e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Emergencia no encontrada"));
        if (p.getUsuario().getRecepcionista() == null
                || !e.getVeterinaria().getIdVeterinaria()
                .equals(p.getUsuario().getRecepcionista().getVeterinaria().getIdVeterinaria())) {
            throw new AccessDeniedException("No pertenece a tu veterinaria");
        }
        return e;
    }

    private void shell(Model m, VetlinkUserPrincipal p) {
        m.addAttribute("usuario", p.getUsuario());
        m.addAttribute("seccionActiva", "emergencias");
        m.addAttribute("navItems", DashboardController.navPara(p.getUsuario()));
    }

    /** Para la vista: nombres de veterinarios de la clínica. */
    public static String nombre(Veterinario v) {
        return v.getUsuario().getNombre() + " " + v.getUsuario().getApellido();
    }
}
