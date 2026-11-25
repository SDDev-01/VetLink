package com.vetlink.mascota.service;

import com.vetlink.comun.validacion.Textos;
import com.vetlink.mascota.model.Especie;
import com.vetlink.mascota.model.Raza;
import com.vetlink.mascota.repository.RazaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * DUE-02: catálogo de razas ordenado alfabéticamente (en español) y razas personalizadas
 * («Otra raza») sin duplicados por mayúsculas, minúsculas o espacios.
 */
@Service
public class RazaCatalogoService {

    /** Una raza escrita a mano: letras, números (p. ej. «F1»), espacios, guiones, apóstrofos, puntos y paréntesis. */
    private static final Pattern RAZA = Pattern.compile("^(?=.*\\p{L})[\\p{L}\\p{M}0-9 '’.()\\-]{2,45}$");
    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");

    private final RazaRepository razas;

    public RazaCatalogoService(RazaRepository razas) {
        this.razas = razas;
    }

    private static Comparator<Raza> orden() {
        Collator c = Collator.getInstance(Locale.forLanguageTag("es-CO"));
        c.setStrength(Collator.PRIMARY);
        return Comparator.comparing(Raza::getNombreRaza, c);
    }

    /** Todas las razas, alfabéticamente (el formulario las filtra por especie). */
    public List<Raza> todasOrdenadas() {
        return razas.findAll().stream().sorted(orden()).toList();
    }

    /** Raza de la especie con el mismo nombre sin importar mayúsculas ni espacios. */
    public Optional<Raza> buscarEquivalente(Integer idEspecie, String nombre) {
        return buscar(idEspecie, nombre, Textos::claveComparacion);
    }

    private Optional<Raza> buscar(Integer idEspecie, String nombre, Function<String, String> clave) {
        String buscada = clave.apply(nombre);
        return razas.findByEspecie_IdEspecie(idEspecie).stream()
                .filter(r -> clave.apply(r.getNombreRaza()).equals(buscada))
                .findFirst();
    }

    private static String sinTildes(String s) {
        return MARCAS.matcher(Normalizer.normalize(Textos.claveComparacion(s), Normalizer.Form.NFD)).replaceAll("");
    }

    /**
     * Devuelve la raza del catálogo equivalente al nombre escrito o la crea como personalizada.
     * La inserción es atómica (INSERT IGNORE + índice único (nombreRaza, idEspecie)): si dos
     * dueños la registran a la vez, ambos terminan usando la misma fila.
     */
    @Transactional
    public Raza obtenerOCrearPersonalizada(Especie especie, String nombreEscrito) {
        String nombre = Textos.normalizar(nombreEscrito);
        if (nombre == null || nombre.isEmpty()) {
            throw new IllegalArgumentException("Escribe el nombre de la raza");
        }
        if (!RAZA.matcher(nombre).matches()) {
            throw new IllegalArgumentException("Escribe un nombre de raza válido (2 a 45 caracteres, con letras)");
        }
        Integer idEspecie = especie.getIdEspecie();
        Optional<Raza> existente = buscarEquivalente(idEspecie, nombre);
        if (existente.isPresent()) return existente.get();
        razas.insertarSiNoExiste(Textos.capitalizarPalabras(nombre), idEspecie);
        // MySQL compara con una colación que ignora mayúsculas y tildes: si ya existía una
        // variante con otra tilde, la inserción se ignoró y se reutiliza esa raza.
        return buscarEquivalente(idEspecie, nombre)
                .or(() -> buscar(idEspecie, nombre, RazaCatalogoService::sinTildes))
                .orElseThrow(() -> new IllegalStateException("No se pudo registrar la raza. Intenta de nuevo."));
    }
}
