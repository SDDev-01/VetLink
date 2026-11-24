"""API de reportes clínicos en PDF (/api/reportes/...).

Autenticación: el navegador nunca habla con FastAPI directamente. Nginx valida la sesión de
VetLink con una subpetición (auth_request) a Spring, que aplica las reglas de acceso y devuelve
la identidad; Nginx la reenvía en X-Vetlink-Usuario / X-Vetlink-Rol / X-Vetlink-Veterinaria junto
con X-Internal-Token. Aquí se exige ese token y se repite la verificación de acceso con SQL.
"""

import secrets
from datetime import date, datetime
from urllib.parse import quote
from zoneinfo import ZoneInfo

from fastapi import APIRouter, Depends, Header, HTTPException, Query, Request, Response

from . import pdf, reportes_repo
from .config import cargar
from .db import obtener_conexion

router = APIRouter(prefix="/api/reportes", tags=["reportes"])

ROLES = {"TUTOR", "VETERINARIO", "RECEPCIONISTA"}


class Identidad:
    def __init__(self, id_usuario: int, rol: str, id_clinica: int | None):
        self.id_usuario = id_usuario
        self.rol = rol
        self.id_clinica = id_clinica


def identidad(
    x_internal_token: str | None = Header(default=None),
    x_vetlink_usuario: str | None = Header(default=None),
    x_vetlink_rol: str | None = Header(default=None),
    x_vetlink_veterinaria: str | None = Header(default=None),
) -> Identidad:
    esperado = cargar().internal_token
    if not esperado or not x_internal_token or not secrets.compare_digest(x_internal_token.encode(), esperado.encode()):
        raise HTTPException(status_code=401, detail="No autenticado")
    try:
        id_usuario = int(x_vetlink_usuario or "")
    except ValueError:
        raise HTTPException(status_code=401, detail="No autenticado")
    rol = (x_vetlink_rol or "").upper()
    if id_usuario <= 0 or rol not in ROLES:
        raise HTTPException(status_code=401, detail="No autenticado")
    clinica = None
    if x_vetlink_veterinaria:
        try:
            clinica = int(x_vetlink_veterinaria)
        except ValueError:
            raise HTTPException(status_code=401, detail="No autenticado")
    if rol == "RECEPCIONISTA" and not clinica:
        raise HTTPException(status_code=403, detail="Sin veterinaria asociada")
    return Identidad(id_usuario, rol, clinica)


def _ahora() -> datetime:
    return datetime.now(ZoneInfo(cargar().zona_horaria)).replace(tzinfo=None)


def _ip(request: Request, x_real_ip: str | None) -> str | None:
    return x_real_ip or (request.client.host if request.client else None)


def _respuesta_pdf(contenido: bytes, nombre: str) -> Response:
    return Response(
        content=contenido,
        media_type="application/pdf",
        headers={
            "Content-Disposition": f"attachment; filename=\"{nombre}\"; filename*=UTF-8''{quote(nombre)}",
            "Cache-Control": "no-store",
            "X-Content-Type-Options": "nosniff",
        },
    )


def _mascota_autorizada(conn, id_mascota: int, quien: Identidad) -> dict:
    m = reportes_repo.mascota(conn, id_mascota)
    # 404 y 403 se responden igual: no se revela si una mascota ajena existe.
    if m is None or not reportes_repo.puede_ver_mascota(conn, m, quien.id_usuario, quien.rol, quien.id_clinica):
        raise HTTPException(status_code=403, detail="No tienes permiso para descargar este reporte")
    return m


def _emisor(conn, quien: Identidad) -> dict | None:
    return reportes_repo.veterinaria(conn, quien.id_clinica) if quien.rol != "TUTOR" else None


@router.get("/health")
def salud() -> dict:
    return {"status": "ok", "servicio": "reportes"}


@router.get("/mascotas/{id_mascota}/historial")
def historial_mascota(
    id_mascota: int,
    request: Request,
    fecha_inicio: date | None = Query(default=None, description="AAAA-MM-DD"),
    fecha_fin: date | None = Query(default=None, description="AAAA-MM-DD"),
    quien: Identidad = Depends(identidad),
    conn=Depends(obtener_conexion),
    x_real_ip: str | None = Header(default=None),
):
    if id_mascota <= 0:
        raise HTTPException(status_code=422, detail="Identificador inválido")
    if fecha_inicio and fecha_fin and fecha_inicio > fecha_fin:
        raise HTTPException(status_code=422, detail="La fecha de inicio no puede ser posterior a la fecha de fin")
    m = _mascota_autorizada(conn, id_mascota, quien)
    filtro_clinica = quien.id_clinica if quien.rol == "RECEPCIONISTA" else None
    regs = reportes_repo.historiales(conn, id_mascota, filtro_clinica, fecha_inicio, fecha_fin)
    vacs = reportes_repo.vacunas(conn, id_mascota, filtro_clinica, fecha_inicio, fecha_fin)
    ahora = _ahora()
    nota = None
    if fecha_inicio or fecha_fin:
        nota = f"Periodo: {pdf.fecha(fecha_inicio) or 'inicio'} a {pdf.fecha(fecha_fin) or 'hoy'}"
    if filtro_clinica:
        nota = (nota + " · " if nota else "") + "Incluye solo los registros de esta veterinaria"
    contenido = pdf.generar("Historial clínico", m, _emisor(conn, quien), regs, vacs, ahora, cargar().uploads_dir, nota)
    tipo = "HISTORIAL_RANGO" if (fecha_inicio or fecha_fin) else "HISTORIAL_COMPLETO"
    reportes_repo.registrar_generacion(conn, quien.id_usuario, quien.rol, tipo, id_mascota, None,
                                       f"desde={fecha_inicio}&hasta={fecha_fin}" if nota else None, _ip(request, x_real_ip))
    return _respuesta_pdf(contenido, pdf.nombre_archivo("historial-clinico", m["nombre"], id_mascota, ahora))


@router.get("/mascotas/{id_mascota}/vacunas")
def vacunas_mascota(
    id_mascota: int,
    request: Request,
    quien: Identidad = Depends(identidad),
    conn=Depends(obtener_conexion),
    x_real_ip: str | None = Header(default=None),
):
    if id_mascota <= 0:
        raise HTTPException(status_code=422, detail="Identificador inválido")
    m = _mascota_autorizada(conn, id_mascota, quien)
    filtro_clinica = quien.id_clinica if quien.rol == "RECEPCIONISTA" else None
    vacs = reportes_repo.vacunas(conn, id_mascota, filtro_clinica)
    ahora = _ahora()
    contenido = pdf.generar("Historial de vacunación", m, _emisor(conn, quien), None, vacs, ahora, cargar().uploads_dir)
    reportes_repo.registrar_generacion(conn, quien.id_usuario, quien.rol, "VACUNAS", id_mascota, None, None,
                                       _ip(request, x_real_ip))
    return _respuesta_pdf(contenido, pdf.nombre_archivo("vacunacion", m["nombre"], id_mascota, ahora))


@router.get("/historial/{numero}/pdf")
def registro_individual(
    numero: int,
    request: Request,
    quien: Identidad = Depends(identidad),
    conn=Depends(obtener_conexion),
    x_real_ip: str | None = Header(default=None),
):
    if numero <= 0:
        raise HTTPException(status_code=422, detail="Identificador inválido")
    h = reportes_repo.historial(conn, numero)
    if h is None:
        raise HTTPException(status_code=403, detail="No tienes permiso para descargar este reporte")
    m = _mascota_autorizada(conn, h["idMascota"], quien)
    if quien.rol == "RECEPCIONISTA" and h.get("idVeterinaria") != quien.id_clinica:
        raise HTTPException(status_code=403, detail="No tienes permiso para descargar este reporte")
    emisor = reportes_repo.veterinaria(conn, h.get("idVeterinaria"))
    ahora = _ahora()
    contenido = pdf.generar("Registro clínico", m, emisor, [h], None, ahora, cargar().uploads_dir)
    reportes_repo.registrar_generacion(conn, quien.id_usuario, quien.rol, "REGISTRO", h["idMascota"], numero, None,
                                       _ip(request, x_real_ip))
    return _respuesta_pdf(contenido, pdf.nombre_archivo(f"registro-{numero}", m["nombre"], h["idMascota"], ahora))
