"""Tokens del pasaporte: creación, listado, revocación, validación pública y registro de aperturas.

Escribe solo en `pasaporte` y `pasaporte_acceso`. Las fechas se calculan con NOW() de MySQL
(sesión en hora de Bogotá, ver db.py), así todas salen del mismo reloj.
"""

import ipaddress
import re
import secrets
from typing import Any

import pymysql

DURACIONES_PERMITIDAS = (24, 168, 720)

# secrets.token_urlsafe solo produce [A-Za-z0-9_-]. Cualquier otra cosa ni se consulta.
_FORMATO_TOKEN = re.compile(r"^[A-Za-z0-9_-]{1,128}$")

_ER_NO_REFERENCED_ROW = 1452
_MAX_USER_AGENT = 512


class ReferenciaInexistente(Exception):
    """La mascota o el usuario no existen."""


def generar_token() -> str:
    return secrets.token_urlsafe(32)


def formato_valido(token: str) -> bool:
    return bool(_FORMATO_TOKEN.fullmatch(token))


def crear(conn, id_mascota: int, id_usuario: int, duracion_horas: int) -> dict[str, Any]:
    if duracion_horas not in DURACIONES_PERMITIDAS:
        raise ValueError(f"duración no permitida: {duracion_horas}")
    token = generar_token()
    with conn.cursor() as cur:
        try:
            cur.execute(
                """
                INSERT INTO pasaporte (token, idMascota, idUsuarioCreador, creado_en, expira_en)
                VALUES (%s, %s, %s, NOW(), NOW() + INTERVAL %s HOUR)
                """,
                (token, id_mascota, id_usuario, duracion_horas),
            )
        except pymysql.err.IntegrityError as e:
            if e.args and e.args[0] == _ER_NO_REFERENCED_ROW:
                raise ReferenciaInexistente from e
            raise
        cur.execute("SELECT expira_en FROM pasaporte WHERE token = %s", (token,))
        expira_en = cur.fetchone()["expira_en"]
    conn.commit()
    return {"token": token, "expira_en": expira_en}


def listar(conn, id_mascota: int) -> list[dict[str, Any]]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT p.token, p.creado_en, p.expira_en, p.revocado_en,
                   CASE
                     WHEN p.revocado_en IS NOT NULL AND p.revocado_en <= NOW() THEN 'REVOCADO'
                     WHEN p.expira_en <= NOW() THEN 'VENCIDO'
                     ELSE 'ACTIVO'
                   END AS estado,
                   COUNT(a.idAcceso) AS aperturas
            FROM pasaporte p
            LEFT JOIN pasaporte_acceso a ON a.idPasaporte = p.idPasaporte
            WHERE p.idMascota = %s
            GROUP BY p.idPasaporte, p.token, p.creado_en, p.expira_en, p.revocado_en
            ORDER BY p.creado_en DESC, p.idPasaporte DESC
            """,
            (id_mascota,),
        )
        return list(cur.fetchall())


def revocar(conn, token: str) -> bool:
    """Marca el pasaporte como revocado. Devuelve False si el token no existe.

    Si ya estaba revocado, conserva la fecha original.
    """
    if not formato_valido(token):
        return False
    with conn.cursor() as cur:
        cur.execute("SELECT idPasaporte FROM pasaporte WHERE token = %s", (token,))
        if cur.fetchone() is None:
            return False
        cur.execute(
            "UPDATE pasaporte SET revocado_en = NOW() WHERE token = %s AND revocado_en IS NULL",
            (token,),
        )
    conn.commit()
    return True


def buscar_vigente(conn, token: str) -> dict[str, Any] | None:
    """El pasaporte si existe, no venció y no fue revocado; si no, None."""
    if not formato_valido(token):
        return None
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT idPasaporte, idMascota, expira_en
            FROM pasaporte
            WHERE token = %s AND revocado_en IS NULL AND expira_en > NOW()
            """,
            (token,),
        )
        return cur.fetchone()


def ip_del_cliente(x_real_ip: str | None, x_forwarded_for: str | None, ip_conexion: str | None) -> str:
    """IP del visitante: Nginx fija X-Real-IP y X-Forwarded-For con $remote_addr."""
    candidatos = [x_real_ip, (x_forwarded_for or "").split(",")[0], ip_conexion]
    for candidato in candidatos:
        candidato = (candidato or "").strip()
        try:
            return str(ipaddress.ip_address(candidato))
        except ValueError:
            continue
    return "0.0.0.0"


def registrar_acceso(conn, id_pasaporte: int, ip: str, user_agent: str | None) -> None:
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO pasaporte_acceso (idPasaporte, consultado_en, ip, user_agent)
            VALUES (%s, NOW(), %s, %s)
            """,
            (id_pasaporte, ip, (user_agent or None) and user_agent[:_MAX_USER_AGENT]),
        )
    conn.commit()
