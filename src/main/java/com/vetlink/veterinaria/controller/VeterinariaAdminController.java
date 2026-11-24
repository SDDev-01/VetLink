package com.vetlink.veterinaria.controller;

import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.usuario.dto.RegistroUsuarioForm;
import com.vetlink.usuario.model.EspecialidadVeterinario;
import com.vetlink.usuario.model.TipoDocumento;
import com.vetlink.usuario.repository.EspecialidadVeterinarioRepository;
import com.vetlink.usuario.repository.TipoDocumentoRepository;
import com.vetlink.usuario.repository.VeterinarioRepository;
import com.vetlink.usuario.repository.RecepcionistaRepository;
import com.vetlink.usuario.service.UsuarioService;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.service.VeterinariaService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** RF0010 (editar info de la veterinaria) y registro de veterinarios por parte
 *  de la recepcionista (los veterinarios NO se auto-registran salvo que el
 *  wizard público de /registro lo permita; este es el camino "oficial"). */
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
                                       com.vetlink.auditoria.AuditoriaService auditoria) {
        this.logos = logos;
        this.auditoria = auditoria;
        this.veterinariaService = veterinariaService;
        this.usuarioService = usuarioService;
        this.veterinarioRepository = veterinarioRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.especialidadVeterinarioRepository = especialidadVeterinarioRepository;
        this.recepcionistaRepository = recepcionistaRepository;
    }

    @GetMapping
    public String panel(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        exigirRecepcionistaPrincipal(principal);
        Veterinaria veterinaria = miVeterinaria(principal);
        cargarShell(model, principal);
        model.addAttribute("veterinaria", veterinaria);
        model.addAttribute("equipo", veterinarioRepository.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()));
        model.addAttribute("recepcionistas", recepcionistaRepository.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()));
        model.addAttribute("formEquipo", new RegistroUsuarioForm());
        model.addAttribute("logoUrl", com.vetlink.veterinaria.service.VeterinariaInfoService.logoUrl(veterinaria));
        model.addAttribute("tamanoMaximoImagenMb", 2);
        model.addAttribute("tiposDocumento", tiposDocumentoPersonal());
        model.addAttribute("especialidades", especialidadVeterinarioRepository.findAll());
        return "veterinarias/admin";
    }

    @PostMapping("/info")
    public String actualizarInfo(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                                 @RequestParam(required = false) String nombre, @RequestParam(required = false) String descripcion,
                                 @RequestParam(required = false) String paginaWeb,
                                 RedirectAttributes redirectAttributes) {
        exigirRecepcionistaPrincipal(principal);
        Veterinaria veterinaria = miVeterinaria(principal);
        String error = null;
        String n = nombre == null ? "" : nombre.trim();
        String web = paginaWeb == null ? "" : paginaWeb.trim();
        if (n.isEmpty()) error = "El nombre de la veterinaria es obligatorio";
        else if (n.length() > 50) error = "El nombre no puede superar 50 caracteres";
        else if (!web.isEmpty() && (web.length() > 50 || !WEB.matcher(web).matches())) error = "Ingresa una página web válida (máx. 50 caracteres)";
        else if (descripcion != null && descripcion.length() > 2000) error = "La descripción no puede superar 2000 caracteres";
        if (error != null) {
            redirectAttributes.addFlashAttribute("error", error);
            return "redirect:/veterinarias/admin";
        }
        try {
            veterinariaService.actualizarInfo(veterinaria.getIdVeterinaria(), principal.getUsuario().getIdUsuario(),
                    n, descripcion == null ? null : descripcion.trim(), web.isEmpty() ? null : web, null);
            auditoria.registrar(principal.getUsuario().getIdUsuario(), "VETERINARIA_EDITAR", "veterinaria",
                    veterinaria.getIdVeterinaria(), null);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
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

    private java.util.List<com.vetlink.usuario.model.TipoDocumento> tiposDocumentoPersonal() {
        return tipoDocumentoRepository.findAll().stream()
                .filter(t -> !TIPO_NO_PERMITIDO_PERSONAL.equalsIgnoreCase(t.getNombreTipo()))
                .toList();
    }

    private boolean esTipoNoPermitido(Integer idTipoDocumento) {
        return idTipoDocumento != null && tipoDocumentoRepository.findById(idTipoDocumento)
                .map(t -> TIPO_NO_PERMITIDO_PERSONAL.equalsIgnoreCase(t.getNombreTipo())).orElse(false);
    }

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
        if (hayErroresDelUsuario(result) || (form.getLicencia() == null || form.getLicencia().isBlank())) {
            if (form.getLicencia() == null || form.getLicencia().isBlank()) {
                result.rejectValue("licencia", "licencia.requerida", "La licencia profesional es obligatoria");
            }
            cargarShell(model, principal);
            model.addAttribute("veterinaria", veterinaria);
            model.addAttribute("equipo", veterinarioRepository.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()));
            model.addAttribute("tiposDocumento", tiposDocumentoPersonal());
            model.addAttribute("especialidades", especialidadVeterinarioRepository.findAll());
            return "veterinarias/admin";
        }

        try {
            usuarioService.registrar(form);
        } catch (RuntimeException e) {
            cargarShell(model, principal);
            model.addAttribute("veterinaria", veterinaria);
            model.addAttribute("equipo", veterinarioRepository.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()));
            model.addAttribute("tiposDocumento", tiposDocumentoPersonal());
            model.addAttribute("especialidades", especialidadVeterinarioRepository.findAll());
            model.addAttribute("errorEquipo", e.getMessage());
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
        form.setRol("RECEPCIONISTA"); form.setIdVeterinariaRecepcion(veterinaria.getIdVeterinaria());
        // VL-08: desde el panel nunca se crea una clínica nueva, así la recepcionista queda con esPrincipal = false.
        form.setNuevaVeterinaria(null);
        form.setConfirmarPassword(form.getPassword()); if (form.getTurno() == null || form.getTurno().isBlank()) form.setTurno("MAÑANA");
        if (esTipoNoPermitido(form.getIdTipoDocumento())) {
            result.rejectValue("idTipoDocumento", "tipo.noPermitido", "Selecciona un tipo de documento válido para personal");
        }
        // VL-21: con errores de validación se vuelve a la vista mostrando qué campo falló.
        if (hayErroresDelUsuario(result)) {
            cargarPanelConError(model, principal, veterinaria, "Revisa los datos de la recepcionista.");
            return "veterinarias/admin";
        }
        // VL-21: un correo o documento repetido ya no termina en error 500.
        try {
            usuarioService.registrar(form);
        } catch (RuntimeException e) {
            cargarPanelConError(model, principal, veterinaria, e.getMessage());
            return "veterinarias/admin";
        }
        return "redirect:/veterinarias/admin?recepcionistaCreada";
    }

    /**
     * VL-21: el panel no envía "rol" ni "confirmarPassword" (los fija este controlador),
     * pero RegistroUsuarioForm los marca @NotBlank y la validación corre antes de fijarlos.
     * Sin este filtro, registrar personal desde el panel fallaba siempre.
     */
    private static final java.util.Set<String> CAMPOS_FIJADOS_POR_EL_SERVIDOR = java.util.Set.of("rol", "confirmarPassword");

    private static boolean hayErroresDelUsuario(BindingResult result) {
        return result.hasGlobalErrors() || result.getFieldErrors().stream()
                .anyMatch(e -> !CAMPOS_FIJADOS_POR_EL_SERVIDOR.contains(e.getField()));
    }

    private void cargarPanelConError(Model model, VetlinkUserPrincipal principal, Veterinaria veterinaria, String mensaje) {
        cargarShell(model, principal);
        model.addAttribute("veterinaria", veterinaria);
        model.addAttribute("equipo", veterinarioRepository.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()));
        model.addAttribute("recepcionistas", recepcionistaRepository.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()));
        model.addAttribute("tiposDocumento", tiposDocumentoPersonal());
        model.addAttribute("especialidades", especialidadVeterinarioRepository.findAll());
        model.addAttribute("errorRecepcion", mensaje);
    }

    private Veterinaria miVeterinaria(VetlinkUserPrincipal principal) {
        if (principal.getUsuario().getRecepcionista() == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo el personal de recepción administra la información de la veterinaria.");
        }
        return principal.getUsuario().getRecepcionista().getVeterinaria();
    }

    private void exigirRecepcionistaPrincipal(VetlinkUserPrincipal principal) {
        if (principal.getUsuario().getRecepcionista() == null
                || !Boolean.TRUE.equals(principal.getUsuario().getRecepcionista().getEsPrincipal())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo la recepcionista principal puede administrar la veterinaria.");
        }
    }

    private void cargarShell(Model model, VetlinkUserPrincipal principal) {
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "veterinaria");
        model.addAttribute("navItems", DashboardController.navRecepcionista());
    }
}
