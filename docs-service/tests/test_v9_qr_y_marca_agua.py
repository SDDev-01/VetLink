"""DUE-04 (URL del QR con la IP local) y DUE-05 (marca de agua en los reportes PDF)."""

import re
from datetime import date, datetime

from app import pdf
from app.config import cargar, problemas_url_publica, resolver_url_publica
from app.main import url_publica


# ------------------------------------------------------------------ DUE-04

def test_ip_reemplaza_localhost_y_conserva_puerto():
    assert resolver_url_publica("http://localhost", "192.168.1.100", "80") == "http://192.168.1.100"
    assert resolver_url_publica("", "192.168.1.100", "8081") == "http://192.168.1.100:8081"
    assert resolver_url_publica("http://localhost:8081", "192.168.1.100", "80") == "http://192.168.1.100:8081"


def test_url_publica_real_tiene_prioridad_sobre_ip():
    assert resolver_url_publica("https://abc.trycloudflare.com", "192.168.1.100", "80") == "https://abc.trycloudflare.com"


def test_sin_ip_se_mantiene_localhost_y_una_ip_invalida_se_ignora():
    assert resolver_url_publica(None, None, None) == "http://localhost"
    assert resolver_url_publica("http://localhost", "no es una ip!", "80") == "http://localhost"


def test_url_del_qr_usa_la_ip_configurada(monkeypatch):
    monkeypatch.setenv("PUBLIC_BASE_URL", "http://localhost")
    monkeypatch.setenv("IP", "192.168.1.100")
    monkeypatch.setenv("NGINX_HTTP_PORT", "80")
    url = url_publica("TOKEN123")
    assert url == "http://192.168.1.100/p/TOKEN123"
    assert "localhost" not in url
    # Sigue advirtiendo que una IP privada solo abre en la misma red.
    assert any("misma red" in p for p in problemas_url_publica(cargar().public_base_url))


# ------------------------------------------------------------------ DUE-05

MASCOTA = {"idMascota": 9, "nombre": "Luna", "genero": "H", "fechaNacimiento": date(2021, 1, 1), "peso": 5.0,
           "situacion": "ACTIVA", "estado": 1, "idTutor": 1, "especie": "Gato", "raza": "Siamés", "tutor": "Ana Ruiz"}


def _registro(n: int) -> dict:
    return {"numeroHistorial": n, "idMascota": 9, "fechaRegistro": datetime(2026, 3, 5, 14, 30), "fechaConsulta": 2026,
            "tipoRegistro": "CONSULTA", "motivoConsulta": "Control " * 40, "antecedentes": "Sin antecedentes " * 20,
            "temperatura": 38.4, "frecuenciaCardiaca": 90, "frecuenciaRespiratoria": 20, "pesoKg": 5.0,
            "evaluacionClinica": "Evaluación " * 30, "hallazgos": "Hallazgo " * 30, "diagnostico": "Sano",
            "tratamiento": "Ninguno", "medicamentosIndicados": None, "recomendaciones": "Dieta " * 20,
            "observaciones": None, "proximaCita": None, "idVeterinaria": 1, "veterinaria": "VetLink Centro",
            "veterinario": "Luis Gómez", "licencia": "MV-1", "medicamentos": [], "adjuntos": []}


def test_marca_agua_tiene_opacidad_entre_5_y_12_por_ciento():
    pdf._marca_agua.cache_clear()
    marca = pdf._marca_agua()
    assert marca is not None, "el logo de VetLink debe estar en app/static/vetlink-logo.png"
    imagen, proporcion = marca
    alfa_max = imagen._image.getchannel("A").getextrema()[1]
    assert 0.05 * 255 <= alfa_max <= 0.12 * 255
    assert 0 < proporcion < 1


def test_marca_agua_en_todas_las_paginas_del_pdf_descargado(monkeypatch):
    llamadas = []
    original = pdf._dibujar_marca_agua
    monkeypatch.setattr(pdf, "_dibujar_marca_agua", lambda c: (llamadas.append(c.getPageNumber()), original(c)))
    contenido = pdf.generar("Historia clínica", MASCOTA, None, [_registro(i) for i in range(1, 12)], None,
                            datetime(2026, 3, 6, 9, 0))
    assert contenido.startswith(b"%PDF")
    total = int(re.findall(rb"/Count (\d+)", contenido)[0])
    assert total > 1, "el reporte de prueba debe ocupar varias páginas"
    assert sorted(set(llamadas)) == list(range(1, total + 1))
    # La imagen queda embebida en el archivo (no depende de la vista previa).
    assert b"/Subtype /Image" in contenido
