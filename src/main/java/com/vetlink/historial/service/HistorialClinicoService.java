package com.vetlink.historial.service;

import com.vetlink.cita.model.Cita;
import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.historial.model.HistorialClinico;

import java.util.List;
import java.util.Optional;

public interface HistorialClinicoService {
    List<HistorialClinico> listarPorMascota(Integer idMascota);
    List<HistorialClinico> listarPorTutor(Integer idTutor);

    /**
     * Crea el registro clínico de una cita EN ATENCIÓN (las reglas de la cita las valida
     * AtencionService). Queda ligado a la cita, a la mascota y a la clínica de la cita.
     */
    HistorialClinico crearParaCita(HistorialClinicoForm form, Cita cita, Integer idVeterinario);

    Optional<HistorialClinico> buscarPorCita(Integer idCita);

    HistorialClinico buscarPorId(Integer numeroHistorial);

    /** RF0041 — solo quien lo creó puede editarlo, y solo durante las 24 horas siguientes (VL-19). */
    HistorialClinico actualizar(Integer numeroHistorial, HistorialClinicoForm form, Integer idUsuarioActual);

    /**
     * VL-19: un registro médico no se borra. Solo su autor lo puede editar durante las
     * 24 horas siguientes a fechaRegistro; los registros antiguos (sin fechaRegistro) no se editan.
     */
    boolean esEditable(HistorialClinico historial, Integer idUsuarioActual);
}
