package com.vetlink.v9;

import com.vetlink.comun.web.UrlPublica;
import com.vetlink.emergencia.model.Emergencia;
import com.vetlink.emergencia.model.EstadoEmergencia;
import com.vetlink.emergencia.repository.EmergenciaRepository;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.model.Raza;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.repository.TelefonoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Requisitos del rol DUEÑO (DUE-01 a DUE-04, DUE-07, DUE-08). */
class DuenioV9Test extends PruebaWebBase {

    @Autowired EmergenciaRepository emergencias;
    @Autowired TelefonoRepository telefonos;

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder nueva(String nombre, Raza raza) {
        return nuevaSinRaza(nombre, raza).param("idRaza", raza.getIdRaza().toString());
    }

    /** Petición sin idRaza (para «Otra raza»): la especie se toma de la raza dada. */
    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder nuevaSinRaza(String nombre, Raza raza) {
        return multipart("/mascotas/nueva").with(csrf())
                .param("nombre", nombre)
                .param("idEspecie", raza.getEspecie().getIdEspecie().toString())
                .param("genero", "H")
                .param("fechaNacimiento", LocalDate.now().minusYears(1).toString())
                .param("peso", "4.5");
    }

    // ------------------------------------------------------------------ DUE-03

    @Test
    void registraMascotaConNombreValidoYQuitaEspaciosSobrantes() throws Exception {
        var duenio = crearDuenio();
        Raza raza = crearRaza(especiePorDefecto(), "Criolla");

        mockMvc.perform(nueva("   María  José-Ñoño  ", raza).with(como(duenio)))
                .andExpect(redirectedUrl("/mascotas?creada"));

        sincronizar();
        assertThat(mascotaRepository.findByTutor_IdUsuario(duenio.getIdUsuario()))
                .extracting(Mascota::getNombre).containsExactly("María José-Ñoño");
    }

    @Test
    void nombreConNumerosOSoloSimbolosSeRechaza() throws Exception {
        var duenio = crearDuenio();
        Raza raza = crearRaza(especiePorDefecto(), "Criolla");

        for (String malo : new String[]{"Max2", "12345", "   ", "@@@", "Max!", "-Luna"}) {
            mockMvc.perform(nueva(malo, raza).with(como(duenio)))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("form", "nombre"));
        }
        sincronizar();
        assertThat(mascotaRepository.findByTutor_IdUsuario(duenio.getIdUsuario())).isEmpty();
    }

    // ------------------------------------------------------------------ DUE-01

    @Test
    void especieGeneroYFechaNoCambianAunqueLaPeticionLosTraiga() throws Exception {
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        var otraEspecie = crearEspecie("Ave");
        LocalDate nacimiento = mascota.getFechaNacimiento();
        Integer especieOriginal = mascota.getEspecie().getIdEspecie();
        Raza otraRazaMismaEspecie = crearRaza(mascota.getEspecie(), "Labrador");

        mockMvc.perform(multipart("/mascotas/" + mascota.getIdMascota() + "/editar").with(csrf()).with(como(duenio))
                        .param("nombre", "Nuevo Nombre")
                        .param("idEspecie", otraEspecie.getIdEspecie().toString())
                        .param("genero", "H")
                        .param("fechaNacimiento", "2010-01-01")
                        .param("idRaza", otraRazaMismaEspecie.getIdRaza().toString())
                        .param("peso", "12.5"))
                .andExpect(redirectedUrl("/mascotas?actualizada"));

        sincronizar();
        Mascota m = mascotaRepository.findById(mascota.getIdMascota()).orElseThrow();
        assertThat(m.getNombre()).isEqualTo("Nuevo Nombre");
        assertThat(m.getPeso()).isEqualTo(12.5);
        assertThat(m.getRaza().getIdRaza()).isEqualTo(otraRazaMismaEspecie.getIdRaza());
        assertThat(m.getEspecie().getIdEspecie()).isEqualTo(especieOriginal);
        assertThat(m.getGenero()).isEqualTo("M");
        assertThat(m.getFechaNacimiento()).isEqualTo(nacimiento);
    }

    @Test
    void edicionMuestraEspecieGeneroYFechaComoSoloLectura() throws Exception {
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);

        mockMvc.perform(get("/mascotas/" + mascota.getIdMascota() + "/editar").with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("vl-readonly")))
                .andExpect(content().string(containsString(mascota.getEspecie().getNombreEspecie())))
                .andExpect(content().string(not(containsString("name=\"idEspecie\""))))
                .andExpect(content().string(not(containsString("name=\"genero\""))))
                .andExpect(content().string(not(containsString("name=\"fechaNacimiento\""))));
    }

    @Test
    void noSePuedeVerNiEditarLaMascotaDeOtroDuenio() throws Exception {
        var mascota = crearMascota(crearDuenio());
        var intruso = crearDuenio();

        mockMvc.perform(get("/mascotas/" + mascota.getIdMascota()).with(como(intruso))).andExpect(status().isForbidden());
        mockMvc.perform(multipart("/mascotas/" + mascota.getIdMascota() + "/editar").with(csrf()).with(como(intruso))
                        .param("nombre", "Robada").param("idRaza", mascota.getRaza().getIdRaza().toString()).param("peso", "3"))
                .andExpect(status().isForbidden());
        sincronizar();
        assertThat(mascotaRepository.findById(mascota.getIdMascota()).orElseThrow().getNombre()).isEqualTo(mascota.getNombre());
    }

    // ------------------------------------------------------------------ DUE-02

    @Test
    void otraRazaSeGuardaYNoSeDuplicaPorMayusculasOEspacios() throws Exception {
        var duenio = crearDuenio();
        var especie = especiePorDefecto();
        Raza base = crearRaza(especie, "Criolla");

        mockMvc.perform(nuevaSinRaza("Kira", base).with(como(duenio)).param("razaPersonalizada", "  pastor   catalán "))
                .andExpect(redirectedUrl("/mascotas?creada"));
        mockMvc.perform(nuevaSinRaza("Toby", base).with(como(duenio)).param("razaPersonalizada", "PASTOR CATALÁN"))
                .andExpect(redirectedUrl("/mascotas?creada"));

        sincronizar();
        var razas = razaRepository.findByEspecie_IdEspecie(especie.getIdEspecie()).stream()
                .filter(r -> r.getNombreRaza().equalsIgnoreCase("Pastor Catalán")).toList();
        assertThat(razas).hasSize(1);
        assertThat(razas.get(0).getNombreRaza()).isEqualTo("Pastor Catalán");
        assertThat(razas.get(0).getPersonalizada()).isTrue();
        assertThat(mascotaRepository.findByTutor_IdUsuario(duenio.getIdUsuario()))
                .allMatch(m -> m.getRaza().getIdRaza().equals(razas.get(0).getIdRaza()));
        // Se muestra en el perfil de la mascota.
        var kira = mascotaRepository.findByTutor_IdUsuario(duenio.getIdUsuario()).get(0);
        mockMvc.perform(get("/mascotas/" + kira.getIdMascota()).with(como(duenio)))
                .andExpect(content().string(containsString("Pastor Catalán")));
    }

    @Test
    void otraRazaVaciaYRazaDeOtraEspecieSeRechazan() throws Exception {
        var duenio = crearDuenio();
        Raza base = crearRaza(especiePorDefecto(), "Criolla");
        Raza deOtraEspecie = crearRaza(crearEspecie("Pez"), "Betta");

        mockMvc.perform(nuevaSinRaza("Kira", base).with(como(duenio)).param("razaPersonalizada", "   "))
                .andExpect(model().attributeHasFieldErrors("form", "idRaza"));
        mockMvc.perform(nuevaSinRaza("Kira", base).with(como(duenio)).param("idRaza", deOtraEspecie.getIdRaza().toString()))
                .andExpect(model().attributeExists("errorGeneral"));
        sincronizar();
        assertThat(mascotaRepository.findByTutor_IdUsuario(duenio.getIdUsuario())).isEmpty();
    }

    @Test
    void formularioListaLasRazasEnOrdenAlfabeticoConOpcionOtraRaza() throws Exception {
        var duenio = crearDuenio();
        var especie = especiePorDefecto();
        crearRaza(especie, "Zeta");
        crearRaza(especie, "Ápice");
        crearRaza(especie, "Beagle");

        String html = mockMvc.perform(get("/mascotas/nueva").with(como(duenio)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf(">Ápice<")).isLessThan(html.indexOf(">Beagle<"));
        assertThat(html.indexOf(">Beagle<")).isLessThan(html.indexOf(">Zeta<"));
        assertThat(html).contains("razaPersonalizada").contains("data-otra-raza");
    }

    // ------------------------------------------------------------------ DUE-04

    @Test
    void urlDelQrUsaLaIpConfiguradaEnLugarDeLocalhost() {
        assertThat(UrlPublica.resolver("http://localhost", "192.168.1.100", "80")).isEqualTo("http://192.168.1.100");
        assertThat(UrlPublica.resolver("", "192.168.1.100", "8081")).isEqualTo("http://192.168.1.100:8081");
        assertThat(UrlPublica.resolver("https://vetlink.midominio.com", "192.168.1.100", "80"))
                .isEqualTo("https://vetlink.midominio.com");
        assertThat(UrlPublica.resolver(null, null, null)).isEqualTo("http://localhost");
        assertThat(UrlPublica.resolver("http://localhost", "no válida", "80")).isEqualTo("http://localhost");
    }

    // ------------------------------------------------------------------ DUE-07

    @Test
    void emergenciaQuedaAsociadaAlSolicitanteYALaMascotaCorrecta() throws Exception {
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        var clinica = crearClinica();

        mockMvc.perform(post("/emergencias").with(csrf()).with(como(duenio))
                        .param("idMascota", mascota.getIdMascota().toString())
                        .param("idVeterinaria", clinica.getIdVeterinaria().toString())
                        .param("motivo", "  Dificultad respiratoria  "))
                .andExpect(status().is3xxRedirection());

        sincronizar();
        Emergencia e = emergencias.findAll().stream()
                .filter(x -> x.getMascota().getIdMascota().equals(mascota.getIdMascota())).findFirst().orElseThrow();
        assertThat(e.getSolicitante().getIdUsuario()).isEqualTo(duenio.getIdUsuario());
        assertThat(e.getMotivo()).isEqualTo("Dificultad respiratoria");
        mockMvc.perform(get("/emergencias").with(como(duenio)))
                .andExpect(content().string(containsString(mascota.getNombre() + " · Dificultad respiratoria")));
    }

    @Test
    void noSeRegistraEmergenciaConMascotaAjena() throws Exception {
        var mascotaAjena = crearMascota(crearDuenio());
        var intruso = crearDuenio();
        long antes = emergencias.count();

        mockMvc.perform(post("/emergencias").with(csrf()).with(como(intruso))
                        .param("idMascota", mascotaAjena.getIdMascota().toString())
                        .param("idVeterinaria", crearClinica().getIdVeterinaria().toString())
                        .param("motivo", "Prueba"))
                .andExpect(status().isForbidden());
        sincronizar();
        assertThat(emergencias.count()).isEqualTo(antes);
    }

    @Test
    void misSolicitudesSoloMuestraLasDelDuenioAunqueLaMascotaSeTransfiera() throws Exception {
        var anterior = crearDuenio();
        var nuevo = crearDuenio();
        var mascota = crearMascota(anterior);
        Emergencia e = new Emergencia();
        e.setMascota(mascota);
        e.setSolicitante(anterior);
        e.setVeterinaria(crearClinica());
        e.setMotivo("Golpe en la pata");
        e.setEstado(EstadoEmergencia.ATENDIDA);
        e.setCreadaEn(LocalDateTime.now().minusDays(3));
        emergencias.save(e);
        // La mascota pasa al nuevo dueño.
        mascota.setTutor(nuevo);
        mascotaRepository.save(mascota);
        sincronizar();

        assertThat(emergencias.deSolicitante(anterior.getIdUsuario())).extracting(Emergencia::getIdEmergencia)
                .containsExactly(e.getIdEmergencia());
        assertThat(emergencias.deSolicitante(nuevo.getIdUsuario())).isEmpty();
        mockMvc.perform(get("/emergencias/" + e.getIdEmergencia()).with(como(nuevo))).andExpect(status().isForbidden());
        mockMvc.perform(get("/emergencias/" + e.getIdEmergencia()).with(como(anterior))).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ DUE-08

    @Test
    void perfilGuardaEnLaBaseYSeMuestraActualizado() throws Exception {
        var u = crearDuenio();
        String correo = correoRepository.findByUsuario_IdUsuario(u.getIdUsuario()).get(0).getCorreo();

        mockMvc.perform(post("/perfil").with(csrf()).with(como(u))
                        .param("nombre", "  Ana   Lucía ").param("apellido", "Pérez Núñez")
                        .param("telefono", "3001112233").param("correo", correo).param("direccion", "Cra 7 # 45-10"))
                .andExpect(redirectedUrl("/perfil?actualizado"));

        sincronizar();
        var guardado = usuarioRepository.findById(u.getIdUsuario()).orElseThrow();
        assertThat(guardado.getNombre()).isEqualTo("Ana Lucía");
        assertThat(guardado.getApellido()).isEqualTo("Pérez Núñez");
        assertThat(guardado.getTutor().getDireccion()).isEqualTo("Cra 7 # 45-10");
        assertThat(telefonos.findByUsuario_IdUsuario(u.getIdUsuario())).extracting("numeroTelefono").containsExactly("3001112233");
        // Aunque la sesión tenga el usuario viejo, la vista lee la base de datos.
        mockMvc.perform(get("/perfil").with(como(u)))
                .andExpect(content().string(containsString("Ana Lucía")))
                .andExpect(content().string(containsString("3001112233")));
    }

    @Test
    void perfilRechazaNombreConNumerosYTelefonoRepetidoSinError500() throws Exception {
        var u = crearDuenio();
        var otro = crearDuenio();
        var tel = new com.vetlink.usuario.model.Telefono();
        tel.setUsuario(otro);
        tel.setNumeroTelefono("3005556677");
        telefonos.save(tel);
        String correo = correoRepository.findByUsuario_IdUsuario(u.getIdUsuario()).get(0).getCorreo();

        mockMvc.perform(post("/perfil").with(csrf()).with(como(u))
                        .param("nombre", "Ana2").param("apellido", u.getApellido())
                        .param("telefono", "3005556677").param("correo", correo))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nombre", "telefono"));
        mockMvc.perform(post("/perfil").with(csrf()).with(como(u))
                        .param("nombre", "Ana").param("apellido", u.getApellido())
                        .param("telefono", "300-555").param("correo", correo))
                .andExpect(model().attributeHasFieldErrors("form", "telefono"));
        sincronizar();
        assertThat(usuarioRepository.findById(u.getIdUsuario()).orElseThrow().getNombre()).isEqualTo("Duenio");
    }

    @Test
    void perfilEsVistaDeConsultaConBotonEditar() throws Exception {
        var u = crearDuenio();
        mockMvc.perform(get("/perfil").with(como(u)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Editar perfil")))
                .andExpect(content().string(containsString("id=\"panel-edicion\"")))
                .andExpect(content().string(containsString("hidden=\"hidden\"")))
                .andExpect(content().string(containsString("Información personal")));
    }
}
