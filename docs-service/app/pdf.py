"""Generación de reportes clínicos en PDF con ReportLab (estructura de expediente médico).

Cada página lleva encabezado (logo y datos de la veterinaria emisora, título, fecha de
generación) y pie con numeración «Página X de Y». Solo se imprime lo que está registrado:
los campos vacíos se omiten, nunca se inventa información clínica.
"""

import io
import os
from datetime import date, datetime
from typing import Any

from reportlab.lib import colors
from reportlab.lib.enums import TA_RIGHT
from reportlab.lib.pagesizes import LETTER
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfgen import canvas as rl_canvas
from reportlab.platypus import (Image, KeepTogether, Paragraph, SimpleDocTemplate, Spacer, Table,
                                TableStyle)
from xml.sax.saxutils import escape

AZUL = colors.HexColor("#1d4ed8")
AZUL_SUAVE = colors.HexColor("#dbeafe")
GRIS = colors.HexColor("#475569")
BORDE = colors.HexColor("#e2e8f0")

TIPOS = {
    "CONSULTA": "Consulta general",
    "CONTROL": "Control veterinario",
    "PROCEDIMIENTO": "Procedimiento clínico",
    "LABORATORIO": "Resultados de laboratorio",
    "VACUNACION": "Registro de vacunación",
    "URGENCIA": "Atención de urgencia",
}

_estilos = getSampleStyleSheet()
E_TITULO = ParagraphStyle("titulo", parent=_estilos["Title"], fontSize=16, textColor=AZUL, spaceAfter=4)
E_SECCION = ParagraphStyle("seccion", parent=_estilos["Heading2"], fontSize=11.5, textColor=AZUL, spaceBefore=10,
                           spaceAfter=4)
E_SUB = ParagraphStyle("sub", parent=_estilos["Heading3"], fontSize=10.5, spaceBefore=6, spaceAfter=2)
E_TEXTO = ParagraphStyle("texto", parent=_estilos["BodyText"], fontSize=9.2, leading=12.5)
E_ETIQ = ParagraphStyle("etiqueta", parent=E_TEXTO, fontSize=7.8, textColor=GRIS)
E_PEQ = ParagraphStyle("pequeno", parent=E_TEXTO, fontSize=8, textColor=GRIS)
E_DER = ParagraphStyle("derecha", parent=E_PEQ, alignment=TA_RIGHT)


def _p(texto: Any, estilo=E_TEXTO) -> Paragraph:
    return Paragraph(escape(str(texto)).replace("\n", "<br/>"), estilo)


def fecha(d: date | datetime | None) -> str:
    if d is None:
        return ""
    if isinstance(d, datetime):
        h = d.hour % 12 or 12
        return f"{d:%d/%m/%Y} {h}:{d:%M} {'a. m.' if d.hour < 12 else 'p. m.'}"
    return f"{d:%d/%m/%Y}"


def edad(nacimiento: date | None, hoy: date) -> str:
    if not nacimiento:
        return ""
    anios = hoy.year - nacimiento.year - ((hoy.month, hoy.day) < (nacimiento.month, nacimiento.day))
    if anios >= 1:
        return f"{anios} año{'s' if anios != 1 else ''}"
    meses = (hoy.year - nacimiento.year) * 12 + hoy.month - nacimiento.month - (hoy.day < nacimiento.day)
    return f"{max(meses, 0)} mes{'es' if meses != 1 else ''}"


class _CanvasNumerado(rl_canvas.Canvas):
    """Canvas que conoce el total de páginas para escribir «Página X de Y»."""

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._paginas: list[dict] = []

    def showPage(self):
        self._paginas.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        total = len(self._paginas)
        for estado in self._paginas:
            self.__dict__.update(estado)
            self.setFont("Helvetica", 7.5)
            self.setFillColor(GRIS)
            self.drawRightString(LETTER[0] - 18 * mm, 10 * mm, f"Página {self._pageNumber} de {total}")
            super().showPage()
        super().save()


def _encabezado(titulo: str, emisor: dict | None, logo_path: str | None, generado: datetime):
    def dibujar(c, doc):
        c.saveState()
        ancho, alto = LETTER
        y = alto - 14 * mm
        x = 18 * mm
        if logo_path:
            try:
                c.drawImage(logo_path, x, y - 12 * mm, width=14 * mm, height=14 * mm, preserveAspectRatio=True, mask="auto")
                x += 17 * mm
            except Exception:  # imagen corrupta: se omite el logo
                pass
        c.setFillColor(AZUL)
        c.setFont("Helvetica-Bold", 11)
        c.drawString(x, y - 3 * mm, (emisor or {}).get("nombre") or "VetLink")
        c.setFillColor(GRIS)
        c.setFont("Helvetica", 7.5)
        datos = []
        if emisor:
            if emisor.get("nit"):
                datos.append(f"NIT {emisor['nit']}")
            if emisor.get("direccion"):
                datos.append(str(emisor["direccion"]).strip())
            if emisor.get("telefono"):
                datos.append(f"Tel. {emisor['telefono']}")
        else:
            datos.append("Expediente consolidado de la plataforma VetLink")
        c.drawString(x, y - 7.5 * mm, " · ".join(datos)[:120])
        c.setFont("Helvetica-Bold", 9)
        c.setFillColor(colors.black)
        c.drawRightString(ancho - 18 * mm, y - 3 * mm, titulo)
        c.setFont("Helvetica", 7.5)
        c.setFillColor(GRIS)
        c.drawRightString(ancho - 18 * mm, y - 7.5 * mm, f"Generado: {fecha(generado)}")
        c.setStrokeColor(AZUL)
        c.setLineWidth(1.2)
        c.line(18 * mm, y - 11 * mm, ancho - 18 * mm, y - 11 * mm)
        c.setFont("Helvetica", 7)
        c.drawString(18 * mm, 10 * mm, "Documento confidencial: contiene información clínica. Uso exclusivo del tutor y del personal autorizado.")
        c.restoreState()

    return dibujar


def _tabla_datos(pares: list[tuple[str, Any]], columnas: int = 3) -> Table | None:
    celdas = [[_p(et, E_ETIQ), _p(val)] for et, val in pares if val not in (None, "")]
    if not celdas:
        return None
    filas = []
    for i in range(0, len(celdas), columnas):
        fila = []
        for celda in celdas[i:i + columnas]:
            fila.append([celda[0], celda[1]])
        while len(fila) < columnas:
            fila.append("")
        filas.append(fila)
    ancho = (LETTER[0] - 36 * mm) / columnas
    t = Table(filas, colWidths=[ancho] * columnas)
    t.setStyle(TableStyle([("VALIGN", (0, 0), (-1, -1), "TOP"), ("BOTTOMPADDING", (0, 0), (-1, -1), 4)]))
    return t


def _bloque_mascota(m: dict, hoy: date) -> list:
    genero = "Hembra" if m.get("genero") == "H" else ("Macho" if m.get("genero") == "M" else "")
    situacion = {"ACTIVA": "Activa", "PERDIDA": "Perdida", "FALLECIDA": "Fallecida"}.get(m.get("situacion") or "", "")
    t = _tabla_datos([
        ("Nombre", m.get("nombre")), ("Especie", m.get("especie")), ("Raza", m.get("raza")),
        ("Sexo", genero), ("Edad", edad(m.get("fechaNacimiento"), hoy)), ("Fecha de nacimiento", fecha(m.get("fechaNacimiento"))),
        ("Peso registrado", f"{m['peso']} kg" if m.get("peso") is not None else ""), ("Situación", situacion),
        ("Tutor", m.get("tutor")),
    ])
    return [Paragraph("Identificación del paciente", E_SECCION), t] if t else []


def _registro(h: dict) -> list:
    tipo = TIPOS.get((h.get("tipoRegistro") or "").upper(), "Consulta general")
    cuando = fecha(h.get("fechaRegistro")) if h.get("fechaRegistro") else f"Año {h.get('fechaConsulta')}"
    partes: list = [
        Paragraph(f"{escape(tipo)} — {escape(cuando)}", E_SUB),
        _p(f"Registro N.º {h['numeroHistorial']} · Establecimiento: {h.get('veterinaria') or 'No identificado'} · "
           f"Profesional: {h.get('veterinario') or ''}"
           + (f" (Reg. prof. {h['licencia']})" if h.get("licencia") else ""), E_PEQ),
    ]
    signos = []
    if h.get("temperatura") is not None:
        signos.append(f"Temperatura {h['temperatura']} °C")
    if h.get("pesoKg") is not None:
        signos.append(f"Peso {h['pesoKg']} kg")
    if h.get("frecuenciaCardiaca") is not None:
        signos.append(f"FC {h['frecuenciaCardiaca']} lpm")
    if h.get("frecuenciaRespiratoria") is not None:
        signos.append(f"FR {h['frecuenciaRespiratoria']} rpm")
    secciones = [
        ("Motivo de consulta", h.get("motivoConsulta")),
        ("Antecedentes", h.get("antecedentes")),
        ("Signos vitales", " · ".join(signos)),
        ("Evaluación clínica", h.get("evaluacionClinica")),
        ("Hallazgos", h.get("hallazgos")),
        ("Diagnóstico", h.get("diagnostico")),
        ("Tratamiento", h.get("tratamiento")),
        ("Medicamentos", "\n".join(filter(None, [h.get("medicamentosIndicados")] + [
            f"{m['medicamento']}" + (f" ({m['cantidad']})" if m.get("cantidad") else "") for m in h.get("medicamentos", [])]))),
        ("Recomendaciones", h.get("recomendaciones")),
        ("Observaciones", h.get("observaciones")),
        ("Próximo control", fecha(h.get("proximaCita"))),
        ("Documentos adjuntos (consultables en VetLink)", "\n".join(
            f"• {a['nombreArchivo']} ({a['tipoArchivo']})" for a in h.get("adjuntos", []))),
    ]
    filas = [[_p(et, E_ETIQ), _p(val)] for et, val in secciones if val]
    if filas:
        t = Table(filas, colWidths=[42 * mm, LETTER[0] - 36 * mm - 42 * mm])
        t.setStyle(TableStyle([
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("LINEBELOW", (0, 0), (-1, -2), 0.3, BORDE),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ("TOPPADDING", (0, 0), (-1, -1), 3),
        ]))
        partes.append(t)
    partes.append(Spacer(1, 6))
    return [KeepTogether(partes[:3]), *partes[3:]]


def _tabla_vacunas(vacunas: list[dict]) -> list:
    if not vacunas:
        return [_p("No hay vacunas registradas.", E_PEQ)]
    filas = [[_p(x, E_ETIQ) for x in ("Vacuna", "Dosis", "Lote", "Aplicación", "Próxima dosis", "Establecimiento / profesional")]]
    for v in vacunas:
        filas.append([_p(v.get("vacuna") or ""), _p(v.get("dosis") or ""), _p(v.get("lote") or ""),
                      _p(fecha(v.get("fechaAplicacion"))), _p(fecha(v.get("proximaDosis"))),
                      _p(" · ".join(filter(None, [v.get("veterinaria"), v.get("veterinario")])))])
    t = Table(filas, colWidths=[38 * mm, 22 * mm, 20 * mm, 22 * mm, 24 * mm, LETTER[0] - 36 * mm - 126 * mm], repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), AZUL_SUAVE),
        ("GRID", (0, 0), (-1, -1), 0.3, BORDE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
    ]))
    return [t]


def generar(titulo: str, mascota: dict, emisor: dict | None, historiales: list[dict] | None,
            vacunas: list[dict] | None, generado: datetime, uploads_dir: str | None = None,
            nota_filtro: str | None = None) -> bytes:
    """PDF completo. `historiales`/`vacunas` en None omiten la sección."""
    salida = io.BytesIO()
    doc = SimpleDocTemplate(salida, pagesize=LETTER, leftMargin=18 * mm, rightMargin=18 * mm,
                            topMargin=30 * mm, bottomMargin=18 * mm, title=titulo,
                            author=(emisor or {}).get("nombre") or "VetLink", subject=f"{titulo} de {mascota.get('nombre')}")
    logo = _logo(emisor, uploads_dir)
    historia: list = [Paragraph(escape(titulo), E_TITULO)]
    if nota_filtro:
        historia.append(_p(nota_filtro, E_PEQ))
    historia += _bloque_mascota(mascota, generado.date())
    if historiales is not None:
        historia.append(Paragraph("Registros clínicos", E_SECCION))
        if not historiales:
            historia.append(_p("No hay registros clínicos en el periodo o establecimiento consultado.", E_PEQ))
        for h in historiales:
            historia += _registro(h)
    if vacunas is not None:
        historia.append(Paragraph("Historial de vacunación", E_SECCION))
        historia += _tabla_vacunas(vacunas)
    historia.append(Spacer(1, 10))
    historia.append(_p("Este reporte se generó con la información registrada en VetLink. Los campos sin dato no se incluyen.", E_PEQ))
    dibujar = _encabezado(titulo, emisor, logo, generado)
    doc.build(historia, onFirstPage=dibujar, onLaterPages=dibujar, canvasmaker=_CanvasNumerado)
    return salida.getvalue()


def _logo(emisor: dict | None, uploads_dir: str | None) -> str | None:
    if not emisor or not uploads_dir:
        return None
    logo = emisor.get("logo") or ""
    if not logo.startswith("veterinarias/"):
        return None
    base = os.path.realpath(uploads_dir)
    ruta = os.path.realpath(os.path.join(base, logo))
    if not ruta.startswith(base + os.sep) or not os.path.isfile(ruta):
        return None
    return ruta


def nombre_archivo(prefijo: str, mascota_nombre: str, ident: int, generado: datetime) -> str:
    """Nombre descriptivo y seguro (solo ASCII, sin rutas)."""
    import unicodedata
    base = unicodedata.normalize("NFKD", mascota_nombre or "mascota").encode("ascii", "ignore").decode()
    import re
    base = re.sub(r"-{2,}", "-", "".join(ch if ch.isalnum() else "-" for ch in base)).strip("-").lower()[:40] or "mascota"
    return f"{prefijo}_{base}_{ident}_{generado:%Y%m%d-%H%M}.pdf"


__all__ = ["generar", "nombre_archivo", "fecha", "edad", "Image"]
