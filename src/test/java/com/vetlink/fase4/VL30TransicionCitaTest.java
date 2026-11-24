package com.vetlink.fase4;

import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.model.TransicionCita;
import com.vetlink.cita.model.TransicionCita.Actor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Máquina de estados de citas: transiciones permitidas por actor (prueba unitaria, sin Spring). */
class VL30TransicionCitaTest {

    @Test
    void flujoPrincipal() {
        assertThat(TransicionCita.permitida(EstadoCita.DISPONIBLE, EstadoCita.PENDIENTE, Actor.TUTOR)).isTrue();
        assertThat(TransicionCita.permitida(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA, Actor.RECEPCIONISTA)).isTrue();
        assertThat(TransicionCita.permitida(EstadoCita.CONFIRMADA, EstadoCita.EN_ATENCION, Actor.VETERINARIO)).isTrue();
        assertThat(TransicionCita.permitida(EstadoCita.EN_ATENCION, EstadoCita.ATENDIDA, Actor.VETERINARIO)).isTrue();
    }

    @Test
    void actoresNoAutorizados() {
        assertThat(TransicionCita.permitida(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA, Actor.TUTOR)).isFalse();
        assertThat(TransicionCita.permitida(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA, Actor.VETERINARIO)).isFalse();
        assertThat(TransicionCita.permitida(EstadoCita.CONFIRMADA, EstadoCita.EN_ATENCION, Actor.RECEPCIONISTA)).isFalse();
        assertThat(TransicionCita.permitida(EstadoCita.EN_ATENCION, EstadoCita.ATENDIDA, Actor.RECEPCIONISTA)).isFalse();
        assertThat(TransicionCita.permitida(EstadoCita.CONFIRMADA, EstadoCita.REPROGRAMADA, Actor.TUTOR)).isFalse();
    }

    @Test
    void noSeSaltaElFlujoClinico() {
        // No se marca atendida sin haber iniciado la atención.
        assertThat(TransicionCita.permitida(EstadoCita.CONFIRMADA, EstadoCita.ATENDIDA, Actor.VETERINARIO)).isFalse();
        assertThat(TransicionCita.permitida(EstadoCita.PENDIENTE, EstadoCita.EN_ATENCION, Actor.VETERINARIO)).isFalse();
        // Las citas cerradas no se reabren.
        for (EstadoCita cerrada : new EstadoCita[]{EstadoCita.ATENDIDA, EstadoCita.CANCELADA, EstadoCita.RECHAZADA,
                EstadoCita.NO_ASISTIO, EstadoCita.REPROGRAMADA}) {
            for (Actor a : Actor.values()) {
                assertThat(TransicionCita.destinos(cerrada, a)).as(cerrada + " por " + a).isEmpty();
            }
        }
    }

    @Test
    void exigirLanzaMensajeLegible() {
        assertThatThrownBy(() -> TransicionCita.exigir(EstadoCita.CANCELADA, EstadoCita.CONFIRMADA, Actor.RECEPCIONISTA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("«Cancelada»").hasMessageContaining("«Confirmada»");
    }

    @Test
    void soloLasReservasYAtencionesOcupanHorario() {
        assertThat(EstadoCita.OCUPAN_HORARIO).containsExactlyInAnyOrder(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA,
                EstadoCita.EN_ATENCION, EstadoCita.ATENDIDA);
    }
}
