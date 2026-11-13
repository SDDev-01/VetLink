package com.vetlink.support;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.historial.repository.HistorialClinicoRepository;
import com.vetlink.mascota.model.Especie;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.model.Raza;
import com.vetlink.mascota.repository.EspecieRepository;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.mascota.repository.RazaRepository;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.ubicacion.model.Ciudad;
import com.vetlink.ubicacion.repository.CiudadRepository;
import com.vetlink.usuario.model.Correo;
import com.vetlink.usuario.model.Recepcionista;
import com.vetlink.usuario.model.TipoDocumento;
import com.vetlink.usuario.model.Tutor;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.model.Veterinario;
import com.vetlink.usuario.repository.CorreoRepository;
import com.vetlink.usuario.repository.RecepcionistaRepository;
import com.vetlink.usuario.repository.TipoDocumentoRepository;
import com.vetlink.usuario.repository.TutorRepository;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.usuario.repository.VeterinarioRepository;
import com.vetlink.veterinaria.model.Servicio;
import com.vetlink.veterinaria.model.ServicioVeterinaria;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.ServicioRepository;
import com.vetlink.veterinaria.repository.ServicioVeterinariaRepository;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Base de las pruebas web: arranca el contexto completo con H2 (perfil "test")
 * y ofrece ayudas para crear datos y autenticarse en MockMvc.
 *
 * Cada prueba corre en una transacción que se revierte al final, así que los
 * datos creados no se filtran a otras pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class PruebaWebBase {

    public static final String PASSWORD_PRUEBAS = "Clave12345";

    private static final AtomicInteger SECUENCIA = new AtomicInteger(1000);

    @Autowired protected MockMvc mockMvc;
    @Autowired protected EntityManager em;
    @Autowired protected PasswordEncoder passwordEncoder;

    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected TutorRepository tutorRepository;
    @Autowired protected VeterinarioRepository veterinarioRepository;
    @Autowired protected RecepcionistaRepository recepcionistaRepository;
    @Autowired protected CorreoRepository correoRepository;
    @Autowired protected TipoDocumentoRepository tipoDocumentoRepository;
    @Autowired protected VeterinariaRepository veterinariaRepository;
    @Autowired protected ServicioRepository servicioRepository;
    @Autowired protected ServicioVeterinariaRepository servicioVeterinariaRepository;
    @Autowired protected EspecieRepository especieRepository;
    @Autowired protected RazaRepository razaRepository;
    @Autowired protected MascotaRepository mascotaRepository;
    @Autowired protected CitaRepository citaRepository;
    @Autowired protected CiudadRepository ciudadRepository;
    @Autowired protected HistorialClinicoRepository historialRepository;

    protected int siguiente() {
        return SECUENCIA.incrementAndGet();
    }

    /** Autentica la petición de MockMvc como el usuario dado. */
    protected RequestPostProcessor como(Usuario usuario) {
        String correo = correoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .stream().findFirst().map(Correo::getCorreo)
                .orElse("u" + usuario.getIdUsuario() + "@pruebas.vetlink");
        return user(new VetlinkUserPrincipal(usuario, correo));
    }

    // ------------------------------------------------------------------
    // Catálogos
    // ------------------------------------------------------------------

    protected TipoDocumento tipoDocumento() {
        return tipoDocumentoRepository.findAll().stream().findFirst().orElseGet(() -> {
            TipoDocumento t = new TipoDocumento();
            t.setNombreTipo("CC");
            t.setDescripcion("Cédula de ciudadanía");
            return tipoDocumentoRepository.save(t);
        });
    }

    protected Especie crearEspecie(String nombre) {
        Especie e = new Especie();
        e.setNombreEspecie(nombre);
        return especieRepository.save(e);
    }

    protected Especie especiePorDefecto() {
        return especieRepository.findAll().stream().findFirst().orElseGet(() -> crearEspecie("Perro"));
    }

    protected Raza crearRaza(Especie especie, String nombre) {
        Raza r = new Raza();
        r.setEspecie(especie);
        r.setNombreRaza(nombre);
        return razaRepository.save(r);
    }

    protected Servicio crearServicio(String nombre) {
        Servicio s = new Servicio();
        s.setNombreServicio(nombre);
        s.setDescripcion(nombre);
        return servicioRepository.save(s);
    }

    /** Servicio compartido por las pruebas (el mismo que usa crearCita). */
    protected Servicio servicioPorDefecto() {
        return servicioRepository.findAll().stream().findFirst()
                .orElseGet(() -> crearServicio("Consulta general"));
    }

    /** Registra que la clínica ofrece el servicio (requisito para agendar). */
    protected ServicioVeterinaria ofrecerServicio(Veterinaria clinica, Servicio servicio) {
        ServicioVeterinaria sv = new ServicioVeterinaria();
        sv.setVeterinaria(clinica);
        sv.setServicio(servicio);
        sv.setPrecio(50000.0);
        sv.setDuracionAprox(30);
        return servicioVeterinariaRepository.save(sv);
    }

    // ------------------------------------------------------------------
    // Usuarios
    // ------------------------------------------------------------------

    protected Usuario crearUsuarioBase(String nombre) {
        int n = siguiente();
        Usuario u = new Usuario();
        u.setNumeroDocumento("DOC" + n);
        u.setTipoDocumento(tipoDocumento());
        u.setNombre(nombre);
        u.setApellido("Prueba" + n);
        u.setPassword(passwordEncoder.encode(PASSWORD_PRUEBAS));
        u.setEstado((byte) 1);
        u = usuarioRepository.save(u);

        Correo c = new Correo();
        c.setUsuario(u);
        c.setCorreo(nombre.toLowerCase() + n + "@pruebas.vetlink");
        correoRepository.save(c);
        return u;
    }

    protected Usuario crearDuenio() {
        Usuario u = crearUsuarioBase("Duenio");
        Tutor t = new Tutor();
        t.setUsuario(u);
        t.setDireccion("Calle 1 # 2-3");
        t = tutorRepository.save(t);
        u.setTutor(t);
        return u;
    }

    protected Veterinaria crearClinica() {
        int n = siguiente();
        Veterinaria v = new Veterinaria();
        v.setNit("NIT-" + n);
        v.setNombre("Clinica " + n);
        v.setDescripcion("Clínica de pruebas " + n);
        v.setEstado((byte) 1);
        return veterinariaRepository.save(v);
    }

    protected Usuario crearRecepcionista(Veterinaria clinica, boolean principal) {
        Usuario u = crearUsuarioBase("Recepcion");
        Recepcionista r = new Recepcionista();
        r.setUsuario(u);
        r.setVeterinaria(clinica);
        r.setFechaContratacion(LocalDate.now());
        r.setTurno("MAÑANA");
        r.setEsPrincipal(principal);
        r = recepcionistaRepository.save(r);
        u.setRecepcionista(r);
        return u;
    }

    protected Usuario crearVeterinario(Veterinaria clinica) {
        Usuario u = crearUsuarioBase("Veterinario");
        Veterinario v = new Veterinario();
        v.setUsuario(u);
        v.setVeterinaria(clinica);
        v.setLicencia("LIC-" + u.getIdUsuario());
        v = veterinarioRepository.save(v);
        u.setVeterinario(v);
        return u;
    }

    // ------------------------------------------------------------------
    // Mascotas y citas
    // ------------------------------------------------------------------

    protected Mascota crearMascota(Usuario duenio) {
        Especie especie = especiePorDefecto();
        return crearMascota(duenio, especie, crearRaza(especie, "Criolla"));
    }

    protected Mascota crearMascota(Usuario duenio, Especie especie, Raza raza) {
        Mascota m = new Mascota();
        m.setNombre("Mascota" + siguiente());
        m.setEspecie(especie);
        m.setRaza(raza);
        m.setGenero("M");
        m.setFechaNacimiento(LocalDate.now().minusYears(2));
        m.setPeso(10.0);
        m.setEstado((byte) 1);
        m.setTutor(duenio);
        return mascotaRepository.save(m);
    }

    protected Cita crearCita(Mascota mascota, Usuario veterinario, Veterinaria clinica,
                             LocalDate fecha, LocalTime hora, EstadoCita estado) {
        Cita c = new Cita();
        c.setMascota(mascota);
        c.setVeterinario(veterinario);
        c.setVeterinaria(clinica);
        c.setServicio(servicioPorDefecto());
        c.setFecha(fecha);
        c.setHora(hora);
        c.setEstado(estado);
        return citaRepository.save(c);
    }

    /** Cita AGENDADA para mañana a las 10:00. */
    protected Cita crearCita(Mascota mascota, Usuario veterinario, Veterinaria clinica) {
        return crearCita(mascota, veterinario, clinica, LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), EstadoCita.AGENDADA);
    }

    /** Historia clínica con la fecha de registro dada (null = registro antiguo, solo con año). */
    protected HistorialClinico crearHistorial(Mascota mascota, Usuario veterinario, java.time.LocalDateTime fechaRegistro) {
        HistorialClinico h = new HistorialClinico();
        h.setMascota(mascota);
        h.setVeterinario(veterinario);
        h.setMotivoConsulta("Control");
        h.setDiagnostico("Sano");
        h.setFechaConsulta(fechaRegistro != null ? fechaRegistro.getYear() : 2025);
        h.setFechaRegistro(fechaRegistro);
        h.setProximaCita(LocalDate.now().plusMonths(6));
        return historialRepository.save(h);
    }

    /**
     * Registra una clínica nueva por el flujo público "Soy Veterinaria" (POST /registro)
     * y devuelve la recepcionista creada, ya sincronizada desde la base.
     */
    protected Usuario registrarClinicaNueva(String nombreClinica) throws Exception {
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
                        .param("password", PASSWORD_PRUEBAS)
                        .param("confirmarPassword", PASSWORD_PRUEBAS)
                        .param("rol", "VETERINARIA")
                        .param("nuevaVeterinaria.nit", "900" + n)
                        .param("nuevaVeterinaria.nombre", nombreClinica)
                        .param("nuevaVeterinaria.telefono", "6011234567")
                        .param("nuevaVeterinaria.correo", "clinica" + n + "@pruebas.vetlink")
                        .param("nuevaVeterinaria.direccion", "Calle 100 # 10-10")
                        .param("nuevaVeterinaria.idCiudad", ciudad.getIdCiudad().toString())
                        .param("nuevaVeterinaria.barrio", "Chicó"))
                .andExpect(redirectedUrl("/login?registradoVet"));
        sincronizar();
        return usuarioRepository.findByNumeroDocumento("CLI" + n).orElseThrow();
    }

    /** Envía los cambios pendientes a la base y vacía el contexto de persistencia. */
    protected void sincronizar() {
        em.flush();
        em.clear();
    }
}
