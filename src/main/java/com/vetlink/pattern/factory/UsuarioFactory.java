package com.vetlink.pattern.factory;

import com.vetlink.usuario.dto.RegistroUsuarioForm;
import com.vetlink.usuario.model.*;
import com.vetlink.veterinaria.model.Veterinaria;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Patron de diseño GoF: FACTORY METHOD.
 *
 * Crear un Usuario en VetLink no es solo insertar una fila en `usuario`:
 * segun el rol elegido hay que crear ademas su fila en tutor, veterinario
 * o recepcionista (herencia 1-a-1 tal como esta modelada en mydb.sql), con
 * campos distintos para cada una. En vez de repartir esa logica de "if rol
 * == X entonces..." por controladores y servicios, se concentra aqui: cada
 * metodo crear* sabe construir el agregado Usuario completo para su rol.
 */
@Component
public class UsuarioFactory {

    private final PasswordEncoder passwordEncoder;

    public UsuarioFactory(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    private Usuario base(RegistroUsuarioForm form, TipoDocumento tipoDocumento) {
        Usuario u = new Usuario();
        u.setNumeroDocumento(form.getNumeroDocumento());
        u.setTipoDocumento(tipoDocumento);
        u.setNombre(form.getNombre());
        u.setApellido(form.getApellido());
        u.setPassword(passwordEncoder.encode(form.getPassword()));
        u.setEstado((byte) 1);
        return u;
    }

    public Usuario crearTutor(RegistroUsuarioForm form, TipoDocumento tipoDocumento) {
        Usuario usuario = base(form, tipoDocumento);
        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);
        tutor.setDireccion(form.getDireccion());
        usuario.setTutor(tutor);
        return usuario;
    }

    public Usuario crearVeterinario(RegistroUsuarioForm form, TipoDocumento tipoDocumento,
                                     EspecialidadVeterinario especialidad, Veterinaria veterinaria) {
        Usuario usuario = base(form, tipoDocumento);
        Veterinario veterinario = new Veterinario();
        veterinario.setUsuario(usuario);
        veterinario.setLicencia(form.getLicencia());
        veterinario.setEspecialidad(especialidad);
        veterinario.setVeterinaria(veterinaria);
        usuario.setVeterinario(veterinario);
        return usuario;
    }

    public Usuario crearRecepcionista(RegistroUsuarioForm form, TipoDocumento tipoDocumento, Veterinaria veterinaria) {
        Usuario usuario = base(form, tipoDocumento);
        Recepcionista recepcionista = new Recepcionista();
        recepcionista.setUsuario(usuario);
        recepcionista.setTurno(form.getTurno());
        recepcionista.setFechaContratacion(java.time.LocalDate.now());
        recepcionista.setVeterinaria(veterinaria);
        usuario.setRecepcionista(recepcionista);
        return usuario;
    }
}
