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

    /** RF0041 — solo quien lo creó puede editarlo. */
    HistorialClinico actualizar(Integer numeroHistorial, HistorialClinicoForm form, Integer idUsuarioActual);

    /** RF0042 — solo quien lo creó, y solo dentro de los primeros 20 minutos. */
    void eliminar(Integer numeroHistorial, Integer idUsuarioActual);

    /** Para la interfaz: cuántos minutos le quedan a este registro para poder eliminarse (o -1 si ya no aplica). */
    long minutosRestantesParaEliminar(HistorialClinico historial);
}
