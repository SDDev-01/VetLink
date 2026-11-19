package com.vetlink.pasaporte;

import com.vetlink.mascota.model.Mascota;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pantalla "Compartir historial" (mascotas/pasaporte) y su botón en la ficha de la mascota. */
class PantallaCompartirTest extends PruebaWebBase {

    @MockitoBean
    PasaporteService pasaporteService;

    private Usuario duenio;
    private Mascota mascota;
    private String ruta;

    @BeforeEach
    void preparar() {
        duenio = crearDuenio();
        mascota = crearMascota(duenio);
        ruta = "/mascotas/" + mascota.getIdMascota() + "/pasaporte";
    }

    @Test
    void muestraLosPasaportesConSuEstadoQrYBotonRevocar() throws Exception {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 19, 10, 15);
        when(pasaporteService.listar(mascota.getIdMascota())).thenReturn(List.of(
                new Pasaporte("tokActivo", "http://localhost/p/tokActivo", ahora, ahora.plusDays(1), null, "ACTIVO", 2),
                new Pasaporte("tokRevocado", "http://localhost/p/tokRevocado", ahora.minusDays(3), ahora.plusDays(4),
                        ahora.minusDays(1), "REVOCADO", 0)));

        mockMvc.perform(get(ruta).with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(mascota.getNombre())))
                .andExpect(content().string(containsString("/p/tokActivo/qr.png")))
                .andExpect(content().string(containsString("http://localhost/p/tokActivo")))
                .andExpect(content().string(containsString(ruta + "/tokActivo/revocar")))
                .andExpect(content().string(containsString("2 aperturas")))
                .andExpect(content().string(containsString("Revocado")))
                .andExpect(content().string(not(containsString("/p/tokRevocado/qr.png"))))
                .andExpect(content().string(not(containsString(ruta + "/tokRevocado/revocar"))))
                .andExpect(content().string(containsString("7 días")))
                .andExpect(content().string(containsString("30 días")));
    }

    @Test
    void sinPasaportesMuestraElMensajeVacio() throws Exception {
        when(pasaporteService.listar(mascota.getIdMascota())).thenReturn(List.of());

        mockMvc.perform(get(ruta).with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Todavía no has compartido el historial")));
    }

    @Test
    void conFastApiApagadoLaPantallaMuestraElMensajeYNoUn500() throws Exception {
        when(pasaporteService.listar(mascota.getIdMascota()))
                .thenThrow(new ServicioNoDisponibleException(new RuntimeException("caído")));

        mockMvc.perform(get(ruta).with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(ServicioNoDisponibleException.MENSAJE)));
    }

    @Test
    void laFichaDeLaMascotaTieneElBotonCompartir() throws Exception {
        mockMvc.perform(get("/mascotas/" + mascota.getIdMascota()).with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(ruta)))
                .andExpect(content().string(containsString("Compartir historial")));
    }
}
