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
    Cita reagendarComoRecepcionista(Integer idCita, java.time.LocalDate nuevaFecha, java.time.LocalTime nuevaHora, Integer idVeterinariaRecepcionista);
    Cita ofertarCupo(OfertarCitaForm form, Integer idVeterinariaRecepcionista);
}
