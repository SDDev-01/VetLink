"""Pruebas sin base de datos: autenticación interna, validación de entrada y formato de token."""

import pytest
from fastapi.testclient import TestClient

from app import pasaportes
from app.db import obtener_conexion
from app.main import app

TOKEN = "token-interno-de-pruebas"


@pytest.fixture
def cliente(monkeypatch):
    monkeypatch.setenv("INTERNAL_TOKEN", TOKEN)
    app.dependency_overrides[obtener_conexion] = lambda: None
    yield TestClient(app)
    app.dependency_overrides.clear()


PETICIONES_INTERNAS = [
    ("post", "/internal/pasaportes", {"idMascota": 1, "idUsuario": 1, "duracionHoras": 24}),
    ("get", "/internal/pasaportes?idMascota=1", None),
    ("post", "/internal/pasaportes/abc/revocar", None),
]


@pytest.mark.parametrize("metodo,ruta,cuerpo", PETICIONES_INTERNAS)
def test_sin_encabezado_da_401(cliente, metodo, ruta, cuerpo):
    respuesta = cliente.request(metodo, ruta, json=cuerpo)
    assert respuesta.status_code == 401


@pytest.mark.parametrize("metodo,ruta,cuerpo", PETICIONES_INTERNAS)
def test_encabezado_incorrecto_da_401(cliente, metodo, ruta, cuerpo):
    respuesta = cliente.request(metodo, ruta, json=cuerpo, headers={"X-Internal-Token": "otro"})
    assert respuesta.status_code == 401


def test_sin_token_configurado_rechaza_todo(cliente, monkeypatch):
    monkeypatch.setenv("INTERNAL_TOKEN", "")
    respuesta = cliente.get("/internal/pasaportes?idMascota=1", headers={"X-Internal-Token": ""})
    assert respuesta.status_code == 401


@pytest.mark.parametrize("duracion", [0, 1, 48, 167, 721, "24"])
def test_duracion_no_permitida_da_422(cliente, duracion):
    respuesta = cliente.post(
        "/internal/pasaportes",
        json={"idMascota": 1, "idUsuario": 1, "duracionHoras": duracion},
        headers={"X-Internal-Token": TOKEN},
    )
    assert respuesta.status_code == 422


def test_cuerpo_incompleto_da_422(cliente):
    respuesta = cliente.post("/internal/pasaportes", json={"idMascota": 1}, headers={"X-Internal-Token": TOKEN})
    assert respuesta.status_code == 422


def test_token_con_formato_invalido_da_404_sin_consultar(cliente):
    respuesta = cliente.get("/p/no%20valido%3B")
    assert respuesta.status_code == 404
    assert "no está disponible" in respuesta.text
    assert respuesta.headers["referrer-policy"] == "no-referrer"


def test_token_generado_es_urlsafe_de_43_caracteres():
    token = pasaportes.generar_token()
    assert len(token) == 43
    assert pasaportes.formato_valido(token)


@pytest.mark.parametrize(
    "real_ip,forwarded,conexion,esperada",
    [
        ("203.0.113.5", "198.51.100.1", "172.18.0.5", "203.0.113.5"),
        (None, "198.51.100.1, 10.0.0.1", "172.18.0.5", "198.51.100.1"),
        ("basura", None, "172.18.0.5", "172.18.0.5"),
        (None, None, None, "0.0.0.0"),
        ("2001:db8::1", None, None, "2001:db8::1"),
    ],
)
def test_ip_del_cliente(real_ip, forwarded, conexion, esperada):
    assert pasaportes.ip_del_cliente(real_ip, forwarded, conexion) == esperada
