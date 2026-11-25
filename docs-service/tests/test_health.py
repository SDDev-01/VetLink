from fastapi.testclient import TestClient

from app.config import problemas_url_publica
from app.main import app


def test_health_responde_ok():
    respuesta = TestClient(app).get("/health")
    assert respuesta.status_code == 200
    assert respuesta.json()["status"] == "ok"


def test_url_publica_local_o_privada_se_reporta():
    assert problemas_url_publica("http://localhost")
    assert problemas_url_publica("http://192.168.1.50")
    assert problemas_url_publica("http://10.0.0.3:8081")
    assert problemas_url_publica("http://127.0.0.1")


def test_url_publica_correcta():
    assert problemas_url_publica("https://vetlink.example.org") == []
    assert problemas_url_publica("https://abc123.trycloudflare.com") == []


def test_produccion_exige_https():
    assert problemas_url_publica("http://vetlink.example.org", "produccion")
    assert problemas_url_publica("https://vetlink.example.org", "produccion") == []
