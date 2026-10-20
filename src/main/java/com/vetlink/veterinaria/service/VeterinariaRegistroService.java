package com.vetlink.veterinaria.service;

import com.vetlink.veterinaria.dto.VeterinariaRegistroForm;
import com.vetlink.veterinaria.model.Veterinaria;

public interface VeterinariaRegistroService {
    /** Crea la Veterinaria, su ubicacion, servicios y horarios. No crea el usuario recepcionista (eso lo hace UsuarioService). */
    Veterinaria registrarVeterinariaCompleta(VeterinariaRegistroForm form);
}
