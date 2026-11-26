package com.vetlink.historial.service;

import com.vetlink.cita.model.EstadoCita;
import com.vetlink.historial.model.ArchivoHistorial;
import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.historial.repository.ArchivoHistorialRepository;
import com.vetlink.historial.repository.HistorialClinicoRepository;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.security.AccesoPacienteService;
import com.vetlink.usuario.model.Rol;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.vacuna.model.CarnetVacuna;
import com.vetlink.vacuna.repository.CarnetVacunaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Arma el "gestor de expedientes": una carpeta por mascota con sus documentos clínicos
 * (registros, vacunas y adjuntos). Cada rol solo recibe lo que puede consultar; la misma
 * regla se aplica luego en el detalle y en la descarga (AccesoPacienteService).
 */
@Service
@Transactional(readOnly = true)
public class ExpedienteService {

    /** {@code fecha} solo ordena; {@code fechaTexto} es lo que se muestra (nunca una fecha inventada). */
    public record Documento(String tipo, String icono, LocalDateTime fecha, String fechaTexto, String veterinaria,
                            String profesional, String url, String urlPdf, int adjuntos, String descripcion) {}

    public record Carpeta(Mascota mascota, List<Documento> documentos, long registros, long vacunas, long archivos) {}

    private final HistorialClinicoRepository historiales;
    private final ArchivoHistorialRepository archivos;
    private final CarnetVacunaRepository carnets;
    private final MascotaRepository mascotas;
    private final AccesoPacienteService acceso;

    public ExpedienteService(HistorialClinicoRepository historiales, ArchivoHistorialRepository archivos,
                             CarnetVacunaRepository carnets, MascotaRepository mascotas, AccesoPacienteService acceso) {
        this.historiales = historiales;
        this.archivos = archivos;
        this.carnets = carnets;
        this.mascotas = mascotas;
        this.acceso = acceso;
    }

    /** Carpetas visibles para el usuario. */
    public List<Carpeta> carpetasPara(Usuario u) {
        List<Mascota> lista = switch (u.getRol()) {
            // Solo las mascotas vigentes del dueño (las mismas de «Mis mascotas», incluidas las
            // fallecidas, que conservan su historial). Antes se listaban también las dadas de baja
            // (estado 0), como «Max» del seed de mydb.sql, que el dueño nunca veía en «Mis mascotas».
            case TUTOR -> mascotas.findByTutor_IdUsuarioAndEstado(u.getIdUsuario(), (byte) 1);
            case VETERINARIO -> mascotas.buscarConCitaConVeterinarioYEstados(u.getIdUsuario(), EstadoCita.RELACION_CLINICA);
            case RECEPCIONISTA -> mascotas.buscarConCitaEnVeterinariaYEstados(
                    u.getRecepcionista().getVeterinaria().getIdVeterinaria(), EstadoCita.RELACION_CLINICA);
        };
        return lista.stream()
                .sorted(Comparator.comparing(Mascota::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(m -> carpeta(m, u))
                .toList();
    }

    /** Carpeta de una mascota (quien llama ya verificó el acceso a la mascota). */
    public Carpeta carpeta(Mascota m, Usuario u) {
        boolean recepcion = u.getRol() == Rol.RECEPCIONISTA;
        Integer clinica = recepcion ? u.getRecepcionista().getVeterinaria().getIdVeterinaria() : null;
        List<HistorialClinico> regs = recepcion
                ? historiales.listarPorMascotaYClinica(m.getIdMascota(), clinica)
                : historiales.listarPorMascotaRecientes(m.getIdMascota());
        List<Documento> docs = new ArrayList<>();
        long nArchivos = 0;
        for (HistorialClinico h : regs) {
            List<ArchivoHistorial> adj = archivos.findByHistorial_NumeroHistorial(h.getNumeroHistorial());
            nArchivos += adj.size();
            docs.add(new Documento(h.getTipo().getEtiqueta(), h.getTipo().getIcono(), fechaDe(h), textoFecha(h),
                    clinicaDe(h), nombre(h.getVeterinario()),
                    "/historial/documento/" + h.getNumeroHistorial(),
                    "/api/reportes/historial/" + h.getNumeroHistorial() + "/pdf", adj.size(),
                    resumen(h.getDiagnostico())));
            for (ArchivoHistorial a : adj) {
                docs.add(new Documento("Adjunto: " + a.getNombreArchivo(), a.getIcono(),
                        a.getSubidoEn() != null ? a.getSubidoEn() : fechaDe(h),
                        a.getSubidoEn() != null ? FMT.fechaHora(a.getSubidoEn()) : textoFecha(h),
                        clinicaDe(h), nombre(h.getVeterinario()),
                        "/historial/archivos/" + a.getIdArchivo(), null, 0, h.getTipo().getEtiqueta()));
            }
        }
        List<CarnetVacuna> vac = carnets.findByMascota_IdMascotaOrderByFechaAplicacionDesc(m.getIdMascota()).stream()
                .filter(c -> !recepcion || (c.getVeterinaria() != null && c.getVeterinaria().getIdVeterinaria().equals(clinica)))
                .toList();
        for (CarnetVacuna c : vac) {
            docs.add(new Documento("Registro de vacunación", "fa-shield-virus", c.getFechaAplicacion().atStartOfDay(),
                    FMT.fecha(c.getFechaAplicacion()), c.getVeterinaria() == null ? "" : c.getVeterinaria().getNombre(), nombre(c.getUsuario()),
                    "/carnet/" + m.getIdMascota() + "#vacuna-" + c.getIdCarnet(),
                    "/api/reportes/mascotas/" + m.getIdMascota() + "/vacunas", 0,
                    c.getVacuna().getNombreVacuna() + " · " + c.getDosis()));
        }
        docs.sort(Comparator.comparing(Documento::fecha, Comparator.nullsLast(Comparator.reverseOrder())));
        return new Carpeta(m, docs, regs.size(), vac.size(), nArchivos);
    }

    public boolean puedeVer(HistorialClinico h, Usuario u) {
        return acceso.puedeVerDocumento(h, u);
    }

    private static final com.vetlink.comun.web.FormatoVista FMT = new com.vetlink.comun.web.FormatoVista();

    private static String textoFecha(HistorialClinico h) {
        if (h.getFechaRegistro() != null) return FMT.fechaHora(h.getFechaRegistro());
        return h.getFechaConsulta() == null ? "Sin fecha" : "Año " + h.getFechaConsulta() + " (fecha aproximada)";
    }

    private static LocalDateTime fechaDe(HistorialClinico h) {
        if (h.getFechaRegistro() != null) return h.getFechaRegistro();
        return h.getFechaConsulta() == null ? null : LocalDateTime.of(h.getFechaConsulta(), 1, 1, 0, 0);
    }

    private static String clinicaDe(HistorialClinico h) {
        if (h.getVeterinaria() != null) return h.getVeterinaria().getNombre();
        if (h.getVeterinario() != null && h.getVeterinario().getVeterinario() != null
                && h.getVeterinario().getVeterinario().getVeterinaria() != null) {
            return h.getVeterinario().getVeterinario().getVeterinaria().getNombre();
        }
        return "";
    }

    private static String nombre(Usuario u) {
        return u == null ? "" : u.getNombre() + " " + u.getApellido();
    }

    private static String resumen(String s) {
        if (s == null) return "";
        return s.length() > 90 ? s.substring(0, 89) + "…" : s;
    }
}
