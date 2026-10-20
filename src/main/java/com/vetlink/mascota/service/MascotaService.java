package com.vetlink.mascota.service;

import com.vetlink.mascota.dto.MascotaForm;
import com.vetlink.mascota.model.Mascota;

import java.util.List;

public interface MascotaService {
    List<Mascota> listarActivasPorTutor(Integer idTutor);
    Mascota buscarPorId(Integer idMascota);
    Mascota crear(MascotaForm form, Integer idTutor);
    Mascota actualizar(MascotaForm form, Integer idTutor);
    void eliminar(Integer idMascota, Integer idTutor);

    /** Usado por RECEPCIONISTA: registra/edita una mascota en nombre de un dueño ya existente. */
    Mascota actualizarSinVerificarTutor(MascotaForm form);
}
