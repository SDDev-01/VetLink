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
    @Pattern(regexp = "^$|\\d{9,11}", message = "El NIT debe tener entre 9 y 11 dígitos, sin puntos ni guiones")
    private String nit;

    @NotBlank(message = "El nombre comercial es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre comercial debe tener entre 3 y 50 caracteres")
    private String nombre;

    @NotBlank(message = "El telefono corporativo es obligatorio; ingresa entre 7 y 15 dígitos")
    @Pattern(regexp = "^$|\\d{7,15}", message = "El teléfono corporativo debe tener entre 7 y 15 dígitos, sin espacios ni guiones")
    private String telefono;

    @NotBlank(message = "El correo de la empresa es obligatorio")
    @Email(message = "Ingresa un correo válido, por ejemplo clinica@dominio.com")
    @Size(max = 45, message = "El correo de la empresa puede tener máximo 45 caracteres")
    private String correo;

    @Size(max = 50, message = "La página web puede tener máximo 50 caracteres")
    @Pattern(regexp = "^$|(?i)https?://[^\\s.]+\\.\\S{2,}", message = "Escribe la dirección completa de la página web, por ejemplo https://www.tuweb.com")
    private String paginaWeb;
    private String descripcion;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(min = 5, message = "Ingresa una dirección válida (mínimo 5 caracteres)")
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
        /** Servicio del catálogo; null cuando se eligió «Otros» y se escribió el nombre. */
        private Integer idServicio;
        /** Opción «Otros»: nombre de un servicio que no está en el catálogo. */
        @Size(max = 45, message = "El nombre del servicio puede tener máximo 45 caracteres")
        private String nombrePersonalizado;
        @PositiveOrZero(message = "El precio no puede ser negativo")
        private Double precio;

        public boolean esPersonalizado() {
            return idServicio == null && nombrePersonalizado != null && !nombrePersonalizado.isBlank();
        }

        public boolean estaVacio() {
            return idServicio == null && (nombrePersonalizado == null || nombrePersonalizado.isBlank());
        }
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
