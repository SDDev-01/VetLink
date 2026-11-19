import os

import pytest


def hay_db() -> bool:
    return bool(os.environ.get("DB_HOST"))


def pytest_collection_modifyitems(config, items):
    if hay_db():
        return
    omitir = pytest.mark.skip(reason="sin DB_HOST: prueba de integración omitida")
    for item in items:
        if "integracion" in item.keywords:
            item.add_marker(omitir)


@pytest.fixture
def conn():
    """Conexión de la prueba con autocommit: cada consulta ve lo que la API acaba de escribir."""
    from app.db import conexion

    with conexion() as c:
        c.autocommit(True)
        yield c


@pytest.fixture
def conn_admin():
    """Conexión con un usuario que puede leer datos del dueño (solo para comprobar privacidad)."""
    usuario = os.environ.get("DB_ADMIN_USER")
    clave = os.environ.get("DB_ADMIN_PASSWORD")
    if not usuario or not clave:
        pytest.skip("sin DB_ADMIN_USER/DB_ADMIN_PASSWORD")
    import pymysql
    import pymysql.cursors

    c = pymysql.connect(
        host=os.environ["DB_HOST"],
        port=int(os.environ.get("DB_PORT", "3306")),
        user=usuario,
        password=clave,
        database=os.environ.get("DB_NAME", "mydb"),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )
    try:
        yield c
    finally:
        c.close()


@pytest.fixture
def id_mascota_con_datos(conn) -> int:
    """Una mascota real que tenga vacunas e historial."""
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT m.idMascota FROM mascota m
            WHERE EXISTS (SELECT 1 FROM carnet_vacuna c WHERE c.idMascota = m.idMascota)
              AND EXISTS (SELECT 1 FROM historial_clinico h WHERE h.idMascota = m.idMascota)
            ORDER BY m.idMascota
            LIMIT 1
            """
        )
        fila = cur.fetchone()
    if fila is None:
        pytest.skip("la base no tiene una mascota con vacunas e historial")
    return fila["idMascota"]
