import pytest

from app import repositorio
from app.main import renderizar_pasaporte

pytestmark = pytest.mark.integracion

CAMPOS_DEL_DUENIO = {"numeroDocumento", "direccion", "numeroTelefono", "correo", "Tutor_idUsuario"}


def test_mascota_inexistente_devuelve_none(conn):
    assert repositorio.obtener_mascota(conn, 999_999_999) is None
    assert repositorio.datos_pasaporte(conn, 999_999_999) is None


def test_datos_de_una_mascota_real(conn, id_mascota_con_datos):
    datos = repositorio.datos_pasaporte(conn, id_mascota_con_datos)

    assert datos["mascota"]["idMascota"] == id_mascota_con_datos
    assert datos["mascota"]["especie"] and datos["mascota"]["raza"]
    assert datos["vacunas"], "la mascota elegida tiene vacunas"
    assert 1 <= len(datos["atenciones"]) <= repositorio.MAX_ATENCIONES
    for atencion in datos["atenciones"]:
        assert atencion["veterinario"]
        assert isinstance(atencion["medicamentos"], list)

    claves = set(datos["mascota"]) | {k for v in datos["vacunas"] for k in v} | {k for a in datos["atenciones"] for k in a}
    assert not claves & CAMPOS_DEL_DUENIO


def test_la_pagina_no_muestra_datos_del_duenio(conn, conn_admin, id_mascota_con_datos):
    with conn_admin.cursor() as cur:
        cur.execute(
            """
            SELECT u.numeroDocumento, t.direccion,
                   (SELECT GROUP_CONCAT(numeroTelefono) FROM telefono WHERE idUsuario = u.idUsuario) AS telefonos,
                   (SELECT GROUP_CONCAT(correo) FROM correo WHERE idUsuario = u.idUsuario) AS correos
            FROM mascota m
            JOIN usuario u ON u.idUsuario = m.Tutor_idUsuario
            LEFT JOIN tutor t ON t.idUsuario = u.idUsuario
            WHERE m.idMascota = %s
            """,
            (id_mascota_con_datos,),
        )
        duenio = cur.fetchone()

    html = renderizar_pasaporte(repositorio.datos_pasaporte(conn, id_mascota_con_datos), None)

    prohibidos = [duenio["numeroDocumento"], duenio["direccion"]]
    prohibidos += (duenio["telefonos"] or "").split(",") + (duenio["correos"] or "").split(",")
    for valor in filter(None, prohibidos):
        assert valor not in html
