package com.vetlink.comun.web;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Formato único de fechas y horas para las vistas: {@code ${@fmt.hora(c.hora)}} → "2:30 p. m.".
 * Nunca se muestran segundos ni milisegundos.
 */
@Component("fmt")
public class FormatoVista {

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String horaTexto(LocalTime hora) {
        if (hora == null) return "";
        int h = hora.getHour();
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return h12 + ":" + String.format("%02d", hora.getMinute()) + (h < 12 ? " a. m." : " p. m.");
    }

    public String hora(LocalTime hora) {
        return horaTexto(hora);
    }

    public String hora(LocalDateTime fechaHora) {
        return fechaHora == null ? "" : horaTexto(fechaHora.toLocalTime());
    }

    public String rango(LocalTime inicio, Integer duracionMin) {
        if (inicio == null) return "";
        if (duracionMin == null || duracionMin <= 0) return horaTexto(inicio);
        return horaTexto(inicio) + " – " + horaTexto(inicio.plusMinutes(duracionMin));
    }

    public String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FECHA_CORTA);
    }

    /** "miércoles 26 de noviembre de 2026". */
    public String fechaLarga(LocalDate fecha) {
        if (fecha == null) return "";
        return fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, ES) + " " + fecha.getDayOfMonth() + " de "
                + fecha.getMonth().getDisplayName(TextStyle.FULL, ES) + " de " + fecha.getYear();
    }

    /** "mié 26 nov". */
    public String fechaCorta(LocalDate fecha) {
        if (fecha == null) return "";
        return fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).replace(".", "") + " " + fecha.getDayOfMonth()
                + " " + fecha.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
    }

    public String fechaHora(LocalDateTime fh) {
        return fh == null ? "" : fecha(fh.toLocalDate()) + " " + horaTexto(fh.toLocalTime());
    }

    public String duracion(Integer minutos) {
        if (minutos == null || minutos <= 0) return "";
        if (minutos < 60) return minutos + " min";
        int h = minutos / 60, m = minutos % 60;
        return h + " h" + (m > 0 ? " " + m + " min" : "");
    }
}
