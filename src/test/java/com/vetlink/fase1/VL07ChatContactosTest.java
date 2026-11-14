package com.vetlink.fase1;

import com.vetlink.chat.repository.ConversacionRepository;
import com.vetlink.chat.service.ContactosChatService;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-07: el chat solo muestra y permite contactos con relación clínica. */
class VL07ChatContactosTest extends PruebaWebBase {

    @Autowired ContactosChatService contactosChat;
    @Autowired ConversacionRepository conversacionRepository;

    private static Collection<Integer> ids(Collection<Usuario> usuarios) {
        return usuarios.stream().map(Usuario::getIdUsuario).toList();
    }

    @Test
    void duenioSinCitasVeListaVacia() throws Exception {
        crearRecepcionista(crearClinica(), true);
        var duenio = crearDuenio();
        sincronizar();

        mockMvc.perform(get("/chat").with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("contactos", org.hamcrest.Matchers.empty()));
    }

    @Test
    void duenioNoPuedeEscribirleAOtroDuenio() throws Exception {
        var duenio = crearDuenio();
        var otro = crearDuenio();
        long antes = conversacionRepository.count();

        mockMvc.perform(post("/chat/nuevo/" + otro.getIdUsuario()).with(csrf()).with(como(duenio)))
                .andExpect(status().isForbidden());

        assertThat(conversacionRepository.count()).isEqualTo(antes);
    }

    @Test
    void duenioConCitaVeALaRecepcionistaYPuedeAbrirChat() throws Exception {
        var clinicaA = crearClinica();
        var recepcionA = crearRecepcionista(clinicaA, false);
        var vetA = crearVeterinario(clinicaA);
        var recepcionB = crearRecepcionista(crearClinica(), false);
        var duenio = crearDuenio();
        crearCita(crearMascota(duenio), vetA, clinicaA);
        sincronizar();

        var contactos = ids(contactosChat.contactosPermitidos(usuarioRepository.findById(duenio.getIdUsuario()).orElseThrow()));
        assertThat(contactos).contains(recepcionA.getIdUsuario(), vetA.getIdUsuario())
                .doesNotContain(recepcionB.getIdUsuario(), duenio.getIdUsuario());

        mockMvc.perform(post("/chat/nuevo/" + recepcionA.getIdUsuario()).with(csrf()).with(como(duenio)))
                .andExpect(redirectedUrlPattern("/chat/*"));
    }

    @Test
    void citaCanceladaNoDaContactoAlDuenio() {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        var duenio = crearDuenio();
        crearCita(crearMascota(duenio), crearVeterinario(clinica), clinica,
                LocalDate.now().plusDays(2), LocalTime.of(9, 0), EstadoCita.CANCELADA);
        sincronizar();

        assertThat(ids(contactosChat.contactosPermitidos(usuarioRepository.findById(duenio.getIdUsuario()).orElseThrow())))
                .doesNotContain(recepcion.getIdUsuario());
    }

    @Test
    void recepcionistaYVeterinarioVenDueniosYPersonalDeSuClinica() {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, true);
        var vet = crearVeterinario(clinica);
        var vet2 = crearVeterinario(clinica);
        var duenioConCita = crearDuenio();
        var duenioSinCita = crearDuenio();
        crearCita(crearMascota(duenioConCita), vet, clinica);
        var inactivo = crearVeterinario(clinica);
        inactivo.setEstado((byte) 0);
        sincronizar();

        var deRecepcion = ids(contactosChat.contactosPermitidos(usuarioRepository.findById(recepcion.getIdUsuario()).orElseThrow()));
        assertThat(deRecepcion).contains(duenioConCita.getIdUsuario(), vet.getIdUsuario(), vet2.getIdUsuario())
                .doesNotContain(duenioSinCita.getIdUsuario(), recepcion.getIdUsuario(), inactivo.getIdUsuario());

        var deVet2 = ids(contactosChat.contactosPermitidos(usuarioRepository.findById(vet2.getIdUsuario()).orElseThrow()));
        assertThat(deVet2).contains(recepcion.getIdUsuario(), vet.getIdUsuario())
                .doesNotContain(duenioConCita.getIdUsuario());
    }

    @Test
    void usuarioSinRolNoApareceNiRecibeMensajes() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        Usuario sinRol = crearUsuarioBase("SinRol");
        sincronizar();

        assertThat(ids(contactosChat.contactosPermitidos(usuarioRepository.findById(recepcion.getIdUsuario()).orElseThrow())))
                .doesNotContain(sinRol.getIdUsuario());
        mockMvc.perform(post("/chat/nuevo/" + sinRol.getIdUsuario()).with(csrf()).with(como(recepcion)))
                .andExpect(status().isForbidden());
    }
}
