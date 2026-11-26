package com.vetlink.veterinaria.service;

import com.vetlink.veterinaria.dto.VeterinariaRegistroForm;
import com.vetlink.veterinaria.model.Veterinaria;

public interface VeterinariaRegistroService {
    /** Crea la Veterinaria, su ubicacion, servicios y horarios. No crea el usuario recepcionista (eso lo hace UsuarioService). */
    Veterinaria registrarVeterinariaCompleta(VeterinariaRegistroForm form);

    /** ¿Ya hay una veterinaria con ese NIT? (validación inmediata del registro). */
    boolean nitEnUso(String nit);

    /** ¿Ya hay una veterinaria con ese nombre comercial (sin importar mayúsculas ni tildes)? */
    boolean nombreEnUso(String nombre);

    /** ¿Ese correo ya lo usa otra veterinaria? */
    boolean correoEnUso(String correo);
}
