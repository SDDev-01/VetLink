package com.vetlink.cita.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Máquina de estados de las citas: qué transición está permitida y qué actor puede hacerla.
 * Es la única fuente de verdad; el backend la aplica siempre y las vistas la consultan para
 * mostrar u ocultar acciones.
 *
 * <pre>
 * DISPONIBLE  → PENDIENTE    TUTOR (reserva; bloquea el cupo)
 * DISPONIBLE  → CONFIRMADA   RECEPCIONISTA (reserva hecha en recepción)
 * PENDIENTE   → CONFIRMADA   RECEPCIONISTA
 * PENDIENTE   → RECHAZADA    RECEPCIONISTA, SISTEMA (expiración)
 * PENDIENTE   → CANCELADA    TUTOR, RECEPCIONISTA
 * PENDIENTE   → REPROGRAMADA RECEPCIONISTA
 * CONFIRMADA  → CANCELADA    TUTOR, RECEPCIONISTA
 * CONFIRMADA  → REPROGRAMADA RECEPCIONISTA
 * CONFIRMADA  → EN_ATENCION  VETERINARIO asignado (dentro de la ventana de inicio)
 * CONFIRMADA  → NO_ASISTIO   RECEPCIONISTA, VETERINARIO (vencida la ventana de inicio)
 * EN_ATENCION → ATENDIDA     VETERINARIO asignado (con el registro obligatorio guardado)
 * </pre>
 */
public final class TransicionCita {

    public enum Actor { TUTOR, RECEPCIONISTA, VETERINARIO, SISTEMA }

    private static final Map<EstadoCita, Map<EstadoCita, Set<Actor>>> REGLAS = new EnumMap<>(EstadoCita.class);

    static {
        permitir(EstadoCita.DISPONIBLE, EstadoCita.PENDIENTE, Actor.TUTOR);
        permitir(EstadoCita.DISPONIBLE, EstadoCita.CONFIRMADA, Actor.RECEPCIONISTA);
        permitir(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA, Actor.RECEPCIONISTA);
        permitir(EstadoCita.PENDIENTE, EstadoCita.RECHAZADA, Actor.RECEPCIONISTA, Actor.SISTEMA);
        permitir(EstadoCita.PENDIENTE, EstadoCita.CANCELADA, Actor.TUTOR, Actor.RECEPCIONISTA);
        permitir(EstadoCita.PENDIENTE, EstadoCita.REPROGRAMADA, Actor.RECEPCIONISTA);
        permitir(EstadoCita.CONFIRMADA, EstadoCita.CANCELADA, Actor.TUTOR, Actor.RECEPCIONISTA);
        permitir(EstadoCita.CONFIRMADA, EstadoCita.REPROGRAMADA, Actor.RECEPCIONISTA);
        permitir(EstadoCita.CONFIRMADA, EstadoCita.EN_ATENCION, Actor.VETERINARIO);
        permitir(EstadoCita.CONFIRMADA, EstadoCita.NO_ASISTIO, Actor.RECEPCIONISTA, Actor.VETERINARIO);
        permitir(EstadoCita.EN_ATENCION, EstadoCita.ATENDIDA, Actor.VETERINARIO);
    }

    private TransicionCita() {
    }

    private static void permitir(EstadoCita desde, EstadoCita hacia, Actor... actores) {
        REGLAS.computeIfAbsent(desde, k -> new EnumMap<>(EstadoCita.class))
                .put(hacia, actores.length == 0 ? EnumSet.noneOf(Actor.class) : EnumSet.of(actores[0], actores));
    }

    public static boolean permitida(EstadoCita desde, EstadoCita hacia, Actor actor) {
        if (desde == null || hacia == null || actor == null) return false;
        Set<Actor> actores = REGLAS.getOrDefault(desde, Map.of()).get(hacia);
        return actores != null && actores.contains(actor);
    }

    /** Lanza IllegalStateException con un mensaje legible si la transición no está permitida. */
    public static void exigir(EstadoCita desde, EstadoCita hacia, Actor actor) {
        if (!permitida(desde, hacia, actor)) {
            throw new IllegalStateException("No se puede pasar una cita de «" + etiqueta(desde) + "» a «"
                    + etiqueta(hacia) + "».");
        }
    }

    public static Set<EstadoCita> destinos(EstadoCita desde, Actor actor) {
        Set<EstadoCita> r = EnumSet.noneOf(EstadoCita.class);
        REGLAS.getOrDefault(desde, Map.of()).forEach((hacia, actores) -> {
            if (actores.contains(actor)) r.add(hacia);
        });
        return r;
    }

    private static String etiqueta(EstadoCita e) {
        return e == null ? "sin estado" : e.getEtiqueta();
    }
}
