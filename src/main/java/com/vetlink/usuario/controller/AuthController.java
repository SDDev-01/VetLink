package com.vetlink.usuario.controller;

import com.vetlink.mascota.repository.EspecieRepository;
import com.vetlink.ubicacion.repository.CiudadRepository;
import com.vetlink.usuario.dto.RegistroUsuarioForm;
import com.vetlink.usuario.repository.EspecialidadVeterinarioRepository;
import com.vetlink.usuario.repository.TipoDocumentoRepository;
import com.vetlink.veterinaria.repository.DiaSemanaRepository;
import com.vetlink.veterinaria.repository.ServicioRepository;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import com.vetlink.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * Login lo maneja Spring Security directamente (ver SecurityConfig,
 * loginProcessingUrl=/login); este controlador solo pinta la vista y
 * procesa el registro del wizard de 2 caminos (Dueño / Veterinaria nueva).
 */
@Controller
public class AuthController {

    private final UsuarioService usuarioService;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final VeterinariaRepository veterinariaRepository;
    private final EspecialidadVeterinarioRepository especialidadRepository;
    private final EspecieRepository especieRepository;
    private final CiudadRepository ciudadRepository;
    private final ServicioRepository servicioRepository;
    private final DiaSemanaRepository diaSemanaRepository;
    private final Validator validator;

    public AuthController(UsuarioService usuarioService, TipoDocumentoRepository tipoDocumentoRepository,
                           VeterinariaRepository veterinariaRepository, EspecialidadVeterinarioRepository especialidadRepository,
                           EspecieRepository especieRepository, CiudadRepository ciudadRepository,
                           ServicioRepository servicioRepository, DiaSemanaRepository diaSemanaRepository,
                           Validator validator) {
        this.usuarioService = usuarioService;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.veterinariaRepository = veterinariaRepository;
        this.especialidadRepository = especialidadRepository;
        this.especieRepository = especieRepository;
        this.ciudadRepository = ciudadRepository;
        this.servicioRepository = servicioRepository;
        this.diaSemanaRepository = diaSemanaRepository;
        this.validator = validator;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/registro")
    public String registroForm(Model model) {
        cargarCatalogos(model);

        var diasOrdenados = diaSemanaRepository.findAll(org.springframework.data.domain.Sort.by("idDiaSemana"));
        model.addAttribute("diasSemana", diasOrdenados);

        RegistroUsuarioForm form = new RegistroUsuarioForm();
        com.vetlink.veterinaria.dto.VeterinariaRegistroForm nuevaVeterinaria = new com.vetlink.veterinaria.dto.VeterinariaRegistroForm();
        for (var dia : diasOrdenados) {
            var horario = new com.vetlink.veterinaria.dto.VeterinariaRegistroForm.HorarioDiaForm();
            horario.setIdDiaSemana(dia.getIdDiaSemana());
            nuevaVeterinaria.getHorarios().add(horario);
        }
        var primerServicio = new com.vetlink.veterinaria.dto.VeterinariaRegistroForm.ServicioOfrecidoForm();
        nuevaVeterinaria.getServicios().add(primerServicio);
        form.setNuevaVeterinaria(nuevaVeterinaria);

        model.addAttribute("form", form);
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("form") RegistroUsuarioForm form, BindingResult result, Model model) {
        // VL-01: el registro público solo crea dueños o clínicas nuevas. Unirse a una
        // clínica existente como veterinario o recepcionista se hace desde su panel.
        String rol = form.getRol() == null ? "" : form.getRol().trim().toUpperCase();
        boolean esDuenio = rol.equals("TUTOR") || rol.equals("DUEÑO");
        boolean esClinicaNueva = (rol.equals("VETERINARIA") || rol.equals("RECEPCIONISTA"))
                && form.getNuevaVeterinaria() != null;
        if (!esDuenio && !esClinicaNueva) {
            throw new AccessDeniedException("Tipo de registro no permitido");
        }
        form.setIdVeterinaria(null);
        form.setIdVeterinariaRecepcion(null);

        if (!form.passwordsCoinciden()) {
            result.rejectValue("confirmarPassword", "mismatch", "Las contraseñas no coinciden");
        }
        validarVeterinariaSiCorresponde(form, result);
        if (result.hasErrors()) {
            cargarCatalogos(model);
            return "auth/registro";
        }
        try {
            usuarioService.registrar(form);
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorGeneral", e.getMessage());
            cargarCatalogos(model);
            return "auth/registro";
        }
        boolean esVeterinaria = ("RECEPCIONISTA".equals(form.getRol()) || "VETERINARIA".equals(form.getRol()))
                && form.getNuevaVeterinaria() != null;
        return "redirect:/login?registrado" + (esVeterinaria ? "Vet" : "");
    }

    /**
     * VL-01: quien se registra es anónimo, y Spring Security convierte un
     * AccessDeniedException de un anónimo en redirección a /login. Aquí se
     * responde 403 con la página de acceso denegado.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String registroNoPermitido(AccessDeniedException ex, Model model) {
        model.addAttribute("mensaje", ex.getMessage());
        return "error/403";
    }

    private void validarVeterinariaSiCorresponde(RegistroUsuarioForm form, BindingResult result) {
        boolean registraVeterinaria = ("RECEPCIONISTA".equals(form.getRol()) || "VETERINARIA".equals(form.getRol()))
                && form.getNuevaVeterinaria() != null;
        if (!registraVeterinaria) {
            return;
        }

        validator.validate(form.getNuevaVeterinaria()).forEach(error -> {
            String campo = error.getPropertyPath().toString();
            result.rejectValue("nuevaVeterinaria." + campo, error.getMessageTemplate(), error.getMessage());
        });
    }

    private void cargarCatalogos(Model model) {
        model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll());
        model.addAttribute("veterinarias", veterinariaRepository.findByEstado((byte) 1));
        model.addAttribute("especialidades", especialidadRepository.findAll());
        model.addAttribute("especies", especieRepository.findAll());
        model.addAttribute("ciudades", ciudadRepository.findAll());
        model.addAttribute("servicios", servicioRepository.findAll());
        model.addAttribute("diasSemana", diaSemanaRepository.findAll());
    }
}
