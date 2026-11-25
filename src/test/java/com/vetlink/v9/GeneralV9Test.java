package com.vetlink.v9;

import com.vetlink.chat.model.Conversacion;
import com.vetlink.chat.repository.ConversacionRepository;
import com.vetlink.chat.repository.MensajeChatRepository;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GEN-01 (chats) y GEN-02 (perfiles de todos los roles). */
class GeneralV9Test extends PruebaWebBase {

    @Autowired ConversacionRepository conversaciones;
    @Autowired MensajeChatRepository mensajes;

    private Conversacion conversacionDuenioClinica(Usuario duenio, Usuario recepcion) throws Exception {
        var clinica = recepcion.getRecepcionista().getVeterinaria();
        String destino = mockMvc.perform(post("/chat/veterinaria/" + clinica.getIdVeterinaria()).with(csrf()).with(como(duenio)))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        sincronizar();
        assertThat(destino).startsWith("/chat/");
        return conversaciones.findById(Integer.valueOf(destino.substring("/chat/".length()))).orElseThrow();
    }

    @Test
    void mensajesVaciosOSoloEspaciosNoSeGuardan() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, true);
        var duenio = crearDuenio();
        var c = conversacionDuenioClinica(duenio, recepcion);
        long antes = mensajes.count();

        for (String vacio : new String[]{"", "    ", "\n\n  \t"}) {
            mockMvc.perform(post("/chat/" + c.getIdConversacion() + "/mensajes").with(csrf()).with(como(duenio)).param("contenido", vacio))
                    .andExpect(redirectedUrl("/chat/" + c.getIdConversacion()))
                    .andExpect(flash().attribute("errorGeneral", "Escribe un mensaje antes de enviarlo."));
        }
        sincronizar();
        assertThat(mensajes.count()).isEqualTo(antes);
    }

    @Test
    void losSaltosDeLineaSeConservanYSeMuestran() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, true);
        var duenio = crearDuenio();
        var c = conversacionDuenioClinica(duenio, recepcion);

        mockMvc.perform(post("/chat/" + c.getIdConversacion() + "/mensajes").with(csrf()).with(como(duenio))
                        .param("contenido", "Hola\nmi perro no come\n\ndesde ayer"))
                .andExpect(redirectedUrl("/chat/" + c.getIdConversacion()));
        sincronizar();
        assertThat(mensajes.findByConversacion_IdConversacionOrderByEnviadoEnAsc(c.getIdConversacion()))
                .extracting("contenido").contains("Hola\nmi perro no come\n\ndesde ayer");
        mockMvc.perform(get("/chat/" + c.getIdConversacion()).with(como(duenio)))
                .andExpect(content().string(containsString("data-vl-chat")))
                .andExpect(content().string(containsString("vl-chat-texto")))
                .andExpect(content().string(containsString("/js/vl-chat.js")))
                .andExpect(content().string(containsString("Shift")));
    }

    @Test
    void perfilDeCadaRolSeMuestraEnModoConsulta() throws Exception {
        var clinica = crearClinica();
        for (Usuario u : new Usuario[]{crearDuenio(), crearVeterinario(clinica), crearRecepcionista(clinica, true)}) {
            mockMvc.perform(get("/perfil").with(como(u)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Editar perfil")))
                    .andExpect(content().string(containsString("Información personal")))
                    .andExpect(content().string(containsString("Guardar cambios")))
                    .andExpect(content().string(containsString("Cancelar")));
        }
    }

    @Test
    void recepcionistaEditaSuPerfilYVeSuVeterinaria() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        String correo = correoRepository.findByUsuario_IdUsuario(recepcion.getIdUsuario()).get(0).getCorreo();
        mockMvc.perform(get("/perfil").with(como(recepcion)))
                .andExpect(content().string(containsString(clinica.getNombre())));
        mockMvc.perform(post("/perfil").with(csrf()).with(como(recepcion))
                        .param("nombre", "Sofía").param("apellido", "Muñoz").param("telefono", "3201112233").param("correo", correo))
                .andExpect(redirectedUrl("/perfil?actualizado"));
        sincronizar();
        assertThat(usuarioRepository.findById(recepcion.getIdUsuario()).orElseThrow().getNombre()).isEqualTo("Sofía");
        assertThat(recepcionistaRepository.findById(recepcion.getIdUsuario()).orElseThrow().getEsPrincipal()).isFalse();
    }
}
