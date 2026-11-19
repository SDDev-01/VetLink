"""Conexión a MySQL con PyMySQL (sin ORM: el servicio solo tiene unas pocas consultas)."""

from collections.abc import Iterator
from contextlib import contextmanager

import pymysql
import pymysql.cursors

from .config import cargar

# Zona horaria de la sesión: America/Bogota. Colombia no tiene horario de verano,
# así que el desfase fijo equivale y no depende de las tablas de zonas de MySQL.
ZONA_HORARIA_SESION = "-05:00"


@contextmanager
def conexion() -> Iterator[pymysql.connections.Connection]:
    cfg = cargar()
    conn = pymysql.connect(
        host=cfg.db_host,
        port=cfg.db_port,
        user=cfg.db_user,
        password=cfg.db_password,
        database=cfg.db_name,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
        connect_timeout=5,
        init_command=f"SET time_zone = '{ZONA_HORARIA_SESION}'",
    )
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def obtener_conexion() -> Iterator[pymysql.connections.Connection]:
    """Dependencia de FastAPI: una conexión por petición."""
    with conexion() as conn:
        yield conn
