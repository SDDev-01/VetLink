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
    private final com.vetlink.veterinaria.service.VeterinariaRegistroService registroVeterinarias;
    private final com.vetlink.veterinaria.service.CatalogoServiciosService catalogoServicios;
    private final com.vetlink.usuario.repository.TelefonoRepository telefonoRepository;

    public AuthController(UsuarioService usuarioService, TipoDocumentoRepository tipoDocumentoRepository,
                           VeterinariaRepository veterinariaRepository, EspecialidadVeterinarioRepository especialidadRepository,
                           EspecieRepository especieRepository, CiudadRepository ciudadRepository,
                           ServicioRepository servicioRepository, DiaSemanaRepository diaSemanaRepository,
                           Validator validator,
                           com.vetlink.veterinaria.service.VeterinariaRegistroService registroVeterinarias,
                           com.vetlink.veterinaria.service.CatalogoServiciosService catalogoServicios,
                           com.vetlink.usuario.repository.TelefonoRepository telefonoRepository) {
        this.usuarioService = usuarioService;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.veterinariaRepository = veterinariaRepository;
        this.especialidadRepository = especialidadRepository;
        this.especieRepository = especieRepository;
        this.ciudadRepository = ciudadRepository;
        this.servicioRepository = servicioRepository;
        this.diaSemanaRepository = diaSemanaRepository;
        this.validator = validator;
        this.registroVeterinarias = registroVeterinarias;
        this.catalogoServicios = catalogoServicios;
        this.telefonoRepository = telefonoRepository;
    }

    /** Respuesta de la validación inmediata del registro. */
    public record Disponibilidad(boolean disponible, String mensaje) {}

    /**
     * Validación inmediata del registro: indica si un dato ya está registrado, para avisar
     * mientras se llena el formulario. Solo responde sí/no con un mensaje genérico (no devuelve
     * datos de la cuenta existente). El envío del formulario vuelve a validar todo.
     */
    @GetMapping("/api/registro/disponible")
    @org.springframework.web.bind.annotation.ResponseBody
    public Disponibilidad disponible(@RequestParam String campo, @RequestParam String valor) {
        String v = valor == null ? "" : valor.strip();
        if (v.isEmpty() || v.length() > 100) return new Disponibilidad(true, "");
        return switch (campo) {
            case "correo" -> usuarioService.correoEnUso(v)
                    ? new Disponibilidad(false, "Ya existe una cuenta con ese correo") : new Disponibilidad(true, "");
            case "documento" -> usuarioService.documentoEnUso(v)
                    ? new Disponibilidad(false, "Ya existe una cuenta con ese número de documento") : new Disponibilidad(true, "");
            case "telefono" -> telefonoRepository.existsByNumeroTelefono(v)
                    ? new Disponibilidad(false, "Ya existe una cuenta con ese número de teléfono") : new Disponibilidad(true, "");
            case "nit" -> registroVeterinarias.nitEnUso(v)
                    ? new Disponibilidad(false, "Ya existe una veterinaria registrada con ese NIT") : new Disponibilidad(true, "");
            case "nombreVeterinaria" -> registroVeterinarias.nombreEnUso(v)
                    ? new Disponibilidad(false, "Ya existe una veterinaria con ese nombre comercial") : new Disponibilidad(true, "");
            case "correoVeterinaria" -> registroVeterinarias.correoEnUso(v)
                    ? new Disponibilidad(false, "Ese correo ya está registrado por otra veterinaria") : new Disponibilidad(true, "");
            default -> throw new IllegalArgumentException("Campo no válido");
        };
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
        // Cuentas de veterinaria: contraseña de mínimo 8 caracteres, con mayúscula y símbolo
        // (la misma regla que muestra el formulario mientras se escribe).
        if (!com.vetlink.comun.validacion.Textos.esPasswordSegura(form.getPassword())) {
            result.rejectValue("password", "password.debil", com.vetlink.comun.validacion.Textos.MENSAJE_PASSWORD_SEGURA);
        }
    }

    private void cargarCatalogos(Model model) {
        model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll());
        model.addAttribute("veterinarias", veterinariaRepository.findByEstado((byte) 1));
        model.addAttribute("especialidades", especialidadRepository.findAll());
        model.addAttribute("especies", especieRepository.findAll());
        model.addAttribute("ciudades", ciudadRepository.findAll());
        model.addAttribute("servicios", catalogoServicios.catalogo());
        model.addAttribute("catalogoServicios", catalogoServicios.catalogo().stream()
                .map(s -> java.util.Map.of("id", s.getIdServicio(), "nombre", s.getNombreServicio())).toList());
        model.addAttribute("diasSemana", diaSemanaRepository.findAll());
    }
}
