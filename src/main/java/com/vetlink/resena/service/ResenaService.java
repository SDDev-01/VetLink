package com.vetlink.resena.service;

import com.vetlink.resena.dto.ResenaForm;
import com.vetlink.resena.model.Resena;

import java.util.List;

public interface ResenaService {
    List<Resena> listarPorVeterinaria(Integer idVeterinaria);
    List<Resena> listarPorTutor(Integer idTutor);
    Double promedioDeVeterinaria(Integer idVeterinaria);
    Resena crear(ResenaForm form, Integer idTutor);
}
