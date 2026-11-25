package com.vetlink.veterinaria.controller;

import com.vetlink.disponibilidad.service.SolicitudDisponibilidadService;
import com.vetlink.disponibilidad.service.SolicitudDisponibilidadService.Franja;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.usuario.dto.RegistroUsuarioForm;
import com.vetlink.usuario.model.Recepcionista;
import com.vetlink.usuario.model.Veterinario;
import com.vetlink.usuario.repository.CorreoRepository;
import com.vetlink.usuario.repository.EspecialidadVeterinarioRepository;
import com.vetlink.usuario.repository.RecepcionistaRepository;
import com.vetlink.usuario.repository.TelefonoRepository;
import com.vetlink.usuario.repository.TipoDocumentoRepository;
import com.vetlink.usuario.repository.VeterinarioRepository;
import com.vetlink.usuario.service.UsuarioService;
import com.vetlink.veterinaria.model.Servicio;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.ServicioVeterinariaRepository;
import com.vetlink.veterinaria.service.GestionClinicaService;
import com.vetlink.veterinaria.service.VeterinariaInfoService;
import com.vetlink.veterinaria.service.VeterinariaService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * REC-09: «Mi veterinaria» como perfil institucional. Toda la recepción de la clínica puede
 * consultarlo; solo la recepcionista principal edita (información, equipo, horarios y servicios).
 * El id de la clínica nunca viene del navegador: siempre es la de quien hace la petición.
 */
@Controller
@RequestMapping("/veterinarias/admin")
public class VeterinariaAdminController {

    private final VeterinariaService veterinariaService;
    private final UsuarioService usuarioService;
    private final VeterinarioRepository veterinarioRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final EspecialidadVeterinarioRepository especialidadVeterinarioRepository;
    private final RecepcionistaRepository recepcionistaRepository;
    private final com.vetlink.veterinaria.service.LogoVeterinariaService logos;
    private final com.vetlink.auditoria.AuditoriaService auditoria;
    private final VeterinariaInfoService info;
    private final ServicioVeterinariaRepository serviciosClinica;
    private final SolicitudDisponibilidadService disponibilidad;
    private final GestionClinicaService gestion;
    private final CorreoRepository correos;
    private final TelefonoRepository telefonos;

    /** El personal de una clínica es mayor de edad: la tarjeta de identidad no aplica en estos formularios. */
    static final String TIPO_NO_PERMITIDO_PERSONAL = "TI";
    private static final java.util.regex.Pattern WEB =
            java.util.regex.Pattern.compile("^(https?://)?([\\w-]+\\.)+[a-zA-Z]{2,}(/[\\w\\-./?%&=]*)?$");

    public VeterinariaAdminController(VeterinariaService veterinariaService, UsuarioService usuarioService,
                                      VeterinarioRepository veterinarioRepository,
                                      TipoDocumentoRepository tipoDocumentoRepository,
                                      EspecialidadVeterinarioRepository especialidadVeterinarioRepository,
                                      RecepcionistaRepository recepcionistaRepository,
                                      com.vetlink.veterinaria.service.LogoVeterinariaService logos,
                                      com.vetlink.auditoria.AuditoriaService auditoria, VeterinariaInfoService info,
                                      ServicioVeterinariaRepository serviciosClinica,
                                      SolicitudDisponibilidadService disponibilidad, GestionClinicaService gestion,
                                      CorreoRepository correos, TelefonoRepository telefonos) {
        this.veterinariaService = veterinariaService;
        this.usuarioService = usuarioService;
        this.veterinarioRepository = veterinarioRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.especialidadVeterinarioRepository = especialidadVeterinarioRepository;
        this.recepcionistaRepository = recepcionistaRepository;
        this.logos = logos;
        this.auditoria = auditoria;
        this.info = info;
        this.serviciosClinica = serviciosClinica;
        this.disponibilidad = disponibilidad;
        this.gestion = gestion;
        this.correos = correos;
        this.telefonos = telefonos;
    }

    @GetMapping
    public String panel(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        Veterinaria veterinaria = miVeterinaria(principal);
        cargarPanel(model, principal, veterinaria);
        if (!model.containsAttribute("formEquipo")) model.addAttribute("formEquipo", new RegistroUsuarioForm());
        return "veterinarias/admin";
    }

    // ------------------------------------------------------------------
    // Información institucional (lápiz «Editar»)
    // ------------------------------------------------------------------

    @PostMapping("/info")
    public String actualizarInfo(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                                 @RequestParam(required = false) String nombre, @RequestParam(required = false) String descripcion,
                                 @RequestParam(required = false) String paginaWeb,
                                 @RequestParam(required = false) String telefono, @RequestParam(required = false) String correo,
                                 RedirectAttributes redirectAttributes) {
        exigirRecepcionistaPrincipal(principal);
        Veterinaria veterinaria = miVeterinaria(principal);
        String error = null;
        String n = nombre == null ? "" : com.vetlink.comun.validacion.Textos.normalizar(nombre);
        String web = paginaWeb == null ? "" : paginaWeb.trim();
        if (n.isEmpty()) error = "El nombre de la veterinaria es obligatorio";
        else if (n.length() > 50) error = "El nombre no puede superar 50 caracteres";
        else if (!web.isEmpty() && (web.length() > 50 || !WEB.matcher(web).matches())) error = "Ingresa una página web válida (máx. 50 caracteres)";
        else if (descripcion != null && descripcion.length() > 2000) error = "La descripción no puede superar 2000 caracteres";
        if (error != null) {
            redirectAttributes.addFlashAttribute("error", error);
            redirectAttributes.addFlashAttribute("editarInfo", true);
            return "redirect:/veterinarias/admin";
        }
        try {
            veterinariaService.actualizarInfo(veterinaria.getIdVeterinaria(), principal.getUsuario().getIdUsuario(),
                    n, descripcion == null ? null : descripcion.trim(), web.isEmpty() ? null : web, null);
            gestion.actualizarContacto(principal.getUsuario(), telefono, correo);
            principal.getUsuario().getRecepcionista().getVeterinaria().setNombre(n);
            auditoria.registrar(principal.getUsuario().getIdUsuario(), "VETERINARIA_EDITAR", "veterinaria",
                    veterinaria.getIdVeterinaria(), null);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("editarInfo", true);
            return "redirect:/veterinarias/admin";
        }
        return "redirect:/veterinarias/admin?actualizada";
    }

    /** Carga real de la imagen de la veterinaria (PNG/JPG, tamaño máximo configurable). */
    @PostMapping("/logo")
    public String subirLogo(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                            @RequestParam(value = "logoArchivo", required = false) org.springframework.web.multipart.MultipartFile archivo,
                            RedirectAttributes redirectAttributes) {
        exigirRecepcionistaPrincipal(principal);
        Veterinaria veterinaria = veterinariaService.buscarPorId(miVeterinaria(principal).getIdVeterinaria());
        try {
            logos.guardar(veterinaria, archivo);
            principal.getUsuario().getRecepcionista().getVeterinaria().setLogo(veterinaria.getLogo());
            auditoria.registrar(principal.getUsuario().getIdUsuario(), "VETERINARIA_LOGO", "veterinaria",
                    veterinaria.getIdVeterinaria(), null);
            redirectAttributes.addFlashAttribute("mensajeOk", "Imagen actualizada. Ya se muestra en el directorio.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/veterinarias/admin";
    }

    // ------------------------------------------------------------------
    // Equipo: alta de personal
    // ------------------------------------------------------------------

    @PostMapping("/equipo")
    public String registrarVeterinario(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                                       @Valid @ModelAttribute("formEquipo") RegistroUsuarioForm form,
                                       BindingResult result, Model model) {
        exigirRecepcionistaPrincipal(principal);
        Veterinaria veterinaria = miVeterinaria(principal);

        // El rol y la veterinaria NUNCA se toman del formulario enviado por el navegador:
        // se fuerzan aquí para que una recepcionista no pueda registrar personal en otra clínica.
        form.setRol("VETERINARIO");
        form.setIdVeterinaria(veterinaria.getIdVeterinaria());
        form.setConfirmarPassword(form.getPassword());

        if (esTipoNoPermitido(form.getIdTipoDocumento())) {
            result.rejectValue("idTipoDocumento", "tipo.noPermitido", "Selecciona un tipo de documento válido para personal");
        }
        if (form.getLicencia() == null || form.getLicencia().isBlank()) {
            result.rejectValue("licencia", "licencia.requerida", "La licencia profesional es obligatoria");
        }
        if (hayErroresDelUsuario(result)) {
            cargarPanel(model, principal, veterinaria);
            model.addAttribute("errorEquipo", "Revisa los datos del veterinario.");
            model.addAttribute("formularioActivo", "veterinario");
            return "veterinarias/admin";
        }
        try {
            usuarioService.registrar(form);
        } catch (RuntimeException e) {
            cargarPanel(model, principal, veterinaria);
            model.addAttribute("errorEquipo", e.getMessage());
            model.addAttribute("formularioActivo", "veterinario");
            return "veterinarias/admin";
        }
        return "redirect:/veterinarias/admin?veterinarioCreado";
    }

    @PostMapping("/recepcionistas")
    public String registrarRecepcionista(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                                         @Valid @ModelAttribute("formEquipo") RegistroUsuarioForm form, BindingResult result,
                                         Model model) {
        exigirRecepcionistaPrincipal(principal);
        Veterinaria veterinaria = miVeterinaria(principal);
        form.setRol("RECEPCIONISTA");
        form.setIdVeterinariaRecepcion(veterinaria.getIdVeterinaria());
        // VL-08: desde el panel nunca se crea una clínica nueva, así la recepcionista queda con esPrincipal = false.
        form.setNuevaVeterinaria(null);
        form.setConfirmarPassword(form.getPassword());
        if (form.getTurno() == null || form.getTurno().isBlank()) form.setTurno("MAÑANA");
        if (!GestionClinicaService.TURNOS.contains(form.getTurno())) {
            result.rejectValue("turno", "turno.invalido", "Selecciona un turno válido");
        }
        if (esTipoNoPermitido(form.getIdTipoDocumento())) {
            result.rejectValue("idTipoDocumento", "tipo.noPermitido", "Selecciona un tipo de documento válido para personal");
        }
        // VL-21: con errores de validación se vuelve a la vista mostrando qué campo falló.
        if (hayErroresDelUsuario(result)) {
            cargarPanel(model, principal, veterinaria);
            model.addAttribute("errorRecepcion", "Revisa los datos de la recepcionista.");
            model.addAttribute("formularioActivo", "recepcionista");
            return "veterinarias/admin";
        }
        // VL-21: un correo o documento repetido ya no termina en error 500.
        try {
            usuarioService.registrar(form);
        } catch (RuntimeException e) {
            cargarPanel(model, principal, veterinaria);
            model.addAttribute("errorRecepcion", e.getMessage());
            model.addAttribute("formularioActivo", "recepcionista");
            return "veterinarias/admin";
        }
        return "redirect:/veterinarias/admin?recepcionistaCreada";
    }

    // ------------------------------------------------------------------
    // Equipo: horarios y servicios
    // ------------------------------------------------------------------

    @PostMapping("/veterinarios/{idVet}/horario")
    public String horarioVeterinario(@PathVariable Integer idVet, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                                     @RequestParam(name = "dia", required = false) List<Integer> dias,
                                     @RequestParam(name = "inicio", required = false) List<String> inicios,
                                     @RequestParam(name = "fin", required = false) List<String> fines,
                                     RedirectAttributes ra) {
        exigirRecepcionistaPrincipal(principal);
        List<Franja> franjas = new ArrayList<>();
        int n = dias == null ? 0 : dias.size();
        for (int i = 0; i < n; i++) franjas.add(new Franja(dias.get(i), hora(inicios, i), hora(fines, i)));
        return ejecutar(ra, "#veterinarios", () -> {
            var r = gestion.asignarHorarioVeterinario(principal.getUsuario(), idVet, franjas);
            return "Horario actualizado. Cupos retirados: " + r.cuposRetirados() + "; cupos nuevos: " + r.cuposCreados()
                    + (r.citasAfectadas().isEmpty() ? "." : ". Hay " + r.citasAfectadas().size()
                    + " cita(s) por reprogramar en «Disponibilidad del equipo».");
        });
    }

    @PostMapping("/veterinarios/{idVet}/servicios")
    public String serviciosVeterinario(@PathVariable Integer idVet, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                                       @RequestParam(name = "servicios", required = false) List<Integer> servicios,
                                       RedirectAttributes ra) {
        exigirRecepcionistaPrincipal(principal);
        return ejecutar(ra, "#veterinarios", () -> {
            var r = gestion.asignarServiciosVeterinario(principal.getUsuario(), idVet, servicios);
            return "Servicios del veterinario actualizados. Cupos nuevos: " + r.cuposCreados() + ".";
        });
    }

    @PostMapping("/recepcionistas/{idRec}/turno")
    public String turnoRecepcionista(@PathVariable Integer idRec, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                                     @RequestParam(required = false) String turno, RedirectAttributes ra) {
        exigirRecepcionistaPrincipal(principal);
        return ejecutar(ra, "#recepcion", () -> {
            gestion.cambiarTurnoRecepcionista(principal.getUsuario(), idRec, turno);
            return "Turno actualizado.";
        });
    }

    // ------------------------------------------------------------------
    // Servicios de la clínica
    // ------------------------------------------------------------------

    @PostMapping("/servicios")
    public String agregarServicio(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                                  @RequestParam(required = false) String nombre, @RequestParam(required = false) String descripcion,
                                  @RequestParam(required = false) Double precio, @RequestParam(required = false) Integer duracion,
                                  @RequestParam(required = false) Servicio.TipoRegistro tipoRegistro, RedirectAttributes ra) {
        exigirRecepcionistaPrincipal(principal);
        return ejecutar(ra, "#servicios", () -> {
            var sv = gestion.agregarServicio(principal.getUsuario(), nombre, descripcion, precio, duracion, tipoRegistro);
            return "Servicio «" + sv.getServicio().getNombreServicio() + "» agregado. Asígnalo a los veterinarios que lo atienden.";
        });
    }

    @PostMapping("/servicios/{idServicio}")
    public String editarServicio(@PathVariable Integer idServicio, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                                 @RequestParam(required = false) Double precio, @RequestParam(required = false) Integer duracion,
                                 RedirectAttributes ra) {
        exigirRecepcionistaPrincipal(principal);
        return ejecutar(ra, "#servicios", () -> {
            gestion.actualizarServicio(principal.getUsuario(), idServicio, precio, duracion);
            return "Servicio actualizado. La agenda ya usa la nueva configuración.";
        });
    }

    @PostMapping("/servicios/{idServicio}/retirar")
    public String retirarServicio(@PathVariable Integer idServicio, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                                  RedirectAttributes ra) {
        exigirRecepcionistaPrincipal(principal);
        return ejecutar(ra, "#servicios", () -> {
            int n = gestion.retirarServicio(principal.getUsuario(), idServicio);
            return "Servicio retirado del catálogo. Cupos libres retirados: " + n + ". Las citas reservadas se conservan.";
        });
    }

    // ------------------------------------------------------------------

    private String ejecutar(RedirectAttributes ra, String ancla, Supplier<String> accion) {
        try {
            ra.addFlashAttribute("mensajeOk", accion.get());
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/veterinarias/admin" + ancla;
    }

    private static LocalTime hora(List<String> lista, int i) {
        if (lista == null || i >= lista.size() || lista.get(i) == null || lista.get(i).isBlank()) return null;
        try {
            return LocalTime.parse(lista.get(i));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Todo lo que muestra el perfil institucional, siempre desde la base de datos. */
    private void cargarPanel(Model model, VetlinkUserPrincipal principal, Veterinaria sesion) {
        Veterinaria veterinaria = veterinariaService.buscarPorId(sesion.getIdVeterinaria());
        Integer id = veterinaria.getIdVeterinaria();
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "veterinaria");
        model.addAttribute("navItems", DashboardController.navRecepcionista());
        model.addAttribute("esPrincipal", Boolean.TRUE.equals(principal.getUsuario().getRecepcionista().getEsPrincipal()));
        model.addAttribute("veterinaria", veterinaria);
        model.addAttribute("logoUrl", VeterinariaInfoService.logoUrl(veterinaria));
        model.addAttribute("tamanoMaximoImagenMb", 2);
        model.addAttribute("direccion", info.direccion(id));
        model.addAttribute("telefonosClinica", info.telefonos(id));
        model.addAttribute("correosClinica", info.correos(id));
        model.addAttribute("horarioClinica", info.horarioSemanal(id));
        var servicios = serviciosClinica.findByVeterinaria_IdVeterinaria(id).stream()
                .sorted(Comparator.comparing(sv -> sv.getServicio().getNombreServicio(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("servicios", servicios);
        model.addAttribute("serviciosAgendables", servicios.stream().filter(sv -> !sv.getServicio().isEmergencia()).toList());

        List<Veterinario> equipo = veterinarioRepository.findByVeterinaria_IdVeterinaria(id);
        model.addAttribute("equipo", equipo);
        Map<Integer, Object> horarios = new LinkedHashMap<>();
        Map<Integer, Object> serviciosVet = new LinkedHashMap<>();
        Map<Integer, Object> idsServiciosVet = new LinkedHashMap<>();
        for (Veterinario v : equipo) {
            Integer idVet = v.getIdUsuario();
            horarios.put(idVet, disponibilidad.horarioAprobado(idVet));
            var aprobados = disponibilidad.serviciosAprobados(idVet);
            serviciosVet.put(idVet, aprobados);
            idsServiciosVet.put(idVet, aprobados.stream().map(s -> s.getServicio().getIdServicio()).toList());
        }
        model.addAttribute("horariosEquipo", horarios);
        model.addAttribute("serviciosEquipo", serviciosVet);
        model.addAttribute("idsServiciosEquipo", idsServiciosVet);

        List<Recepcionista> recepcion = recepcionistaRepository.findByVeterinaria_IdVeterinaria(id);
        model.addAttribute("recepcionistas", recepcion);
        Map<Integer, String> correoDe = new LinkedHashMap<>();
        Map<Integer, String> telefonoDe = new LinkedHashMap<>();
        equipo.forEach(v -> contacto(v.getIdUsuario(), correoDe, telefonoDe));
        recepcion.forEach(r -> contacto(r.getIdUsuario(), correoDe, telefonoDe));
        model.addAttribute("correoDe", correoDe);
        model.addAttribute("telefonoDe", telefonoDe);

        model.addAttribute("tiposDocumento", tiposDocumentoPersonal());
        model.addAttribute("especialidades", especialidadVeterinarioRepository.findAll());
        model.addAttribute("turnos", List.of("MAÑANA", "TARDE", "NOCHE", "COMPLETO"));
        model.addAttribute("tiposRegistro", Servicio.TipoRegistro.values());
        model.addAttribute("nombresDia", List.of("", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"));
    }

    private void contacto(Integer idUsuario, Map<Integer, String> correoDe, Map<Integer, String> telefonoDe) {
        var c = correos.findByUsuario_IdUsuario(idUsuario);
        var t = telefonos.findByUsuario_IdUsuario(idUsuario);
        correoDe.put(idUsuario, c.isEmpty() ? "" : c.get(0).getCorreo());
        telefonoDe.put(idUsuario, t.isEmpty() ? "" : t.get(0).getNumeroTelefono());
    }

    private List<com.vetlink.usuario.model.TipoDocumento> tiposDocumentoPersonal() {
        return tipoDocumentoRepository.findAll().stream()
                .filter(t -> !TIPO_NO_PERMITIDO_PERSONAL.equalsIgnoreCase(t.getNombreTipo()))
                .toList();
    }

    private boolean esTipoNoPermitido(Integer idTipoDocumento) {
        return idTipoDocumento != null && tipoDocumentoRepository.findById(idTipoDocumento)
                .map(t -> TIPO_NO_PERMITIDO_PERSONAL.equalsIgnoreCase(t.getNombreTipo())).orElse(false);
    }

    /**
     * VL-21: el panel no envía "rol" ni "confirmarPassword" (los fija este controlador),
     * pero RegistroUsuarioForm los marca @NotBlank y la validación corre antes de fijarlos.
     */
    private static final java.util.Set<String> CAMPOS_FIJADOS_POR_EL_SERVIDOR = java.util.Set.of("rol", "confirmarPassword");

    private static boolean hayErroresDelUsuario(BindingResult result) {
        return result.hasGlobalErrors() || result.getFieldErrors().stream()
                .anyMatch(e -> !CAMPOS_FIJADOS_POR_EL_SERVIDOR.contains(e.getField()));
    }

    private Veterinaria miVeterinaria(VetlinkUserPrincipal principal) {
        if (principal.getUsuario().getRecepcionista() == null) {
            throw new AccessDeniedException("Solo el personal de recepción administra la información de la veterinaria.");
        }
        return principal.getUsuario().getRecepcionista().getVeterinaria();
    }

    private void exigirRecepcionistaPrincipal(VetlinkUserPrincipal principal) {
        if (principal.getUsuario().getRecepcionista() == null
                || !Boolean.TRUE.equals(principal.getUsuario().getRecepcionista().getEsPrincipal())) {
            throw new AccessDeniedException("Solo la recepcionista principal puede administrar la veterinaria.");
        }
    }
}
