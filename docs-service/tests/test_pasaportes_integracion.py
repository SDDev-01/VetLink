"""Flujo completo del pasaporte contra MySQL (usuario vetlink_fast)."""

from datetime import datetime, timedelta

import pytest
from fastapi.testclient import TestClient

from app.main import app

pytestmark = pytest.mark.integracion

TOKEN = "token-interno-de-pruebas"
BASE = "http://vetlink.pruebas"
INTERNO = {"X-Internal-Token": TOKEN}
TOKEN_INVENTADO = "TokenQueNoExiste_1234567890abcdefghijklmnopq"


@pytest.fixture
def cliente(monkeypatch):
    monkeypatch.setenv("INTERNAL_TOKEN", TOKEN)
    monkeypatch.setenv("PUBLIC_BASE_URL", BASE)
    return TestClient(app)


@pytest.fixture
def mascota(conn) -> dict:
    with conn.cursor() as cur:
        cur.execute("SELECT idMascota, nombre FROM mascota ORDER BY idMascota LIMIT 1")
        fila = cur.fetchone()
    if fila is None:
        pytest.skip("la base no tiene mascotas")
    return fila


@pytest.fixture
def id_usuario(conn) -> int:
    with conn.cursor() as cur:
        cur.execute("SELECT MIN(idUsuario) AS id FROM usuario")
        return cur.fetchone()["id"]


def crear(cliente, mascota, id_usuario, horas=24):
    respuesta = cliente.post(
        "/internal/pasaportes",
        json={"idMascota": mascota["idMascota"], "idUsuario": id_usuario, "duracionHoras": horas},
        headers=INTERNO,
    )
    assert respuesta.status_code == 201, respuesta.text
    return respuesta.json()


def listar(cliente, mascota) -> list[dict]:
    respuesta = cliente.get(f"/internal/pasaportes?idMascota={mascota['idMascota']}", headers=INTERNO)
    assert respuesta.status_code == 200
    return respuesta.json()


def fila_de(lista, token) -> dict:
    return next(p for p in lista if p["token"] == token)


def aperturas(conn, token) -> list[dict]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT a.ip, a.user_agent FROM pasaporte_acceso a
            JOIN pasaporte p ON p.idPasaporte = a.idPasaporte WHERE p.token = %s
            """,
            (token,),
        )
        return list(cur.fetchall())


def test_crear_devuelve_token_url_y_vencimiento(cliente, conn, mascota, id_usuario):
    creado = crear(cliente, mascota, id_usuario, horas=168)

    assert len(creado["token"]) == 43
    assert creado["url"] == f"{BASE}/p/{creado['token']}"
    with conn.cursor() as cur:
        cur.execute("SELECT creado_en FROM pasaporte WHERE token = %s", (creado["token"],))
        creado_en = cur.fetchone()["creado_en"]
    assert datetime.fromisoformat(creado["expiraEn"]) - creado_en == timedelta(hours=168)


def test_mascota_inexistente_da_404(cliente, id_usuario):
    respuesta = cliente.post(
        "/internal/pasaportes",
        json={"idMascota": 999_999_999, "idUsuario": id_usuario, "duracionHoras": 24},
        headers=INTERNO,
    )
    assert respuesta.status_code == 404


def test_usuario_inexistente_da_404(cliente, mascota):
    respuesta = cliente.post(
        "/internal/pasaportes",
        json={"idMascota": mascota["idMascota"], "idUsuario": 999_999_999, "duracionHoras": 24},
        headers=INTERNO,
    )
    assert respuesta.status_code == 404


def test_listar_incluye_el_pasaporte_activo(cliente, mascota, id_usuario):
    creado = crear(cliente, mascota, id_usuario)
    fila = fila_de(listar(cliente, mascota), creado["token"])
    assert fila["estado"] == "ACTIVO"
    assert fila["revocadoEn"] is None
    assert fila["aperturas"] == 0
    assert fila["url"] == creado["url"]


def test_apertura_valida_muestra_la_pagina_y_queda_registrada(cliente, conn, mascota, id_usuario):
    creado = crear(cliente, mascota, id_usuario)

    respuesta = cliente.get(
        f"/p/{creado['token']}",
        headers={"X-Real-IP": "203.0.113.9", "X-Forwarded-For": "203.0.113.9", "User-Agent": "pytest-navegador/1.0"},
    )

    assert respuesta.status_code == 200
    assert mascota["nombre"] in respuesta.text
    assert respuesta.headers["cache-control"] == "no-store"
    assert aperturas(conn, creado["token"]) == [{"ip": "203.0.113.9", "user_agent": "pytest-navegador/1.0"}]
    assert fila_de(listar(cliente, mascota), creado["token"])["aperturas"] == 1


def test_qr_es_un_png(cliente, mascota, id_usuario):
    creado = crear(cliente, mascota, id_usuario)
    respuesta = cliente.get(f"/p/{creado['token']}/qr.png")
    assert respuesta.status_code == 200
    assert respuesta.headers["content-type"] == "image/png"
    assert respuesta.content.startswith(b"\x89PNG\r\n\x1a\n")


def test_token_inventado_da_404(cliente):
    assert cliente.get(f"/p/{TOKEN_INVENTADO}").status_code == 404
    assert cliente.get(f"/p/{TOKEN_INVENTADO}/qr.png").status_code == 404


def test_token_revocado_da_404(cliente, conn, mascota, id_usuario):
    creado = crear(cliente, mascota, id_usuario)

    assert cliente.post(f"/internal/pasaportes/{creado['token']}/revocar", headers=INTERNO).status_code == 204

    assert cliente.get(f"/p/{creado['token']}").status_code == 404
    assert cliente.get(f"/p/{creado['token']}/qr.png").status_code == 404
    assert aperturas(conn, creado["token"]) == []
    fila = fila_de(listar(cliente, mascota), creado["token"])
    assert fila["estado"] == "REVOCADO"
    assert fila["revocadoEn"] is not None


def test_revocar_dos_veces_conserva_la_fecha(cliente, mascota, id_usuario):
    creado = crear(cliente, mascota, id_usuario)
    ruta = f"/internal/pasaportes/{creado['token']}/revocar"

    assert cliente.post(ruta, headers=INTERNO).status_code == 204
    primera = fila_de(listar(cliente, mascota), creado["token"])["revocadoEn"]
    assert cliente.post(ruta, headers=INTERNO).status_code == 204
    segunda = fila_de(listar(cliente, mascota), creado["token"])["revocadoEn"]

    assert primera == segunda


def test_revocar_token_inexistente_da_404(cliente):
    assert cliente.post(f"/internal/pasaportes/{TOKEN_INVENTADO}/revocar", headers=INTERNO).status_code == 404


def test_token_vencido_da_404(cliente, conn, mascota, id_usuario):
    token = "PYTEST-vencido-" + datetime.now().strftime("%Y%m%d%H%M%S%f")
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO pasaporte (token, idMascota, idUsuarioCreador, creado_en, expira_en)
            VALUES (%s, %s, %s, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY)
            """,
            (token, mascota["idMascota"], id_usuario),
        )
    conn.commit()

    assert cliente.get(f"/p/{token}").status_code == 404
    assert cliente.get(f"/p/{token}/qr.png").status_code == 404
    assert fila_de(listar(cliente, mascota), token)["estado"] == "VENCIDO"
