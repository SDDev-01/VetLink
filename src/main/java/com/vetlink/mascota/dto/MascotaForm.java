package com.vetlink.mascota.dto;

import com.vetlink.comun.validacion.Textos;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class MascotaForm {

    /**
     * DUE-01: datos que no cambian después del registro. En la edición no se muestran como
     * campos editables y el servidor los ignora aunque lleguen en la petición.
     */
    public static final Set<String> CAMPOS_INMUTABLES = Set.of("idEspecie", "genero", "fechaNacimiento");

    private Integer idMascota;

    /** DUE-03: letras, tildes, ñ, espacios y separadores; sin números. Se recorta antes de validar. */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 45, message = "El nombre no puede superar 45 caracteres")
    @Pattern(regexp = Textos.REGEX_NOMBRE, message = Textos.MENSAJE_NOMBRE)
    private String nombre;

    @NotNull(message = "Selecciona una especie")
    private Integer idEspecie;

    @NotBlank(message = "Selecciona el género")
    @Pattern(regexp = "H|M", message = "Género inválido")
    private String genero;

    /** Raza del catálogo. Puede venir vacía si se eligió «Otra raza» (ver {@link #razaPersonalizada}). */
    private Integer idRaza;

    /** DUE-02: nombre escrito por el dueño cuando su raza no está en el catálogo. */
    @Size(max = 45, message = "La raza no puede superar 45 caracteres")
    private String razaPersonalizada;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;

    @NotNull(message = "El peso es obligatorio")
    @Positive(message = "El peso debe ser mayor a 0")
    @Max(value = 1500, message = "Peso no válido")
    private Double peso;

    /** VL-22: solo para la vista previa en el formulario; el servidor no guarda lo que llegue aquí. */
    @Size(max = 500)
    private String foto;

    /** Spring llama al setter al enlazar: el nombre llega ya sin espacios sobrantes. */
    public void setNombre(String nombre) {
        this.nombre = Textos.normalizar(nombre);
    }

    public void setRazaPersonalizada(String razaPersonalizada) {
        String r = Textos.normalizar(razaPersonalizada);
        this.razaPersonalizada = r == null || r.isEmpty() ? null : r;
    }

    public boolean usaRazaPersonalizada() {
        return razaPersonalizada != null && !razaPersonalizada.isBlank();
    }
}
