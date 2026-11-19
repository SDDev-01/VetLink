"""El usuario de FastAPI (vetlink_fast) no puede tocar datos de Spring: lo impide MySQL."""

import pymysql
import pytest

pytestmark = pytest.mark.integracion

ER_TABLEACCESS_DENIED = 1142
ER_COLUMNACCESS_DENIED = 1143


def ejecutar(conn, sql):
    with conn.cursor() as cur:
        cur.execute(sql)
        return cur.fetchall()


def test_no_puede_modificar_mascotas(conn):
    with pytest.raises(pymysql.err.OperationalError) as error:
        ejecutar(conn, "UPDATE mascota SET nombre = nombre WHERE idMascota = 1")
    assert error.value.args[0] == ER_TABLEACCESS_DENIED


def test_no_puede_borrar_pasaportes(conn):
    with pytest.raises(pymysql.err.OperationalError) as error:
        ejecutar(conn, "DELETE FROM pasaporte WHERE 1 = 0")
    assert error.value.args[0] == ER_TABLEACCESS_DENIED


def test_no_puede_leer_el_documento_del_usuario(conn):
    with pytest.raises(pymysql.err.OperationalError) as error:
        ejecutar(conn, "SELECT numeroDocumento FROM usuario LIMIT 1")
    assert error.value.args[0] == ER_COLUMNACCESS_DENIED


@pytest.mark.parametrize("tabla", ["tutor", "telefono", "correo"])
def test_no_puede_leer_datos_de_contacto(conn, tabla):
    with pytest.raises(pymysql.err.OperationalError) as error:
        ejecutar(conn, f"SELECT * FROM {tabla} LIMIT 1")
    assert error.value.args[0] == ER_TABLEACCESS_DENIED
