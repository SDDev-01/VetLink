package com.vetlink.veterinaria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Datos de la clinica nueva que se registra junto con su recepcionista
 * administradora (paso "Soy Veterinaria" del wizard de registro). No existe
 * un concepto de "dueño unico" en mydb.sql: el primer recepcionista que
 * completa este formulario queda administrando la veterinaria.
 */
@Getter
@Setter
public class VeterinariaRegistroForm {

    @NotBlank(message = "El NIT es obligatorio")
    private String nit;

    @NotBlank(message = "El nombre comercial es obligatorio")
    private String nombre;

    @NotBlank(message = "El telefono corporativo es obligatorio; ingresa entre 7 y 15 dígitos")
    private String telefono;

    @NotBlank(message = "El correo de la empresa es obligatorio")
    @Email(message = "Ingresa un correo válido, por ejemplo clinica@dominio.com")
    private String correo;

    private String paginaWeb;
    private String descripcion;

    @NotBlank(message = "La direccion es obligatoria")
    private String direccion;

    @NotNull(message = "Selecciona una ciudad")
    private Integer idCiudad;

    @NotBlank(message = "El barrio es obligatorio")
    private String barrio;

    private Double latitud;
    private Double longitud;

    @Valid
    private List<ServicioOfrecidoForm> servicios = new ArrayList<>();

    @Valid
    private List<HorarioDiaForm> horarios = new ArrayList<>();

    @Getter
    @Setter
    public static class ServicioOfrecidoForm {
        @NotNull(message = "Selecciona un servicio")
        private Integer idServicio;
        @NotNull(message = "Indica un precio")
        @PositiveOrZero
        private Double precio;
    }

    @Getter
    @Setter
    public static class HorarioDiaForm {
        @NotNull
        private Integer idDiaSemana;
        private boolean abierto;
        private String horaApertura = "08:00";
        private String horaCierre = "18:00";
    }
}
