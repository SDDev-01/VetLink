"""Servicio docs-service: pasaporte de salud compartible de VetLink.

Rutas internas (/internal/...): solo para Spring, con X-Internal-Token. Nginx las bloquea desde fuera.
Rutas públicas (/p/...): la página del pasaporte y su QR, sin sesión.
"""

import io
import logging
from contextlib import asynccontextmanager
import secrets
from datetime import datetime
from pathlib import Path
from typing import Literal

import qrcode
from fastapi import APIRouter, Depends, FastAPI, Header, HTTPException, Query, Request, Response
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from pydantic import BaseModel, Field

from . import pasaportes, reportes, repositorio
from .config import cargar, problemas_url_publica
from .db import obtener_conexion

@asynccontextmanager
async def ciclo_de_vida(_app: FastAPI):
    """Al arrancar avisa si la URL del QR no será accesible desde Internet."""
    cfg = cargar()
    for problema in problemas_url_publica(cfg.public_base_url, cfg.entorno):
        logging.getLogger("vetlink.qr").warning("PUBLIC_BASE_URL=%s: %s", cfg.public_base_url, problema)
    yield


app = FastAPI(title="VetLink docs-service", version="0.2.0", lifespan=ciclo_de_vida)

templates = Jinja2Templates(directory=Path(__file__).parent / "templates")

# El token va en la URL: que no se filtre por Referer, cachés ni buscadores.
ENCABEZADOS_PUBLICOS = {
    "Cache-Control": "no-store",
    "Referrer-Policy": "no-referrer",
    "X-Robots-Tag": "noindex, nofollow",
    "X-Content-Type-Options": "nosniff",
}


@app.get("/health")
def health() -> dict:
    cfg = cargar()
    problemas = problemas_url_publica(cfg.public_base_url, cfg.entorno)
    return {"status": "ok", "urlPublica": cfg.public_base_url, "urlPublicaValida": not problemas, "avisos": problemas}



def renderizar_pasaporte(datos: dict, expira_en, detalle_clinico: bool | None = None) -> str:
    """HTML de la página pública. `datos` sale de repositorio.datos_pasaporte.

    Sin detalle clínico (por defecto) no se muestran motivos, diagnósticos ni medicamentos:
    el enlace/QR es público y no debe exponer información clínica privada.
    """
    if detalle_clinico is None:
        detalle_clinico = cargar().pasaporte_detalle_clinico
    return templates.get_template("pasaporte.html").render(expira_en=expira_en, detalle_clinico=detalle_clinico, **datos)


def url_publica(token: str) -> str:
    return f"{cargar().public_base_url}/p/{token}"


# ---------------------------------------------------------------------------
# Rutas internas
# ---------------------------------------------------------------------------

def verificar_token_interno(x_internal_token: str | None = Header(default=None)) -> None:
    esperado = cargar().internal_token
    if not esperado or not x_internal_token or not secrets.compare_digest(
        x_internal_token.encode(), esperado.encode()
    ):
        raise HTTPException(status_code=401, detail="Token interno ausente o incorrecto")


interno = APIRouter(prefix="/internal", dependencies=[Depends(verificar_token_interno)])


class SolicitudPasaporte(BaseModel):
    idMascota: int = Field(gt=0)
    idUsuario: int = Field(gt=0)
    duracionHoras: Literal[24, 168, 720]


class PasaporteCreado(BaseModel):
    token: str
    url: str
    expiraEn: datetime


class PasaporteResumen(BaseModel):
    token: str
    url: str
    creadoEn: datetime
    expiraEn: datetime
    revocadoEn: datetime | None
    estado: Literal["ACTIVO", "VENCIDO", "REVOCADO"]
    aperturas: int


@interno.post("/pasaportes", status_code=201, response_model=PasaporteCreado)
def crear_pasaporte(solicitud: SolicitudPasaporte, conn=Depends(obtener_conexion)):
    try:
        creado = pasaportes.crear(conn, solicitud.idMascota, solicitud.idUsuario, solicitud.duracionHoras)
    except pasaportes.ReferenciaInexistente:
        raise HTTPException(status_code=404, detail="La mascota o el usuario no existen")
    return PasaporteCreado(token=creado["token"], url=url_publica(creado["token"]), expiraEn=creado["expira_en"])


@interno.get("/pasaportes", response_model=list[PasaporteResumen])
def listar_pasaportes(idMascota: int = Query(gt=0), conn=Depends(obtener_conexion)):
    return [
        PasaporteResumen(
            token=p["token"],
            url=url_publica(p["token"]),
            creadoEn=p["creado_en"],
            expiraEn=p["expira_en"],
            revocadoEn=p["revocado_en"],
            estado=p["estado"],
            aperturas=p["aperturas"],
        )
        for p in pasaportes.listar(conn, idMascota)
    ]


@interno.post("/pasaportes/{token}/revocar", status_code=204)
def revocar_pasaporte(token: str, conn=Depends(obtener_conexion)) -> Response:
    if not pasaportes.revocar(conn, token):
        raise HTTPException(status_code=404, detail="El pasaporte no existe")
    return Response(status_code=204)


app.include_router(interno)
app.include_router(reportes.router)


# ---------------------------------------------------------------------------
# Rutas públicas
# ---------------------------------------------------------------------------

def _no_disponible() -> HTMLResponse:
    html = templates.get_template("no_disponible.html").render()
    return HTMLResponse(html, status_code=404, headers=ENCABEZADOS_PUBLICOS)


@app.get("/p/{token}", response_class=HTMLResponse)
def ver_pasaporte(
    token: str,
    request: Request,
    conn=Depends(obtener_conexion),
    user_agent: str | None = Header(default=None),
    x_real_ip: str | None = Header(default=None),
    x_forwarded_for: str | None = Header(default=None),
):
    pasaporte = pasaportes.buscar_vigente(conn, token)
    if pasaporte is None:
        return _no_disponible()
    datos = repositorio.datos_pasaporte(conn, pasaporte["idMascota"])
    if datos is None:
        return _no_disponible()
    ip = pasaportes.ip_del_cliente(x_real_ip, x_forwarded_for, request.client.host if request.client else None)
    pasaportes.registrar_acceso(conn, pasaporte["idPasaporte"], ip, user_agent)
    return HTMLResponse(renderizar_pasaporte(datos, pasaporte["expira_en"]), headers=ENCABEZADOS_PUBLICOS)


@app.get("/p/{token}/qr.png")
def qr_pasaporte(token: str, conn=Depends(obtener_conexion)) -> Response:
    if pasaportes.buscar_vigente(conn, token) is None:
        return _no_disponible()
    imagen = qrcode.make(url_publica(token))
    salida = io.BytesIO()
    imagen.save(salida, format="PNG")
    return Response(salida.getvalue(), media_type="image/png", headers=ENCABEZADOS_PUBLICOS)
