package com.vetlink.cita.service.impl;

import com.vetlink.cita.dto.AgendarCitaForm;
import com.vetlink.cita.dto.FiltroCitaForm;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.pattern.strategy.CompositeValidadorCita;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.veterinaria.model.Servicio;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.ServicioRepository;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import com.vetlink.veterinaria.repository.ServicioVeterinariaRepository;
import com.vetlink.cita.service.CitaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CitaServiceImpl implements CitaService {

    private final CitaRepository citaRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;
    private final VeterinariaRepository veterinariaRepository;
    private final ServicioVeterinariaRepository servicioVeterinariaRepository;
    private final CompositeValidadorCita validador;

    public CitaServiceImpl(CitaRepository citaRepository, MascotaRepository mascotaRepository,
                            UsuarioRepository usuarioRepository, ServicioRepository servicioRepository,
                            VeterinariaRepository veterinariaRepository, ServicioVeterinariaRepository servicioVeterinariaRepository,
                            CompositeValidadorCita validador) {
        this.citaRepository = citaRepository;
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioRepository = servicioRepository;
        this.veterinariaRepository = veterinariaRepository;
        this.servicioVeterinariaRepository = servicioVeterinariaRepository;
        this.validador = validador;
    }

    @Override
    public List<Cita> listarPorMascota(Integer idMascota) {
        return citaRepository.findByMascota_IdMascotaOrderByFechaDescHoraDesc(idMascota);
    }

    @Override
    public List<Cita> listarPorTutor(Integer idTutor) {
        return citaRepository.findByMascota_Tutor_IdUsuarioOrderByFechaDescHoraDesc(idTutor);
    }

    @Override
    @Transactional
    public Cita agendar(AgendarCitaForm form, Integer idTutor) {
        Mascota mascota = mascotaRepository.findById(form.getIdMascota())
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        if (!mascota.getTutor().getIdUsuario().equals(idTutor)) {
            throw new SecurityException("Esta mascota no te pertenece");
        }
        return crearCitaValidada(form, mascota);
    }

    @Override
    @Transactional
    public Cita agendarComoRecepcionista(AgendarCitaForm form, Integer idVeterinariaRecepcionista) {
        if (!form.getIdVeterinaria().equals(idVeterinariaRecepcionista)) {
            throw new SecurityException("Solo puedes agendar citas en tu propia veterinaria");
        }
        Mascota mascota = mascotaRepository.findById(form.getIdMascota())
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        return crearCitaValidada(form, mascota);
    }

    /** Validaciones y creación comunes a agendar() y agendarComoRecepcionista(). */
    private Cita crearCitaValidada(AgendarCitaForm form, Mascota mascota) {
        validador.validarTodo(form);

        if (citaRepository.existsByMascota_IdMascotaAndFechaAndHoraAndEstado(
                form.getIdMascota(), form.getFecha(), form.getHora(), EstadoCita.AGENDADA)) {
            throw new IllegalStateException("La mascota ya tiene una cita programada en ese horario");
        }
        if (citaRepository.existsByVeterinario_IdUsuarioAndFechaAndHoraAndEstado(
                form.getIdVeterinario(), form.getFecha(), form.getHora(), EstadoCita.AGENDADA)) {
            throw new IllegalStateException("El veterinario ya tiene una cita en ese horario");
        }

        Usuario veterinario = usuarioRepository.findById(form.getIdVeterinario())
                .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));
        Servicio servicio = servicioRepository.findById(form.getIdServicio())
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado"));
        Veterinaria veterinaria = veterinariaRepository.findById(form.getIdVeterinaria())
                .orElseThrow(() -> new IllegalArgumentException("Veterinaria no encontrada"));
        if (veterinario.getVeterinario() == null
                || veterinario.getVeterinario().getVeterinaria() == null
                || !veterinario.getVeterinario().getVeterinaria().getIdVeterinaria().equals(veterinaria.getIdVeterinaria())) {
            throw new IllegalArgumentException("El veterinario no pertenece a la veterinaria seleccionada");
        }
        if (servicioVeterinariaRepository.findByVeterinaria_IdVeterinaria(form.getIdVeterinaria()).stream()
                .noneMatch(sv -> sv.getServicio().getIdServicio().equals(form.getIdServicio()))) {
            throw new IllegalArgumentException("El servicio no está disponible en la veterinaria seleccionada");
        }

        // El dueño reserva un cupo que la clínica publicó previamente; no crea
        // horarios arbitrarios fuera de la disponibilidad real.
        Cita cita = citaRepository
                .findByVeterinario_IdUsuarioAndServicio_IdServicioAndFechaAndHoraAndEstado(
                        veterinario.getIdUsuario(), servicio.getIdServicio(), form.getFecha(), form.getHora(),
                        EstadoCita.DISPONIBLE)
                .orElseThrow(() -> new IllegalStateException(
                        "Ese horario ya no está disponible. Elige un cupo ofertado."));
        cita.setMascota(mascota);
        cita.setEstado(EstadoCita.AGENDADA);
        return citaRepository.save(cita);
    }

    @Override
    @Transactional
    public void cancelar(Integer idCita, Integer idTutor) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        if (!cita.getMascota().getTutor().getIdUsuario().equals(idTutor)) {
            throw new SecurityException("No tienes permiso sobre esta cita");
        }
        cancelarInterno(cita);
    }

    @Override
    @Transactional
    public void cancelarComoRecepcionista(Integer idCita, Integer idVeterinariaRecepcionista) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        verificarCitaDeSuVeterinaria(cita, idVeterinariaRecepcionista);
        cancelarInterno(cita);
    }

    private void cancelarInterno(Cita cita) {
        if (cita.getEstado() != EstadoCita.AGENDADA) {
            throw new IllegalStateException("Solo se pueden cancelar citas que están agendadas");
        }
        if (cita.getFecha().isBefore(java.time.LocalDate.now())) {
            throw new IllegalStateException("No se pueden cancelar citas pasadas");
        }
        cita.setEstado(EstadoCita.CANCELADA);
        citaRepository.save(cita);
    }

    @Override
    @Transactional
    public Cita reagendarComoRecepcionista(Integer idCita, java.time.LocalDate nuevaFecha, java.time.LocalTime nuevaHora,
                                            Integer idVeterinariaRecepcionista) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        verificarCitaDeSuVeterinaria(cita, idVeterinariaRecepcionista);
        if (cita.getEstado() != EstadoCita.AGENDADA) {
            throw new IllegalStateException("Solo se pueden reagendar citas que están agendadas");
        }
        if (citaRepository.existsByVeterinario_IdUsuarioAndFechaAndHoraAndEstado(
                cita.getVeterinario().getIdUsuario(), nuevaFecha, nuevaHora, EstadoCita.AGENDADA)) {
            throw new IllegalStateException("El veterinario ya tiene una cita en ese horario");
        }
        cita.setFecha(nuevaFecha);
        cita.setHora(nuevaHora);
        return citaRepository.save(cita);
    }

    @Override
    @Transactional
    public Cita ofertarCupo(com.vetlink.cita.dto.OfertarCitaForm form, Integer idVeterinariaRecepcionista) {
        Usuario veterinario = usuarioRepository.findById(form.getIdVeterinario())
                .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));
        Servicio servicio = servicioRepository.findById(form.getIdServicio())
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado"));
        Veterinaria veterinaria = veterinariaRepository.findById(idVeterinariaRecepcionista)
                .orElseThrow(() -> new IllegalArgumentException("Veterinaria no encontrada"));
        if (veterinario.getVeterinario() == null || !idVeterinariaRecepcionista.equals(
                veterinario.getVeterinario().getVeterinaria().getIdVeterinaria())) {
            throw new SecurityException("El veterinario no pertenece a tu veterinaria");
        }
        if (servicioVeterinariaRepository.findByVeterinaria_IdVeterinaria(idVeterinariaRecepcionista).stream()
                .noneMatch(sv -> sv.getServicio().getIdServicio().equals(form.getIdServicio()))) {
            throw new IllegalArgumentException("El servicio no está habilitado en tu veterinaria");
        }
        if (citaRepository.existsByVeterinario_IdUsuarioAndFechaAndHoraAndEstado(
                form.getIdVeterinario(), form.getFecha(), form.getHora(), EstadoCita.DISPONIBLE)
                || citaRepository.existsByVeterinario_IdUsuarioAndFechaAndHoraAndEstado(
                form.getIdVeterinario(), form.getFecha(), form.getHora(), EstadoCita.AGENDADA)) {
            throw new IllegalStateException("El veterinario ya tiene un cupo o cita en ese horario");
        }
        Cita cupo = new Cita();
        cupo.setFecha(form.getFecha());
        cupo.setHora(form.getHora());
        cupo.setEstado(EstadoCita.DISPONIBLE);
        cupo.setVeterinario(veterinario);
        cupo.setServicio(servicio);
        cupo.setVeterinaria(veterinaria);
        return citaRepository.save(cupo);
    }

    private void verificarCitaDeSuVeterinaria(Cita cita, Integer idVeterinariaRecepcionista) {
        if (!cita.getVeterinaria().getIdVeterinaria().equals(idVeterinariaRecepcionista)) {
            throw new SecurityException("Esta cita no pertenece a tu veterinaria");
        }
    }

    @Override
    public List<Cita> buscarConFiltros(FiltroCitaForm filtro) {
        return citaRepository.buscarConFiltros(
                filtro.getIdVeterinaria(), filtro.getEstado(), filtro.getDesde(), filtro.getHasta(), filtro.getIdVeterinario());
    }
}
