package com.vetlink.fase1;

import com.vetlink.emergencia.model.Emergencia;
import com.vetlink.emergencia.model.EstadoEmergencia;
import com.vetlink.emergencia.repository.EmergenciaRepository;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.Veterinaria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-04: reglas de cambio de estado de emergencias por rol. */
class VL04EmergenciaEstadoTest extends PruebaWebBase {

    @Autowired EmergenciaRepository emergenciaRepository;

    private Emergencia crearEmergencia(Mascota mascota, Veterinaria clinica, Usuario vet, EstadoEmergencia estado) {
        Emergencia e = new Emergencia();
        e.setMascota(mascota);
        e.setVeterinaria(clinica);
        e.setVeterinario(vet);
        e.setMotivo("Dificultad respiratoria");
        e.setEstado(estado);
        e.setCreadaEn(LocalDateTime.now());
        return emergenciaRepository.save(e);
    }

    private EstadoEmergencia estadoDe(Emergencia e) {
        sincronizar();
        return emergenciaRepository.findById(e.getIdEmergencia()).orElseThrow().getEstado();
    }

    @Test
    void duenioNoCambiaEmergenciaAjena() throws Exception {
        var clinica = crearClinica();
        var e = crearEmergencia(crearMascota(crearDuenio()), clinica, null, EstadoEmergencia.PENDIENTE);
        var otroDuenio = crearDuenio();

        mockMvc.perform(post("/emergencias/" + e.getIdEmergencia() + "/estado").with(csrf()).with(como(otroDuenio))
                        .param("estado", "ATENDIDA"))
                .andExpect(status().isForbidden());

        assertThat(estadoDe(e)).isEqualTo(EstadoEmergencia.PENDIENTE);
    }

    @Test
    void duenioCancelaSuEmergenciaPendiente() throws Exception {
        var duenio = crearDuenio();
        var e = crearEmergencia(crearMascota(duenio), crearClinica(), null, EstadoEmergencia.PENDIENTE);

        mockMvc.perform(post("/emergencias/" + e.getIdEmergencia() + "/estado").with(csrf()).with(como(duenio))
                        .param("estado", "CANCELADA"))
                .andExpect(redirectedUrl("/emergencias?actualizada"));

        assertThat(estadoDe(e)).isEqualTo(EstadoEmergencia.CANCELADA);
    }

    @Test
    void recepcionistaNoMarcaAtendida() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        var e = crearEmergencia(crearMascota(crearDuenio()), clinica, crearVeterinario(clinica), EstadoEmergencia.ASIGNADA);

        mockMvc.perform(post("/emergencias/" + e.getIdEmergencia() + "/estado").with(csrf()).with(como(recepcion))
                        .param("estado", "ATENDIDA"))
                .andExpect(status().isForbidden());

        assertThat(estadoDe(e)).isEqualTo(EstadoEmergencia.ASIGNADA);
    }

    @Test
    void recepcionistaDeOtraClinicaNoCancela() throws Exception {
        var clinica = crearClinica();
        var e = crearEmergencia(crearMascota(crearDuenio()), clinica, null, EstadoEmergencia.PENDIENTE);
        var recepcionOtra = crearRecepcionista(crearClinica(), false);

        mockMvc.perform(post("/emergencias/" + e.getIdEmergencia() + "/estado").with(csrf()).with(como(recepcionOtra))
                        .param("estado", "CANCELADA"))
                .andExpect(status().isForbidden());
    }

    @Test
    void veterinarioAsignadoMarcaAtendida() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var e = crearEmergencia(crearMascota(crearDuenio()), clinica, vet, EstadoEmergencia.ASIGNADA);

        mockMvc.perform(post("/emergencias/" + e.getIdEmergencia() + "/estado").with(csrf()).with(como(vet))
                        .param("estado", "ATENDIDA"))
                .andExpect(redirectedUrl("/emergencias?actualizada"));

        assertThat(estadoDe(e)).isEqualTo(EstadoEmergencia.ATENDIDA);
    }

    @Test
    void veterinarioNoAsignadoEsRechazado() throws Exception {
        var clinica = crearClinica();
        var e = crearEmergencia(crearMascota(crearDuenio()), clinica, crearVeterinario(clinica), EstadoEmergencia.ASIGNADA);

        mockMvc.perform(post("/emergencias/" + e.getIdEmergencia() + "/estado").with(csrf())
                        .with(como(crearVeterinario(clinica))).param("estado", "ATENDIDA"))
                .andExpect(status().isForbidden());
    }

    @Test
    void emergenciaAtendidaNoSePuedeCambiar() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var e = crearEmergencia(crearMascota(crearDuenio()), clinica, vet, EstadoEmergencia.ATENDIDA);

        mockMvc.perform(post("/emergencias/" + e.getIdEmergencia() + "/estado").with(csrf()).with(como(vet))
                        .param("estado", "ASIGNADA"))
                .andExpect(redirectedUrl("/emergencias"))
                .andExpect(flash().attribute("error", "La emergencia ya está cerrada"));

        assertThat(estadoDe(e)).isEqualTo(EstadoEmergencia.ATENDIDA);
    }

    @Test
    void listaSeMuestraParaCadaRol() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        crearEmergencia(crearMascota(duenio), clinica, vet, EstadoEmergencia.ASIGNADA);
        sincronizar();

        mockMvc.perform(get("/emergencias").with(como(duenio))).andExpect(status().isOk());
        mockMvc.perform(get("/emergencias").with(como(vet))).andExpect(status().isOk());
        mockMvc.perform(get("/emergencias").with(como(crearRecepcionista(clinica, false)))).andExpect(status().isOk());
    }
}
