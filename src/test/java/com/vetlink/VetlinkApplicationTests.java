package com.vetlink;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.vetlink.historial.model.ArchivoHistorial;
import jakarta.persistence.Column;

@SpringBootTest
@ActiveProfiles("test")
class VetlinkApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void sharedAppCssIncludesFontAwesome() throws IOException {
		Path cssPath = Path.of("src/main/resources/static/css/app.css");
		String css = Files.readString(cssPath);

		assertThat(css).contains("cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css");
	}

	@Test
	void agendaCalendarLinksUseTheCitaIdFromJson() throws IOException {
		Path htmlPath = Path.of("src/main/resources/templates/citas/agenda.html");
		String html = Files.readString(htmlPath);

		assertThat(html).contains("i.idCita");
	}

	@Test
	void archivoHistorialTipoArchivoAllowsLongMimeTypes() throws NoSuchFieldException {
		Field field = ArchivoHistorial.class.getDeclaredField("tipoArchivo");
		Column column = field.getAnnotation(Column.class);

		assertThat(column.length()).isGreaterThanOrEqualTo(255);
	}

	@Test
	void recepcionMascotaEditFormUsesUsuarioOwnerProperties() throws IOException {
		Path htmlPath = Path.of("src/main/resources/templates/recepcion/mascotas/formulario.html");
		String html = Files.readString(htmlPath);

		assertThat(html).contains("dueno.nombre");
		assertThat(html).doesNotContain("dueno.usuario.nombre");
	}

	// VL-25: la prueba "veterinarioPacientesListSkipsCitasWithoutMascota" buscaba el texto
	// "c.getMascota() != null" en el controlador. Ese recorrido se cambió por una consulta JPQL;
	// el comportamiento (los cupos sin mascota no rompen la lista) lo cubre VL25ConsultasDirectasTest.

	@Test
	void citaReportShowsAvailableSlotWhenMascotaIsMissing() throws IOException {
		Path htmlPath = Path.of("src/main/resources/templates/citas/reporte.html");
		String html = Files.readString(htmlPath);

		assertThat(html).contains("c.mascota != null ? c.mascota.nombre : 'Cupo disponible'");
	}

}
