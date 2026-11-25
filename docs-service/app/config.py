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
        public_base_url=resolver_url_publica(os.environ.get("PUBLIC_BASE_URL"), os.environ.get("IP"),
                                             os.environ.get("NGINX_HTTP_PORT")),
        entorno=os.environ.get("VETLINK_ENTORNO", "desarrollo").strip().lower(),
        # Por privacidad, el pasaporte público no muestra motivos, diagnósticos ni medicamentos
        # salvo que se habilite explícitamente.
        pasaporte_detalle_clinico=_bool(os.environ.get("PASAPORTE_DETALLE_CLINICO"), False),
        uploads_dir=os.environ.get("UPLOADS_DIR", "/uploads"),
        zona_horaria=os.environ.get("TZ", "America/Bogota"),
    )


_HOSTS_LOCALES = ("localhost", "127.0.0.1", "0.0.0.0", "::1")


def _host_valido(valor: str) -> bool:
    """IPv4/IPv6 o nombre de host (sin esquema, ruta ni puerto)."""
    try:
        ipaddress.ip_address(valor)
        return True
    except ValueError:
        pass
    partes = valor.split(".")
    return all(p and len(p) <= 63 and p.replace("-", "").isalnum() and not p.startswith("-") for p in partes)


def resolver_url_publica(public_base_url: str | None, ip: str | None, puerto: str | None) -> str:
    """DUE-04: URL que va dentro del QR.

    - Si se configuró IP (p. ej. IP=192.168.1.100) y PUBLIC_BASE_URL está vacía o apunta a
      localhost, se usa http://IP[:puerto]: el QR abre desde otros dispositivos de la misma red.
    - Si PUBLIC_BASE_URL es pública (dominio o túnel HTTPS), tiene prioridad.
    - Sin ninguna de las dos, se usa http://localhost (solo funciona en este equipo).
    El puerto es el que publica Nginx (NGINX_HTTP_PORT); el 80 no se escribe.
    """
    base = (public_base_url or "").strip().rstrip("/")
    ip = (ip or "").strip()
    partes = urlparse(base) if base else None
    host_base = (partes.hostname or "").lower() if partes else ""
    if ip and _host_valido(ip) and (not base or host_base in _HOSTS_LOCALES):
        esquema = partes.scheme if partes and partes.scheme in ("http", "https") else "http"
        p = str(partes.port) if partes and partes.port else (puerto or "").strip()
        defecto = "443" if esquema == "https" else "80"
        host = f"[{ip}]" if ":" in ip else ip
        return f"{esquema}://{host}" + ("" if p in ("", defecto) else f":{p}")
    return base or "http://localhost"


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
