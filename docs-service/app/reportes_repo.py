"""Consultas de SOLO LECTURA para los reportes clínicos en PDF y su control de acceso.

Reglas de acceso (las mismas que aplica Spring en AccesoPacienteService; se repiten aquí
como defensa en profundidad, por si alguien llegara a FastAPI saltándose Nginx):
  TUTOR          -> solo sus mascotas actuales.
  VETERINARIO    -> mascotas con una cita CONFIRMADA / EN_ATENCION / ATENDIDA con él, o
                    con una emergencia asignada a él.
  RECEPCIONISTA  -> mascotas con una cita autorizada en su clínica, y solo los registros
                    y vacunas hechos en su clínica.
Nunca se leen documento, dirección, teléfono ni correo del tutor.
"""

from datetime import date
from typing import Any

Fila = dict[str, Any]

ESTADOS_RELACION = ("CONFIRMADA", "EN_ATENCION", "ATENDIDA")


def mascota(conn, id_mascota: int) -> Fila | None:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT m.idMascota, m.nombre, m.genero, m.fechaNacimiento, m.peso, m.situacion,
                   m.Tutor_idUsuario AS idTutor, e.nombreEspecie AS especie, r.nombreRaza AS raza,
                   CONCAT(u.nombre, ' ', u.apellido) AS tutor
            FROM mascota m
            JOIN especie e ON e.idEspecie = m.idEspecie
            JOIN raza r ON r.idRaza = m.idRaza
            JOIN usuario u ON u.idUsuario = m.Tutor_idUsuario
            WHERE m.idMascota = %s
            """,
            (id_mascota,),
        )
        return cur.fetchone()


def puede_ver_mascota(conn, mascota_fila: Fila, id_usuario: int, rol: str, id_clinica: int | None) -> bool:
    id_mascota = mascota_fila["idMascota"]
    marcadores = ", ".join(["%s"] * len(ESTADOS_RELACION))
    with conn.cursor() as cur:
        if rol == "TUTOR":
            return mascota_fila["idTutor"] == id_usuario
        if rol == "VETERINARIO":
            cur.execute(
                f"""
                SELECT 1 FROM cita WHERE idMascota = %s AND Veterinario_idUsuario = %s AND estado IN ({marcadores})
                UNION ALL
                SELECT 1 FROM emergencia WHERE idMascota = %s AND idVeterinario = %s
                LIMIT 1
                """,
                (id_mascota, id_usuario, *ESTADOS_RELACION, id_mascota, id_usuario),
            )
            return cur.fetchone() is not None
        if rol == "RECEPCIONISTA" and id_clinica:
            cur.execute(
                f"""
                SELECT 1 FROM cita WHERE idMascota = %s AND Veterinaria_idVeterinaria = %s AND estado IN ({marcadores})
                LIMIT 1
                """,
                (id_mascota, id_clinica, *ESTADOS_RELACION),
            )
            return cur.fetchone() is not None
    return False


def veterinaria(conn, id_veterinaria: int | None) -> Fila | None:
    if not id_veterinaria:
        return None
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT v.idVeterinaria, v.nombre, v.nit, v.logo, v.paginaWeb,
                   (SELECT t.numeroTelefono FROM telefono_veterinaria t WHERE t.idVeterinaria = v.idVeterinaria LIMIT 1) AS telefono,
                   (SELECT c.correo FROM correo_veterinaria c WHERE c.idVeterinaria = v.idVeterinaria LIMIT 1) AS correo,
                   (SELECT CONCAT(u.calle, ' ', COALESCE(u.numero, '')) FROM ubicacion_veterinaria u
                     WHERE u.idVeterinaria = v.idVeterinaria LIMIT 1) AS direccion
            FROM veterinaria v WHERE v.idVeterinaria = %s
            """,
            (id_veterinaria,),
        )
        return cur.fetchone()


def historiales(conn, id_mascota: int, id_clinica_filtro: int | None, desde: date | None, hasta: date | None) -> list[Fila]:
    condiciones = ["h.idMascota = %s"]
    params: list[Any] = [id_mascota]
    if id_clinica_filtro:
        condiciones.append("h.idVeterinaria = %s")
        params.append(id_clinica_filtro)
    if desde:
        condiciones.append("(h.fechaRegistro IS NULL AND h.fechaConsulta >= %s OR DATE(h.fechaRegistro) >= %s)")
        params += [desde.year, desde]
    if hasta:
        condiciones.append("(h.fechaRegistro IS NULL AND h.fechaConsulta <= %s OR DATE(h.fechaRegistro) <= %s)")
        params += [hasta.year, hasta]
    return _historiales(conn, " AND ".join(condiciones), params)


def historial(conn, numero: int) -> Fila | None:
    filas = _historiales(conn, "h.numeroHistorial = %s", [numero])
    return filas[0] if filas else None


def _historiales(conn, where: str, params: list[Any]) -> list[Fila]:
    with conn.cursor() as cur:
        cur.execute(
            f"""
            SELECT h.numeroHistorial, h.idMascota, h.fechaRegistro, h.fechaConsulta, h.tipoRegistro,
                   h.motivoConsulta, h.antecedentes, h.temperatura, h.frecuenciaCardiaca, h.frecuenciaRespiratoria,
                   h.pesoKg, h.evaluacionClinica, h.hallazgos, h.diagnostico, h.tratamiento,
                   h.medicamentosIndicados, h.recomendaciones, h.observaciones, h.proximaCita,
                   COALESCE(h.idVeterinaria, vt.idVeterinaria) AS idVeterinaria,
                   vet.nombre AS veterinaria,
                   CONCAT(u.nombre, ' ', u.apellido) AS veterinario, vt.licencia
            FROM historial_clinico h
            JOIN usuario u ON u.idUsuario = h.Veterinario_idUsuario
            LEFT JOIN veterinario vt ON vt.idUsuario = h.Veterinario_idUsuario
            LEFT JOIN veterinaria vet ON vet.idVeterinaria = COALESCE(h.idVeterinaria, vt.idVeterinaria)
            WHERE {where}
            ORDER BY h.fechaRegistro IS NULL, h.fechaRegistro DESC, h.numeroHistorial DESC
            """,
            params,
        )
        filas = list(cur.fetchall())
        if not filas:
            return filas
        numeros = [f["numeroHistorial"] for f in filas]
        marcadores = ", ".join(["%s"] * len(numeros))
        cur.execute(
            f"""
            SELECT hm.Historial_Clinico_numeroHistorial AS numero, md.nombreMedicamento AS medicamento, hm.cantidad
            FROM historial_medicamento hm JOIN medicamento md ON md.idMedicamento = hm.Medicamento_idMedicamento
            WHERE hm.Historial_Clinico_numeroHistorial IN ({marcadores})
            """,
            numeros,
        )
        meds: dict[int, list[Fila]] = {}
        for m in cur.fetchall():
            meds.setdefault(m.pop("numero"), []).append(m)
        cur.execute(
            f"""
            SELECT numeroHistorial AS numero, nombreArchivo, tipoArchivo, subidoEn
            FROM archivo_historial WHERE numeroHistorial IN ({marcadores}) ORDER BY idArchivo
            """,
            numeros,
        )
        adj: dict[int, list[Fila]] = {}
        for a in cur.fetchall():
            adj.setdefault(a.pop("numero"), []).append(a)
    for f in filas:
        f["medicamentos"] = meds.get(f["numeroHistorial"], [])
        f["adjuntos"] = adj.get(f["numeroHistorial"], [])
    return filas


def vacunas(conn, id_mascota: int, id_clinica_filtro: int | None, desde: date | None = None,
            hasta: date | None = None) -> list[Fila]:
    condiciones = ["c.idMascota = %s"]
    params: list[Any] = [id_mascota]
    if id_clinica_filtro:
        condiciones.append("c.idVeterinaria = %s")
        params.append(id_clinica_filtro)
    if desde:
        condiciones.append("c.fechaAplicacion >= %s")
        params.append(desde)
    if hasta:
        condiciones.append("c.fechaAplicacion <= %s")
        params.append(hasta)
    with conn.cursor() as cur:
        cur.execute(
            f"""
            SELECT v.nombreVacuna AS vacuna, c.dosis, c.fechaAplicacion, c.proximaDosis, c.observaciones,
                   l.numeroLote AS lote, vet.nombre AS veterinaria, CONCAT(u.nombre, ' ', u.apellido) AS veterinario
            FROM carnet_vacuna c
            JOIN vacuna v ON v.idVacuna = c.idVacuna
            LEFT JOIN lote_vacuna l ON l.idLote = c.idLote
            LEFT JOIN veterinaria vet ON vet.idVeterinaria = c.idVeterinaria
            LEFT JOIN usuario u ON u.idUsuario = c.idUsuario
            WHERE {" AND ".join(condiciones)}
            ORDER BY c.fechaAplicacion DESC, c.idCarnet DESC
            """,
            params,
        )
        return list(cur.fetchall())


def registrar_generacion(conn, id_usuario: int, rol: str, tipo: str, id_mascota: int | None,
                         numero: int | None, parametros: str | None, ip: str | None) -> None:
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO reporte_generado (idUsuario, rol, tipo, idMascota, numeroHistorial, parametros, ip, generadoEn)
            VALUES (%s, %s, %s, %s, %s, %s, %s, NOW())
            """,
            (id_usuario, rol, tipo, id_mascota, numero, (parametros or None) and parametros[:300], (ip or None) and ip[:45]),
        )
    conn.commit()
