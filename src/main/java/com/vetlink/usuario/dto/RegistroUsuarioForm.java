package com.vetlink.usuario.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/** Formulario unico de registro; segun "rol" se exigen unos campos u otros. */
@Getter
@Setter
public class RegistroUsuarioForm {

    @NotBlank(message = "El numero de documento es obligatorio")
    @Size(max = 45)
    private String numeroDocumento;

    @NotNull(message = "Selecciona un tipo de documento")
    private Integer idTipoDocumento;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 50)
    private String apellido;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingresa un correo válido, por ejemplo nombre@dominio.com")
    private String correo;

    @NotBlank(message = "El telefono es obligatorio")
    @Pattern(regexp = "\\d{7,15}", message = "Ingresa entre 7 y 15 dígitos, sin espacios ni guiones")
    private String telefono;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "Confirma la contraseña")
    private String confirmarPassword;

    @NotBlank(message = "Selecciona un rol")
    private String rol; // TUTOR | VETERINARIO | RECEPCIONISTA

    // --- Solo TUTOR ---
    private String direccion;

    // --- Solo TUTOR: mascota opcional creada en el mismo wizard (paso 3, "omitir" la deja en null) ---
    private String mascotaNombre;
    private Integer mascotaIdEspecie;
    private Integer mascotaIdRaza;
    private String mascotaGenero;
    private java.time.LocalDate mascotaFechaNacimiento;
    private Double mascotaPeso;

    public boolean tieneMascotaInicial() {
        return mascotaNombre != null && !mascotaNombre.isBlank();
    }

    // --- Solo VETERINARIO ---
    private String licencia;
    private Integer idEspecialidad;
    private Integer idVeterinaria;

    // --- Solo RECEPCIONISTA que se une a una veterinaria YA EXISTENTE ---
    private String turno;
    private Integer idVeterinariaRecepcion;

    // --- Solo RECEPCIONISTA que registra una veterinaria NUEVA (paso "Soy Veterinaria" del wizard) ---
    private com.vetlink.veterinaria.dto.VeterinariaRegistroForm nuevaVeterinaria;

    public boolean passwordsCoinciden() {
        return password != null && password.equals(confirmarPassword);
    }
}
