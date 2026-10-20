package com.vetlink.usuario.controller;

import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.dto.PerfilForm;
import com.vetlink.usuario.model.Rol;
import com.vetlink.usuario.repository.CorreoRepository;
import com.vetlink.usuario.repository.TelefonoRepository;
import com.vetlink.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PerfilController {

    private final UsuarioService usuarioService;
    private final CorreoRepository correoRepository;
    private final TelefonoRepository telefonoRepository;

    public PerfilController(UsuarioService usuarioService, CorreoRepository correoRepository, TelefonoRepository telefonoRepository) {
        this.usuarioService = usuarioService;
        this.correoRepository = correoRepository;
        this.telefonoRepository = telefonoRepository;
    }

    @GetMapping("/perfil")
    public String ver(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        var usuario = principal.getUsuario();
        cargarModeloComun(model, usuario);

        PerfilForm form = new PerfilForm();
        form.setNombre(usuario.getNombre());
        form.setApellido(usuario.getApellido());
        var telefonos = telefonoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
        form.setTelefono(telefonos.isEmpty() ? "" : telefonos.get(0).getNumeroTelefono());
        model.addAttribute("form", form);

        var correos = correoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
        model.addAttribute("correo", correos.isEmpty() ? "" : correos.get(0).getCorreo());

        return "perfil/ver";
    }

    @PostMapping("/perfil")
    public String actualizar(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                              @Valid @ModelAttribute("form") PerfilForm form, BindingResult result, Model model) {
        var usuario = principal.getUsuario();
        if (result.hasErrors()) {
            cargarModeloComun(model, usuario);
            var correos = correoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
            model.addAttribute("correo", correos.isEmpty() ? "" : correos.get(0).getCorreo());
            return "perfil/ver";
        }
        usuarioService.actualizarPerfil(usuario.getIdUsuario(), form);
        return "redirect:/perfil?actualizado";
    }

    @PostMapping("/perfil/eliminar")
    public String eliminarCuenta(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                                 @RequestParam String passwordActual, HttpServletRequest request) {
        usuarioService.desactivarCuenta(principal.getUsuario().getIdUsuario(), passwordActual);
        try { request.logout(); } catch (jakarta.servlet.ServletException e) { throw new IllegalStateException(e); }
        return "redirect:/login?cuentaEliminada";
    }

    private void cargarModeloComun(Model model, com.vetlink.usuario.model.Usuario usuario) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("seccionActiva", "perfil");
        Rol rol = usuario.getRol();
        model.addAttribute("navItems", switch (rol) {
            case TUTOR -> DashboardController.navTutor();
            case VETERINARIO -> DashboardController.navVeterinario();
            case RECEPCIONISTA -> DashboardController.navRecepcionista();
        });
    }
}
