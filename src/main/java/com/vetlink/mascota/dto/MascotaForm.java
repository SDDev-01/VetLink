package com.vetlink.mascota.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MascotaForm {

    private Integer idMascota;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 45)
    private String nombre;

    @NotNull(message = "Selecciona una especie")
    private Integer idEspecie;

    @NotBlank(message = "Selecciona el genero")
    @Pattern(regexp = "H|M", message = "Genero invalido")
    private String genero;

    @NotNull(message = "Selecciona una raza")
    private Integer idRaza;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;

    @NotNull(message = "El peso es obligatorio")
    @Positive(message = "El peso debe ser mayor a 0")
    private Double peso;

    /** VL-22: solo para la vista previa en el formulario; el servidor no guarda lo que llegue aquí. */
    @Size(max = 500)
    private String foto;
}
