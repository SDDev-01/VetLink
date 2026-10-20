package com.vetlink.vacuna.service;

import com.vetlink.vacuna.model.CarnetVacuna;

import java.util.List;

public interface CarnetVacunaService {
    List<CarnetVacuna> listarPorMascota(Integer idMascota);

    /** RF0046 — registrar una vacuna aplicada. */
    CarnetVacuna registrar(Integer idMascota, Integer idVacuna, Integer idLote, String dosis,
                           java.time.LocalDate fechaAplicacion, java.time.LocalDate proximaDosis,
                           String observaciones, Integer idUsuarioQueAplica);
}
