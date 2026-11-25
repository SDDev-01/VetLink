package com.vetlink.v9;

import com.vetlink.disponibilidad.model.EstadoAprobacion;
import com.vetlink.disponibilidad.repository.HorarioVeterinarioRepository;
import com.vetlink.emergencia.model.Emergencia;
import com.vetlink.emergencia.model.EstadoEmergencia;
import com.vetlink.emergencia.repository.EmergenciaRepository;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.Veterinaria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Requisitos del rol VETERINARIO (VET-01 a VET-05). */
class VeterinarioV9Test extends PruebaWebBase {

    @Autowired EmergenciaRepository emergencias;
    @Autowired HorarioVeterinarioRepository horarios;

    private Emergencia emergenciaAsignada(Mascota mascota, Veterinaria clinica, Usuario vet) {
        Emergencia e = new Emergencia();
        e.setMascota(mascota);
        e.setSolicitante(mascota.getTutor());
        e.setVeterinaria(clinica);
        e.setVeterinario(vet);
        e.setMotivo("Convulsiones");
        e.setEstado(EstadoEmergencia.ASIGNADA);
        e.setCreadaEn(LocalDateTime.now().minusMinutes(20));
        return emergencias.save(e);
    }

    private MockHttpServletRequestBuilder atender(Emergencia e, Usuario vet, Mascota mascota) {
        return atender(e, vet, mascota, "Crisis epiléptica");
    }

    private MockHttpServletRequestBuilder atender(Emergencia e, Usuario vet, Mascota mascota, String diagnostico) {
        return post("/emergencias/" + e.getIdEmergencia() + "/atender").with(csrf()).with(como(vet))
                .param("idMascota", mascota.getIdMascota().toString())
                .param("tipoRegistro", "URGENCIA")
                .param("motivoConsulta", "Convulsiones")
                .param("diagnostico", diagnostico)
                .param("tratamiento", "Diazepam");
    }

    private EstadoEmergencia estado(Emergencia e) {
        sincronizar();
        return emergencias.findById(e.getIdEmergencia()).orElseThrow().getEstado();
    }

    // ------------------------------------------------------------------ VET-03

    @Test
    void guardarElRegistroClinicoCierraLaEmergenciaYQuedanLigados() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        var e = emergenciaAsignada(mascota, clinica, vet);

        mockMvc.perform(get("/emergencias/" + e.getIdEmergencia() + "/atender").with(como(vet)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Guardar y cerrar emergencia")));
        mockMvc.perform(atender(e, vet, mascota))
                .andExpect(redirectedUrl("/emergencias/" + e.getIdEmergencia()));

        assertThat(estado(e)).isEqualTo(EstadoEmergencia.ATENDIDA);
        var h = historialRepository.findByEmergencia_IdEmergencia(e.getIdEmergencia()).orElseThrow();
        assertThat(h.getMascota().getIdMascota()).isEqualTo(mascota.getIdMascota());
        assertThat(h.getVeterinario().getIdUsuario()).isEqualTo(vet.getIdUsuario());
        assertThat(h.getTipoRegistro()).isEqualTo("URGENCIA");
        assertThat(emergencias.findById(e.getIdEmergencia()).orElseThrow().getAtendidaEn()).isNotNull();
        // El dueño ve el nuevo estado.
        mockMvc.perform(get("/emergencias").with(como(mascota.getTutor()))).andExpect(content().string(containsString("Atendida")));
    }

    @Test
    void siElRegistroNoEsValidoLaEmergenciaNoCambia() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        var e = emergenciaAsignada(mascota, clinica, vet);

        mockMvc.perform(atender(e, vet, mascota, ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "diagnostico"));

        assertThat(estado(e)).isEqualTo(EstadoEmergencia.ASIGNADA);
        assertThat(historialRepository.findByEmergencia_IdEmergencia(e.getIdEmergencia())).isEmpty();
    }

    @Test
    void registroDeOtraMascotaNoCierraLaEmergencia() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        var otra = crearMascota(crearDuenio());
        var e = emergenciaAsignada(mascota, clinica, vet);

        mockMvc.perform(atender(e, vet, otra))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorGeneral", "La mascota no coincide con la emergencia"));

        assertThat(estado(e)).isEqualTo(EstadoEmergencia.ASIGNADA);
        assertThat(historialRepository.findByEmergencia_IdEmergencia(e.getIdEmergencia())).isEmpty();
    }

    @Test
    void reintentoNoDuplicaElRegistro() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        var e = emergenciaAsignada(mascota, clinica, vet);

        mockMvc.perform(atender(e, vet, mascota)).andExpect(status().is3xxRedirection());
        sincronizar();
        mockMvc.perform(atender(e, vet, mascota))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensajeOk", "Esta emergencia ya fue atendida y tiene su registro clínico."));

        sincronizar();
        assertThat(historialRepository.findAll().stream()
                .filter(h -> h.getEmergencia() != null && h.getEmergencia().getIdEmergencia().equals(e.getIdEmergencia())))
                .hasSize(1);
    }

    @Test
    void veterinarioNoAsignadoNoPuedeAtenderNiMarcarAtendida() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var otroVet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        var e = emergenciaAsignada(mascota, clinica, vet);

        mockMvc.perform(atender(e, otroVet, mascota)).andExpect(status().isForbidden());
        mockMvc.perform(post("/emergencias/" + e.getIdEmergencia() + "/estado").with(csrf()).with(como(vet))
                .param("estado", "ATENDIDA")).andExpect(status().isForbidden());
        assertThat(estado(e)).isEqualTo(EstadoEmergencia.ASIGNADA);
    }

    @Test
    void detalleYaNoTieneBotonDeMarcarAtendida() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var e = emergenciaAsignada(crearMascota(crearDuenio()), clinica, vet);

        mockMvc.perform(get("/emergencias/" + e.getIdEmergencia()).with(como(vet)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Marcar atendida"))))
                .andExpect(content().string(containsString("Registrar atención en el historial")));
    }

    // ------------------------------------------------------------------ VET-01

    @Test
    void solicitudDeHorarioValidaQuedaPendienteYLaInvalidaSeRechaza() throws Exception {
        var vet = crearVeterinario(crearClinica());

        mockMvc.perform(post("/disponibilidad/horario").with(csrf()).with(como(vet))
                        .param("dia", "1", "1", "3").param("inicio", "08:00", "14:00", "09:00").param("fin", "12:00", "18:00", "13:00"))
                .andExpect(flash().attribute("mensajeOk", "Solicitud de horario enviada a recepción."));
        sincronizar();
        assertThat(horarios.findAll().stream().filter(h -> h.getVeterinario().getIdUsuario().equals(vet.getIdUsuario())
                && h.getEstado() == EstadoAprobacion.PENDIENTE)).hasSize(3);

        mockMvc.perform(post("/disponibilidad/horario").with(csrf()).with(como(vet))
                        .param("dia", "2", "2").param("inicio", "08:00", "11:00").param("fin", "12:00", "15:00"))
                .andExpect(flash().attribute("errorGeneral", "Hay franjas que se cruzan el mismo día"));
        mockMvc.perform(post("/disponibilidad/horario").with(csrf()).with(como(vet))
                        .param("dia", "2").param("inicio", "12:00").param("fin", "08:00"))
                .andExpect(flash().attribute("errorGeneral", "En cada franja la hora de inicio debe ser anterior a la de fin"));
    }

    @Test
    void pantallaDeDisponibilidadMuestraLaSemanaYLasNovedades() throws Exception {
        var vet = crearVeterinario(crearClinica());
        mockMvc.perform(get("/disponibilidad").with(como(vet)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("vl-dia-card")))
                .andExpect(content().string(containsString("Domingo")))
                .andExpect(content().string(containsString("data-posterior-a=\"inicioA\"")));
    }

    // ------------------------------------------------------------------ VET-04 / VET-05

    @Test
    void perfilDelVeterinarioMuestraSuVeterinariaYGuardaCambios() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        String correo = correoRepository.findByUsuario_IdUsuario(vet.getIdUsuario()).get(0).getCorreo();

        mockMvc.perform(get("/perfil").with(como(vet)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Veterinaria vinculada")))
                .andExpect(content().string(containsString(clinica.getNombre())))
                .andExpect(content().string(containsString(vet.getVeterinario().getLicencia())));

        mockMvc.perform(post("/perfil").with(csrf()).with(como(vet))
                        .param("nombre", "Laura").param("apellido", "Gómez").param("telefono", "3104445566").param("correo", correo))
                .andExpect(redirectedUrl("/perfil?actualizado"));
        sincronizar();
        assertThat(usuarioRepository.findById(vet.getIdUsuario()).orElseThrow().getNombre()).isEqualTo("Laura");
        // La vinculación institucional no cambió.
        assertThat(veterinarioRepository.findById(vet.getIdUsuario()).orElseThrow().getVeterinaria().getIdVeterinaria())
                .isEqualTo(clinica.getIdVeterinaria());
    }

    @Test
    void perfilIgnoraIntentosDeCambiarLaVinculacion() throws Exception {
        var clinica = crearClinica();
        var otra = crearClinica();
        var vet = crearVeterinario(clinica);
        String correo = correoRepository.findByUsuario_IdUsuario(vet.getIdUsuario()).get(0).getCorreo();

        mockMvc.perform(post("/perfil").with(csrf()).with(como(vet))
                        .param("nombre", "Laura").param("apellido", "Gómez").param("telefono", "3104445567").param("correo", correo)
                        .param("idVeterinaria", otra.getIdVeterinaria().toString()).param("rol", "RECEPCIONISTA"))
                .andExpect(redirectedUrl("/perfil?actualizado"));
        sincronizar();
        var v = veterinarioRepository.findById(vet.getIdUsuario()).orElseThrow();
        assertThat(v.getVeterinaria().getIdVeterinaria()).isEqualTo(clinica.getIdVeterinaria());
        assertThat(usuarioRepository.findById(vet.getIdUsuario()).orElseThrow().getRecepcionista()).isNull();
    }

    @Test
    void veterinarioSinVeterinariaVeUnEstadoInformativo() throws Exception {
        var vet = crearVeterinario(crearClinica());
        var v = veterinarioRepository.findById(vet.getIdUsuario()).orElseThrow();
        v.setVeterinaria(null);
        veterinarioRepository.save(v);
        sincronizar();
        var fresco = usuarioRepository.findById(vet.getIdUsuario()).orElseThrow();

        mockMvc.perform(get("/perfil").with(como(fresco)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No estás vinculado(a) a ninguna veterinaria")));
    }
}
