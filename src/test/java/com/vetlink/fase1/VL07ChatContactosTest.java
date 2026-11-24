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
    void duenioIniciaConversacionConUnaVeterinariaYVeAlVeterinarioQueLoAtendio() throws Exception {
        var clinicaA = crearClinica();
        var recepcionA = crearRecepcionista(clinicaA, true);
        var vetA = crearVeterinario(clinicaA);
        var vetSinAtender = crearVeterinario(clinicaA);
        var duenio = crearDuenio();
        crearCita(crearMascota(duenio), vetA, clinicaA, LocalDate.now().minusDays(3), LocalTime.of(9, 0), EstadoCita.ATENDIDA);
        sincronizar();

        var contactos = ids(contactosChat.contactosPermitidos(usuarioRepository.findById(duenio.getIdUsuario()).orElseThrow()));
        assertThat(contactos).contains(vetA.getIdUsuario())
                .doesNotContain(vetSinAtender.getIdUsuario(), duenio.getIdUsuario());

        mockMvc.perform(post("/chat/veterinaria/" + clinicaA.getIdVeterinaria()).with(csrf()).with(como(duenio)))
                .andExpect(redirectedUrlPattern("/chat/*"));
        sincronizar();
        var conv = conversacionRepository.findByUsuarioA_IdUsuarioAndUsuarioB_IdUsuario(duenio.getIdUsuario(), recepcionA.getIdUsuario());
        assertThat(conv).isPresent();
        assertThat(conv.get().getVeterinaria().getIdVeterinaria()).isEqualTo(clinicaA.getIdVeterinaria());
    }

    @Test
    void laBandejaSoloMuestraConversacionesPropias() throws Exception {
        var clinica = crearClinica();
        crearRecepcionista(clinica, true);
        var duenio = crearDuenio();
        var otro = crearDuenio();
        mockMvc.perform(post("/chat/veterinaria/" + clinica.getIdVeterinaria()).with(csrf()).with(como(otro)));
        sincronizar();

        var modelo = mockMvc.perform(get("/chat").with(como(duenio))).andReturn().getModelAndView().getModel();
        assertThat((Collection<?>) modelo.get("bandeja")).isEmpty();
        var conv = conversacionRepository.findAll().stream()
                .filter(c -> c.participa(otro.getIdUsuario())).findFirst().orElseThrow();
        mockMvc.perform(get("/chat/" + conv.getIdConversacion()).with(como(duenio)))
                .andExpect(status().isForbidden());
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

        // El veterinario solo ve a tutores de pacientes que ha atendido (no a todo el personal ni a cualquier dueño).
        var deVet = ids(contactosChat.contactosPermitidos(usuarioRepository.findById(vet.getIdUsuario()).orElseThrow()));
        assertThat(deVet).doesNotContain(duenioConCita.getIdUsuario(), recepcion.getIdUsuario(), vet2.getIdUsuario());
        var duenioAtendido = crearDuenio();
        crearCita(crearMascota(duenioAtendido), vet2, clinica, LocalDate.now().minusDays(1), LocalTime.of(10, 0), EstadoCita.ATENDIDA);
        sincronizar();
        var deVet2 = ids(contactosChat.contactosPermitidos(usuarioRepository.findById(vet2.getIdUsuario()).orElseThrow()));
        assertThat(deVet2).containsExactly(duenioAtendido.getIdUsuario());
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
