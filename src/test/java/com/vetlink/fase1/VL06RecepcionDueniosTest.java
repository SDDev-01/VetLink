package com.vetlink.fase1;

import com.vetlink.cita.dto.AgendarCitaForm;
import com.vetlink.cita.service.CitaService;
import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-06: recepción ya no ve a todos los dueños ni mascotas, y valida el dueño al crear. */
class VL06RecepcionDueniosTest extends PruebaWebBase {

    @Autowired CitaService citaService;

    @Test
    void agendarNoMuestraMascotasNoBuscadas() throws Exception {
        var clinicaA = crearClinica();
        var clinicaB = crearClinica();
        var recepcionA = crearRecepcionista(clinicaA, false);
        var mascotaOtraClinica = crearMascota(crearDuenio());
        crearCita(mascotaOtraClinica, crearVeterinario(clinicaB), clinicaB);
        sincronizar();

        mockMvc.perform(get("/citas/recepcion/agendar").with(como(recepcionA)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(mascotaOtraClinica.getNombre()))));
    }

    @Test
    void buscarDuenioPorDocumentoMuestraSoloSusMascotasActivas() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        var duenio = crearDuenio();
        var activa = crearMascota(duenio);
        var inactiva = crearMascota(duenio);
        inactiva.setEstado((byte) 0);
        var deOtro = crearMascota(crearDuenio());
        sincronizar();

        mockMvc.perform(get("/citas/recepcion/agendar").with(como(recepcion))
                        .param("buscarDuenio", duenio.getNumeroDocumento()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(activa.getNombre())))
                .andExpect(content().string(not(containsString(inactiva.getNombre()))))
                .andExpect(content().string(not(containsString(deOtro.getNombre()))));
    }

    @Test
    void buscarDuenioPorCorreoFunciona() throws Exception {
        var recepcion = crearRecepcionista(crearClinica(), false);
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        String correo = correoRepository.findByUsuario_IdUsuario(duenio.getIdUsuario()).get(0).getCorreo();
        sincronizar();

        mockMvc.perform(get("/citas/recepcion/agendar").with(como(recepcion))
                        .param("buscarDuenio", correo.toUpperCase()))
                .andExpect(content().string(containsString(mascota.getNombre())));
    }

    @Test
    void duenioInexistenteMuestraMensaje() throws Exception {
        var recepcion = crearRecepcionista(crearClinica(), false);

        mockMvc.perform(get("/recepcion/mascotas/nueva").with(como(recepcion)).param("buscarDuenio", "NO-EXISTE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay un dueño registrado con ese documento o correo")));
    }

    @Test
    void crearMascotaANombreDeUnVeterinarioEsRechazado() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        var vet = crearVeterinario(clinica);
        var especie = especiePorDefecto();
        var raza = crearRaza(especie, "Criolla");
        long antes = mascotaRepository.count();

        mockMvc.perform(post("/recepcion/mascotas/nueva").with(csrf()).with(como(recepcion))
                        .param("idTutor", vet.getIdUsuario().toString())
                        .param("nombre", "Firulais")
                        .param("idEspecie", especie.getIdEspecie().toString())
                        .param("idRaza", raza.getIdRaza().toString())
                        .param("genero", "M")
                        .param("fechaNacimiento", LocalDate.now().minusYears(1).toString())
                        .param("peso", "8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dueño no válido")));

        assertThat(mascotaRepository.count()).isEqualTo(antes);
    }

    @Test
    void recepcionNoAgendaMascotaInactiva() {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        mascota.setEstado((byte) 0);
        sincronizar();

        AgendarCitaForm form = new AgendarCitaForm();
        form.setIdMascota(mascota.getIdMascota());
        form.setIdVeterinaria(clinica.getIdVeterinaria());
        form.setIdVeterinario(vet.getIdUsuario());
        form.setIdServicio(servicioPorDefecto().getIdServicio());
        form.setFecha(LocalDate.now().plusDays(1));
        form.setHora(LocalTime.of(9, 0));

        assertThatThrownBy(() -> citaService.agendarComoRecepcionista(form, clinica.getIdVeterinaria()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La mascota no está activa");
    }
}
