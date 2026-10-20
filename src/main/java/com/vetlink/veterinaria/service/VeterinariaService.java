package com.vetlink.veterinaria.service;

import com.vetlink.veterinaria.model.Veterinaria;

import java.util.List;

public interface VeterinariaService {
    List<Veterinaria> listarActivas();
    List<Veterinaria> buscarPorNombre(String nombre);
    Veterinaria buscarPorId(Integer idVeterinaria);
    void desactivar(Integer idVeterinaria, Integer idRecepcionista);

    /** RF0010 — cualquier recepcionista vinculada a la veterinaria puede editar su información pública. */
    Veterinaria actualizarInfo(Integer idVeterinaria, Integer idRecepcionista,
                               String nombre, String descripcion, String paginaWeb, String logo);
}
