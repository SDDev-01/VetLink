package com.vetlink.cita.service;

import com.vetlink.cita.dto.AgendarCitaForm;
import com.vetlink.cita.dto.FiltroCitaForm;
import com.vetlink.cita.dto.OfertarCitaForm;
import com.vetlink.cita.model.Cita;

import java.util.List;

public interface CitaService {
    List<Cita> listarPorMascota(Integer idMascota);
    List<Cita> listarPorTutor(Integer idTutor);
    Cita agendar(AgendarCitaForm form, Integer idTutor);
    void cancelar(Integer idCita, Integer idTutor);
    List<Cita> buscarConFiltros(FiltroCitaForm filtro);

    /* ---- Operaciones de RECEPCIONISTA sobre las citas de SU veterinaria ---- */
    Cita agendarComoRecepcionista(AgendarCitaForm form, Integer idVeterinariaRecepcionista);
    void cancelarComoRecepcionista(Integer idCita, Integer idVeterinariaRecepcionista);
    /** VL-23: mueve la cita a otro cupo DISPONIBLE de la misma clínica y libera el horario original. */
    Cita reagendarComoRecepcionista(Integer idCita, Integer idCupoNuevo, Integer idVeterinariaRecepcionista);

    /** VL-23: cupos a los que se puede mover la cita (mismo servicio, misma clínica, futuros). */
    List<Cita> cuposParaReagendar(Integer idCita, Integer idVeterinariaRecepcionista);
    Cita ofertarCupo(OfertarCitaForm form, Integer idVeterinariaRecepcionista);
}
