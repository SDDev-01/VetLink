package com.vetlink.veterinaria.service.impl;

import com.vetlink.ubicacion.model.Barrio;
import com.vetlink.ubicacion.model.Ciudad;
import com.vetlink.ubicacion.model.Localidad;
import com.vetlink.ubicacion.model.UbicacionVeterinaria;
import com.vetlink.ubicacion.repository.BarrioRepository;
import com.vetlink.ubicacion.repository.CiudadRepository;
import com.vetlink.ubicacion.repository.LocalidadRepository;
import com.vetlink.ubicacion.repository.UbicacionVeterinariaRepository;
import com.vetlink.veterinaria.dto.VeterinariaRegistroForm;
import com.vetlink.veterinaria.model.*;
import com.vetlink.veterinaria.repository.*;
import com.vetlink.veterinaria.service.VeterinariaRegistroService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
public class VeterinariaRegistroServiceImpl implements VeterinariaRegistroService {

    private final VeterinariaRepository veterinariaRepository;
    private final TelefonoVeterinariaRepository telefonoVeterinariaRepository;
    private final CorreoVeterinariaRepository correoVeterinariaRepository;
    private final CiudadRepository ciudadRepository;
    private final LocalidadRepository localidadRepository;
    private final BarrioRepository barrioRepository;
    private final UbicacionVeterinariaRepository ubicacionVeterinariaRepository;
    private final ServicioRepository servicioRepository;
    private final ServicioVeterinariaRepository servicioVeterinariaRepository;
    private final DiaSemanaRepository diaSemanaRepository;
    private final HorarioVeterinariaRepository horarioVeterinariaRepository;
    private final com.vetlink.veterinaria.service.CatalogoServiciosService catalogoServicios;
    private final java.time.Clock reloj;

    public VeterinariaRegistroServiceImpl(VeterinariaRepository veterinariaRepository,
                                           TelefonoVeterinariaRepository telefonoVeterinariaRepository,
                                           CorreoVeterinariaRepository correoVeterinariaRepository,
                                           CiudadRepository ciudadRepository,
                                           LocalidadRepository localidadRepository, BarrioRepository barrioRepository,
                                           UbicacionVeterinariaRepository ubicacionVeterinariaRepository,
                                           ServicioRepository servicioRepository, ServicioVeterinariaRepository servicioVeterinariaRepository,
                                           DiaSemanaRepository diaSemanaRepository, HorarioVeterinariaRepository horarioVeterinariaRepository,
                                           com.vetlink.veterinaria.service.CatalogoServiciosService catalogoServicios,
                                           java.time.Clock reloj) {
        this.veterinariaRepository = veterinariaRepository;
        this.telefonoVeterinariaRepository = telefonoVeterinariaRepository;
        this.correoVeterinariaRepository = correoVeterinariaRepository;
        this.ciudadRepository = ciudadRepository;
        this.localidadRepository = localidadRepository;
        this.barrioRepository = barrioRepository;
        this.ubicacionVeterinariaRepository = ubicacionVeterinariaRepository;
        this.servicioRepository = servicioRepository;
        this.servicioVeterinariaRepository = servicioVeterinariaRepository;
        this.diaSemanaRepository = diaSemanaRepository;
        this.horarioVeterinariaRepository = horarioVeterinariaRepository;
        this.catalogoServicios = catalogoServicios;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Veterinaria registrarVeterinariaCompleta(VeterinariaRegistroForm form) {
        String nit = form.getNit() == null ? "" : form.getNit().strip();
        String nombre = com.vetlink.comun.validacion.Textos.normalizar(form.getNombre());
        if (nitEnUso(nit)) {
            throw new IllegalArgumentException("Ya existe una veterinaria registrada con ese NIT");
        }
        if (nombreEnUso(nombre)) {
            throw new IllegalArgumentException("Ya existe una veterinaria registrada con ese nombre comercial");
        }
        if (correoEnUso(form.getCorreo())) {
            throw new IllegalArgumentException("Ese correo ya está registrado por otra veterinaria");
        }
        validarHorarios(form.getHorarios());
        List<ServicioElegido> elegidos = resolverServicios(form.getServicios());

        Veterinaria veterinaria = new Veterinaria();
        veterinaria.setNit(nit);
        veterinaria.setNombre(nombre);
        veterinaria.setDescripcion(form.getDescripcion());
        veterinaria.setPaginaWeb(form.getPaginaWeb());
        // Aprobación automática: al completar el registro (con todas sus validaciones) la clínica
        // queda activa. Se guarda cuándo y que fue automática; la suspensión sigue siendo estado = 0.
        veterinaria.setEstado((byte) 1);
        veterinaria.setAprobadaEn(java.time.LocalDateTime.now(reloj));
        veterinaria.setAprobacionAutomatica(true);
        veterinaria = veterinariaRepository.save(veterinaria);

        TelefonoVeterinaria telefono = new TelefonoVeterinaria();
        telefono.setVeterinaria(veterinaria);
        telefono.setNumeroTelefono(form.getTelefono());
        telefonoVeterinariaRepository.save(telefono);

        CorreoVeterinaria correo = new CorreoVeterinaria();
        correo.setVeterinaria(veterinaria);
        correo.setCorreo(form.getCorreo());
        correoVeterinariaRepository.save(correo);

        Barrio barrio = resolverBarrio(form.getIdCiudad(), form.getBarrio());

        UbicacionVeterinaria ubicacion = new UbicacionVeterinaria();
        ubicacion.setVeterinaria(veterinaria);
        ubicacion.setCalle(form.getDireccion());
        ubicacion.setNumero("S/N");
        ubicacion.setEstrato(3);
        ubicacion.setLatitud(form.getLatitud() != null ? form.getLatitud() : 4.6482);
        ubicacion.setLongitud(form.getLongitud() != null ? form.getLongitud() : -74.0648);
        ubicacion.setBarrio(barrio);
        ubicacionVeterinariaRepository.save(ubicacion);

        for (ServicioElegido e : elegidos) {
            Servicio servicio = e.servicio() != null ? e.servicio()
                    : catalogoServicios.obtenerOCrear(e.nombrePersonalizado(), null, Servicio.TipoRegistro.CLINICO);
            ServicioVeterinaria sv = new ServicioVeterinaria();
            sv.setVeterinaria(veterinaria);
            sv.setServicio(servicio);
            sv.setPrecio(e.precio() == null ? 0.0 : e.precio());
            sv.setDuracionAprox(30);
            servicioVeterinariaRepository.save(sv);
        }

        for (VeterinariaRegistroForm.HorarioDiaForm h : form.getHorarios()) {
            // Puede haber huecos en la lista si en el formulario se quitó un rango extra (VL-14).
            if (h == null || !h.isAbierto()) continue;
            DiaSemana dia = diaSemanaRepository.findById(h.getIdDiaSemana())
                    .orElseThrow(() -> new IllegalArgumentException("Dia invalido"));
            HorarioVeterinaria horario = new HorarioVeterinaria();
            horario.setVeterinaria(veterinaria);
            horario.setDiaSemana(dia);
            horario.setHoraApertura(LocalTime.parse(h.getHoraApertura()));
            horario.setHoraCierre(LocalTime.parse(h.getHoraCierre()));
            horario.setEstado((byte) 1);
            horarioVeterinariaRepository.save(horario);
        }

        return veterinaria;
    }

    private record ServicioElegido(Servicio servicio, String nombrePersonalizado, Double precio) {}

    /**
     * Servicios del selector: del catálogo (por id) o «Otros» (nombre escrito). Se ignoran las
     * filas vacías, se exige al menos uno y no se aceptan servicios repetidos (un «Otros» que ya
     * existe en el catálogo cuenta como ese servicio).
     */
    private List<ServicioElegido> resolverServicios(List<VeterinariaRegistroForm.ServicioOfrecidoForm> lista) {
        java.util.List<ServicioElegido> r = new java.util.ArrayList<>();
        java.util.Set<String> claves = new java.util.HashSet<>();
        if (lista != null) {
            for (VeterinariaRegistroForm.ServicioOfrecidoForm s : lista) {
                if (s == null || s.estaVacio()) continue;
                if (s.getPrecio() == null || s.getPrecio() < 0 || s.getPrecio() > 100_000_000) {
                    throw new IllegalArgumentException("Ingresa un precio válido (0 o mayor) para cada servicio");
                }
                ServicioElegido e;
                String clave;
                if (s.getIdServicio() != null) {
                    Servicio servicio = servicioRepository.findById(s.getIdServicio())
                            .orElseThrow(() -> new IllegalArgumentException("Servicio invalido"));
                    e = new ServicioElegido(servicio, null, s.getPrecio());
                    clave = com.vetlink.comun.validacion.Textos.claveSinTildes(servicio.getNombreServicio());
                } else {
                    String n = com.vetlink.veterinaria.service.CatalogoServiciosService.normalizarNombre(s.getNombrePersonalizado());
                    var existente = catalogoServicios.buscarPorNombre(n);
                    e = existente.map(sv -> new ServicioElegido(sv, null, s.getPrecio()))
                            .orElseGet(() -> new ServicioElegido(null, n, s.getPrecio()));
                    clave = com.vetlink.comun.validacion.Textos.claveSinTildes(n);
                }
                if (!claves.add(clave)) {
                    String nombre = e.servicio() != null ? e.servicio().getNombreServicio() : e.nombrePersonalizado();
                    throw new IllegalArgumentException("El servicio «" + nombre + "» está repetido");
                }
                r.add(e);
            }
        }
        if (r.isEmpty()) throw new IllegalArgumentException("Agrega al menos un servicio");
        return r;
    }

    @Override
    public boolean nitEnUso(String nit) {
        String n = nit == null ? "" : nit.strip();
        return !n.isEmpty() && veterinariaRepository.findAll().stream().anyMatch(v -> v.getNit().equalsIgnoreCase(n));
    }

    @Override
    public boolean nombreEnUso(String nombre) {
        String clave = com.vetlink.comun.validacion.Textos.claveSinTildes(nombre);
        return !clave.isEmpty() && veterinariaRepository.findAll().stream()
                .anyMatch(v -> com.vetlink.comun.validacion.Textos.claveSinTildes(v.getNombre()).equals(clave));
    }

    @Override
    public boolean correoEnUso(String correo) {
        String c = correo == null ? "" : correo.strip();
        return !c.isEmpty() && correoVeterinariaRepository.findAll().stream().anyMatch(x -> x.getCorreo().equalsIgnoreCase(c));
    }

    /**
     * VL-14: cada rango abierto debe tener apertura anterior al cierre, y los rangos
     * de un mismo día no se pueden cruzar. Se puede tener más de un rango por día.
     */
    private void validarHorarios(List<VeterinariaRegistroForm.HorarioDiaForm> horarios) {
        java.util.Map<Integer, List<LocalTime[]>> porDia = new java.util.HashMap<>();
        for (VeterinariaRegistroForm.HorarioDiaForm h : horarios) {
            if (h == null || !h.isAbierto()) continue;
            String nombreDia = diaSemanaRepository.findById(h.getIdDiaSemana())
                    .map(DiaSemana::getDiaSemana)
                    .orElseThrow(() -> new IllegalArgumentException("Dia invalido"));
            LocalTime apertura;
            LocalTime cierre;
            try {
                apertura = LocalTime.parse(h.getHoraApertura());
                cierre = LocalTime.parse(h.getHoraCierre());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Revisa las horas del " + nombreDia + ": formato inválido");
            }
            if (!apertura.isBefore(cierre)) {
                throw new IllegalArgumentException("El " + nombreDia
                        + ", la hora de apertura debe ser anterior a la de cierre");
            }
            for (LocalTime[] otro : porDia.computeIfAbsent(h.getIdDiaSemana(), k -> new java.util.ArrayList<>())) {
                if (apertura.isBefore(otro[1]) && otro[0].isBefore(cierre)) {
                    throw new IllegalArgumentException("El " + nombreDia + " tiene horarios que se cruzan");
                }
            }
            porDia.get(h.getIdDiaSemana()).add(new LocalTime[]{apertura, cierre});
        }
    }

    /**
     * El wizard solo pide Ciudad (dropdown) y Barrio (texto libre) - no pide
     * Localidad, aunque el esquema la exige como paso intermedio. Se busca el
     * barrio por nombre dentro de esa ciudad; si no existe, se crea bajo una
     * localidad generica "Sin localidad especificada" (se crea tambien si
     * hace falta). Es una adaptacion pragmatica del formulario original al
     * modelo relacional real.
     */
    private Barrio resolverBarrio(Integer idCiudad, String nombreBarrio) {
        Ciudad ciudad = ciudadRepository.findById(idCiudad)
                .orElseThrow(() -> new IllegalArgumentException("Ciudad invalida"));

        List<Localidad> localidades = localidadRepository.findByCiudad_IdCiudad(idCiudad);

        for (Localidad loc : localidades) {
            var existente = barrioRepository.findByLocalidad_IdLocalidad(loc.getIdLocalidad()).stream()
                    .filter(b -> b.getNombre().equalsIgnoreCase(nombreBarrio.trim()))
                    .findFirst();
            if (existente.isPresent()) return existente.get();
        }

        Localidad localidadDestino = localidades.stream()
                .filter(l -> l.getNombre().equalsIgnoreCase("Sin localidad especificada"))
                .findFirst()
                .orElseGet(() -> {
                    Localidad nueva = new Localidad();
                    nueva.setNombre("Sin localidad especificada");
                    nueva.setCiudad(ciudad);
                    return localidadRepository.save(nueva);
                });

        Barrio nuevoBarrio = new Barrio();
        nuevoBarrio.setNombre(nombreBarrio.trim());
        nuevoBarrio.setLocalidad(localidadDestino);
        return barrioRepository.save(nuevoBarrio);
    }
}
