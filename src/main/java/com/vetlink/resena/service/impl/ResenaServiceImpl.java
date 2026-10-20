package com.vetlink.resena.service.impl;

import com.vetlink.resena.dto.ResenaForm;
import com.vetlink.resena.model.Calificacion;
import com.vetlink.resena.model.Resena;
import com.vetlink.resena.repository.CalificacionRepository;
import com.vetlink.resena.repository.ResenaRepository;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import com.vetlink.resena.service.ResenaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class ResenaServiceImpl implements ResenaService {

    private final ResenaRepository resenaRepository;
    private final CalificacionRepository calificacionRepository;
    private final VeterinariaRepository veterinariaRepository;
    private final UsuarioRepository usuarioRepository;

    public ResenaServiceImpl(ResenaRepository resenaRepository, CalificacionRepository calificacionRepository,
                              VeterinariaRepository veterinariaRepository, UsuarioRepository usuarioRepository) {
        this.resenaRepository = resenaRepository;
        this.calificacionRepository = calificacionRepository;
        this.veterinariaRepository = veterinariaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<Resena> listarPorVeterinaria(Integer idVeterinaria) {
        return resenaRepository.findByVeterinaria_IdVeterinariaOrderByFechaPublicacionDesc(idVeterinaria);
    }

    @Override
    public List<Resena> listarPorTutor(Integer idTutor) {
        return resenaRepository.findByTutor_IdUsuarioOrderByFechaPublicacionDesc(idTutor);
    }

    @Override
    public Double promedioDeVeterinaria(Integer idVeterinaria) {
        return resenaRepository.promedioDeVeterinaria(idVeterinaria);
    }

    @Override
    @Transactional
    public Resena crear(ResenaForm form, Integer idTutor) {
        if (form.getIdVeterinaria() == null || form.getValor() == null) {
            throw new IllegalArgumentException("Selecciona una veterinaria y una calificación");
        }
        if (form.getValor() < 1 || form.getValor() > 5) {
            throw new IllegalArgumentException("La calificación debe estar entre 1 y 5");
        }
        if (!resenaRepository.tutorTuvoCitaEnVeterinaria(form.getIdVeterinaria(), idTutor)) {
            throw new IllegalStateException("Debes haber tenido una cita en esta veterinaria para dejar una reseña");
        }

        Calificacion calificacion = buscarPorValor(form.getValor());
        Veterinaria veterinaria = veterinariaRepository.findById(form.getIdVeterinaria())
                .orElseThrow(() -> new IllegalArgumentException("Veterinaria no encontrada"));
        Usuario tutor = usuarioRepository.findById(idTutor)
                .orElseThrow(() -> new IllegalArgumentException("Tutor no encontrado"));

        Resena resena = new Resena();
        resena.setFechaPublicacion(LocalDate.now());
        resena.setComentario(form.getComentario());
        resena.setCalificacion(calificacion);
        resena.setVeterinaria(veterinaria);
        resena.setTutor(tutor);
        return resenaRepository.save(resena);
    }

    private Calificacion buscarPorValor(Integer valor) {
        return calificacionRepository.findAll().stream()
                .filter(c -> Objects.equals(c.getValor(), valor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Calificacion invalida"));
    }
}
