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

    public VeterinariaAdminController(VeterinariaService veterinariaService, UsuarioService usuarioService,
                                       VeterinarioRepository veterinarioRepository,
                                       TipoDocumentoRepository tipoDocumentoRepository,
                                       EspecialidadVeterinarioRepository especialidadVeterinarioRepository,
                                       RecepcionistaRepository recepcionistaRepository) {
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
        model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll());
        model.addAttribute("especialidades", especialidadVeterinarioRepository.findAll());
        return "veterinarias/admin";
    }

    @PostMapping("/info")
    public String actualizarInfo(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                                  @RequestParam String nombre, @RequestParam(required = false) String descripcion,
                                   @RequestParam(required = false) String paginaWeb, @RequestParam(required = false) String logo,
                                   RedirectAttributes redirectAttributes) {
        exigirRecepcionistaPrincipal(principal);
        Veterinaria veterinaria = miVeterinaria(principal);
        try {
            veterinariaService.actualizarInfo(veterinaria.getIdVeterinaria(), principal.getUsuario().getIdUsuario(),
                    nombre, descripcion, paginaWeb, logo);
        } catch (RuntimeException e) {
            // VL-21: el mensaje va como atributo flash, no en la URL (tildes, espacios o & lo cortaban).
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/veterinarias/admin";
        }
        return "redirect:/veterinarias/admin?actualizada";
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

        if (hayErroresDelUsuario(result) || (form.getLicencia() == null || form.getLicencia().isBlank())) {
            if (form.getLicencia() == null || form.getLicencia().isBlank()) {
                result.rejectValue("licencia", "licencia.requerida", "La licencia profesional es obligatoria");
            }
            cargarShell(model, principal);
            model.addAttribute("veterinaria", veterinaria);
            model.addAttribute("equipo", veterinarioRepository.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()));
            model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll());
            model.addAttribute("especialidades", especialidadVeterinarioRepository.findAll());
            return "veterinarias/admin";
        }

        try {
            usuarioService.registrar(form);
        } catch (RuntimeException e) {
            cargarShell(model, principal);
            model.addAttribute("veterinaria", veterinaria);
            model.addAttribute("equipo", veterinarioRepository.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()));
            model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll());
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
        model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll());
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
