package com.vetlink.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerfilForm {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;
    @NotBlank(message = "El telefono es obligatorio")
    private String telefono;
    /** Opcional: si se deja en blanco no se cambia la contraseña actual. */
    private String nuevaPassword;
}
