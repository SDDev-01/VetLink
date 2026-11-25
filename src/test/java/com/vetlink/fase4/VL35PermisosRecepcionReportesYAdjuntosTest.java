package com.vetlink.fase4;

import com.vetlink.cita.model.EstadoCita;
import com.vetlink.emergencia.model.Emergencia;
import com.vetlink.emergencia.repository.EmergenciaRepository;
import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.historial.repository.ArchivoHistorialRepository;
import com.vetlink.mascota.repository.MascotaEventoRepository;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.veterinaria.model.Servicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Recepción solo actualiza peso; autorización de reportes PDF; adjuntos clínicos; emergencias con veterinario ocupado. */
class VL35PermisosRecepcionReportesYAdjuntosTest extends PruebaWebBase {

    @Autowired MascotaEventoRepository eventos;
    @Autowired ArchivoHistorialRepository archivos;
    @Autowired EmergenciaRepository emergencias;

    // ------------------------------------------------------------------ Recepción: solo peso

    @Test
    void recepcionActualizaSoloElPesoConTrazabilidad() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, true);
        var mascota = crearMascota(crearDuenio());
        crearCita(mascota, crearVeterinario(clinica), clinica);
        sincronizar();

        mockMvc.perform(post("/recepcion/mascotas/" + mascota.getIdMascota() + "/peso").with(csrf()).with(como(recepcion))
                        .param("peso", "12.35"))
                .andExpect(flash().attribute("mensajeOk", "Peso actualizado."));
        mockMvc.perform(post("/recepcion/mascotas/" + mascota.getIdMascota() + "/peso").with(csrf()).with(como(recepcion))
                        .param("peso", "-3"))
                .andExpect(flash().attribute("errorGeneral", "Ingresa un peso mayor que cero"));
        mockMvc.perform(post("/recepcion/mascotas/" + mascota.getIdMascota() + "/editar").with(csrf()).with(como(recepcion))
                        .param("nombre", "Otro nombre"))
                .andExpect(status().isForbidden());
        sincronizar();
        var m = mascotaRepository.findById(mascota.getIdMascota()).orElseThrow();
        assertThat(m.getPeso()).isEqualTo(12.35);
        assertThat(m.getNombre()).isEqualTo(mascota.getNombre());
        var evento = eventos.findByMascota_IdMascotaOrderByFechaDesc(mascota.getIdMascota()).get(0);
        assertThat(evento.getTipo()).isEqualTo("PESO_ACTUALIZADO");
        assertThat(evento.getValorAnterior()).isEqualTo("10.0");
        assertThat(evento.getUsuario().getIdUsuario()).isEqualTo(recepcion.getIdUsuario());
    }

    @Test
    void recepcionNoRegistraHistoriasNiVacunasYVeSoloLosDocumentosDeSuClinica() throws Exception {
        var clinica = crearClinica();
        var otra = crearClinica();
        var recepcion = crearRecepcionista(clinica, true);
        var vet = crearVeterinario(clinica);
        var vetOtra = crearVeterinario(otra);
        var mascota = crearMascota(crearDuenio());
        crearCita(mascota, vet, clinica, LocalDate.now().minusDays(2), LocalTime.of(9, 0), EstadoCita.ATENDIDA);
        HistorialClinico propio = crearHistorial(mascota, vet, LocalDateTime.now().minusDays(2));
        propio.setVeterinaria(clinica);
        propio.setDiagnostico("Diagnóstico de mi clínica");
        HistorialClinico ajeno = crearHistorial(mascota, vetOtra, LocalDateTime.now().minusDays(10));
        ajeno.setVeterinaria(otra);
        ajeno.setDiagnostico("Diagnóstico de otra clínica");
        sincronizar();

        mockMvc.perform(get("/historial/pacientes").with(como(recepcion)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Diagnóstico de mi clínica")))
                .andExpect(content().string(not(containsString("Diagnóstico de otra clínica"))));
        mockMvc.perform(get("/historial/documento/" + ajeno.getNumeroHistorial()).with(como(recepcion)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/historial/" + propio.getNumeroHistorial() + "/editar").with(como(recepcion)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/carnet/" + mascota.getIdMascota() + "/nueva").with(csrf()).with(como(recepcion))
                        .param("idVacuna", "1").param("idLote", "1").param("dosis", "1")
                        .param("proximaDosis", LocalDate.now().plusYears(1).toString()))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ Autorización de reportes PDF (auth_request)

    @Test
    void autorizacionDeReportesPorRolYRecurso() throws Exception {
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        var ajena = crearMascota(crearDuenio());
        var clinica = crearClinica();
        var otra = crearClinica();
        var vet = crearVeterinario(clinica);
        crearCita(mascota, vet, clinica, LocalDate.now().minusDays(1), LocalTime.of(9, 0), EstadoCita.ATENDIDA);
        HistorialClinico h = crearHistorial(mascota, vet, LocalDateTime.now().minusDays(1));
        h.setVeterinaria(clinica);
        var recepcionOtra = crearRecepcionista(otra, true);
        sincronizar();

        // Sin sesión: 401 (no redirección) para que Nginx lo trate como no autenticado.
        mockMvc.perform(get("/internal/auth/reportes").header("X-Original-URI", "/api/reportes/mascotas/" + mascota.getIdMascota() + "/historial"))
                .andExpect(status().isUnauthorized());
        // Dueño: su mascota sí (con identidad en encabezados), la ajena no aunque cambie el id.
        mockMvc.perform(get("/internal/auth/reportes").with(como(duenio))
                        .header("X-Original-URI", "/api/reportes/mascotas/" + mascota.getIdMascota() + "/historial?fecha_inicio=2026-01-01"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("X-Vetlink-Usuario", duenio.getIdUsuario().toString()))
                .andExpect(header().string("X-Vetlink-Rol", "TUTOR"));
        mockMvc.perform(get("/internal/auth/reportes").with(como(duenio))
                        .header("X-Original-URI", "/api/reportes/mascotas/" + ajena.getIdMascota() + "/vacunas"))
                .andExpect(status().isForbidden());
        // Veterinario que atendió: registro individual permitido, con su clínica.
        mockMvc.perform(get("/internal/auth/reportes").with(como(vet))
                        .header("X-Original-URI", "/api/reportes/historial/" + h.getNumeroHistorial() + "/pdf"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("X-Vetlink-Veterinaria", clinica.getIdVeterinaria().toString()));
        // Recepción de otra clínica: no.
        mockMvc.perform(get("/internal/auth/reportes").with(como(recepcionOtra))
                        .header("X-Original-URI", "/api/reportes/historial/" + h.getNumeroHistorial() + "/pdf"))
                .andExpect(status().isForbidden());
        // Rutas no reconocidas: denegadas.
        mockMvc.perform(get("/internal/auth/reportes").with(como(duenio)).header("X-Original-URI", "/api/reportes/../admin"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ Adjuntos clínicos

    @Test
    void adjuntosValidadosYProtegidos() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        var cita = citaEnAtencion(mascota, vet, clinica, servicioPorDefecto());
        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/historial").with(csrf()).with(como(vet))
                .param("idMascota", mascota.getIdMascota().toString())
                .param("motivoConsulta", "Cojera").param("diagnostico", "Esguince"));

        var falso = new MockMultipartFile("archivo", "resultado.pdf", "application/pdf", "no soy un pdf".getBytes());
        mockMvc.perform(multipart("/agenda/" + cita.getIdCita() + "/archivos").file(falso).with(csrf()).with(como(vet)))
                .andExpect(flash().attribute("errorArchivo", "El formato del archivo no está permitido"));
        var exe = new MockMultipartFile("archivo", "virus.exe", "application/octet-stream", "MZ".getBytes());
        mockMvc.perform(multipart("/agenda/" + cita.getIdCita() + "/archivos").file(exe).with(csrf()).with(como(vet)))
                .andExpect(flash().attribute("errorArchivo", "El formato del archivo no está permitido"));

        var pdf = new MockMultipartFile("archivo", "../../hemograma.pdf", "application/pdf", "%PDF-1.4\n%prueba\n".getBytes());
        mockMvc.perform(multipart("/agenda/" + cita.getIdCita() + "/archivos").file(pdf).with(csrf()).with(como(vet)))
                .andExpect(flash().attribute("mensajeOk", "Archivo adjuntado."));
        sincronizar();
        var guardado = archivos.findAll().stream().filter(a -> "hemograma.pdf".equals(a.getNombreArchivo())).findFirst().orElseThrow();
        assertThat(guardado.getRutaArchivo()).startsWith("historial/").doesNotContain("..").endsWith(".pdf");
        assertThat(guardado.getTipoArchivo()).isEqualTo("application/pdf");

        mockMvc.perform(get("/historial/archivos/" + guardado.getIdArchivo()).with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mockMvc.perform(get("/historial/archivos/" + guardado.getIdArchivo()).with(como(crearDuenio())))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ Emergencias

    @Test
    void unVeterinarioEnAtencionNoRecibeEmergencias() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, true);
        var ocupado = crearVeterinario(clinica);
        var libre = crearVeterinario(clinica);
        horarioAprobado(ocupado, LocalTime.MIN, LocalTime.of(23, 59));
        horarioAprobado(libre, LocalTime.MIN, LocalTime.of(23, 59));
        citaEnAtencion(crearMascota(crearDuenio()), ocupado, clinica, servicioPorDefecto());
        var mascota = crearMascota(crearDuenio());
        Emergencia e = new Emergencia();
        e.setMascota(mascota);
        e.setVeterinaria(clinica);
        e.setMotivo("Convulsiones");
        e.setCreadaEn(LocalDateTime.now());
        e = emergencias.save(e);
        sincronizar();

        mockMvc.perform(get("/emergencias/" + e.getIdEmergencia()).with(como(recepcion)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Convulsiones")));
        mockMvc.perform(post("/emergencias/" + e.getIdEmergencia() + "/asignar").with(csrf()).with(como(recepcion))
                        .param("idVeterinario", ocupado.getIdUsuario().toString()))
                .andExpect(flash().attribute("error", containsString("no está disponible")));
        mockMvc.perform(post("/emergencias/" + e.getIdEmergencia() + "/asignar").with(csrf()).with(como(recepcion))
                        .param("idVeterinario", libre.getIdUsuario().toString()))
                .andExpect(redirectedUrl("/emergencias?asignada"));
        // Con una emergencia asignada abierta, el veterinario libre deja de estar disponible para otra.
        sincronizar();
        Emergencia otra = new Emergencia();
        otra.setMascota(crearMascota(crearDuenio()));
        otra.setVeterinaria(clinica);
        otra.setMotivo("Atropello");
        otra.setCreadaEn(LocalDateTime.now());
        otra = emergencias.save(otra);
        mockMvc.perform(post("/emergencias/" + otra.getIdEmergencia() + "/asignar").with(csrf()).with(como(recepcion))
                        .param("idVeterinario", libre.getIdUsuario().toString()))
                .andExpect(flash().attribute("error", containsString("no está disponible")));
    }

    // ------------------------------------------------------------------ Mi veterinaria

    @Test
    void elFormularioDePersonalNoOfreceTarjetaDeIdentidad() throws Exception {
        var ti = new com.vetlink.usuario.model.TipoDocumento();
        ti.setNombreTipo("TI");
        ti.setDescripcion("Tarjeta de identidad");
        tipoDocumentoRepository.save(ti);
        var recepcion = crearRecepcionista(crearClinica(), true);
        sincronizar();
        mockMvc.perform(get("/veterinarias/admin").with(como(recepcion)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(">TI<"))))
                .andExpect(content().string(containsString("Imagen de la veterinaria")));
    }

    @Test
    void imagenDeLaVeterinariaSeValidaYSePublica() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, true);
        sincronizar();
        var noImagen = new MockMultipartFile("logoArchivo", "logo.png", "image/png", "texto".getBytes());
        mockMvc.perform(multipart("/veterinarias/admin/logo").file(noImagen).with(csrf()).with(como(recepcion)))
                .andExpect(flash().attribute("error", containsString("formato")));
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        mockMvc.perform(multipart("/veterinarias/admin/logo").file(new MockMultipartFile("logoArchivo", "logo.png", "image/png", png))
                        .with(csrf()).with(como(recepcion)))
                .andExpect(flash().attributeExists("mensajeOk"));
        sincronizar();
        assertThat(veterinariaRepository.findById(clinica.getIdVeterinaria()).orElseThrow().getLogo()).startsWith("veterinarias/");
        mockMvc.perform(get("/directorio/" + clinica.getIdVeterinaria() + "/logo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        assertThat(Servicio.TipoRegistro.values()).isNotEmpty();
    }
}
