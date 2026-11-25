package com.vetlink.historial.service.impl;

import com.vetlink.cita.model.Cita;
import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.historial.model.TipoRegistroClinico;
import com.vetlink.historial.repository.HistorialClinicoRepository;
import com.vetlink.historial.service.HistorialClinicoService;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.veterinaria.model.Servicio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class HistorialClinicoServiceImpl implements HistorialClinicoService {

    /** VL-19: plazo de edición de una historia clínica para su autor. */
    static final long HORAS_EDICION = 24;

    private final HistorialClinicoRepository historialRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock reloj;

    public HistorialClinicoServiceImpl(HistorialClinicoRepository historialRepository,
                                       UsuarioRepository usuarioRepository, Clock reloj) {
        this.historialRepository = historialRepository;
        this.usuarioRepository = usuarioRepository;
        this.reloj = reloj;
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
    public HistorialClinico crearParaCita(HistorialClinicoForm form, Cita cita, Integer idVeterinario) {
        if (cita.getMascota() == null || !cita.getMascota().getIdMascota().equals(form.getIdMascota())) {
            throw new IllegalArgumentException("La mascota no coincide con la cita");
        }
        if (historialRepository.findByCita_IdCita(cita.getIdCita()).isPresent()) {
            throw new IllegalStateException("Esta cita ya tiene un registro clínico. Puedes editarlo durante las "
                    + HORAS_EDICION + " horas siguientes a su creación.");
        }
        Usuario veterinario = usuarioRepository.findById(idVeterinario)
                .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        HistorialClinico h = new HistorialClinico();
        h.setMascota(cita.getMascota());
        h.setVeterinario(veterinario);
        h.setCita(cita);
        h.setVeterinaria(cita.getVeterinaria());
        h.setFechaConsulta(ahora.getYear());
        h.setFechaRegistro(ahora);
        h.setTipoRegistro(tipoPorDefecto(form, cita.getServicio()));
        aplicar(h, form);
        return historialRepository.save(h);
    }

    @Override
    @Transactional
    public HistorialClinico crearParaEmergencia(HistorialClinicoForm form, com.vetlink.emergencia.model.Emergencia emergencia,
                                                Integer idVeterinario) {
        if (emergencia.getMascota() == null || form.getIdMascota() == null
                || !emergencia.getMascota().getIdMascota().equals(form.getIdMascota())) {
            throw new IllegalArgumentException("La mascota no coincide con la emergencia");
        }
        if (historialRepository.findByEmergencia_IdEmergencia(emergencia.getIdEmergencia()).isPresent()) {
            throw new IllegalStateException("Esta emergencia ya tiene un registro clínico.");
        }
        Usuario veterinario = usuarioRepository.findById(idVeterinario)
                .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        HistorialClinico h = new HistorialClinico();
        h.setMascota(emergencia.getMascota());
        h.setVeterinario(veterinario);
        h.setEmergencia(emergencia);
        h.setVeterinaria(emergencia.getVeterinaria());
        h.setFechaConsulta(ahora.getYear());
        h.setFechaRegistro(ahora);
        h.setTipoRegistro(TipoRegistroClinico.URGENCIA.name());
        aplicar(h, form);
        return historialRepository.saveAndFlush(h);
    }

    @Override
    public Optional<HistorialClinico> buscarPorEmergencia(Integer idEmergencia) {
        return historialRepository.findByEmergencia_IdEmergencia(idEmergencia);
    }

    @Override
    public Optional<HistorialClinico> buscarPorCita(Integer idCita) {
        return historialRepository.findByCita_IdCita(idCita);
    }

    @Override
    public HistorialClinico buscarPorId(Integer numeroHistorial) {
        return historialRepository.findById(numeroHistorial)
                .orElseThrow(() -> new IllegalArgumentException("Historial no encontrado"));
    }

    @Override
    @Transactional
    public HistorialClinico actualizar(Integer numeroHistorial, HistorialClinicoForm form, Integer idUsuarioActual) {
        HistorialClinico historial = buscarPorId(numeroHistorial);
        if (!historial.getVeterinario().getIdUsuario().equals(idUsuarioActual)) {
            throw new SecurityException("Solo quien registró este historial puede modificarlo.");
        }
        if (!esEditable(historial, idUsuarioActual)) {
            throw new SecurityException("La historia ya no se puede editar");
        }
        if (form.getTipoRegistro() != null && !form.getTipoRegistro().isBlank()) {
            historial.setTipoRegistro(form.getTipoRegistro());
        }
        aplicar(historial, form);
        return historialRepository.save(historial);
    }

    @Override
    public boolean esEditable(HistorialClinico historial, Integer idUsuarioActual) {
        return historial.getVeterinario().getIdUsuario().equals(idUsuarioActual)
                && historial.getFechaRegistro() != null
                && LocalDateTime.now(reloj).isBefore(historial.getFechaRegistro().plusHours(HORAS_EDICION));
    }

    private static void aplicar(HistorialClinico h, HistorialClinicoForm f) {
        h.setMotivoConsulta(f.getMotivoConsulta().trim());
        h.setDiagnostico(f.getDiagnostico().trim());
        h.setAntecedentes(texto(f.getAntecedentes()));
        h.setTemperatura(f.getTemperatura());
        h.setFrecuenciaCardiaca(f.getFrecuenciaCardiaca());
        h.setFrecuenciaRespiratoria(f.getFrecuenciaRespiratoria());
        h.setPesoKg(f.getPesoKg());
        h.setEvaluacionClinica(texto(f.getEvaluacionClinica()));
        h.setHallazgos(texto(f.getHallazgos()));
        h.setTratamiento(texto(f.getTratamiento()));
        h.setMedicamentosIndicados(texto(f.getMedicamentosIndicados()));
        h.setRecomendaciones(texto(f.getRecomendaciones()));
        h.setObservaciones(texto(f.getObservaciones()));
        h.setProximaCita(f.getProximaCita());
    }

    private static String tipoPorDefecto(HistorialClinicoForm f, Servicio servicio) {
        if (f.getTipoRegistro() != null && !f.getTipoRegistro().isBlank()) return f.getTipoRegistro();
        if (servicio == null) return TipoRegistroClinico.CONSULTA.name();
        if (servicio.getTipoRegistro() == Servicio.TipoRegistro.VACUNACION) return TipoRegistroClinico.VACUNACION.name();
        if (servicio.isEmergencia()) return TipoRegistroClinico.URGENCIA.name();
        String n = servicio.getNombreServicio() == null ? "" : servicio.getNombreServicio().toLowerCase();
        if (n.contains("laborator")) return TipoRegistroClinico.LABORATORIO.name();
        if (n.contains("cirug") || n.contains("esteriliz") || n.contains("procedim") || n.contains("odontolog")) {
            return TipoRegistroClinico.PROCEDIMIENTO.name();
        }
        if (n.contains("control")) return TipoRegistroClinico.CONTROL.name();
        return TipoRegistroClinico.CONSULTA.name();
    }

    private static String texto(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
