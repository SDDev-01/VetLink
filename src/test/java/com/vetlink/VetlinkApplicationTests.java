package com.vetlink;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vetlink.historial.model.ArchivoHistorial;
import jakarta.persistence.Column;

@SpringBootTest
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

	@Test
	void veterinarioPacientesListSkipsCitasWithoutMascota() throws IOException {
		Path controllerPath = Path.of("src/main/java/com/vetlink/historial/controller/HistorialClinicoController.java");
		String controller = Files.readString(controllerPath);

		assertThat(controller).contains("c.getMascota() != null");
	}

	@Test
	void citaReportShowsAvailableSlotWhenMascotaIsMissing() throws IOException {
		Path htmlPath = Path.of("src/main/resources/templates/citas/reporte.html");
		String html = Files.readString(htmlPath);

		assertThat(html).contains("c.mascota != null ? c.mascota.nombre : 'Cupo disponible'");
	}

}
