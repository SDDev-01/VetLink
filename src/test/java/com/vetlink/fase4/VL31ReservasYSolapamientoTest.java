package com.vetlink.fase4;

import com.vetlink.cita.dto.AgendarCitaForm;
import com.vetlink.cita.dto.OfertarCitaForm;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.repository.CitaHistorialEstadoRepository;
import com.vetlink.cita.service.CitaService;
import com.vetlink.cita.service.ReservaConcurrenteException;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.notificacion.NotificacionRepository;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.Veterinaria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reserva, confirmación, rechazo, cancelación con margen, expiración y solapamiento por duración. */
class VL31ReservasYSolapamientoTest extends PruebaWebBase {

    @Autowired CitaService citaService;
    @Autowired CitaHistorialEstadoRepository historialEstados;
    @Autowired NotificacionRepository notificaciones;

    private Cita cupo(Usuario vet, Veterinaria clinica, LocalDate fecha, LocalTime hora, int duracion) {
        Cita c = crearCita(null, vet, clinica, fecha, hora, EstadoCita.DISPONIBLE);
        c.setDuracionMin(duracion);
        return citaRepository.save(c);
    }

    private AgendarCitaForm form(Cita cupo, Mascota m) {
        AgendarCitaForm f = new AgendarCitaForm();
        f.setIdCupo(cupo.getIdCita());
        f.setIdMascota(m.getIdMascota());
        f.setIdVeterinaria(cupo.getVeterinaria().getIdVeterinaria());
        f.setIdVeterinario(cupo.getVeterinario().getIdUsuario());
        f.setIdServicio(cupo.getServicio().getIdServicio());
        f.setFecha(cupo.getFecha());
        f.setHora(cupo.getHora());
        f.setMotivo("Control");
        return f;
    }

    @Test
    void reservaQuedaPendienteBloqueaElCupoYAvisaARecepcion() {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var vet = crearVeterinario(clinica);
        var recepcion = crearRecepcionista(clinica, true);
        var duenio = crearDuenio();
        var c = cupo(vet, clinica, LocalDate.now().plusDays(2), LocalTime.of(9, 0), 30);

        Cita reservada = citaService.agendar(form(c, crearMascota(duenio)), duenio.getIdUsuario());

        assertThat(reservada.getEstado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(reservada.getExpiraEn()).isNotNull().isBefore(reservada.getInicio());
        assertThat(reservada.getMotivo()).isEqualTo("Control");
        assertThat(historialEstados.findByCita_IdCitaOrderByFechaAscIdHistorialEstadoAsc(c.getIdCita()))
                .extracting(h -> h.getEstadoNuevo()).containsExactly(EstadoCita.PENDIENTE);
        assertThat(notificaciones.countByIdUsuarioAndLeidaFalse(recepcion.getIdUsuario())).isEqualTo(1);

        // Un segundo dueño ya no puede tomar el mismo cupo.
        var otro = crearDuenio();
        assertThatThrownBy(() -> citaService.agendar(form(c, crearMascota(otro)), otro.getIdUsuario()))
                .isInstanceOf(ReservaConcurrenteException.class)
                .hasMessage(ReservaConcurrenteException.MENSAJE);
    }

    @Test
    void confirmarYRechazarDesdeRecepcion() {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var vet = crearVeterinario(clinica);
        var recepcion = crearRecepcionista(clinica, true);
        var duenio = crearDuenio();
        var c1 = cupo(vet, clinica, LocalDate.now().plusDays(3), LocalTime.of(9, 0), 30);
        var c2 = cupo(vet, clinica, LocalDate.now().plusDays(3), LocalTime.of(10, 0), 30);
        citaService.agendar(form(c1, crearMascota(duenio)), duenio.getIdUsuario());
        citaService.agendar(form(c2, crearMascota(duenio)), duenio.getIdUsuario());

        assertThat(citaService.confirmar(c1.getIdCita(), clinica.getIdVeterinaria(), recepcion).getEstado())
                .isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(citaService.rechazar(c2.getIdCita(), clinica.getIdVeterinaria(), recepcion, "Sin anestesiólogo").getEstado())
                .isEqualTo(EstadoCita.RECHAZADA);
        sincronizar();
        // El rechazo libera el horario como un cupo nuevo.
        assertThat(citaRepository.findAll().stream().filter(x -> x.getEstado() == EstadoCita.DISPONIBLE
                && x.getHora().equals(LocalTime.of(10, 0)) && x.getVeterinario().getIdUsuario().equals(vet.getIdUsuario()))
                .count()).isEqualTo(1);
        // Recepción de otra clínica no puede confirmar.
        var ajena = crearRecepcionista(crearClinica(), true);
        assertThatThrownBy(() -> citaService.confirmar(c1.getIdCita(), ajena.getRecepcionista().getVeterinaria().getIdVeterinaria(), ajena))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void cancelacionSinMargenNoDevuelveElCupo() {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        // Cita confirmada que empieza en 90 minutos (margen de liberación por defecto: 120).
        LocalDateTime pronto = LocalDateTime.now().plusMinutes(90).withSecond(0).withNano(0);
        var cita = crearCita(mascota, vet, clinica, pronto.toLocalDate(), pronto.toLocalTime(), EstadoCita.CONFIRMADA);

        citaService.cancelar(cita.getIdCita(), duenio.getIdUsuario());
        sincronizar();

        assertThat(citaRepository.findById(cita.getIdCita()).orElseThrow().getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(citaRepository.findAll().stream().anyMatch(x -> x.getEstado() == EstadoCita.DISPONIBLE
                && x.getVeterinario().getIdUsuario().equals(vet.getIdUsuario()))).isFalse();
    }

    @Test
    void solapamientoPorDuracionDelVeterinarioNoSePermite() {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var vet = crearVeterinario(clinica);
        LocalDate fecha = LocalDate.now().plusDays(4);
        // Cita A 8:00–8:40 confirmada; cupo B 8:20–9:00 del mismo veterinario (dato heredado).
        var a = crearCita(crearMascota(crearDuenio()), vet, clinica, fecha, LocalTime.of(8, 0), EstadoCita.CONFIRMADA);
        a.setDuracionMin(40);
        citaRepository.save(a);
        var b = cupo(vet, clinica, fecha, LocalTime.of(8, 20), 40);
        var duenio = crearDuenio();

        assertThatThrownBy(() -> citaService.agendar(form(b, crearMascota(duenio)), duenio.getIdUsuario()))
                .isInstanceOf(ReservaConcurrenteException.class);
        assertThat(citaRepository.findById(b.getIdCita()).orElseThrow().getEstado()).isEqualTo(EstadoCita.DISPONIBLE);
    }

    @Test
    void unaMascotaNoTieneCitasSimultaneas() {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var vet1 = crearVeterinario(clinica);
        var vet2 = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        LocalDate fecha = LocalDate.now().plusDays(5);
        citaService.agendar(form(cupo(vet1, clinica, fecha, LocalTime.of(9, 0), 60), mascota), duenio.getIdUsuario());
        var otro = cupo(vet2, clinica, fecha, LocalTime.of(9, 30), 30);

        assertThatThrownBy(() -> citaService.agendar(form(otro, mascota), duenio.getIdUsuario()))
                .hasMessage("La mascota ya tiene otra cita que se cruza con ese horario.");
    }

    @Test
    void ofertarCupoQueSeCruzaPorDuracionSeRechaza() {
        var clinica = crearClinica();
        var servicio = servicioPorDefecto();
        ofrecerServicio(clinica, servicio); // 30 min
        var vet = crearVeterinario(clinica);
        autorizarServicio(vet, servicio);
        LocalDate fecha = LocalDate.now().plusDays(6);
        OfertarCitaForm f = new OfertarCitaForm();
        f.setFecha(fecha);
        f.setHora(LocalTime.of(10, 0));
        f.setIdVeterinario(vet.getIdUsuario());
        f.setIdServicio(servicio.getIdServicio());
        citaService.ofertarCupo(f, clinica.getIdVeterinaria());

        f.setHora(LocalTime.of(10, 15));
        assertThatThrownBy(() -> citaService.ofertarCupo(f, clinica.getIdVeterinaria()))
                .hasMessageContaining("se cruza");
        f.setHora(LocalTime.of(10, 30));
        assertThat(citaService.ofertarCupo(f, clinica.getIdVeterinaria()).getDuracionMin()).isEqualTo(30);
    }

    @Test
    void reservasPendientesVencidasExpiranYLiberanElCupo() {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        LocalDate fecha = LocalDate.now().plusDays(3);
        var pendiente = crearCita(crearMascota(duenio), vet, clinica, fecha, LocalTime.of(11, 0), EstadoCita.PENDIENTE);
        pendiente.setExpiraEn(LocalDateTime.now().minusMinutes(1));
        citaRepository.save(pendiente);
        sincronizar();

        assertThat(citaService.expirarPendientes()).isEqualTo(1);
        sincronizar();
        assertThat(citaRepository.findById(pendiente.getIdCita()).orElseThrow().getEstado()).isEqualTo(EstadoCita.RECHAZADA);
        assertThat(citaRepository.findAll().stream().anyMatch(x -> x.getEstado() == EstadoCita.DISPONIBLE
                && x.getFecha().equals(fecha) && x.getHora().equals(LocalTime.of(11, 0)))).isTrue();
        assertThat(notificaciones.countByIdUsuarioAndLeidaFalse(duenio.getIdUsuario())).isEqualTo(1);
        // Idempotente: una segunda pasada no hace nada.
        assertThat(citaService.expirarPendientes()).isZero();
    }

    @Test
    void reservaConMenosDeLaAnticipacionMinimaSeRechaza() {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        LocalDateTime enMediaHora = LocalDateTime.now().plusMinutes(30).withSecond(0).withNano(0);
        var c = cupo(vet, clinica, enMediaHora.toLocalDate(), enMediaHora.toLocalTime(), 30);

        assertThatThrownBy(() -> citaService.agendar(form(c, crearMascota(duenio)), duenio.getIdUsuario()))
                .hasMessageContaining("anticipación");
    }
}
