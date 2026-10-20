package com.vetlink.historial.service.impl;

import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.historial.repository.HistorialClinicoRepository;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.historial.service.HistorialClinicoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class HistorialClinicoServiceImpl implements HistorialClinicoService {

    private final HistorialClinicoRepository historialRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;

    public HistorialClinicoServiceImpl(HistorialClinicoRepository historialRepository, MascotaRepository mascotaRepository,
                                        UsuarioRepository usuarioRepository) {
        this.historialRepository = historialRepository;
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<HistorialClinico> listarPorMascota(Integer idMascota) {
        return historialRepository.findByMascota_IdMascotaOrderByNumeroHistorialDesc(idMascota);
    }

    @Override
    public List<HistorialClinico> listarPorTutor(Integer idTutor) {
        return historialRepository.findByMascota_Tutor_IdUsuarioOrderByNumeroHistorialDesc(idTutor);
    }

    @Override
    @Transactional
    public HistorialClinico crear(HistorialClinicoForm form, Integer idVeterinario) {
        Mascota mascota = mascotaRepository.findById(form.getIdMascota())
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        Usuario veterinario = usuarioRepository.findById(idVeterinario)
                .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));

        HistorialClinico historial = new HistorialClinico();
        historial.setMascota(mascota);
        historial.setVeterinario(veterinario);
        historial.setFechaConsulta(LocalDate.now().getYear());
        historial.setFechaRegistro(java.time.LocalDateTime.now());
        historial.setMotivoConsulta(form.getMotivoConsulta());
        historial.setDiagnostico(form.getDiagnostico());
        historial.setObservaciones(form.getObservaciones());
        historial.setProximaCita(form.getProximaCita());
        return historialRepository.save(historial);
    }

    @Override
    public HistorialClinico buscarPorId(Integer numeroHistorial) {
        return historialRepository.findById(numeroHistorial)
                .orElseThrow(() -> new IllegalArgumentException("Historial no encontrado"));
    }

    @Override
    @Transactional
    public HistorialClinico actualizar(Integer numeroHistorial, HistorialClinicoForm form, Integer idUsuarioActual) {
        HistorialClinico historial = obtenerYVerificarAutor(numeroHistorial, idUsuarioActual);
        historial.setMotivoConsulta(form.getMotivoConsulta());
        historial.setDiagnostico(form.getDiagnostico());
        historial.setObservaciones(form.getObservaciones());
        historial.setProximaCita(form.getProximaCita());
        return historialRepository.save(historial);
    }

    @Override
    @Transactional
    public void eliminar(Integer numeroHistorial, Integer idUsuarioActual) {
        HistorialClinico historial = obtenerYVerificarAutor(numeroHistorial, idUsuarioActual);
        if (minutosRestantesParaEliminar(historial) <= 0) {
            throw new IllegalStateException("Ya pasaron los 20 minutos permitidos para eliminar este historial.");
        }
        historialRepository.delete(historial);
    }

    @Override
    public long minutosRestantesParaEliminar(HistorialClinico historial) {
        if (historial.getFechaRegistro() == null) return -1; // registro anterior a esta regla: no editable por tiempo
        long transcurridos = java.time.Duration.between(historial.getFechaRegistro(), java.time.LocalDateTime.now()).toMinutes();
        return 20 - transcurridos;
    }

    private HistorialClinico obtenerYVerificarAutor(Integer numeroHistorial, Integer idUsuarioActual) {
        HistorialClinico historial = historialRepository.findById(numeroHistorial)
                .orElseThrow(() -> new IllegalArgumentException("Historial no encontrado"));
        if (!historial.getVeterinario().getIdUsuario().equals(idUsuarioActual)) {
            throw new SecurityException("Solo quien registró este historial puede modificarlo o eliminarlo.");
        }
        return historial;
    }
}
