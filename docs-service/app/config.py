"""Configuración del servicio, leída de variables de entorno (ver docs/contrato-documentos.md y API_REPORTES.md)."""

import ipaddress
import os
from dataclasses import dataclass
from urllib.parse import urlparse


def _bool(valor: str | None, defecto: bool = False) -> bool:
    if valor is None or valor == "":
        return defecto
    return valor.strip().lower() in ("1", "true", "si", "sí", "yes", "on")


@dataclass(frozen=True)
class Config:
    db_host: str
    db_port: int
    db_name: str
    db_user: str
    db_password: str
    internal_token: str
    public_base_url: str
    entorno: str
    pasaporte_detalle_clinico: bool
    uploads_dir: str
    zona_horaria: str


def cargar() -> Config:
    return Config(
        db_host=os.environ.get("DB_HOST", "mysql"),
        db_port=int(os.environ.get("DB_PORT", "3306")),
        db_name=os.environ.get("DB_NAME", "mydb"),
        db_user=os.environ.get("DB_USER", "vetlink_fast"),
        db_password=os.environ.get("DB_PASSWORD", ""),
        internal_token=os.environ.get("INTERNAL_TOKEN", ""),
        public_base_url=os.environ.get("PUBLIC_BASE_URL", "http://localhost").rstrip("/"),
        entorno=os.environ.get("VETLINK_ENTORNO", "desarrollo").strip().lower(),
        # Por privacidad, el pasaporte público no muestra motivos, diagnósticos ni medicamentos
        # salvo que se habilite explícitamente.
        pasaporte_detalle_clinico=_bool(os.environ.get("PASAPORTE_DETALLE_CLINICO"), False),
        uploads_dir=os.environ.get("UPLOADS_DIR", "/uploads"),
        zona_horaria=os.environ.get("TZ", "America/Bogota"),
    )


def problemas_url_publica(url: str, entorno: str = "desarrollo") -> list[str]:
    """Motivos por los que la URL del QR no funcionaría fuera de la red local (vacío = correcta)."""
    problemas: list[str] = []
    try:
        partes = urlparse(url)
    except ValueError:
        return ["PUBLIC_BASE_URL no es una URL válida"]
    if partes.scheme not in ("http", "https") or not partes.hostname:
        return ["PUBLIC_BASE_URL debe ser una URL absoluta, p. ej. https://vetlink.midominio.com"]
    host = partes.hostname.lower()
    if host in ("localhost", "0.0.0.0") or host.endswith(".local") or host.endswith(".localhost"):
        problemas.append(f"'{host}' solo existe en este equipo: el QR no abrirá desde otros dispositivos")
    else:
        try:
            ip = ipaddress.ip_address(host)
            if ip.is_private or ip.is_loopback or ip.is_link_local:
                problemas.append(f"{host} es una IP privada: el QR solo abrirá dentro de la misma red Wi-Fi")
        except ValueError:
            pass
    if entorno == "produccion" and partes.scheme != "https":
        problemas.append("En producción el QR debe usar HTTPS")
    return problemas
