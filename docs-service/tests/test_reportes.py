"""Reportes PDF: generación, autenticación por encabezados de Nginx y control de acceso."""

import os
from datetime import date, datetime

import pytest
from fastapi.testclient import TestClient

from app import pdf
from app.main import app

TOKEN = "token-de-prueba"


@pytest.fixture(autouse=True)
def token(monkeypatch):
    monkeypatch.setenv("INTERNAL_TOKEN", os.environ.get("INTERNAL_TOKEN") or TOKEN)


def cabeceras(usuario, rol, clinica=None, token=None):
    h = {"X-Internal-Token": token or os.environ["INTERNAL_TOKEN"], "X-Vetlink-Usuario": str(usuario), "X-Vetlink-Rol": rol}
    if clinica:
        h["X-Vetlink-Veterinaria"] = str(clinica)
    return h


MASCOTA = {"idMascota": 9, "nombre": "Ñoño Pérez", "genero": "M", "fechaNacimiento": date(2022, 5, 1), "peso": 8.2,
           "situacion": "ACTIVA", "idTutor": 1, "especie": "Perro", "raza": "Criolla", "tutor": "Ana Ruiz"}
REGISTRO = {"numeroHistorial": 3, "idMascota": 9, "fechaRegistro": datetime(2026, 3, 5, 14, 30), "fechaConsulta": 2026,
            "tipoRegistro": "CONSULTA", "motivoConsulta": "Cojera", "antecedentes": None, "temperatura": 38.4,
            "frecuenciaCardiaca": 90, "frecuenciaRespiratoria": None, "pesoKg": 8.1, "evaluacionClinica": None,
            "hallazgos": "Dolor en pata trasera", "diagnostico": "Esguince", "tratamiento": "Reposo",
            "medicamentosIndicados": "Meloxicam 0,1 mg/kg", "recomendaciones": None, "observaciones": None,
            "proximaCita": None, "idVeterinaria": 1, "veterinaria": "VetLink Centro", "veterinario": "Luis Gómez",
            "licencia": "MV-123", "medicamentos": [], "adjuntos": [{"nombreArchivo": "rx.png", "tipoArchivo": "image/png"}]}


# ---------------------------------------------------------------- unitarias

def test_pdf_valido_con_numeracion_y_datos():
    contenido = pdf.generar("Historial clínico", MASCOTA, {"nombre": "VetLink Centro", "nit": "900"}, [REGISTRO],
                            [{"vacuna": "Rabia", "dosis": "1", "fechaAplicacion": date(2026, 1, 1), "proximaDosis": date(2027, 1, 1)}],
                            datetime(2026, 11, 24, 9, 0))
    assert contenido.startswith(b"%PDF")
    assert len(contenido) > 1500


def test_registros_vacios_no_inventan_datos():
    contenido = pdf.generar("Historial clínico", MASCOTA, None, [], [], datetime(2026, 11, 24, 9, 0))
    assert contenido.startswith(b"%PDF")


def test_nombre_de_archivo_seguro():
    nombre = pdf.nombre_archivo("historial-clinico", "Ñoño/../Pérez", 9, datetime(2026, 11, 24, 9, 5))
    assert nombre == "historial-clinico_nono-perez_9_20261124-0905.pdf"
    assert "/" not in nombre and ".." not in nombre


def test_formato_de_fechas_sin_segundos():
    assert pdf.fecha(datetime(2026, 3, 5, 14, 30, 59)) == "05/03/2026 2:30 p. m."
    assert pdf.edad(date(2025, 11, 1), date(2026, 11, 24)) == "1 año"


def test_sin_token_interno_responde_401():
    cliente = TestClient(app)
    r = cliente.get("/api/reportes/mascotas/1/historial", headers={"X-Vetlink-Usuario": "1", "X-Vetlink-Rol": "TUTOR"})
    assert r.status_code == 401
    r = cliente.get("/api/reportes/mascotas/1/historial", headers=cabeceras(1, "TUTOR", token="otro"))
    assert r.status_code == 401


def test_rol_desconocido_o_usuario_invalido_responde_401():
    cliente = TestClient(app)
    assert cliente.get("/api/reportes/mascotas/1/vacunas", headers=cabeceras(1, "ADMIN")).status_code == 401
    assert cliente.get("/api/reportes/mascotas/1/vacunas", headers=cabeceras("x", "TUTOR")).status_code == 401


def test_recepcion_sin_clinica_responde_403():
    r = TestClient(app).get("/api/reportes/mascotas/1/vacunas", headers=cabeceras(5, "RECEPCIONISTA"))
    assert r.status_code == 403


def test_health_publico():
    assert TestClient(app).get("/api/reportes/health").json()["status"] == "ok"


# ---------------------------------------------------------------- integración (MySQL real)

@pytest.fixture
def mascota_con_historial(conn):
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT m.idMascota, m.Tutor_idUsuario AS tutor
            FROM mascota m
            WHERE EXISTS (SELECT 1 FROM historial_clinico h WHERE h.idMascota = m.idMascota)
            ORDER BY m.idMascota LIMIT 1
            """
        )
        fila = cur.fetchone()
    if fila is None:
        pytest.skip("la base no tiene mascotas con historial")
    return fila


@pytest.mark.integracion
def test_tutor_descarga_su_historial_y_queda_registrado(conn, mascota_con_historial):
    id_m, tutor = mascota_con_historial["idMascota"], mascota_con_historial["tutor"]
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) AS n FROM reporte_generado")
        antes = cur.fetchone()["n"]
    r = TestClient(app).get(f"/api/reportes/mascotas/{id_m}/historial", headers=cabeceras(tutor, "TUTOR"))
    assert r.status_code == 200
    assert r.headers["content-type"] == "application/pdf"
    assert r.headers["content-disposition"].startswith("attachment;")
    assert r.content.startswith(b"%PDF")
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) AS n FROM reporte_generado")
        assert cur.fetchone()["n"] == antes + 1


@pytest.mark.integracion
def test_otro_usuario_no_descarga_aunque_cambie_el_id(conn, mascota_con_historial):
    id_m, tutor = mascota_con_historial["idMascota"], mascota_con_historial["tutor"]
    otro = tutor + 100000
    cliente = TestClient(app)
    assert cliente.get(f"/api/reportes/mascotas/{id_m}/historial", headers=cabeceras(otro, "TUTOR")).status_code == 403
    assert cliente.get(f"/api/reportes/mascotas/{id_m}/vacunas", headers=cabeceras(otro, "VETERINARIO")).status_code == 403
    assert cliente.get("/api/reportes/mascotas/999999999/historial", headers=cabeceras(tutor, "TUTOR")).status_code == 403


@pytest.mark.integracion
def test_registro_individual_y_rango_de_fechas(conn, mascota_con_historial):
    id_m, tutor = mascota_con_historial["idMascota"], mascota_con_historial["tutor"]
    with conn.cursor() as cur:
        cur.execute("SELECT numeroHistorial FROM historial_clinico WHERE idMascota = %s LIMIT 1", (id_m,))
        numero = cur.fetchone()["numeroHistorial"]
    cliente = TestClient(app)
    r = cliente.get(f"/api/reportes/historial/{numero}/pdf", headers=cabeceras(tutor, "TUTOR"))
    assert r.status_code == 200 and r.content.startswith(b"%PDF")
    r = cliente.get(f"/api/reportes/mascotas/{id_m}/historial?fecha_inicio=2020-01-01&fecha_fin=2030-12-31",
                    headers=cabeceras(tutor, "TUTOR"))
    assert r.status_code == 200
    r = cliente.get(f"/api/reportes/mascotas/{id_m}/historial?fecha_inicio=2030-01-01&fecha_fin=2020-01-01",
                    headers=cabeceras(tutor, "TUTOR"))
    assert r.status_code == 422
