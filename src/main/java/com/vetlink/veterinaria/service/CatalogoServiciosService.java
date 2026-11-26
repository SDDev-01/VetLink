package com.vetlink.veterinaria.service;

import com.vetlink.comun.validacion.Textos;
import com.vetlink.veterinaria.model.Servicio;
import com.vetlink.veterinaria.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Catálogo general de servicios (tabla servicio). Es la única fuente de las opciones del
 * selector de servicios y el único punto donde se crea un servicio personalizado («Otros»).
 *
 * <p>Los duplicados se detectan sin importar mayúsculas, espacios ni tildes, igual que la
 * intercalación de MySQL ({@code utf8mb4_0900_ai_ci}): «Ecografia» es el mismo servicio que
 * «Ecografía». Antes la comparación sí distinguía tildes y el INSERT violaba
 * {@code nombreServicio_UNIQUE}.</p>
 */
@Service
public class CatalogoServiciosService {

    public static final String MENSAJE_NOMBRE = "Escribe un nombre de servicio válido (3 a 45 caracteres, con letras)";
    private static final Pattern NOMBRE_SERVICIO = Pattern.compile("^(?=.*\\p{L})[\\p{L}\\p{M}0-9 '’.,()/+\\-]{3,45}$");

    private final ServicioRepository servicios;

    public CatalogoServiciosService(ServicioRepository servicios) {
        this.servicios = servicios;
    }

    /** Catálogo ordenado alfabéticamente (sin tildes) para los selectores. */
    @Transactional(readOnly = true)
    public List<Servicio> catalogo() {
        return servicios.findAll().stream()
                .sorted(Comparator.comparing(s -> Textos.claveSinTildes(s.getNombreServicio())))
                .toList();
    }

    /** Servicio del catálogo con el mismo nombre (sin importar mayúsculas, espacios ni tildes). */
    @Transactional(readOnly = true)
    public Optional<Servicio> buscarPorNombre(String nombre) {
        String clave = Textos.claveSinTildes(nombre);
        if (clave.isEmpty()) return Optional.empty();
        return servicios.findAll().stream()
                .filter(s -> Textos.claveSinTildes(s.getNombreServicio()).equals(clave))
                .findFirst();
    }

    /** Valida el nombre de un servicio personalizado y lo devuelve normalizado («ecografia  felina» → «Ecografia Felina»). */
    public static String normalizarNombre(String nombre) {
        String n = Textos.capitalizarPalabras(nombre);
        if (n == null || !NOMBRE_SERVICIO.matcher(n).matches()) {
            throw new IllegalArgumentException(MENSAJE_NOMBRE);
        }
        return n;
    }

    /**
     * Servicio para la opción «Otros»: si ya existe en el catálogo (aunque se escriba distinto)
     * se reutiliza; si no, se crea como servicio clínico que no es de urgencias.
     */
    @Transactional
    public Servicio obtenerOCrear(String nombre, String descripcion, Servicio.TipoRegistro tipoRegistro) {
        String n = normalizarNombre(nombre);
        return buscarPorNombre(n).orElseGet(() -> {
            Servicio s = new Servicio();
            s.setNombreServicio(n);
            s.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.strip());
            s.setTipoRegistro(tipoRegistro == null ? Servicio.TipoRegistro.CLINICO : tipoRegistro);
            s.setEsEmergencia(false);
            return servicios.saveAndFlush(s);
        });
    }
}
