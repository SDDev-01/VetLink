package com.vetlink.comun.validacion;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reglas de texto compartidas por todos los formularios (la misma regla existe en
 * static/js/vl-validate.js con {@code data-tipo="nombre"} y {@code data-tipo="telefono"}).
 *
 * <ul>
 *   <li><b>Nombres</b> (personas y mascotas): letras con tildes, ñ, espacios y los separadores
 *       apóstrofo, guion y punto. Sin números. Debe empezar con una letra.</li>
 *   <li><b>Teléfonos</b>: solo dígitos (7 a 15).</li>
 * </ul>
 * No se aplica a direcciones, razones sociales ni descripciones, que pueden llevar números.
 */
public final class Textos {

    /** Letras (incluye tildes, ü y ñ) separadas por espacios, apóstrofos, guiones o puntos. */
    public static final String REGEX_NOMBRE = "^[\\p{L}\\p{M}]+(?:[ '’.\\-]{1,2}[\\p{L}\\p{M}]+)*\\.?$";
    public static final String MENSAJE_NOMBRE = "Usa solo letras, espacios, guiones o apóstrofos (sin números)";

    public static final String REGEX_TELEFONO = "^\\d{7,15}$";
    public static final String MENSAJE_TELEFONO = "El teléfono solo puede tener números (entre 7 y 15 dígitos)";

    private static final Pattern NOMBRE = Pattern.compile(REGEX_NOMBRE);
    private static final Pattern TELEFONO = Pattern.compile(REGEX_TELEFONO);
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    private Textos() {
    }

    /** Quita espacios al inicio y al final y deja un solo espacio entre palabras. null queda null. */
    public static String normalizar(String texto) {
        if (texto == null) return null;
        return ESPACIOS.matcher(texto.strip()).replaceAll(" ");
    }

    public static boolean esNombreValido(String texto) {
        String n = normalizar(texto);
        return n != null && !n.isEmpty() && NOMBRE.matcher(n).matches();
    }

    public static boolean esTelefonoValido(String texto) {
        return texto != null && TELEFONO.matcher(texto.strip()).matches();
    }

    /** Clave para comparar sin importar mayúsculas ni espacios (p. ej. razas duplicadas). */
    public static String claveComparacion(String texto) {
        String n = normalizar(texto);
        return n == null ? "" : n.toLowerCase(Locale.ROOT);
    }

    /** «pastor  ganadero» → «Pastor Ganadero». Respeta tildes y ñ. */
    public static String capitalizarPalabras(String texto) {
        String n = normalizar(texto);
        if (n == null || n.isEmpty()) return n;
        StringBuilder sb = new StringBuilder(n.length());
        boolean inicio = true;
        for (char ch : n.toCharArray()) {
            sb.append(inicio ? Character.toUpperCase(ch) : ch);
            inicio = ch == ' ' || ch == '-';
        }
        return sb.toString();
    }
}
