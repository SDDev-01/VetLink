package com.vetlink.fase4;

import com.vetlink.cita.model.EstadoCita;
import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * VL-37: una mascota solo aparece al dueño donde también aparece en «Mis mascotas».
 *
 * Causa del caso «Max»: el seed de mydb.sql trae mascotas dadas de baja (estado 0) con citas
 * e historias. «Mis mascotas» filtraba por estado = 1, pero «Mis citas», el calendario del
 * panel y la ficha por URL directa no, así que el dueño veía en esas vistas una mascota que
 * no estaba en su lista. La regla quedó en un solo sitio (AccesoPacienteService.esDuenioVigente).
 */
class VL37MascotasDelDuenioEnTodasLasVistasTest extends PruebaWebBase {

    @Test
    void mascotaDadaDeBajaNoApareceEnNingunaVistaDelDuenio() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var activa = crearMascota(duenio);
        var deBaja = crearMascota(duenio);
        deBaja.setNombre("Maxbaja");
        deBaja.setEstado((byte) 0);
        crearCita(activa, vet, clinica, LocalDate.now().minusDays(3), LocalTime.of(9, 0), EstadoCita.ATENDIDA);
        var citaBaja = crearCita(deBaja, vet, clinica, LocalDate.now().minusDays(2), LocalTime.of(9, 0), EstadoCita.ATENDIDA);
        crearHistorial(deBaja, vet, LocalDateTime.now().minusDays(2));
        sincronizar();

        // Listas: ni «Mis mascotas» ni «Mis citas» (ambas pestañas) ni el panel la muestran.
        mockMvc.perform(get("/mascotas").with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(activa.getNombre())))
                .andExpect(content().string(not(containsString("Maxbaja"))));
        mockMvc.perform(get("/citas/mias").param("tab", "historial").with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(activa.getNombre())))
                .andExpect(content().string(not(containsString("Maxbaja"))));
        mockMvc.perform(get("/dashboard").with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Maxbaja"))));
        mockMvc.perform(get("/historial/mias").with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Maxbaja"))));

        // Peticiones directas: el backend las rechaza aunque el dueño conozca los id.
        mockMvc.perform(get("/mascotas/" + deBaja.getIdMascota()).with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/citas/" + citaBaja.getIdCita()).with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/citas/mascota/" + deBaja.getIdMascota()).with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/historial/mascota/" + deBaja.getIdMascota()).with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/carnet/" + deBaja.getIdMascota()).with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/mascotas/" + deBaja.getIdMascota() + "/pasaporte").with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/internal/auth/reportes").with(como(duenio))
                        .header("X-Original-URI", "/api/reportes/mascotas/" + deBaja.getIdMascota() + "/historial"))
                .andExpect(status().isForbidden());

        // El veterinario que la atendió conserva el acceso clínico (reglas de negocio sin cambios).
        mockMvc.perform(get("/historial/mascota/" + deBaja.getIdMascota()).with(como(vet))).andExpect(status().isOk());
    }

    @Test
    void unDuenioNoAccedeALasMascotasDeOtroPorUrlDirecta() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var otro = crearDuenio();
        var ajena = crearMascota(otro);
        var cita = crearCita(ajena, vet, clinica, LocalDate.now().minusDays(1), LocalTime.of(9, 0), EstadoCita.ATENDIDA);
        var h = crearHistorial(ajena, vet, LocalDateTime.now().minusDays(1));
        sincronizar();

        mockMvc.perform(get("/mascotas").with(como(duenio)))
                .andExpect(content().string(not(containsString(ajena.getNombre()))));
        mockMvc.perform(get("/citas/mias").param("tab", "historial").with(como(duenio)))
                .andExpect(content().string(not(containsString(ajena.getNombre()))));

        mockMvc.perform(get("/mascotas/" + ajena.getIdMascota()).with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/mascotas/" + ajena.getIdMascota() + "/editar").with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/mascotas/" + ajena.getIdMascota() + "/foto").with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/citas/" + cita.getIdCita()).with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/citas/mascota/" + ajena.getIdMascota()).with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/historial/mascota/" + ajena.getIdMascota()).with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/historial/documento/" + h.getNumeroHistorial()).with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/carnet/" + ajena.getIdMascota()).with(como(duenio))).andExpect(status().isForbidden());
        mockMvc.perform(get("/mascotas/" + ajena.getIdMascota() + "/pasaporte").with(como(duenio))).andExpect(status().isForbidden());

        // Su dueño sí accede.
        mockMvc.perform(get("/mascotas/" + ajena.getIdMascota()).with(como(otro))).andExpect(status().isOk());
        mockMvc.perform(get("/citas/" + cita.getIdCita()).with(como(otro))).andExpect(status().isOk());
    }
}
