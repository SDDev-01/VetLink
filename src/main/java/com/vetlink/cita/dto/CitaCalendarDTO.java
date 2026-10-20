package com.vetlink.cita.dto;

/** Proyección liviana de Cita para pintar el calendario en el cliente (evita
 *  serializar la entidad JPA completa con sus relaciones perezosas). */
public record CitaCalendarDTO(Integer idCita, String fecha, String hora, String estado, String mascota, String servicio) {}
