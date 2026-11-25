package com.vetlink.usuario.dto;

import com.vetlink.comun.validacion.Textos;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos personales que el propio usuario puede cambiar (GEN-02). El rol, el documento y las
 * vinculaciones institucionales NO están aquí: no se pueden modificar desde el perfil.
 */
@Getter
@Setter
public class PerfilForm {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede superar 50 caracteres")
    @Pattern(regexp = Textos.REGEX_NOMBRE, message = Textos.MENSAJE_NOMBRE)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 50, message = "El apellido no puede superar 50 caracteres")
    @Pattern(regexp = Textos.REGEX_NOMBRE, message = Textos.MENSAJE_NOMBRE)
    private String apellido;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = Textos.REGEX_TELEFONO, message = Textos.MENSAJE_TELEFONO)
    private String telefono;

    /** VL-20: el correo es el usuario de login; si cambia, se cierra la sesión. */
    @NotBlank(message = "El correo es obligatorio")
    @Email(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$", message = "Ingresa un correo válido, por ejemplo nombre@dominio.com")
    @Size(max = 45, message = "El correo no puede superar 45 caracteres")
    private String correo;

    /** Solo para dueños (la columna tutor.direccion es obligatoria). Puede llevar números. */
    @Size(max = 45, message = "La dirección no puede superar 45 caracteres")
    private String direccion;

    /** VL-20: obligatoria solo para cambiar la contraseña. */
    private String passwordActual;

    /** Opcional: si se deja en blanco no se cambia la contraseña actual. Mínimo 8 caracteres. */
    private String nuevaPassword;

    public boolean cambiaPassword() {
        return nuevaPassword != null && !nuevaPassword.isBlank();
    }

    public void setNombre(String nombre) {
        this.nombre = Textos.normalizar(nombre);
    }

    public void setApellido(String apellido) {
        this.apellido = Textos.normalizar(apellido);
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono == null ? null : telefono.strip();
    }

    public void setCorreo(String correo) {
        this.correo = correo == null ? null : correo.strip();
    }

    public void setDireccion(String direccion) {
        this.direccion = Textos.normalizar(direccion);
    }
}
