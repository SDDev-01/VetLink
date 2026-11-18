package com.vetlink.fase3;

import com.vetlink.support.PruebaWebBase;
import com.vetlink.veterinaria.service.VeterinariaService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-21: los errores del panel de administración no terminan en 500 ni se cortan en la URL. */
class VL21ErroresAdminTest extends PruebaWebBase {

    @MockitoSpyBean VeterinariaService veterinariaService;

    @Test
    void recepcionistaConCorreoRepetidoMuestraErrorSin500() throws Exception {
        var clinica = crearClinica();
        var principal = crearRecepcionista(clinica, true);
        String correoRepetido = correoRepository.findByUsuario_IdUsuario(crearDuenio().getIdUsuario()).get(0).getCorreo();

        mockMvc.perform(post("/veterinarias/admin/recepcionistas").with(csrf()).with(como(principal))
                        .param("numeroDocumento", "REP" + siguiente())
                        .param("idTipoDocumento", tipoDocumento().getIdTipoDocumento().toString())
                        .param("nombre", "Rosa").param("apellido", "Repetida")
                        .param("correo", correoRepetido)
                        .param("telefono", "3001234567")
                        .param("password", "Clave12345"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ya existe una cuenta con ese correo")));
    }

    @Test
    void recepcionistaConDatosInvalidosMuestraErrorPorCampo() throws Exception {
        var principal = crearRecepcionista(crearClinica(), true);

        mockMvc.perform(post("/veterinarias/admin/recepcionistas").with(csrf()).with(como(principal))
                        .param("numeroDocumento", "REP" + siguiente())
                        .param("idTipoDocumento", tipoDocumento().getIdTipoDocumento().toString())
                        .param("nombre", "Rosa").param("apellido", "Mal")
                        .param("correo", "no-es-un-correo")
                        .param("telefono", "3001234567")
                        .param("password", "Clave12345"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("formEquipo", "correo"))
                .andExpect(content().string(containsString("Ingresa un correo válido")));
    }

    @Test
    void errorConTildesYAmpersandSeMuestraCompleto() throws Exception {
        var clinica = crearClinica();
        var principal = crearRecepcionista(clinica, true);
        String mensaje = "El nombre «Peluquería & Spa Ñandú» no es válido";
        doThrow(new IllegalArgumentException(mensaje)).when(veterinariaService)
                .actualizarInfo(anyInt(), anyInt(), any(), any(), any(), any());

        mockMvc.perform(post("/veterinarias/admin/info").with(csrf()).with(como(principal))
                        .param("nombre", "Peluquería & Spa Ñandú"))
                .andExpect(redirectedUrl("/veterinarias/admin"))
                .andExpect(flash().attribute("error", mensaje));

        sincronizar();
        mockMvc.perform(get("/veterinarias/admin").with(como(principal)).flashAttr("error", mensaje))
                .andExpect(content().string(containsString("Peluquería &amp; Spa Ñandú")));
    }

    @Test
    void veterinarioDesdeElPanelComoLoEnviaElNavegador() throws Exception {
        var principal = crearRecepcionista(crearClinica(), true);
        String doc = "VP" + siguiente();

        mockMvc.perform(post("/veterinarias/admin/equipo").with(csrf()).with(como(principal))
                        .param("numeroDocumento", doc)
                        .param("idTipoDocumento", tipoDocumento().getIdTipoDocumento().toString())
                        .param("nombre", "Luis").param("apellido", "Vet")
                        .param("correo", "vp" + siguiente() + "@pruebas.vetlink")
                        .param("telefono", "3001234567").param("password", "Clave12345")
                        .param("licencia", "L-1"))
                .andExpect(redirectedUrl("/veterinarias/admin?veterinarioCreado"));

        org.assertj.core.api.Assertions.assertThat(usuarioRepository.findByNumeroDocumento(doc)).isPresent();
    }
}
