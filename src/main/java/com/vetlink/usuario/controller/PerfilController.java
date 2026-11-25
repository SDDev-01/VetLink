package com.vetlink.usuario.controller;

import com.vetlink.disponibilidad.service.SolicitudDisponibilidadService;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.dto.PerfilForm;
import com.vetlink.usuario.model.Rol;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.CorreoRepository;
import com.vetlink.usuario.repository.TelefonoRepository;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.usuario.service.UsuarioService;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.service.VeterinariaInfoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * «Mi perfil» de todos los roles (GEN-02): vista de consulta con un modo edición explícito.
 *
 * <p>DUE-08 / VET-04: la causa de que «no se guardara» era que la vista y la barra superior
 * leían el {@code Usuario} guardado en la sesión al iniciar sesión, no el de la base de datos:
 * el cambio sí llegaba a MySQL, pero se seguía mostrando el valor viejo hasta volver a entrar.
 * Ahora la vista se arma con los datos frescos de la base de datos y, tras guardar, se
 * actualiza también el usuario de la sesión.</p>
 */
@Controller
public class PerfilController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final CorreoRepository correoRepository;
    private final TelefonoRepository telefonoRepository;
    private final MascotaRepository mascotaRepository;
    private final VeterinariaInfoService infoVeterinaria;
    private final SolicitudDisponibilidadService disponibilidad;

    public PerfilController(UsuarioService usuarioService, UsuarioRepository usuarioRepository,
                            CorreoRepository correoRepository, TelefonoRepository telefonoRepository,
                            MascotaRepository mascotaRepository, VeterinariaInfoService infoVeterinaria,
                            SolicitudDisponibilidadService disponibilidad) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.correoRepository = correoRepository;
        this.telefonoRepository = telefonoRepository;
        this.mascotaRepository = mascotaRepository;
        this.infoVeterinaria = infoVeterinaria;
        this.disponibilidad = disponibilidad;
    }

    @GetMapping("/perfil")
    @Transactional(readOnly = true)
    public String ver(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        Usuario usuario = fresco(principal);
        model.addAttribute("form", formDesde(usuario));
        cargarModelo(model, principal, usuario, false);
        return "perfil/ver";
    }

    @PostMapping("/perfil")
    @Transactional
    public String actualizar(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                             @Valid @ModelAttribute("form") PerfilForm form, BindingResult result, Model model,
                             HttpServletRequest request) {
        Usuario usuario = fresco(principal);
        Integer id = usuario.getIdUsuario();
        String correoActual = usuarioService.correoPrincipal(id);

        // VL-20: reglas de contraseña y correo, con el error en su campo.
        if (form.cambiaPassword()) {
            if (form.getNuevaPassword().length() < 8) {
                result.rejectValue("nuevaPassword", "corta", "La nueva contraseña debe tener al menos 8 caracteres");
            }
            if (!usuarioService.passwordCoincide(id, form.getPasswordActual())) {
                result.rejectValue("passwordActual", "incorrecta", "La contraseña actual no es correcta");
            }
        }
        boolean cambiaCorreo = form.getCorreo() != null && !form.getCorreo().equalsIgnoreCase(correoActual);
        if (cambiaCorreo && !result.hasFieldErrors("correo") && correoRepository.existsByCorreoIgnoreCase(form.getCorreo())) {
            result.rejectValue("correo", "enUso", "Ese correo ya está en uso por otra cuenta");
        }
        // telefono.numeroTelefono es UNIQUE: antes un número repetido terminaba en error 500.
        if (!result.hasFieldErrors("telefono") && form.getTelefono() != null
                && telefonoRepository.existsByNumeroTelefonoAndUsuario_IdUsuarioNot(form.getTelefono(), id)) {
            result.rejectValue("telefono", "enUso", "Ese teléfono ya está registrado en otra cuenta");
        }
        // La dirección solo se exige si el formulario la envía (si no llega, se conserva la guardada).
        if (usuario.getRol() == Rol.TUTOR && form.getDireccion() != null && form.getDireccion().isBlank()) {
            result.rejectValue("direccion", "requerida", "La dirección es obligatoria");
        }

        if (result.hasErrors()) {
            cargarModelo(model, principal, usuario, true);
            return "perfil/ver";
        }
        Usuario guardado;
        try {
            guardado = usuarioService.actualizarPerfil(id, form);
        } catch (IllegalArgumentException e) {
            cargarModelo(model, principal, usuario, true);
            model.addAttribute("errorGeneral", e.getMessage());
            return "perfil/ver";
        }
        // La sesión guarda una copia del usuario: se actualiza para que la barra superior y
        // las demás vistas muestren el cambio de inmediato (no solo tras volver a iniciar sesión).
        Usuario enSesion = principal.getUsuario();
        enSesion.setNombre(guardado.getNombre());
        enSesion.setApellido(guardado.getApellido());
        if (enSesion.getTutor() != null && guardado.getTutor() != null) {
            enSesion.getTutor().setDireccion(guardado.getTutor().getDireccion());
        }

        // El correo es el usuario de login, y la contraseña la credencial: si cambian, se inicia sesión de nuevo.
        if (cambiaCorreo || form.cambiaPassword()) {
            try { request.logout(); } catch (jakarta.servlet.ServletException e) { throw new IllegalStateException(e); }
            return cambiaCorreo ? "redirect:/login?correoActualizado" : "redirect:/login?passwordActualizada";
        }
        return "redirect:/perfil?actualizado";
    }

    @PostMapping("/perfil/eliminar")
    public String eliminarCuenta(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                                 @RequestParam String passwordActual, HttpServletRequest request) {
        usuarioService.desactivarCuenta(principal.getUsuario().getIdUsuario(), passwordActual);
        try { request.logout(); } catch (jakarta.servlet.ServletException e) { throw new IllegalStateException(e); }
        return "redirect:/login?cuentaEliminada";
    }

    private Usuario fresco(VetlinkUserPrincipal principal) {
        return usuarioRepository.findById(principal.getUsuario().getIdUsuario())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
    }

    private PerfilForm formDesde(Usuario usuario) {
        PerfilForm form = new PerfilForm();
        form.setNombre(usuario.getNombre());
        form.setApellido(usuario.getApellido());
        var telefonos = telefonoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
        form.setTelefono(telefonos.isEmpty() ? "" : telefonos.get(0).getNumeroTelefono());
        form.setCorreo(usuarioService.correoPrincipal(usuario.getIdUsuario()));
        if (usuario.getTutor() != null) form.setDireccion(usuario.getTutor().getDireccion());
        return form;
    }

    /** Datos de consulta (siempre visibles) según el rol; los institucionales no se editan aquí. */
    private void cargarModelo(Model model, VetlinkUserPrincipal principal, Usuario usuario, boolean modoEdicion) {
        Integer id = usuario.getIdUsuario();
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("perfil", usuario);
        model.addAttribute("seccionActiva", "perfil");
        model.addAttribute("navItems", DashboardController.navPara(principal.getUsuario()));
        model.addAttribute("modoEdicion", modoEdicion);
        model.addAttribute("correo", usuarioService.correoPrincipal(id));
        var telefonos = telefonoRepository.findByUsuario_IdUsuario(id);
        model.addAttribute("telefono", telefonos.isEmpty() ? null : telefonos.get(0).getNumeroTelefono());
        Rol rol = usuario.getRol();
        model.addAttribute("rolEtiqueta", switch (rol) {
            case TUTOR -> "Dueño de mascota";
            case VETERINARIO -> "Veterinario(a)";
            case RECEPCIONISTA -> "Recepcionista";
        });
        if (rol == Rol.TUTOR) {
            model.addAttribute("totalMascotas", mascotaRepository.findByTutor_IdUsuarioAndEstado(id, (byte) 1).size());
        }
        Veterinaria clinica = null;
        if (rol == Rol.VETERINARIO && usuario.getVeterinario() != null) {
            clinica = usuario.getVeterinario().getVeterinaria();
            model.addAttribute("serviciosAutorizados", disponibilidad.serviciosAprobados(id));
            model.addAttribute("horarioAprobado", disponibilidad.horarioAprobado(id));
        } else if (rol == Rol.RECEPCIONISTA && usuario.getRecepcionista() != null) {
            clinica = usuario.getRecepcionista().getVeterinaria();
        }
        // VET-05: vinculación institucional obtenida de la base de datos (el modelo admite una clínica por profesional).
        if (clinica != null) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("nombre", clinica.getNombre());
            c.put("nit", clinica.getNit());
            c.put("direccion", infoVeterinaria.direccion(clinica.getIdVeterinaria()));
            c.put("telefonos", infoVeterinaria.telefonos(clinica.getIdVeterinaria()));
            c.put("correos", infoVeterinaria.correos(clinica.getIdVeterinaria()));
            c.put("web", clinica.getPaginaWeb());
            c.put("logo", VeterinariaInfoService.logoUrl(clinica));
            c.put("activa", clinica.isActiva());
            model.addAttribute("clinica", c);
        }
    }
}
