package com.vetlink.fase2;

import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.historial.service.HistorialClinicoService;
import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-12: el historial muestra fecha y hora completas y se ordena del más reciente al más antiguo. */
class VL12FechaHistorialTest extends PruebaWebBase {

    @Autowired HistorialClinicoService historialService;

    @Test
    void listaOrdenadaDelMasRecienteAlMasAntiguoConAntiguosAlFinal() {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        var antiguo = crearHistorial(mascota, vet, null);
        var haceUnMes = crearHistorial(mascota, vet, LocalDateTime.now().minusMonths(1));
        var ayer = crearHistorial(mascota, vet, LocalDateTime.now().minusDays(1));
        var haceUnAnio = crearHistorial(mascota, vet, LocalDateTime.now().minusYears(1));
        sincronizar();

        var esperado = new Integer[]{ayer.getNumeroHistorial(), haceUnMes.getNumeroHistorial(),
                haceUnAnio.getNumeroHistorial(), antiguo.getNumeroHistorial()};
        assertThat(historialService.listarPorMascota(mascota.getIdMascota()))
                .extracting(HistorialClinico::getNumeroHistorial).containsExactly(esperado);
        assertThat(historialService.listarPorTutor(duenio.getIdUsuario()))
                .extracting(HistorialClinico::getNumeroHistorial).containsExactly(esperado);
    }

    @Test
    void pantallaMuestraFechaCompletaOAproximada() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        crearCita(mascota, vet, clinica);
        crearHistorial(mascota, vet, null);
        crearHistorial(mascota, vet, LocalDateTime.of(2026, 3, 5, 14, 30));
        sincronizar();

        for (String url : new String[]{"/historial/mascota/" + mascota.getIdMascota(), "/historial/mias",
                "/mascotas/" + mascota.getIdMascota()}) {
            mockMvc.perform(get(url).with(como(duenio)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("05/03/2026 14:30")))
                    .andExpect(content().string(containsString("Año 2025 (fecha aproximada)")));
        }
        mockMvc.perform(get("/historial/paciente/" + mascota.getIdMascota()).with(como(vet)))
                .andExpect(content().string(containsString("05/03/2026 14:30")));
    }
}
