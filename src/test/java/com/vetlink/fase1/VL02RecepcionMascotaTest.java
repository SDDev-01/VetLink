package com.vetlink.fase1;

import com.vetlink.mascota.model.Mascota;
import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-02: la recepcionista solo edita mascotas que son pacientes de su clínica. */
class VL02RecepcionMascotaTest extends PruebaWebBase {

    private MockHttpServletRequestBuilder postEditar(Mascota m, String nuevoNombre) {
        return post("/recepcion/mascotas/" + m.getIdMascota() + "/editar").with(csrf())
                .param("nombre", nuevoNombre)
                .param("idEspecie", m.getEspecie().getIdEspecie().toString())
                .param("idRaza", m.getRaza().getIdRaza().toString())
                .param("genero", "M")
                .param("fechaNacimiento", m.getFechaNacimiento().toString())
                .param("peso", "12.5");
    }

    @Test
    void getDeMascotaSinCitasEnMiClinicaEsRechazado() throws Exception {
        var clinicaA = crearClinica();
        var clinicaB = crearClinica();
        var recepcionA = crearRecepcionista(clinicaA, false);
        var mascota = crearMascota(crearDuenio());
        crearCita(mascota, crearVeterinario(clinicaB), clinicaB);

        mockMvc.perform(get("/recepcion/mascotas/" + mascota.getIdMascota() + "/editar").with(como(recepcionA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void postDeMascotaAjenaEsRechazadoYNoCambia() throws Exception {
        var clinicaA = crearClinica();
        var recepcionA = crearRecepcionista(clinicaA, false);
        var mascota = crearMascota(crearDuenio());
        String nombreOriginal = mascota.getNombre();

        mockMvc.perform(postEditar(mascota, "Hackeada").with(como(recepcionA)))
                .andExpect(status().isForbidden());

        sincronizar();
        assertThat(mascotaRepository.findById(mascota.getIdMascota()).orElseThrow().getNombre())
                .isEqualTo(nombreOriginal);
    }

    @Test
    void mascotaConCitaEnMiClinicaSePuedeEditar() throws Exception {
        var clinicaA = crearClinica();
        var recepcionA = crearRecepcionista(clinicaA, false);
        var mascota = crearMascota(crearDuenio());
        crearCita(mascota, crearVeterinario(clinicaA), clinicaA);
        sincronizar();

        mockMvc.perform(get("/recepcion/mascotas/" + mascota.getIdMascota() + "/editar").with(como(recepcionA)))
                .andExpect(status().isOk());
    }
}
