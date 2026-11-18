package com.vetlink.fase3;

import com.vetlink.cita.model.EstadoCita;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-25: las verificaciones usan consultas directas y muestran los mismos datos. */
class VL25ConsultasDirectasTest extends PruebaWebBase {

    @Test
    void agendaSoloDejaVerLasCitasPropiasODeLaClinica() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var cita = crearCita(crearMascota(crearDuenio()), vet, clinica);
        sincronizar();

        mockMvc.perform(get("/agenda/" + cita.getIdCita()).with(como(vet))).andExpect(status().isOk());
        mockMvc.perform(get("/agenda/" + cita.getIdCita()).with(como(crearRecepcionista(clinica, false))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/agenda/" + cita.getIdCita()).with(como(crearVeterinario(clinica))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/agenda/" + cita.getIdCita()).with(como(crearRecepcionista(crearClinica(), false))))
                .andExpect(status().isForbidden());
    }

    @Test
    void veterinariosDeUnaClinica() throws Exception {
        var clinica = crearClinica();
        var vet1 = crearVeterinario(clinica);
        crearVeterinario(clinica);
        crearVeterinario(crearClinica());
        sincronizar();

        mockMvc.perform(get("/citas/veterinarios/" + clinica.getIdVeterinaria()).with(como(crearDuenio())))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.id == " + vet1.getIdUsuario() + ")].nombre",
                        org.hamcrest.Matchers.contains(vet1.getNombre() + " " + vet1.getApellido())));
    }

    @Test
    void emergenciasDeRecepcionListanSoloVeterinariosDeSuClinica() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var vetOtra = crearVeterinario(crearClinica());
        var recepcion = crearRecepcionista(clinica, false);
        sincronizar();

        var model = mockMvc.perform(get("/emergencias").with(como(recepcion)))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        @SuppressWarnings("unchecked")
        var ids = ((Collection<com.vetlink.usuario.model.Usuario>) model.get("veterinarios")).stream()
                .map(com.vetlink.usuario.model.Usuario::getIdUsuario).toList();
        assertThat(ids).containsExactly(vet.getIdUsuario()).doesNotContain(vetOtra.getIdUsuario());
    }

    @Test
    void pacientesSinDuplicadosYSinRomperseConCuposVacios() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var recepcion = crearRecepcionista(clinica, false);
        var m1 = crearMascota(crearDuenio());
        var m2 = crearMascota(crearDuenio());
        var ajena = crearMascota(crearDuenio());
        crearCita(m1, vet, clinica);
        crearCita(m1, vet, clinica, LocalDate.now().minusDays(10), LocalTime.of(9, 0), EstadoCita.ATENDIDA);
        crearCita(m2, crearVeterinario(clinica), clinica);
        crearCita(null, vet, clinica, LocalDate.now().plusDays(3), LocalTime.of(9, 0), EstadoCita.DISPONIBLE);
        var otraClinica = crearClinica();
        crearCita(ajena, crearVeterinario(otraClinica), otraClinica);
        sincronizar();

        assertThat(pacientes(vet)).containsExactly(m1.getIdMascota());
        assertThat(pacientes(recepcion)).containsExactlyInAnyOrder(m1.getIdMascota(), m2.getIdMascota());
    }

    private Collection<Integer> pacientes(com.vetlink.usuario.model.Usuario u) throws Exception {
        var model = mockMvc.perform(get("/historial/pacientes").with(como(u)))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        @SuppressWarnings("unchecked")
        var lista = (Collection<Mascota>) model.get("pacientes");
        return lista.stream().map(Mascota::getIdMascota).toList();
    }
}
