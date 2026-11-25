package com.vetlink.cita.dto;

import com.vetlink.cita.model.Cita;
import com.vetlink.comun.web.FormatoVista;

/**
 * Proyección liviana de Cita para pintar calendarios y tarjetas en el cliente (evita
 * serializar la entidad JPA). La hora va sin segundos ("14:30") y ya formateada ("2:30 p. m.").
 */
public record CitaCalendarDTO(Integer idCita, String fecha, String hora, String estado, String mascota, String servicio,
                              String horaTexto, String estadoEtiqueta, String estadoClase, String especie,
                              String tutor, String veterinario, Integer duracion) {

    public static CitaCalendarDTO de(Cita c) {
        var m = c.getMascota();
        return new CitaCalendarDTO(
                c.getIdCita(),
                c.getFecha().toString(),
                String.format("%02d:%02d", c.getHora().getHour(), c.getHora().getMinute()),
                c.getEstado().name(),
                m == null ? "Cupo disponible" : m.getNombre(),
                c.getServicio().getNombreServicio(),
                FormatoVista.horaTexto(c.getHora()),
                c.getEstado().getEtiqueta(),
                c.getEstado().getClase(),
                m == null ? "" : m.getEspecie().getNombreEspecie(),
                m == null ? "" : m.getTutor().getNombre() + " " + m.getTutor().getApellido(),
                c.getVeterinario().getNombre() + " " + c.getVeterinario().getApellido(),
                c.getDuracionMin());
    }
}
