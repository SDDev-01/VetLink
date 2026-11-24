package com.vetlink.fase4;

import com.vetlink.cita.model.EstadoCita;
import com.vetlink.mascota.model.SituacionMascota;
import com.vetlink.mascota.model.TransferenciaMascota;
import com.vetlink.mascota.repository.MascotaEventoRepository;
import com.vetlink.mascota.service.GestionMascotaService;
import com.vetlink.pasaporte.service.PasaporteService;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Transferencia de mascotas (solicitud → aceptación) y situación (perdida, encontrada, fallecida). */
class VL34TransferenciaYEstadoMascotaTest extends PruebaWebBase {

    @Autowired GestionMascotaService gestion;
    @Autowired MascotaEventoRepository eventos;
    @MockitoBean PasaporteService pasaportes;

    private String correoDe(Usuario u) {
        return correoRepository.findByUsuario_IdUsuario(u.getIdUsuario()).get(0).getCorreo();
    }

    @Test
    void laPropiedadSoloCambiaCuandoElNuevoTutorAcepta() throws Exception {
        when(pasaportes.listar(org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        var origen = crearDuenio();
        var destino = crearDuenio();
        var mascota = crearMascota(origen);
        crearHistorial(mascota, crearVeterinario(crearClinica()), java.time.LocalDateTime.now());
        sincronizar();

        mockMvc.perform(post("/mascotas/" + mascota.getIdMascota() + "/transferir").with(csrf()).with(como(origen))
                        .param("destino", correoDe(destino)))
                .andExpect(flash().attributeExists("mensajeOk"));
        sincronizar();
        assertThat(mascotaRepository.findById(mascota.getIdMascota()).orElseThrow().getTutor().getIdUsuario())
                .isEqualTo(origen.getIdUsuario());
        TransferenciaMascota t = gestion.recibidasPendientes(destino.getIdUsuario()).get(0);

        mockMvc.perform(post("/mascotas/transferencias/" + t.getIdTransferencia() + "/aceptar").with(csrf()).with(como(destino)))
                .andExpect(flash().attributeExists("mensajeOk"));
        sincronizar();
        var transferida = mascotaRepository.findById(mascota.getIdMascota()).orElseThrow();
        assertThat(transferida.getTutor().getIdUsuario()).isEqualTo(destino.getIdUsuario());
        // El historial clínico se conserva y ahora lo ve el nuevo tutor; el anterior pierde el acceso.
        assertThat(historialRepository.listarPorMascotaRecientes(mascota.getIdMascota())).hasSize(1);
        mockMvc.perform(get("/historial/mascota/" + mascota.getIdMascota()).with(como(destino))).andExpect(status().isOk());
        mockMvc.perform(get("/historial/mascota/" + mascota.getIdMascota()).with(como(origen))).andExpect(status().isForbidden());
        assertThat(eventos.findByMascota_IdMascotaOrderByFechaDesc(mascota.getIdMascota()))
                .extracting(e -> e.getTipo()).contains("TRANSFERENCIA_SOLICITADA", "TRANSFERIDA");
    }

    @Test
    void rechazoConservaAlPropietarioYQuedaTrazabilidad() {
        var origen = crearDuenio();
        var destino = crearDuenio();
        var mascota = crearMascota(origen);
        sincronizar();
        var t = gestion.solicitarTransferencia(mascota.getIdMascota(), origen.getIdUsuario(), correoDe(destino), "Me mudo");
        gestion.rechazar(t.getIdTransferencia(), destino.getIdUsuario());
        sincronizar();
        assertThat(mascotaRepository.findById(mascota.getIdMascota()).orElseThrow().getTutor().getIdUsuario())
                .isEqualTo(origen.getIdUsuario());
        var guardada = gestion.enviadas(origen.getIdUsuario()).get(0);
        assertThat(guardada.getEstado()).isEqualTo(TransferenciaMascota.Estado.RECHAZADA);
        assertThat(guardada.getResueltaEn()).isNotNull();
    }

    @Test
    void validacionesDeTransferencia() {
        var origen = crearDuenio();
        var destino = crearDuenio();
        var mascota = crearMascota(origen);
        sincronizar();
        assertThatThrownBy(() -> gestion.solicitarTransferencia(mascota.getIdMascota(), origen.getIdUsuario(), correoDe(origen), null))
                .hasMessage("No puedes transferirte una mascota a ti mismo.");
        assertThatThrownBy(() -> gestion.solicitarTransferencia(mascota.getIdMascota(), origen.getIdUsuario(), "nadie@no.existe", null))
                .hasMessageContaining("No hay un tutor registrado");
        assertThatThrownBy(() -> gestion.solicitarTransferencia(mascota.getIdMascota(), destino.getIdUsuario(), correoDe(origen), null))
                .isInstanceOf(AccessDeniedException.class);
        var t = gestion.solicitarTransferencia(mascota.getIdMascota(), origen.getIdUsuario(), correoDe(destino), null);
        assertThatThrownBy(() -> gestion.solicitarTransferencia(mascota.getIdMascota(), origen.getIdUsuario(), correoDe(destino), null))
                .hasMessage("Ya hay una transferencia pendiente para esta mascota.");
        // Solo el destinatario acepta.
        assertThatThrownBy(() -> gestion.aceptar(t.getIdTransferencia(), origen.getIdUsuario())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void noSeTransfiereConCitasVigentes() {
        var origen = crearDuenio();
        var mascota = crearMascota(origen);
        var clinica = crearClinica();
        crearCita(mascota, crearVeterinario(clinica), clinica);
        sincronizar();
        assertThatThrownBy(() -> gestion.solicitarTransferencia(mascota.getIdMascota(), origen.getIdUsuario(), correoDe(crearDuenio()), null))
                .hasMessageContaining("citas pendientes, confirmadas o en atención");
    }

    @Test
    void perdidaEncontradaYFallecimiento() throws Exception {
        when(pasaportes.listar(org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        var clinica = crearClinica();
        var cita = crearCita(mascota, crearVeterinario(clinica), clinica, LocalDate.now().plusDays(4), LocalTime.of(9, 0),
                EstadoCita.CONFIRMADA);
        sincronizar();

        gestion.reportarPerdida(mascota.getIdMascota(), duenio.getIdUsuario(), "Parque");
        assertThat(mascotaRepository.findById(mascota.getIdMascota()).orElseThrow().getSituacion()).isEqualTo(SituacionMascota.PERDIDA);
        gestion.reportarEncontrada(mascota.getIdMascota(), duenio.getIdUsuario(), null);
        assertThat(mascotaRepository.findById(mascota.getIdMascota()).orElseThrow().getSituacion()).isEqualTo(SituacionMascota.ACTIVA);

        gestion.reportarFallecimiento(mascota.getIdMascota(), duenio.getIdUsuario(), null);
        sincronizar();
        var fallecida = mascotaRepository.findById(mascota.getIdMascota()).orElseThrow();
        assertThat(fallecida.getSituacion()).isEqualTo(SituacionMascota.FALLECIDA);
        assertThat(fallecida.isAgendable()).isFalse();
        // Sus citas futuras se cancelaron y no se le puede agendar.
        assertThat(citaRepository.findById(cita.getIdCita()).orElseThrow().getEstado()).isEqualTo(EstadoCita.CANCELADA);
        mockMvc.perform(get("/citas/agendar/" + mascota.getIdMascota()).with(como(duenio)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/mascotas"));
    }

    @Test
    void elBorradoDirectoYaNoExiste() throws Exception {
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        mockMvc.perform(post("/mascotas/" + mascota.getIdMascota() + "/eliminar").with(csrf()).with(como(duenio)))
                .andExpect(flash().attributeExists("errorMascota"));
        sincronizar();
        assertThat(mascotaRepository.findById(mascota.getIdMascota()).orElseThrow().getEstado()).isEqualTo((byte) 1);
    }
}
