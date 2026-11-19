package com.vetlink.pasaporte.service;

import com.vetlink.pasaporte.model.DuracionPasaporte;
import com.vetlink.pasaporte.model.Pasaporte;

import java.util.List;

/**
 * Pasaportes de salud compartibles. Quien llama ya verificó que el usuario es el dueño de la mascota.
 *
 * @throws ServicioNoDisponibleException si el servicio de pasaportes no responde o falla
 */
public interface PasaporteService {

    /** @throws PasaporteNoEncontradoException si la mascota o el usuario no existen */
    Pasaporte crear(Integer idMascota, Integer idUsuario, DuracionPasaporte duracion);

    /** Pasaportes de la mascota, del más reciente al más antiguo. */
    List<Pasaporte> listar(Integer idMascota);

    /** @throws PasaporteNoEncontradoException si el token no existe */
    void revocar(String token);
}
