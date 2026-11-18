package com.vetlink.historial.service;

import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.historial.model.HistorialClinico;

import java.util.List;

public interface HistorialClinicoService {
    List<HistorialClinico> listarPorMascota(Integer idMascota);
    List<HistorialClinico> listarPorTutor(Integer idTutor);
    HistorialClinico crear(HistorialClinicoForm form, Integer idVeterinario);

    /** Necesario para poder editar/eliminar un registro puntual. */
    HistorialClinico buscarPorId(Integer numeroHistorial);

    /** RF0041 — solo quien lo creó puede editarlo, y solo durante las 24 horas siguientes (VL-19). */
    HistorialClinico actualizar(Integer numeroHistorial, HistorialClinicoForm form, Integer idUsuarioActual);

    /**
     * VL-19: un registro médico no se borra. Solo su autor lo puede editar durante las
     * 24 horas siguientes a fechaRegistro; los registros antiguos (sin fechaRegistro) no se editan.
     */
    boolean esEditable(HistorialClinico historial, Integer idUsuarioActual);
}
