package com.vetlink.pasaporte;

import com.vetlink.mascota.model.Mascota;
import com.vetlink.pasaporte.model.DuracionPasaporte;
import com.vetlink.pasaporte.model.Pasaporte;
import com.vetlink.pasaporte.service.PasaporteService;
import com.vetlink.pasaporte.service.ServicioNoDisponibleException;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controlador "Compartir historial": solo el dueño, y nunca un 500 si FastAPI no responde. */
class PasaporteControllerTest extends PruebaWebBase {

    @MockitoBean
    PasaporteService pasaporteService;

    private Usuario duenio;
    private Usuario otroDuenio;
    private Mascota mascota;
    private String ruta;

    @BeforeEach
    void preparar() {
        duenio = crearDuenio();
        otroDuenio = crearDuenio();
        mascota = crearMascota(duenio);
        ruta = "/mascotas/" + mascota.getIdMascota() + "/pasaporte";
    }

    private static Pasaporte activo(String token) {
        return new Pasaporte(token, "http://localhost/p/" + token, LocalDateTime.now(),
                LocalDateTime.now().plusDays(1), null, "ACTIVO", 0);
    }

    @Test
    void otroDuenioRecibe403AlCrear() throws Exception {
        mockMvc.perform(post(ruta).param("horas", "24").with(csrf()).with(como(otroDuenio)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pasaporteService);
    }

    @Test
    void otroDuenioRecibe403AlVerYAlRevocar() throws Exception {
        mockMvc.perform(get(ruta).with(como(otroDuenio)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ruta + "/t1/revocar").with(csrf()).with(como(otroDuenio)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pasaporteService);
    }

    @Test
    void elDuenioCreaUnPasaporte() throws Exception {
        Pasaporte creado = activo("nuevo");
        when(pasaporteService.crear(mascota.getIdMascota(), duenio.getIdUsuario(), DuracionPasaporte.HORAS_24))
                .thenReturn(creado);

        mockMvc.perform(post(ruta).param("horas", "24").with(csrf()).with(como(duenio)))
                .andExpect(redirectedUrl(ruta))
                .andExpect(flash().attribute("creado", creado));

        verify(pasaporteService).crear(mascota.getIdMascota(), duenio.getIdUsuario(), DuracionPasaporte.HORAS_24);
    }

    @Test
    void duracionNoPermitidaNoLlamaAlServicio() throws Exception {
        mockMvc.perform(post(ruta).param("horas", "48").with(csrf()).with(como(duenio)))
                .andExpect(redirectedUrl(ruta))
                .andExpect(flash().attributeExists("error"));

        verify(pasaporteService, never()).crear(any(), any(), any());
    }

    @Test
    void conFastApiApagadoCrearMuestraElMensajeYNoUn500() throws Exception {
        when(pasaporteService.crear(any(), any(), any()))
                .thenThrow(new ServicioNoDisponibleException(new RuntimeException("caído")));

        mockMvc.perform(post(ruta).param("horas", "168").with(csrf()).with(como(duenio)))
                .andExpect(redirectedUrl(ruta))
                .andExpect(flash().attribute("error", ServicioNoDisponibleException.MENSAJE));
    }

    @Test
    void elDuenioRevocaUnPasaporteDeSuMascota() throws Exception {
        when(pasaporteService.listar(mascota.getIdMascota())).thenReturn(List.of(activo("t1")));

        mockMvc.perform(post(ruta + "/t1/revocar").with(csrf()).with(como(duenio)))
                .andExpect(redirectedUrl(ruta))
                .andExpect(flash().attribute("revocado", true));

        verify(pasaporteService).revocar("t1");
    }

    @Test
    void noSePuedeRevocarUnTokenDeOtraMascota() throws Exception {
        when(pasaporteService.listar(mascota.getIdMascota())).thenReturn(List.of(activo("t1")));

        mockMvc.perform(post(ruta + "/de-otra-mascota/revocar").with(csrf()).with(como(duenio)))
                .andExpect(status().isNotFound());

        verify(pasaporteService, never()).revocar(any());
    }

    @Test
    void conFastApiApagadoRevocarMuestraElMensajeYNoUn500() throws Exception {
        when(pasaporteService.listar(mascota.getIdMascota()))
                .thenThrow(new ServicioNoDisponibleException(new RuntimeException("caído")));

        mockMvc.perform(post(ruta + "/t1/revocar").with(csrf()).with(como(duenio)))
                .andExpect(redirectedUrl(ruta))
                .andExpect(flash().attribute("error", ServicioNoDisponibleException.MENSAJE));
    }
}
