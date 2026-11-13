package com.vetlink.fase1;

import com.vetlink.support.PruebaWebBase;
import com.vetlink.ubicacion.model.Ciudad;
import com.vetlink.ubicacion.repository.CiudadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-08: quien registra una clínica nueva queda como recepcionista principal. */
class VL08ClinicaNuevaPrincipalTest extends PruebaWebBase {

    @Autowired CiudadRepository ciudadRepository;

    @Test
    void registrarClinicaNuevaDejaRecepcionistaPrincipal() throws Exception {
        Ciudad ciudad = new Ciudad();
        ciudad.setNombre("Bogotá");
        ciudad = ciudadRepository.save(ciudad);
        int n = siguiente();

        mockMvc.perform(post("/registro").with(csrf())
                        .param("numeroDocumento", "CLI" + n)
                        .param("idTipoDocumento", tipoDocumento().getIdTipoDocumento().toString())
                        .param("nombre", "Ana")
                        .param("apellido", "Admin")
                        .param("correo", "admin" + n + "@pruebas.vetlink")
                        .param("telefono", "3001234567")
                        .param("password", "Clave12345")
                        .param("confirmarPassword", "Clave12345")
                        .param("rol", "VETERINARIA")
                        .param("nuevaVeterinaria.nit", "900" + n)
                        .param("nuevaVeterinaria.nombre", "Clínica Nueva " + n)
                        .param("nuevaVeterinaria.telefono", "6011234567")
                        .param("nuevaVeterinaria.correo", "clinica" + n + "@pruebas.vetlink")
                        .param("nuevaVeterinaria.direccion", "Calle 100 # 10-10")
                        .param("nuevaVeterinaria.idCiudad", ciudad.getIdCiudad().toString())
                        .param("nuevaVeterinaria.barrio", "Chicó"))
                .andExpect(redirectedUrl("/login?registradoVet"));

        sincronizar();
        var usuario = usuarioRepository.findByNumeroDocumento("CLI" + n).orElseThrow();
        assertThat(usuario.getRecepcionista()).isNotNull();
        assertThat(usuario.getRecepcionista().getEsPrincipal()).isTrue();

        mockMvc.perform(get("/veterinarias/admin").with(como(usuario)))
                .andExpect(status().isOk());
    }

    @Test
    void recepcionistaCreadaDesdeElPanelNoEsPrincipal() throws Exception {
        var clinica = crearClinica();
        var principal = crearRecepcionista(clinica, true);
        int n = siguiente();

        mockMvc.perform(post("/veterinarias/admin/recepcionistas").with(csrf()).with(como(principal))
                        .param("numeroDocumento", "PAN" + n)
                        .param("idTipoDocumento", tipoDocumento().getIdTipoDocumento().toString())
                        .param("nombre", "Rosa")
                        .param("apellido", "Panel")
                        .param("correo", "panel" + n + "@pruebas.vetlink")
                        .param("telefono", "3001234567")
                        .param("password", "Clave12345")
                        .param("confirmarPassword", "Clave12345")
                        .param("rol", "RECEPCIONISTA"))
                .andExpect(redirectedUrl("/veterinarias/admin?recepcionistaCreada"));

        sincronizar();
        var creada = usuarioRepository.findByNumeroDocumento("PAN" + n).orElseThrow();
        assertThat(creada.getRecepcionista().getEsPrincipal()).isFalse();
        assertThat(creada.getRecepcionista().getVeterinaria().getIdVeterinaria()).isEqualTo(clinica.getIdVeterinaria());
    }
}
