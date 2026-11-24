"""Configuración del servicio, leída de variables de entorno (ver docs/contrato-documentos.md)."""

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Config:
    db_host: str
    db_port: int
    db_name: str
    db_user: str
    db_password: str
    internal_token: str
    public_base_url: str


def cargar() -> Config:
    return Config(
        db_host=os.environ.get("DB_HOST", "mysql"),
        db_port=int(os.environ.get("DB_PORT", "3306")),
        db_name=os.environ.get("DB_NAME", "mydb"),
        db_user=os.environ.get("DB_USER", "vetlink_fast"),
        db_password=os.environ.get("DB_PASSWORD", ""),
        internal_token=os.environ.get("INTERNAL_TOKEN", ""),
        public_base_url=os.environ.get("PUBLIC_BASE_URL", "http://localhost").rstrip("/"),
    )
