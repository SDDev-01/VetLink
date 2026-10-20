package com.vetlink.mascota.service.impl;

import com.vetlink.mascota.dto.MascotaForm;
import com.vetlink.mascota.model.Especie;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.model.Raza;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.mascota.repository.EspecieRepository;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.mascota.repository.RazaRepository;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.mascota.service.MascotaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MascotaServiceImpl implements MascotaService {

    private final MascotaRepository mascotaRepository;
    private final EspecieRepository especieRepository;
    private final RazaRepository razaRepository;
    private final UsuarioRepository usuarioRepository;

    public MascotaServiceImpl(MascotaRepository mascotaRepository, EspecieRepository especieRepository,
                               RazaRepository razaRepository, UsuarioRepository usuarioRepository) {
        this.mascotaRepository = mascotaRepository;
        this.especieRepository = especieRepository;
        this.razaRepository = razaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<Mascota> listarActivasPorTutor(Integer idTutor) {
        return mascotaRepository.findByTutor_IdUsuarioAndEstado(idTutor, (byte) 1);
    }

    @Override
    public Mascota buscarPorId(Integer idMascota) {
        return mascotaRepository.findById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
    }

    @Override
    @Transactional
    public Mascota crear(MascotaForm form, Integer idTutor) {
        Usuario tutor = usuarioRepository.findById(idTutor)
                .orElseThrow(() -> new IllegalArgumentException("Tutor no encontrado"));
        Mascota mascota = new Mascota();
        aplicarForm(mascota, form);
        mascota.setTutor(tutor);
        mascota.setEstado((byte) 1);
        return mascotaRepository.save(mascota);
    }

    @Override
    @Transactional
    public Mascota actualizar(MascotaForm form, Integer idTutor) {
        Mascota mascota = buscarPorId(form.getIdMascota());
        verificarPropietario(mascota, idTutor);
        aplicarForm(mascota, form);
        return mascotaRepository.save(mascota);
    }

    @Override
    @Transactional
    public void eliminar(Integer idMascota, Integer idTutor) {
        Mascota mascota = buscarPorId(idMascota);
        verificarPropietario(mascota, idTutor);
        mascota.setEstado((byte) 0);
        mascotaRepository.save(mascota);
    }

    @Override
    @Transactional
    public Mascota actualizarSinVerificarTutor(MascotaForm form) {
        Mascota mascota = buscarPorId(form.getIdMascota());
        aplicarForm(mascota, form);
        return mascotaRepository.save(mascota);
    }

    private void verificarPropietario(Mascota mascota, Integer idTutor) {
        if (!mascota.getTutor().getIdUsuario().equals(idTutor)) {
            throw new SecurityException("No tienes permiso sobre esta mascota");
        }
    }

    private void aplicarForm(Mascota mascota, MascotaForm form) {
        Especie especie = especieRepository.findById(form.getIdEspecie())
                .orElseThrow(() -> new IllegalArgumentException("Especie invalida"));
        Raza raza = razaRepository.findById(form.getIdRaza())
                .orElseThrow(() -> new IllegalArgumentException("Raza invalida"));
        if (!raza.getEspecie().getIdEspecie().equals(especie.getIdEspecie())) {
            throw new IllegalArgumentException("La raza seleccionada no pertenece a la especie elegida");
        }
        mascota.setNombre(form.getNombre());
        mascota.setEspecie(especie);
        mascota.setRaza(raza);
        mascota.setGenero(form.getGenero());
        mascota.setFechaNacimiento(form.getFechaNacimiento());
        mascota.setPeso(form.getPeso());
        mascota.setFoto(form.getFoto());
    }
}
