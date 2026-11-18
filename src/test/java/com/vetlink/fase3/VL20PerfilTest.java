package com.vetlink.fase3;

import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.TelefonoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-20: contraseña segura con la actual, teléfono que se crea y cambio de correo. */
class VL20PerfilTest extends PruebaWebBase {

    @Autowired TelefonoRepository telefonoRepository;

    private String correoDe(Usuario u) {
        return correoRepository.findByUsuario_IdUsuario(u.getIdUsuario()).get(0).getCorreo();
    }

    private MockHttpServletRequestBuilder perfil(Usuario u) {
        return post("/perfil").with(csrf()).with(como(u))
                .param("nombre", u.getNombre())
                .param("apellido", u.getApellido())
                .param("telefono", "3009998877")
                .param("correo", correoDe(u));
    }

    @Test
    void nuevaPasswordDeCuatroCaracteresDaError() throws Exception {
        var u = crearDuenio();
        String hashAntes = u.getPassword();

        mockMvc.perform(perfil(u).param("passwordActual", PASSWORD_PRUEBAS).param("nuevaPassword", "abcd"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nuevaPassword"));

        sincronizar();
        assertThat(usuarioRepository.findById(u.getIdUsuario()).orElseThrow().getPassword()).isEqualTo(hashAntes);
    }

    @Test
    void passwordActualIncorrectaDaError() throws Exception {
        var u = crearDuenio();

        mockMvc.perform(perfil(u).param("passwordActual", "otraClave999").param("nuevaPassword", "NuevaClave2026"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "passwordActual"));
    }

    @Test
    void cambiarPasswordCorrectamenteCierraLaSesion() throws Exception {
        var u = crearDuenio();

        mockMvc.perform(perfil(u).param("passwordActual", PASSWORD_PRUEBAS).param("nuevaPassword", "NuevaClave2026"))
                .andExpect(redirectedUrl("/login?passwordActualizada"));

        sincronizar();
        assertThat(passwordEncoder.matches("NuevaClave2026",
                usuarioRepository.findById(u.getIdUsuario()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void correoUsadoPorOtraCuentaDaError() throws Exception {
        var u = crearDuenio();
        var otro = crearDuenio();

        mockMvc.perform(post("/perfil").with(csrf()).with(como(u))
                        .param("nombre", u.getNombre()).param("apellido", u.getApellido())
                        .param("telefono", "3009998877")
                        .param("correo", correoDe(otro).toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "correo"));
    }

    @Test
    void cambiarCorreoActualizaLaFilaYPideIniciarSesion() throws Exception {
        var u = crearDuenio();
        String nuevo = "nuevo" + siguiente() + "@pruebas.vetlink";

        mockMvc.perform(post("/perfil").with(csrf()).with(como(u))
                        .param("nombre", u.getNombre()).param("apellido", u.getApellido())
                        .param("telefono", "3009998877")
                        .param("correo", nuevo))
                .andExpect(redirectedUrl("/login?correoActualizado"));

        sincronizar();
        assertThat(correoDe(u)).isEqualTo(nuevo);
        assertThat(usuarioRepository.findByCorreo(nuevo)).isPresent();
    }

    @Test
    void usuarioSinTelefonoGuardaUnoYQuedaLaFila() throws Exception {
        var u = crearDuenio();
        assertThat(telefonoRepository.findByUsuario_IdUsuario(u.getIdUsuario())).isEmpty();

        mockMvc.perform(perfil(u))
                .andExpect(redirectedUrl("/perfil?actualizado"));

        sincronizar();
        var telefonos = telefonoRepository.findByUsuario_IdUsuario(u.getIdUsuario());
        assertThat(telefonos).hasSize(1);
        assertThat(telefonos.get(0).getNumeroTelefono()).isEqualTo("3009998877");
    }
}
