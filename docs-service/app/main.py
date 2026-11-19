"""Servicio docs-service: pasaporte de salud compartible de VetLink."""

from pathlib import Path

from fastapi import FastAPI
from fastapi.templating import Jinja2Templates

app = FastAPI(title="VetLink docs-service", version="0.1.0")

templates = Jinja2Templates(directory=Path(__file__).parent / "templates")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


def renderizar_pasaporte(datos: dict, expira_en) -> str:
    """HTML de la página pública. `datos` sale de repositorio.datos_pasaporte."""
    return templates.get_template("pasaporte.html").render(expira_en=expira_en, **datos)
