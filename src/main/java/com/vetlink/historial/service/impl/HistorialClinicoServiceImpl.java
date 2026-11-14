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

    /** VL-19: plazo de edición de una historia clínica para su autor. */
    static final long HORAS_EDICION = 24;

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
        return historialRepository.listarPorMascotaRecientes(idMascota);
    }

    @Override
    public List<HistorialClinico> listarPorTutor(Integer idTutor) {
        return historialRepository.listarPorTutorRecientes(idTutor);
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
        if (!esEditable(historial, idUsuarioActual)) {
            throw new SecurityException("La historia ya no se puede editar");
        }
        historial.setMotivoConsulta(form.getMotivoConsulta());
        historial.setDiagnostico(form.getDiagnostico());
        historial.setObservaciones(form.getObservaciones());
        historial.setProximaCita(form.getProximaCita());
        return historialRepository.save(historial);
    }

    @Override
    public boolean esEditable(HistorialClinico historial, Integer idUsuarioActual) {
        return historial.getVeterinario().getIdUsuario().equals(idUsuarioActual)
                && historial.getFechaRegistro() != null
                && java.time.LocalDateTime.now().isBefore(historial.getFechaRegistro().plusHours(HORAS_EDICION));
    }

    private HistorialClinico obtenerYVerificarAutor(Integer numeroHistorial, Integer idUsuarioActual) {
        HistorialClinico historial = historialRepository.findById(numeroHistorial)
                .orElseThrow(() -> new IllegalArgumentException("Historial no encontrado"));
        if (!historial.getVeterinario().getIdUsuario().equals(idUsuarioActual)) {
            throw new SecurityException("Solo quien registró este historial puede modificarlo.");
        }
        return historial;
    }
}
