package com.vetlink.fase2;

import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-09: una clínica recién registrada queda pendiente y no es pública. */
class VL09ClinicaPendienteTest extends PruebaWebBase {

    @Test
    void clinicaNuevaQuedaPendienteYNoApareceEnDirectorio() throws Exception {
        var activa = crearClinica();
        var recepcion = registrarClinicaNueva("Clinica Pendiente Zeta");
        var clinica = recepcion.getRecepcionista().getVeterinaria();

        assertThat(clinica.getEstado()).isEqualTo((byte) 0);
        mockMvc.perform(get("/directorio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(activa.getNombre())))
                .andExpect(content().string(not(containsString("Clinica Pendiente Zeta"))));
        mockMvc.perform(get("/directorio/" + clinica.getIdVeterinaria()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Clinica Pendiente Zeta"))));
        mockMvc.perform(get("/directorio").param("q", "Pendiente Zeta"))
                .andExpect(content().string(not(containsString("Clinica Pendiente Zeta"))));
    }

    @Test
    void recepcionistaPrincipalEntraAlDashboardYVeElAviso() throws Exception {
        var recepcion = registrarClinicaNueva("Clinica Aviso");

        mockMvc.perform(get("/dashboard").with(como(recepcion)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tu clínica está pendiente de verificación")));
        mockMvc.perform(get("/veterinarias/admin").with(como(recepcion)))
                .andExpect(status().isOk());
    }

    @Test
    void clinicaVerificadaNoMuestraAviso() throws Exception {
        var recepcion = crearRecepcionista(crearClinica(), true);
        sincronizar();

        mockMvc.perform(get("/dashboard").with(como(recepcion)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("pendiente de verificación"))));
    }
}
