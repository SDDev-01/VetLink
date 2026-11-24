package com.vetlink.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    /** VL-20: el correo es el usuario de login; si cambia, se cierra la sesión. */
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingresa un correo válido, por ejemplo nombre@dominio.com")
    @Size(max = 45, message = "El correo no puede superar 45 caracteres")
    private String correo;

    /** VL-20: obligatoria solo para cambiar la contraseña. */
    private String passwordActual;

    /** Opcional: si se deja en blanco no se cambia la contraseña actual. Mínimo 8 caracteres. */
    private String nuevaPassword;

    public boolean cambiaPassword() {
        return nuevaPassword != null && !nuevaPassword.isBlank();
    }
}
