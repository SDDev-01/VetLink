package com.vetlink.fase4;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.chat.model.Conversacion;
import com.vetlink.chat.repository.ConversacionRepository;
import com.vetlink.emergencia.model.Emergencia;
import com.vetlink.emergencia.repository.EmergenciaRepository;
import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.pasaporte.service.PasaporteService;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba de humo: cada pantalla de cada rol se renderiza sin errores de plantilla y sin horas con segundos. */
class VL36PantallasPorRolTest extends PruebaWebBase {

    @Autowired ConversacionRepository conversaciones;
    @Autowired EmergenciaRepository emergencias;
    @MockitoBean PasaporteService pasaportes;

    private void ok(Usuario u, String url) throws Exception {
        mockMvc.perform(get(url).with(como(u)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(":00.000"))))
                .andExpect(content().string(not(containsString("AGENDADA"))));
    }

    @Test
    void todasLasPantallasRenderizan() throws Exception {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var recepcion = crearRecepcionista(clinica, true);
        var vet = crearVeterinario(clinica);
        autorizarServicio(vet, servicioPorDefecto());
        horarioAprobado(vet, LocalTime.of(0, 0), LocalTime.of(23, 59));
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        Cita confirmada = crearCita(mascota, vet, clinica, LocalDate.now().plusDays(1), LocalTime.of(10, 0), EstadoCita.CONFIRMADA);
        Cita pendiente = crearCita(crearMascota(duenio), vet, clinica, LocalDate.now().plusDays(2), LocalTime.of(11, 0), EstadoCita.PENDIENTE);
        crearCita(null, vet, clinica, LocalDate.now().plusDays(3), LocalTime.of(9, 0), EstadoCita.DISPONIBLE);
        Cita enAtencion = citaEnAtencion(crearMascota(duenio), vet, clinica, servicioPorDefecto());
        HistorialClinico h = crearHistorial(mascota, vet, LocalDateTime.now().minusHours(1));
        h.setVeterinaria(clinica);
        crearCita(mascota, vet, clinica, LocalDate.now().minusDays(3), LocalTime.of(8, 0), EstadoCita.ATENDIDA);
        Conversacion conv = new Conversacion();
        conv.setUsuarioA(duenio);
        conv.setUsuarioB(recepcion);
        conv.setVeterinaria(clinica);
        conv.setCreadaEn(LocalDateTime.now());
        conv = conversaciones.save(conv);
        Emergencia e = new Emergencia();
        e.setMascota(mascota);
        e.setVeterinaria(clinica);
        e.setMotivo("Vómito");
        e.setCreadaEn(LocalDateTime.now());
        e = emergencias.save(e);
        sincronizar();

        // Tutor
        for (String url : new String[]{"/dashboard", "/mascotas", "/mascotas/nueva", "/mascotas/" + mascota.getIdMascota(),
                "/mascotas/" + mascota.getIdMascota() + "/editar", "/mascotas/transferencias", "/citas/mias",
                "/citas/" + confirmada.getIdCita(), "/citas/agendar", "/citas/agendar/" + mascota.getIdMascota(),
                "/citas/mascota/" + mascota.getIdMascota(), "/historial/mias", "/historial/mascota/" + mascota.getIdMascota(),
                "/historial/documento/" + h.getNumeroHistorial(), "/carnet/" + mascota.getIdMascota(), "/chat",
                "/chat/" + conv.getIdConversacion(), "/emergencias", "/emergencias/" + e.getIdEmergencia(),
                "/mascotas/" + mascota.getIdMascota() + "/pasaporte"}) {
            ok(duenio, url);
        }
        // Veterinario
        for (String url : new String[]{"/dashboard", "/agenda", "/agenda/" + confirmada.getIdCita(),
                "/agenda/" + enAtencion.getIdCita() + "/atencion", "/historial/pacientes",
                "/historial/paciente/" + mascota.getIdMascota(), "/historial/documento/" + h.getNumeroHistorial(),
                "/historial/" + h.getNumeroHistorial() + "/editar", "/disponibilidad", "/chat", "/emergencias"}) {
            ok(vet, url);
        }
        // Recepción
        for (String url : new String[]{"/dashboard", "/agenda", "/agenda/" + pendiente.getIdCita(),
                "/agenda/" + confirmada.getIdCita(), "/recepcion/disponibilidad", "/recepcion/mascotas",
                "/recepcion/mascotas/nueva", "/recepcion/mascotas/" + mascota.getIdMascota(), "/historial/pacientes",
                "/historial/mascota/" + mascota.getIdMascota(), "/veterinarias/admin", "/reportes/citas",
                "/citas/recepcion/agendar", "/citas/recepcion/ofertar", "/citas/recepcion/" + confirmada.getIdCita() + "/reagendar",
                "/chat", "/chat/" + conv.getIdConversacion(), "/emergencias", "/emergencias/" + e.getIdEmergencia()}) {
            ok(recepcion, url);
        }
        // Público
        mockMvc.perform(get("/directorio")).andExpect(status().isOk());
        mockMvc.perform(get("/directorio").param("buscar", "1").param("emergencia", "true")).andExpect(status().isOk());
        mockMvc.perform(get("/directorio").param("buscar", "1").param("idServicio", servicioPorDefecto().getIdServicio().toString())
                .param("fecha", LocalDate.now().plusDays(3).toString()).param("lat", "4.65").param("lng", "-74.1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(clinica.getNombre())));
        mockMvc.perform(get("/directorio/" + clinica.getIdVeterinaria()).param("volver", "/directorio?buscar=1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/directorio?buscar=1")));
        mockMvc.perform(get("/directorio/" + clinica.getIdVeterinaria()).param("volver", "https://malicioso.example"))
                .andExpect(content().string(not(containsString("malicioso"))));
    }
}
