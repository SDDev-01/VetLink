from datetime import date, datetime

from app.main import renderizar_pasaporte


def datos_ejemplo(nombre="Lula"):
    return {
        "mascota": {
            "idMascota": 1, "nombre": nombre, "genero": "H", "fechaNacimiento": date(2024, 3, 3),
            "peso": 2.5, "especie": "Perro", "raza": "Criolla",
        },
        "vacunas": [
            {"vacuna": "Rabia", "dosis": "Única", "fechaAplicacion": date(2026, 1, 10), "proximaDosis": date(2027, 1, 10)},
        ],
        "atenciones": [
            {
                "numeroHistorial": 7, "fechaRegistro": datetime(2026, 11, 1, 9, 30), "fechaConsulta": 2026,
                "motivoConsulta": "Control", "diagnostico": "Sano", "veterinario": "Ana Ruiz",
                "medicamentos": [{"medicamento": "Vermífugo", "cantidad": "1 tableta"}],
            },
            {
                "numeroHistorial": 3, "fechaRegistro": None, "fechaConsulta": 2024,
                "motivoConsulta": "Vacunación", "diagnostico": "Sin novedad", "veterinario": "Ana Ruiz",
                "medicamentos": [],
            },
        ],
    }


def test_muestra_datos_de_la_mascota():
    html = renderizar_pasaporte(datos_ejemplo(), datetime(2026, 11, 20, 10, 15))
    for texto in ["Lula", "Perro", "Criolla", "Hembra", "03/03/2024", "2.5 kg", "Rabia",
                  "Control", "Vermífugo (1 tableta)", "Ana Ruiz", "2024", "20/11/2026 a las 10:15"]:
        assert texto in html


def test_escapa_html():
    html = renderizar_pasaporte(datos_ejemplo(nombre="<script>x</script>"), None)
    assert "<script>x</script>" not in html
    assert "&lt;script&gt;" in html


def test_sin_vacunas_ni_atenciones():
    datos = datos_ejemplo()
    datos["vacunas"] = []
    datos["atenciones"] = []
    html = renderizar_pasaporte(datos, None)
    assert "No hay vacunas registradas." in html
    assert "No hay atenciones registradas." in html
