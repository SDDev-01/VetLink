package com.vetlink.usuario.service.impl;

import com.vetlink.usuario.dto.RegistroUsuarioForm;
import com.vetlink.usuario.model.*;
import com.vetlink.mascota.dto.MascotaForm;
import com.vetlink.mascota.service.MascotaService;
import com.vetlink.pattern.factory.UsuarioFactory;
import com.vetlink.usuario.repository.*;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import com.vetlink.veterinaria.service.VeterinariaRegistroService;
import com.vetlink.usuario.service.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CorreoRepository correoRepository;
    private final TelefonoRepository telefonoRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final EspecialidadVeterinarioRepository especialidadRepository;
    private final VeterinariaRepository veterinariaRepository;
    private final VeterinariaRegistroService veterinariaRegistroService;
    private final MascotaService mascotaService;
    private final UsuarioFactory usuarioFactory;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, CorreoRepository correoRepository,
                               TelefonoRepository telefonoRepository, TipoDocumentoRepository tipoDocumentoRepository,
                               EspecialidadVeterinarioRepository especialidadRepository,
                               VeterinariaRepository veterinariaRepository, VeterinariaRegistroService veterinariaRegistroService,
                               MascotaService mascotaService, UsuarioFactory usuarioFactory,
                               org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.correoRepository = correoRepository;
        this.telefonoRepository = telefonoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.especialidadRepository = especialidadRepository;
        this.veterinariaRepository = veterinariaRepository;
        this.veterinariaRegistroService = veterinariaRegistroService;
        this.mascotaService = mascotaService;
        this.usuarioFactory = usuarioFactory;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Usuario actualizarPerfil(Integer idUsuario, com.vetlink.usuario.dto.PerfilForm form) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setNombre(form.getNombre());
        usuario.setApellido(form.getApellido());
        // VL-20: cambiar la contraseña exige la actual y al menos 8 caracteres
        // (el controlador ya lo valida por campo; aquí se repite como defensa).
        if (form.cambiaPassword()) {
            if (form.getNuevaPassword().length() < 8) {
                throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres");
            }
            if (form.getPasswordActual() == null || !passwordEncoder.matches(form.getPasswordActual(), usuario.getPassword())) {
                throw new IllegalArgumentException("La contraseña actual no es válida");
            }
            usuario.setPassword(passwordEncoder.encode(form.getNuevaPassword()));
        }
        usuarioRepository.save(usuario);

        // VL-20: si no tenía teléfono, se crea la fila.
        var telefonos = telefonoRepository.findByUsuario_IdUsuario(idUsuario);
        Telefono telefono = telefonos.isEmpty() ? new Telefono() : telefonos.get(0);
        telefono.setUsuario(usuario);
        telefono.setNumeroTelefono(form.getTelefono());
        telefonoRepository.save(telefono);

        // VL-20: cambio de correo (el de login).
        String nuevoCorreo = form.getCorreo() == null ? null : form.getCorreo().trim();
        if (nuevoCorreo != null && !nuevoCorreo.isEmpty()) {
            var correos = correoRepository.findByUsuario_IdUsuario(idUsuario);
            Correo correo = correos.isEmpty() ? new Correo() : correos.get(0);
            if (!nuevoCorreo.equalsIgnoreCase(correo.getCorreo())) {
                if (correoRepository.existsByCorreoIgnoreCase(nuevoCorreo)) {
                    throw new IllegalArgumentException("Ese correo ya está en uso por otra cuenta");
                }
                correo.setUsuario(usuario);
                correo.setCorreo(nuevoCorreo);
                correoRepository.save(correo);
            }
        }
        return usuario;
    }

    @Override
    public boolean passwordCoincide(Integer idUsuario, String password) {
        return password != null && usuarioRepository.findById(idUsuario)
                .map(u -> passwordEncoder.matches(password, u.getPassword()))
                .orElse(false);
    }

    @Override
    public String correoPrincipal(Integer idUsuario) {
        var correos = correoRepository.findByUsuario_IdUsuario(idUsuario);
        return correos.isEmpty() ? "" : correos.get(0).getCorreo();
    }

    @Override
    @Transactional
    public void desactivarCuenta(Integer idUsuario, String passwordActual) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (passwordActual == null || !passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es válida");
        }
        // Baja lógica: conserva la historia médica y citas necesarias para trazabilidad.
        usuario.setEstado((byte) 0);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario registrar(RegistroUsuarioForm form) {
        if (!form.passwordsCoinciden()) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        if (correoEnUso(form.getCorreo())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        }
        if (documentoEnUso(form.getNumeroDocumento())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese numero de documento");
        }

        TipoDocumento tipoDocumento = tipoDocumentoRepository.findById(form.getIdTipoDocumento())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de documento invalido"));

        Usuario usuario;
        Rol rol = normalizarRol(form.getRol());
        switch (rol) {
            case TUTOR -> usuario = usuarioFactory.crearTutor(form, tipoDocumento);
            case VETERINARIO -> {
                EspecialidadVeterinario especialidad = form.getIdEspecialidad() != null
                        ? especialidadRepository.findById(form.getIdEspecialidad()).orElse(null) : null;
                Veterinaria veterinaria = veterinariaRepository.findById(form.getIdVeterinaria())
                        .orElseThrow(() -> new IllegalArgumentException("Selecciona una veterinaria valida"));
                usuario = usuarioFactory.crearVeterinario(form, tipoDocumento, especialidad, veterinaria);
            }
            case RECEPCIONISTA -> {
                Veterinaria veterinaria;
                if (form.getNuevaVeterinaria() != null) {
                    // Paso "Soy Veterinaria" del wizard: se crea la clinica desde cero
                    // y quien se registra queda como su recepcionista administradora.
                    if (form.getTurno() == null || form.getTurno().isBlank()) {
                        form.setTurno("MAÑANA");
                    }
                    veterinaria = veterinariaRegistroService.registrarVeterinariaCompleta(form.getNuevaVeterinaria());
                } else {
                    veterinaria = veterinariaRepository.findById(form.getIdVeterinariaRecepcion())
                            .orElseThrow(() -> new IllegalArgumentException("Selecciona una veterinaria valida"));
                }
                usuario = usuarioFactory.crearRecepcionista(form, tipoDocumento, veterinaria);
                // VL-08: quien registra una clínica nueva queda como su administradora.
                // Las recepcionistas creadas desde el panel siguen con esPrincipal = false.
                if (form.getNuevaVeterinaria() != null) {
                    usuario.getRecepcionista().setEsPrincipal(true);
                }
            }
            default -> throw new IllegalArgumentException("Rol invalido");
        }

        usuario = usuarioRepository.save(usuario);

        Correo correo = new Correo();
        correo.setUsuario(usuario);
        correo.setCorreo(form.getCorreo());
        correoRepository.save(correo);

        Telefono telefono = new Telefono();
        telefono.setUsuario(usuario);
        telefono.setNumeroTelefono(form.getTelefono());
        telefonoRepository.save(telefono);

        if (rol == Rol.TUTOR && form.tieneMascotaInicial()) {
            MascotaForm mascotaForm = new MascotaForm();
            mascotaForm.setNombre(form.getMascotaNombre());
            mascotaForm.setIdEspecie(form.getMascotaIdEspecie());
            mascotaForm.setIdRaza(form.getMascotaIdRaza());
            mascotaForm.setGenero(form.getMascotaGenero());
            mascotaForm.setFechaNacimiento(form.getMascotaFechaNacimiento());
            mascotaForm.setPeso(form.getMascotaPeso());
            mascotaService.crear(mascotaForm, usuario.getIdUsuario());
        }

        return usuario;
    }

    private Rol normalizarRol(String rol) {
        if ("DUEÑO".equalsIgnoreCase(rol)) {
            return Rol.TUTOR;
        }
        if ("VETERINARIA".equalsIgnoreCase(rol)) {
            return Rol.RECEPCIONISTA;
        }
        try {
            return Rol.valueOf(rol.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Rol invalido");
        }
    }

    @Override
    public boolean correoEnUso(String correo) {
        return correoRepository.existsByCorreoIgnoreCase(correo);
    }

    @Override
    public boolean documentoEnUso(String numeroDocumento) {
        return usuarioRepository.existsByNumeroDocumento(numeroDocumento);
    }
}
