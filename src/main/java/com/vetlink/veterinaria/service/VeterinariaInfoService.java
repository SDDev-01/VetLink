package com.vetlink.veterinaria.service;

import com.vetlink.ubicacion.model.UbicacionVeterinaria;
import com.vetlink.ubicacion.repository.UbicacionVeterinariaRepository;
import com.vetlink.veterinaria.model.CorreoVeterinaria;
import com.vetlink.veterinaria.model.HorarioVeterinaria;
import com.vetlink.veterinaria.model.TelefonoVeterinaria;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.CorreoVeterinariaRepository;
import com.vetlink.veterinaria.repository.HorarioVeterinariaRepository;
import com.vetlink.veterinaria.repository.TelefonoVeterinariaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Datos de presentación de una veterinaria: dirección, contactos, horario y logo. */
@Service
@Transactional(readOnly = true)
public class VeterinariaInfoService {

    private final UbicacionVeterinariaRepository ubicaciones;
    private final TelefonoVeterinariaRepository telefonos;
    private final CorreoVeterinariaRepository correos;
    private final HorarioVeterinariaRepository horarios;

    public VeterinariaInfoService(UbicacionVeterinariaRepository ubicaciones, TelefonoVeterinariaRepository telefonos,
                                  CorreoVeterinariaRepository correos, HorarioVeterinariaRepository horarios) {
        this.ubicaciones = ubicaciones;
        this.telefonos = telefonos;
        this.correos = correos;
        this.horarios = horarios;
    }

    public Optional<UbicacionVeterinaria> ubicacion(Integer idVeterinaria) {
        return ubicaciones.findByVeterinaria_IdVeterinaria(idVeterinaria);
    }

    public String direccion(Integer idVeterinaria) {
        return ubicacion(idVeterinaria).map(u -> {
            StringBuilder sb = new StringBuilder();
            if (u.getCalle() != null) sb.append(u.getCalle().trim());
            if (u.getNumero() != null && !u.getNumero().isBlank()) {
                String n = u.getNumero().trim();
                sb.append(n.startsWith("#") ? " " : " # ").append(n);
            }
            if (u.getBarrio() != null && u.getBarrio().getNombre() != null) sb.append(", ").append(u.getBarrio().getNombre());
            if (u.getBarrio() != null && u.getBarrio().getLocalidad() != null
                    && u.getBarrio().getLocalidad().getCiudad() != null) {
                sb.append(", ").append(u.getBarrio().getLocalidad().getCiudad().getNombre());
            }
            return sb.toString();
        }).orElse("");
    }

    public List<String> telefonos(Integer idVeterinaria) {
        return telefonos.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .map(TelefonoVeterinaria::getNumeroTelefono).toList();
    }

    public List<String> correos(Integer idVeterinaria) {
        return correos.findByVeterinaria_IdVeterinaria(idVeterinaria).stream().map(CorreoVeterinaria::getCorreo).toList();
    }

    /** Franjas válidas de la clínica para un día. */
    public List<HorarioVeterinaria> horarioDelDia(Integer idVeterinaria, DayOfWeek dia) {
        return horarios.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .filter(h -> h.getEstado() != null && h.getEstado() == 1)
                .filter(h -> h.getHoraApertura().isBefore(h.getHoraCierre()))
                .filter(h -> h.getDiaSemana() != null && normal(h.getDiaSemana().getDiaSemana()).equals(nombreDia(dia)))
                .sorted(Comparator.comparing(HorarioVeterinaria::getHoraApertura))
                .toList();
    }

    public List<HorarioVeterinaria> horarioSemanal(Integer idVeterinaria) {
        return horarios.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .filter(h -> h.getEstado() != null && h.getEstado() == 1)
                .filter(h -> h.getHoraApertura().isBefore(h.getHoraCierre()))
                .sorted(Comparator.comparing((HorarioVeterinaria h) -> ordenDia(h.getDiaSemana().getDiaSemana()))
                        .thenComparing(HorarioVeterinaria::getHoraApertura))
                .toList();
    }

    /** URL del logo: la subida (servida por /directorio/{id}/logo) o la imagen por defecto. */
    public static String logoUrl(Veterinaria v) {
        if (v == null) return "/resource/veterinaria-default.svg";
        String logo = v.getLogo();
        if (logo != null && logo.startsWith("veterinarias/")) return "/directorio/" + v.getIdVeterinaria() + "/logo";
        return "/resource/veterinaria-default.svg";
    }

    private static int ordenDia(String nombre) {
        return switch (normal(nombre)) {
            case "lunes" -> 1;
            case "martes" -> 2;
            case "miercoles" -> 3;
            case "jueves" -> 4;
            case "viernes" -> 5;
            case "sabado" -> 6;
            default -> 7;
        };
    }

    private static String nombreDia(DayOfWeek d) {
        return switch (d) {
            case MONDAY -> "lunes";
            case TUESDAY -> "martes";
            case WEDNESDAY -> "miercoles";
            case THURSDAY -> "jueves";
            case FRIDAY -> "viernes";
            case SATURDAY -> "sabado";
            case SUNDAY -> "domingo";
        };
    }

    private static String normal(String s) {
        return Normalizer.normalize(String.valueOf(s), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }
}
