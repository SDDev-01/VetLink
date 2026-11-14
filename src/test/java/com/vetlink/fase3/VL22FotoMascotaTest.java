package com.vetlink.fase3;

import com.vetlink.mascota.model.Mascota;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-22: foto de mascota como archivo, validada y servida solo a quien tiene acceso. */
class VL22FotoMascotaTest extends PruebaWebBase {

    /** PNG válido de 1x1 píxel. */
    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R',
            0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89, 0, 0, 0, 0x0A,
            'I', 'D', 'A', 'T', 0x78, (byte) 0x9C, 0x63, 0, 1, 0, 0, 5, 0, 1, 0x0D, 0x0A, 0x2D, (byte) 0xB4,
            0, 0, 0, 0, 'I', 'E', 'N', 'D', (byte) 0xAE, 'B', 0x60, (byte) 0x82};

    private MockMultipartHttpServletRequestBuilder editar(Usuario duenio, Mascota m, MockMultipartFile archivo) {
        return (MockMultipartHttpServletRequestBuilder) multipart("/mascotas/" + m.getIdMascota() + "/editar")
                .file(archivo)
                .param("nombre", m.getNombre())
                .param("idEspecie", m.getEspecie().getIdEspecie().toString())
                .param("idRaza", m.getRaza().getIdRaza().toString())
                .param("genero", "M")
                .param("fechaNacimiento", LocalDate.now().minusYears(2).toString())
                .param("peso", "10")
                .with(csrf()).with(como(duenio));
    }

    @Test
    void subirPdfComoFotoDaError() throws Exception {
        var duenio = crearDuenio();
        var m = crearMascota(duenio);
        var pdf = new MockMultipartFile("archivoFoto", "doc.pdf", "application/pdf", "%PDF-1.4 hola".getBytes());

        mockMvc.perform(editar(duenio, m, pdf))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "foto"));

        sincronizar();
        assertThat(mascotaRepository.findById(m.getIdMascota()).orElseThrow().getFoto()).isNull();
    }

    @Test
    void pdfDisfrazadoDePngTambienSeRechaza() throws Exception {
        var duenio = crearDuenio();
        var m = crearMascota(duenio);
        var falso = new MockMultipartFile("archivoFoto", "foto.png", "image/png", "%PDF-1.4 hola".getBytes());

        mockMvc.perform(editar(duenio, m, falso))
                .andExpect(model().attributeHasFieldErrors("form", "foto"));
    }

    @Test
    void archivoDeMasDe5MbSeRechaza() throws Exception {
        var duenio = crearDuenio();
        var m = crearMascota(duenio);
        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);

        mockMvc.perform(editar(duenio, m, new MockMultipartFile("archivoFoto", "g.png", "image/png", grande)))
                .andExpect(model().attributeHasFieldErrors("form", "foto"));
    }

    @Test
    void pngSeGuardaYSeDevuelveAlDuenioPeroNoAOtro() throws Exception {
        var duenio = crearDuenio();
        var m = crearMascota(duenio);

        mockMvc.perform(editar(duenio, m, new MockMultipartFile("archivoFoto", "f.png", "image/png", PNG)))
                .andExpect(redirectedUrl("/mascotas?actualizada"));

        sincronizar();
        String foto = mascotaRepository.findById(m.getIdMascota()).orElseThrow().getFoto();
        assertThat(foto).matches("mascotas/" + m.getIdMascota() + "/[0-9a-f-]{36}\\.png");

        mockMvc.perform(get("/mascotas/" + m.getIdMascota() + "/foto").with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG));
        mockMvc.perform(get("/mascotas/" + m.getIdMascota() + "/foto").with(como(crearDuenio())))
                .andExpect(status().isForbidden());

        // El veterinario que la atiende también la ve.
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        crearCita(mascotaRepository.findById(m.getIdMascota()).orElseThrow(), vet, clinica);
        mockMvc.perform(get("/mascotas/" + m.getIdMascota() + "/foto").with(como(vet)))
                .andExpect(status().isOk());
    }

    @Test
    void sinFotoSeMuestraLaHuellaYNoUnaImagenRota() throws Exception {
        var duenio = crearDuenio();
        var m = crearMascota(duenio);
        var conUrl = crearMascota(duenio);
        conUrl.setFoto("https://ejemplo.com/perro.jpg");
        sincronizar();

        mockMvc.perform(get("/mascotas/" + m.getIdMascota()).with(como(duenio)))
                .andExpect(content().string(containsString("fa-paw")))
                .andExpect(content().string(not(containsString("/uploads/"))));
        mockMvc.perform(get("/mascotas").with(como(duenio)))
                .andExpect(content().string(containsString("https://ejemplo.com/perro.jpg")));
    }
}
