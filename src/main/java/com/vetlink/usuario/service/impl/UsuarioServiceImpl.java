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
        if (form.getNuevaPassword() != null && !form.getNuevaPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(form.getNuevaPassword()));
        }
        usuarioRepository.save(usuario);

        var telefonos = telefonoRepository.findByUsuario_IdUsuario(idUsuario);
        if (!telefonos.isEmpty()) {
            var telefono = telefonos.get(0);
            telefono.setNumeroTelefono(form.getTelefono());
            telefonoRepository.save(telefono);
        }
        return usuario;
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
