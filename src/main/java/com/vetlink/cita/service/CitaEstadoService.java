package com.vetlink.cita.service;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.CitaHistorialEstado;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.model.TransicionCita;
import com.vetlink.cita.model.TransicionCita.Actor;
import com.vetlink.cita.repository.CitaHistorialEstadoRepository;
import com.vetlink.usuario.model.Usuario;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Único punto por el que cambia el estado de una cita: valida la transición con
 * {@link TransicionCita} y registra el cambio en cita_historial_estado.
 */
@Service
public class CitaEstadoService {

    private final CitaHistorialEstadoRepository historial;
    private final Clock reloj;

    public CitaEstadoService(CitaHistorialEstadoRepository historial, Clock reloj) {
        this.historial = historial;
        this.reloj = reloj;
    }

    public void cambiar(Cita cita, EstadoCita nuevo, Actor actor, Usuario usuario, String comentario) {
        EstadoCita anterior = cita.getEstado();
        TransicionCita.exigir(anterior, nuevo, actor);
        cita.setEstado(nuevo);
        registrar(cita, anterior, nuevo, usuario, comentario);
    }

    /** Registro sin validar (creación de cupos, migraciones de datos). */
    public void registrar(Cita cita, EstadoCita anterior, EstadoCita nuevo, Usuario usuario, String comentario) {
        CitaHistorialEstado h = new CitaHistorialEstado();
        h.setCita(cita);
        h.setEstadoAnterior(anterior);
        h.setEstadoNuevo(nuevo);
        h.setUsuario(usuario);
        h.setComentario(comentario == null ? null : (comentario.length() > 500 ? comentario.substring(0, 500) : comentario));
        h.setFecha(LocalDateTime.now(reloj));
        historial.save(h);
    }

    public List<CitaHistorialEstado> historialDe(Integer idCita) {
        return historial.findByCita_IdCitaOrderByFechaAscIdHistorialEstadoAsc(idCita);
    }
}
