"""Consultas de SOLO LECTURA sobre las tablas de Spring.

Las columnas que se leen aquí están listadas en docs/contrato-documentos.md (sección 5).
Nada de este módulo lee datos personales del dueño (documento, dirección, teléfono, correo).
"""

from typing import Any

Fila = dict[str, Any]

MAX_ATENCIONES = 5


def obtener_mascota(conn, id_mascota: int) -> Fila | None:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT m.idMascota, m.nombre, m.genero, m.fechaNacimiento, m.peso,
                   e.nombreEspecie AS especie, r.nombreRaza AS raza
            FROM mascota m
            JOIN especie e ON e.idEspecie = m.idEspecie
            JOIN raza r ON r.idRaza = m.idRaza
            WHERE m.idMascota = %s
            """,
            (id_mascota,),
        )
        return cur.fetchone()


def listar_vacunas(conn, id_mascota: int) -> list[Fila]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT v.nombreVacuna AS vacuna, c.dosis, c.fechaAplicacion, c.proximaDosis
            FROM carnet_vacuna c
            JOIN vacuna v ON v.idVacuna = c.idVacuna
            WHERE c.idMascota = %s
            ORDER BY c.fechaAplicacion DESC, c.idCarnet DESC
            """,
            (id_mascota,),
        )
        return list(cur.fetchall())


def ultimas_atenciones(conn, id_mascota: int, limite: int = MAX_ATENCIONES) -> list[Fila]:
    """Últimas atenciones con el nombre del veterinario y los medicamentos formulados.

    Los registros antiguos no tienen fechaRegistro (solo el año en fechaConsulta):
    van después de los que sí la tienen.
    """
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT h.numeroHistorial, h.fechaRegistro, h.fechaConsulta,
                   h.motivoConsulta, h.diagnostico,
                   CONCAT(u.nombre, ' ', u.apellido) AS veterinario
            FROM historial_clinico h
            JOIN usuario u ON u.idUsuario = h.Veterinario_idUsuario
            WHERE h.idMascota = %s
            ORDER BY h.fechaRegistro IS NULL, h.fechaRegistro DESC, h.numeroHistorial DESC
            LIMIT %s
            """,
            (id_mascota, limite),
        )
        atenciones = list(cur.fetchall())
        if not atenciones:
            return atenciones

        numeros = [a["numeroHistorial"] for a in atenciones]
        marcadores = ", ".join(["%s"] * len(numeros))
        cur.execute(
            f"""
            SELECT hm.Historial_Clinico_numeroHistorial AS numeroHistorial,
                   md.nombreMedicamento AS medicamento, hm.cantidad
            FROM historial_medicamento hm
            JOIN medicamento md ON md.idMedicamento = hm.Medicamento_idMedicamento
            WHERE hm.Historial_Clinico_numeroHistorial IN ({marcadores})
            ORDER BY md.nombreMedicamento
            """,
            numeros,
        )
        por_historial: dict[int, list[Fila]] = {}
        for fila in cur.fetchall():
            por_historial.setdefault(fila.pop("numeroHistorial"), []).append(fila)

    for atencion in atenciones:
        atencion["medicamentos"] = por_historial.get(atencion["numeroHistorial"], [])
    return atenciones


def datos_pasaporte(conn, id_mascota: int) -> Fila | None:
    """Todo lo que muestra la página pública del pasaporte, o None si la mascota no existe."""
    mascota = obtener_mascota(conn, id_mascota)
    if mascota is None:
        return None
    return {
        "mascota": mascota,
        "vacunas": listar_vacunas(conn, id_mascota),
        "atenciones": ultimas_atenciones(conn, id_mascota),
    }
