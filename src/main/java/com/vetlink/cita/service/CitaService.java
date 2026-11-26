package com.vetlink.cita.service;

import com.vetlink.cita.dto.AgendarCitaForm;
import com.vetlink.cita.dto.FiltroCitaForm;
import com.vetlink.cita.dto.OfertarCitaForm;
import com.vetlink.cita.model.Cita;
import com.vetlink.usuario.model.Usuario;

import java.util.List;

public interface CitaService {
    List<Cita> listarPorMascota(Integer idMascota);
    List<Cita> listarPorTutor(Integer idTutor);
    List<Cita> buscarConFiltros(FiltroCitaForm filtro);

    /* ---- TUTOR ---- */
    /** Reserva un cupo: queda PENDIENTE de confirmación y bloquea el horario. */
    Cita agendar(AgendarCitaForm form, Integer idTutor);
    void cancelar(Integer idCita, Integer idTutor);
    /** No cambia el estado: avisa a recepción para que gestione la reprogramación. */
    void solicitarReprogramacion(Integer idCita, Integer idTutor, String motivo);

    /* ---- RECEPCIONISTA (siempre sobre SU veterinaria) ---- */
    Cita agendarComoRecepcionista(AgendarCitaForm form, Integer idVeterinariaRecepcionista);
    void cancelarComoRecepcionista(Integer idCita, Integer idVeterinariaRecepcionista);
    Cita confirmar(Integer idCita, Integer idVeterinariaRecepcionista, Usuario recepcionista);
    Cita rechazar(Integer idCita, Integer idVeterinariaRecepcionista, Usuario recepcionista, String motivo);
    Cita marcarNoAsistio(Integer idCita, Usuario actor);
    Cita reagendarComoRecepcionista(Integer idCita, Integer idCupoNuevo, Integer idVeterinariaRecepcionista);
    List<Cita> cuposParaReagendar(Integer idCita, Integer idVeterinariaRecepcionista);

    /** El dueño acepta el horario que propuso la clínica al reprogramar. */
    Cita confirmarReprogramacion(Integer idCita, Integer idTutor);

    /** El dueño elige otro horario del mismo servicio y clínica (queda pendiente de confirmación de la clínica). */
    Cita modificarReprogramacion(Integer idCita, Integer idCupoNuevo, Integer idTutor);

    /** Horarios a los que el dueño puede mover una cita reprogramada (mismo servicio y clínica). */
    List<Cita> cuposParaModificarReprogramacion(Integer idCita, Integer idTutor);
    Cita ofertarCupo(OfertarCitaForm form, Integer idVeterinariaRecepcionista);

    /* ---- SISTEMA (tareas programadas) ---- */
    int expirarPendientes();
    int enviarRecordatorios();
}
