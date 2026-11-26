package com.vetlink.mascota.service.impl;

import com.vetlink.comun.validacion.Textos;
import com.vetlink.mascota.dto.MascotaForm;
import com.vetlink.mascota.model.Especie;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.model.Raza;
import com.vetlink.mascota.repository.EspecieRepository;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.mascota.repository.RazaRepository;
import com.vetlink.mascota.service.MascotaService;
import com.vetlink.mascota.service.RazaCatalogoService;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MascotaServiceImpl implements MascotaService {

    private final MascotaRepository mascotaRepository;
    private final EspecieRepository especieRepository;
    private final RazaRepository razaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RazaCatalogoService catalogoRazas;

    public MascotaServiceImpl(MascotaRepository mascotaRepository, EspecieRepository especieRepository,
                              RazaRepository razaRepository, UsuarioRepository usuarioRepository,
                              RazaCatalogoService catalogoRazas) {
        this.mascotaRepository = mascotaRepository;
        this.especieRepository = especieRepository;
        this.razaRepository = razaRepository;
        this.usuarioRepository = usuarioRepository;
        this.catalogoRazas = catalogoRazas;
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
        Especie especie = especieRepository.findById(form.getIdEspecie())
                .orElseThrow(() -> new IllegalArgumentException("Especie inválida"));
        if (form.getGenero() == null || !form.getGenero().matches("H|M")) {
            throw new IllegalArgumentException("Género inválido");
        }
        if (form.getFechaNacimiento() == null || form.getFechaNacimiento().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
        Mascota mascota = new Mascota();
        mascota.setEspecie(especie);
        mascota.setGenero(form.getGenero());
        mascota.setFechaNacimiento(form.getFechaNacimiento());
        aplicarEditables(mascota, form);
        mascota.setTutor(tutor);
        mascota.setEstado((byte) 1);
        return mascotaRepository.save(mascota);
    }

    /**
     * DUE-01: especie, género y fecha de nacimiento NO se modifican después del registro,
     * aunque la petición HTTP los traiga cambiados: se conservan los valores guardados.
     */
    @Override
    @Transactional
    public Mascota actualizar(MascotaForm form, Integer idTutor) {
        Mascota mascota = buscarPorId(form.getIdMascota());
        verificarPropietario(mascota, idTutor);
        mascota.exigirEditable();
        aplicarEditables(mascota, form);
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
        mascota.exigirEditable();
        aplicarEditables(mascota, form);
        return mascotaRepository.save(mascota);
    }

    private void verificarPropietario(Mascota mascota, Integer idTutor) {
        if (!mascota.getTutor().getIdUsuario().equals(idTutor)) {
            throw new SecurityException("No tienes permiso sobre esta mascota");
        }
    }

    /** Nombre, raza y peso: lo único que se puede cambiar de una mascota registrada. */
    private void aplicarEditables(Mascota mascota, MascotaForm form) {
        String nombre = Textos.normalizar(form.getNombre());
        if (!Textos.esNombreValido(nombre)) {
            throw new IllegalArgumentException("Nombre inválido: " + Textos.MENSAJE_NOMBRE.toLowerCase());
        }
        if (nombre.length() > 45) throw new IllegalArgumentException("El nombre no puede superar 45 caracteres");
        if (form.getPeso() == null || form.getPeso() <= 0 || form.getPeso() > 1500) {
            throw new IllegalArgumentException("Ingresa un peso mayor que cero");
        }
        mascota.setNombre(nombre);
        mascota.setRaza(resolverRaza(mascota.getEspecie(), form));
        mascota.setPeso(form.getPeso());
        // VL-22: la foto ya no viene como texto del formulario; solo la cambia FotoMascotaService al subir un archivo.
    }

    /** DUE-02: raza del catálogo de la especie o «Otra raza» escrita por el dueño. */
    private Raza resolverRaza(Especie especie, MascotaForm form) {
        if (form.usaRazaPersonalizada()) {
            return catalogoRazas.obtenerOCrearPersonalizada(especie, form.getRazaPersonalizada());
        }
        if (form.getIdRaza() == null) {
            throw new IllegalArgumentException("Selecciona una raza o elige «Otra raza» y escríbela");
        }
        Raza raza = razaRepository.findById(form.getIdRaza())
                .orElseThrow(() -> new IllegalArgumentException("Raza inválida"));
        if (!raza.getEspecie().getIdEspecie().equals(especie.getIdEspecie())) {
            throw new IllegalArgumentException("La raza seleccionada no pertenece a la especie de la mascota");
        }
        return raza;
    }
}
