#!/usr/bin/env python3
# =====================================================================
# database/pruebas/semilla_500.py — datos de prueba de VetLink (EXACTAMENTE 500 registros).
#
# Lo ejecuta scripts/semilla-pruebas.sh (dentro de un contenedor python:3.12-slim, sin
# dependencias). No cambia la estructura: solo genera sentencias INSERT para las tablas
# que ya existen. El script de shell vacía antes las tablas (TRUNCATE: los ID vuelven a 1).
#
#   python semilla_500.py                 imprime el SQL (INSERT) por la salida estándar
#   python semilla_500.py --adjuntos DIR  escribe en DIR los PDF de archivo_historial
#   python semilla_500.py --resumen       muestra cuántas filas genera por tabla
#   --ahora "AAAA-MM-DD HH:MM"            fija la hora de referencia (por defecto: ahora en Bogotá)
#
# Todas las fechas son relativas al momento de ejecución (historia, presente y futuro).
# Reglas que respeta (ver DisponibilidadService y CitaServiceImpl):
#   * Las franjas de los horarios APROBADOS quedan cubiertas por citas en todo el horizonte
#     (hoy .. hoy + vetlink.reglas.horizonte-agenda-dias), así la generación automática de
#     cupos no agrega filas al arrancar Spring.
#   * Las citas CONFIRMADAS de las próximas 24 h ya tienen su recordatorio (claveUnica), así
#     la tarea de recordatorios no duplica ni agrega filas.
#   * Las reservas PENDIENTES vencen después de la verificación (expiraEn > ahora + 20 h).
# La tabla «auditoria» completa el total: su cantidad se ajusta para que la suma sea 500.
#
# Contraseña de todas las cuentas: 123456V (hash BCrypt, el mismo PasswordEncoder de Spring).
# Correos: dueños 1..10@gmail.com, veterinarios 100..110@gmail.com, recepcionistas 1000..1010@gmail.com
# =====================================================================
import datetime as dt
import hashlib
import random
import sys
import uuid

TOTAL_OBJETIVO = 500
HORIZONTE_DIAS = 14          # vetlink.reglas.horizonte-agenda-dias
BOGOTA = dt.timezone(dt.timedelta(hours=-5))   # Colombia no tiene horario de verano
HASH_123456V = "$2a$10$sPB.CSZt/k/q.ZPqZ3lVBOCSBnt4.SMboKx41dCwLNthwbFmIl4bi"

args = sys.argv[1:]
AHORA = dt.datetime.now(BOGOTA).replace(tzinfo=None, second=0, microsecond=0)
if "--ahora" in args:
    AHORA = dt.datetime.strptime(args[args.index("--ahora") + 1], "%Y-%m-%d %H:%M")
HOY = AHORA.date()
rnd = random.Random(20261127)

TABLAS = {}      # tabla -> (columnas, filas)
ORDEN = []


def tabla(nombre, columnas):
    TABLAS[nombre] = (columnas, [])
    ORDEN.append(nombre)
    return TABLAS[nombre][1]


def fila(nombre, *valores):
    cols, filas = TABLAS[nombre]
    assert len(valores) == len(cols), (nombre, valores)
    filas.append(valores)
    return len(filas)


def dia(offset):
    return HOY + dt.timedelta(days=offset)


def momento(fecha, hhmm):
    h, m = map(int, hhmm.split(":"))
    return dt.datetime.combine(fecha, dt.time(h, m))


def proximo(dia_semana, desde):
    """Primera fecha >= desde con ese día ISO (1 = lunes)."""
    return desde + dt.timedelta(days=(dia_semana - desde.isoweekday()) % 7)


# =====================================================================
# 1. Catálogos
# =====================================================================
tabla("tipo_documento", ["idTipoDocumento", "nombreTipo", "descripcion"])
for r in [(1, "CC", "Cedula de ciudadania"), (2, "TI", "Tarjeta de identidad"),
          (3, "CE", "Cedula de extranjeria"), (4, "PA", "Pasaporte")]:
    fila("tipo_documento", *r)

tabla("ciudad", ["idCiudad", "nombre"])
for r in [(1, "Bogotá D.C."), (2, "Medellín"), (3, "Cali")]:
    fila("ciudad", *r)

tabla("localidad", ["idLocalidad", "nombre", "idCiudad"])
for r in [(1, "Chapinero", 1), (2, "Suba", 1), (3, "Kennedy", 1),
          (4, "El Poblado", 2), (5, "Comuna 19", 3)]:
    fila("localidad", *r)

tabla("barrio", ["idBarrio", "nombre", "idLocalidad"])
for r in [(1, "Chicó Norte", 1), (2, "Niza", 2), (3, "Castilla", 3),
          (4, "Manila", 4), (5, "San Fernando", 5)]:
    fila("barrio", *r)

tabla("dia_semana", ["idDiaSemana", "diaSemana"])
for i, n in enumerate(["Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"], 1):
    fila("dia_semana", i, n)

tabla("calificacion", ["idCalificacion", "valor", "descripcion"])
for r in [(1, 5, "Excelente"), (2, 4, "Bueno"), (3, 3, "Regular"), (4, 2, "Malo"), (5, 1, "Muy malo")]:
    fila("calificacion", *r)

tabla("especialidad_veterinario", ["idEspecialidadVeterinario", "nombreEspecialidad", "descripcion"])
ESP_GENERAL, ESP_CIRUGIA, ESP_DERMA, ESP_ODONTO, ESP_OFTALMO = 1, 2, 3, 4, 5
for r in [(1, "Medicina General", "Consulta, prevención y medicina interna de pequeños animales"),
          (2, "Cirugía", "Cirugía de tejidos blandos, esterilizaciones y ortopedia básica"),
          (3, "Dermatología", "Enfermedades de piel, oído y alergias"),
          (4, "Odontología", "Profilaxis, extracciones y salud oral"),
          (5, "Oftalmología", "Diagnóstico y tratamiento de enfermedades oculares")]:
    fila("especialidad_veterinario", *r)

tabla("especie", ["idEspecie", "nombreEspecie"])
PERRO, GATO, CONEJO, AVE, HAMSTER = 1, 2, 3, 4, 5
for r in [(1, "Perro"), (2, "Gato"), (3, "Conejo"), (4, "Ave"), (5, "Hámster")]:
    fila("especie", *r)

tabla("raza", ["idRaza", "nombreRaza", "idEspecie", "personalizada"])
RAZAS = [("Criollo", PERRO), ("Labrador Retriever", PERRO), ("Golden Retriever", PERRO),
         ("French Poodle", PERRO), ("Shih Tzu", PERRO), ("Schnauzer Miniatura", PERRO), ("Otra", PERRO),
         ("Criollo", GATO), ("Siamés", GATO), ("Persa", GATO), ("Otra", GATO),
         ("Cabeza de León", CONEJO), ("Otra", CONEJO),
         ("Periquito Australiano", AVE), ("Otra", AVE),
         ("Sirio", HAMSTER), ("Otra", HAMSTER)]
RAZA = {}
for i, (n, e) in enumerate(RAZAS, 1):
    fila("raza", i, n, e, 0)
    RAZA[(n, e)] = i

tabla("fabricante_vacuna", ["idFabricante_Vacuna", "nombre"])
for r in [(1, "Zoetis"), (2, "MSD Salud Animal"), (3, "Boehringer Ingelheim"), (4, "Virbac")]:
    fila("fabricante_vacuna", *r)

tabla("tipo_vacuna", ["idTipoVacuna", "nombreTipo"])
for r in [(1, "Antirrábica"), (2, "Polivalente")]:
    fila("tipo_vacuna", *r)

tabla("vacuna", ["idVacuna", "nombreVacuna", "descripcion", "idEspecie", "idFabricanteVacuna", "idTipoVacuna"])
VAC_RABIA_C, VAC_POLI_C, VAC_TRIPLE_F, VAC_RABIA_F, VAC_RHD = 1, 2, 3, 4, 5
for r in [(1, "Antirrábica Canina", "Virus inactivado. Primera dosis desde las 12 semanas y refuerzo anual. Obligatoria en Colombia.", PERRO, 2, 1),
          (2, "Polivalente Canina DHPPi+L", "Moquillo, hepatitis, parvovirus, parainfluenza y leptospira. Serie de cachorro y refuerzo anual.", PERRO, 1, 2),
          (3, "Triple Felina RCP", "Rinotraqueitis, calicivirus y panleucopenia felina. Refuerzo anual.", GATO, 1, 2),
          (4, "Antirrábica Felina", "Virus inactivado con adyuvante. Refuerzo anual.", GATO, 3, 1),
          (5, "Mixomatosis y Hemorrágica Viral", "Vacuna combinada para conejos desde las 5 semanas. Refuerzo anual.", CONEJO, 4, 2)]:
    fila("vacuna", *r)

tabla("lote_vacuna", ["idLote", "numeroLote", "fechaVencimiento"])
LOTE = {VAC_RABIA_C: 1, VAC_POLI_C: 2, VAC_TRIPLE_F: 3, VAC_RABIA_F: 4}
for r in [(1, "RB2611-04A", dt.date(2027, 8, 31)), (2, "VG5CVL-3318", dt.date(2027, 5, 31)),
          (3, "FC3-24417A", dt.date(2027, 3, 31)), (4, "RABF-7731", dt.date(2027, 6, 30))]:
    fila("lote_vacuna", *r)

tabla("medicamento", ["idMedicamento", "nombreMedicamento", "dosis", "fabricante"])
M_AMOXI, M_MELOX, M_MAROP, M_OMEP, M_TRAM, M_METRO, M_OCLA, M_IVER, M_SAME, M_OTICO = range(1, 11)
for r in [(1, "Amoxicilina + Ácido clavulánico", "250 mg tableta", "Zoetis"),
          (2, "Meloxicam", "1,5 mg/ml suspensión oral", "Boehringer Ingelheim"),
          (3, "Maropitant", "10 mg/ml inyectable", "Zoetis"),
          (4, "Omeprazol", "20 mg cápsula", "Tecnoquímicas"),
          (5, "Tramadol", "50 mg tableta", "Genfar"),
          (6, "Metronidazol", "250 mg tableta", "Genfar"),
          (7, "Oclacitinib", "16 mg tableta", "Zoetis"),
          (8, "Ivermectina", "1 % solución", "Laboratorios Erma"),
          (9, "S-Adenosilmetionina + Silibina", "225 mg tableta", "Nutramax"),
          (10, "Gentamicina + Betametasona + Clotrimazol", "Suspensión ótica 15 ml", "MSD Salud Animal")]:
    fila("medicamento", *r)

tabla("tratamiento", ["idTratamiento", "nombreTratamiento", "descripcion"])
T_DESPAR, T_ANTIBIO, T_FLUIDO, T_DOLOR, T_DERMA, T_DIETA, T_PESO = range(1, 8)
for r in [(1, "Desparasitación", "Control de parásitos internos y externos según especie y peso."),
          (2, "Antibioticoterapia", "Antibiótico sistémico por tiempo definido según el diagnóstico."),
          (3, "Fluidoterapia", "Hidratación intravenosa o subcutánea con cristaloides."),
          (4, "Manejo del dolor", "Analgesia multimodal con AINE y/u opioides."),
          (5, "Tratamiento dermatológico", "Terapia tópica y sistémica de piel y oídos."),
          (6, "Dieta terapéutica", "Plan nutricional para enfermedad digestiva, renal o hepática."),
          (7, "Control de peso", "Plan de alimentación y ejercicio para reducir la condición corporal.")]:
    fila("tratamiento", *r)

tabla("servicio", ["idServicio", "nombreServicio", "descripcion", "tipoRegistro", "esEmergencia"])
S_CONSULTA, S_VACUNA, S_DESPAR, S_URGENCIA, S_ESTERIL, S_LAB, S_ECO, S_ODONTO, S_DERMA = range(1, 10)
for r in [(1, "Consulta General", "Valoración médica completa, diagnóstico y plan de tratamiento.", "CLINICO", 0),
          (2, "Vacunación", "Aplicación de vacunas según el plan de la especie y registro en el carné.", "VACUNACION", 0),
          (3, "Desparasitación", "Control de parásitos internos y externos con revisión de peso.", "CLINICO", 0),
          (4, "Urgencias 24 Horas", "Atención inmediata de pacientes en estado crítico.", "CLINICO", 1),
          (5, "Esterilización", "Ovariohisterectomía u orquiectomía con anestesia inhalada.", "CLINICO", 0),
          (6, "Laboratorio Clínico", "Hemograma, química sanguínea, uroanálisis y coprológico.", "CLINICO", 0),
          (7, "Ecografía", "Ecografía abdominal y gestacional.", "CLINICO", 0),
          (8, "Odontología Veterinaria", "Profilaxis dental, destartraje y extracciones.", "CLINICO", 0),
          (9, "Dermatología", "Consulta especializada de piel, pelaje y oídos.", "CLINICO", 0)]:
    fila("servicio", *r)

# =====================================================================
# 2. Veterinarias (3 en Bogotá, 1 en Medellín)
# =====================================================================
tabla("veterinaria", ["idVeterinaria", "nit", "nombre", "logo", "descripcion", "paginaWeb", "estado",
                      "aprobadaEn", "aprobacionAutomatica"])
tabla("ubicacion_veterinaria", ["idUbicacion_Veterinaria", "calle", "numero", "estrato", "latitud", "longitud",
                                "idVeterinaria", "idBarrio"])
tabla("telefono_veterinaria", ["idTelefonoVeterinaria", "idVeterinaria", "numeroTelefono"])
tabla("correo_veterinaria", ["idCorreo_Veterinaria", "idVeterinaria", "correo"])
CLINICAS = [
    (1, "901547823-4", "Clínica Veterinaria Patitas del Chicó",
     "Medicina preventiva, dermatología y odontología para perros, gatos y aves en el norte de Bogotá.",
     "www.patitasdelchico.com.co", "2024-01-22 10:15:00",
     ("Calle 93", "# 14-20", 6, 4.677021, -74.049113, 1), "6017452318", "contacto@patitasdelchico.com.co"),
    (2, "900873214-1", "Centro Veterinario Niza Suba",
     "Consulta general, cirugía y laboratorio clínico propio. Atención con cita previa.",
     "www.vetnizasuba.com", "2023-09-01 09:00:00",
     ("Avenida Calle 127", "# 70G-35", 4, 4.712053, -74.073218, 2), "6016248975", "citas@vetnizasuba.com"),
    (3, "901236598-7", "Veterinaria Huellitas de Castilla",
     "Atención veterinaria cercana para Kennedy y Soacha, con servicio de urgencias.",
     None, "2022-11-10 14:30:00",
     ("Carrera 78B", "# 8-21", 3, 4.634826, -74.139517, 3), "6014526391", "huellitascastilla@gmail.com"),
    (4, "900945612-3", "Clínica Veterinaria Manila",
     "Clínica de pequeños animales en El Poblado: medicina interna, cirugía, ecografía y laboratorio.",
     "www.vetmanila.com.co", "2024-07-08 11:45:00",
     ("Carrera 43B", "# 10-38", 5, 6.210884, -75.569882, 4), "6043127845", "info@vetmanila.com.co"),
]
for (i, nit, nombre, desc, web, aprob, ub, tel, correo) in CLINICAS:
    fila("veterinaria", i, nit, nombre, "default-logo.png", desc, web, 1, aprob, 0)
    fila("ubicacion_veterinaria", i, ub[0], ub[1], ub[2], ub[3], ub[4], i, ub[5])
    fila("telefono_veterinaria", i, i, tel)
    fila("correo_veterinaria", i, i, correo)

tabla("horario_veterinaria", ["idHorario_Veterinaria", "idDiaSemana", "horaApertura", "horaCierre", "estado",
                              "idVeterinaria"])
HORARIO_CLINICA = {
    1: {d: ("08:00", "19:00") for d in range(1, 6)} | {6: ("09:00", "14:00")},
    2: {d: ("08:00", "18:00") for d in range(1, 7)},
    3: {d: ("07:00", "19:00") for d in range(1, 7)},
    4: {d: ("08:00", "18:00") for d in range(1, 6)},
}
for c, dias in HORARIO_CLINICA.items():
    for d, (a, b) in dias.items():
        fila("horario_veterinaria", len(TABLAS["horario_veterinaria"][1]) + 1, d, a + ":00", b + ":00", 1, c)

tabla("servicio_veterinaria", ["idServicio", "idVeterinaria", "precio", "duracionAprox"])
SERV_CLINICA = {   # (precio COP, duración en minutos)
    1: {S_CONSULTA: (65000, 30), S_VACUNA: (75000, 30), S_DESPAR: (48000, 30), S_URGENCIA: (180000, 60),
        S_ODONTO: (320000, 60), S_DERMA: (95000, 30)},
    2: {S_CONSULTA: (55000, 30), S_VACUNA: (68000, 30), S_DESPAR: (40000, 30), S_ESTERIL: (420000, 90)},
    3: {S_CONSULTA: (45000, 30), S_VACUNA: (60000, 30), S_DESPAR: (35000, 30), S_URGENCIA: (140000, 60)},
    4: {S_CONSULTA: (70000, 30), S_VACUNA: (80000, 30), S_ESTERIL: (480000, 90), S_LAB: (110000, 30),
        S_ECO: (150000, 30)},
}
for c, servs in SERV_CLINICA.items():
    for s, (precio, dur) in servs.items():
        fila("servicio_veterinaria", s, c, float(precio), dur)

# =====================================================================
# 3. Usuarios: 10 dueños, 11 veterinarios, 11 recepcionistas (todos con 123456V)
# =====================================================================
tabla("usuario", ["idUsuario", "numeroDocumento", "idTipoDocumento", "nombre", "apellido", "password", "estado"])
tabla("correo", ["idCorreo", "idUsuario", "correo"])
tabla("telefono", ["idTelefono", "idUsuario", "numeroTelefono"])
tabla("tutor", ["idUsuario", "direccion"])
tabla("veterinario", ["idUsuario", "licencia", "idEspecialidad", "idVeterinaria"])
tabla("recepcionista", ["idUsuario", "fechaContratacion", "turno", "idVeterinaria", "esPrincipal"])

TUTORES = [  # (correo, nombre, apellido, documento, teléfono, dirección)
    ("1@gmail.com", "Laura Camila", "Rodríguez Peña", "1020817436", "3104827715", "Calle 147 # 19-32 Apto 502, Cedritos, Bogotá"),
    ("2@gmail.com", "Andrés Felipe", "Gómez Restrepo", "80176452", "3157720498", "Carrera 70 # 127-45, Niza, Bogotá"),
    ("3@gmail.com", "María José", "Herrera Castaño", "1030654897", "3208841263", "Calle 8 Sur # 78B-20, Castilla, Bogotá"),
    ("4@gmail.com", "Juan David", "Martínez Ospina", "1037598216", "3017456632", "Carrera 43A # 11-55, El Poblado, Medellín"),
    ("5@gmail.com", "Valentina", "Ríos Cárdenas", "1019134587", "3183379021", "Carrera 11 # 93-40 Apto 301, Chicó, Bogotá"),
    ("6@gmail.com", "Santiago", "Morales Quintero", "79856321", "3112290547", "Avenida Pradilla # 5-31 Este, Chía"),
    ("7@gmail.com", "Daniela", "Vargas Londoño", "1152198743", "3046612398", "Calle 11A # 43B-20, Manila, Medellín"),
    ("8@gmail.com", "Carlos Eduardo", "Pineda Rojas", "79412358", "3125548870", "Calle 128B # 58C-12, Suba, Bogotá"),
    ("9@gmail.com", "Natalia Andrea", "Salazar Mejía", "1073245698", "3219087734", "Calle 13 # 7-45, San Mateo, Soacha"),
    ("10@gmail.com", "Jorge Iván", "Castro Bermúdez", "19456782", "3006173345", "Carrera 80 # 6A-14, Castilla, Bogotá"),
]
VETERINARIOS = [  # (correo, nombre, apellido, documento, teléfono, clínica, especialidad, licencia)
    ("100@gmail.com", "Paula Andrea", "Jiménez Ruiz", "1020734581", "3165524107", 1, ESP_GENERAL, "MV-14852"),
    ("101@gmail.com", "Sebastián", "Torres Arango", "1015443920", "3002218764", 1, ESP_DERMA, "MV-17309"),
    ("102@gmail.com", "Catalina", "Mendoza Rueda", "52984317", "3138806652", 1, ESP_ODONTO, "MV-11274"),
    ("103@gmail.com", "Alejandro", "Duque Valencia", "80234915", "3176654281", 2, ESP_CIRUGIA, "MV-09865"),
    ("104@gmail.com", "Diana Marcela", "Forero Silva", "1018456732", "3015589043", 2, ESP_GENERAL, "MV-15521"),
    ("105@gmail.com", "Felipe", "Cárdenas Bonilla", "1032478219", "3209914576", 2, ESP_GENERAL, "MV-19044"),
    ("106@gmail.com", "Mónica", "Patiño Garzón", "52713864", "3114478302", 3, ESP_GENERAL, "MV-10388"),
    ("107@gmail.com", "Julián Andrés", "Rincón Acosta", "1023897145", "3187763019", 3, ESP_GENERAL, "MV-18267"),
    ("108@gmail.com", "Lorena", "Beltrán Moreno", "1012365987", "3052241786", 3, ESP_OFTALMO, "MV-20113"),
    ("109@gmail.com", "Esteban", "Gaviria Muñoz", "1037612458", "3128850934", 4, ESP_GENERAL, "MV-16730"),
    ("110@gmail.com", "Isabel Cristina", "Zuluaga Henao", "43987216", "3014426657", 4, ESP_CIRUGIA, "MV-12459"),
]
RECEPCIONISTAS = [  # (correo, nombre, apellido, tipo doc, documento, teléfono, clínica, turno, contratación, principal)
    ("1000@gmail.com", "Angélica María", "Suárez Pinzón", 1, "1019087423", "3103345128", 1, "MAÑANA", "2024-02-12", 1),
    ("1001@gmail.com", "Camilo", "Ortiz Barrera", 1, "1022945718", "3159902371", 1, "TARDE", "2025-03-03", 0),
    ("1002@gmail.com", "Yuliana", "Castañeda León", 1, "1007854129", "3226618840", 1, "COMPLETO", "2025-08-19", 0),
    ("1003@gmail.com", "Marcela", "Gutiérrez Romero", 1, "52398741", "3117730256", 2, "MAÑANA", "2023-09-04", 1),
    ("1004@gmail.com", "Brayan Stiven", "Cruz Velandia", 1, "1000348912", "3234410987", 2, "TARDE", "2025-01-20", 0),
    ("1005@gmail.com", "Leidy Johana", "Parra Niño", 1, "1031567203", "3196627745", 2, "MAÑANA", "2026-04-06", 0),
    ("1006@gmail.com", "Luz Dary", "Moreno Cifuentes", 1, "39778452", "3143389910", 3, "MAÑANA", "2022-11-14", 1),
    ("1007@gmail.com", "Kevin Alexander", "Rojas Ávila", 1, "1001287634", "3005521469", 3, "TARDE", "2025-06-09", 0),
    ("1008@gmail.com", "Daniela Alejandra", "Pérez Lozano", 3, "1158743", "3248876512", 3, "COMPLETO", "2026-02-02", 0),
    ("1009@gmail.com", "Sara", "Restrepo Vélez", 1, "1036654128", "3016649083", 4, "MAÑANA", "2024-07-15", 1),
    ("1010@gmail.com", "Mateo", "Arango Cadavid", 1, "1152447896", "3127790345", 4, "TARDE", "2025-10-01", 0),
]
uid = 0
TUT, VET, REC = {}, {}, {}           # número de orden (1..n) -> idUsuario
VET_CLINICA, REC_CLINICA, NOMBRE = {}, {}, {}
for n, (correo, nom, ape, doc, tel, dirc) in enumerate(TUTORES, 1):
    uid += 1
    TUT[n] = uid
    NOMBRE[uid] = nom.split()[0]
    fila("usuario", uid, doc, 1, nom, ape, HASH_123456V, 1)
    fila("correo", uid, uid, correo)
    fila("telefono", uid, uid, tel)
    assert len(dirc) <= 45, dirc
    fila("tutor", uid, dirc)
for n, (correo, nom, ape, doc, tel, cli, esp, lic) in enumerate(VETERINARIOS, 1):
    uid += 1
    VET[n] = uid
    VET_CLINICA[uid] = cli
    NOMBRE[uid] = nom.split()[0]
    fila("usuario", uid, doc, 1, nom, ape, HASH_123456V, 1)
    fila("correo", uid, uid, correo)
    fila("telefono", uid, uid, tel)
    fila("veterinario", uid, lic, esp, cli)
PRINCIPAL = {}
for n, (correo, nom, ape, td, doc, tel, cli, turno, desde, princ) in enumerate(RECEPCIONISTAS, 1):
    uid += 1
    REC[n] = uid
    REC_CLINICA[uid] = cli
    NOMBRE[uid] = nom.split()[0]
    if princ:
        PRINCIPAL[cli] = uid
    fila("usuario", uid, doc, td, nom, ape, HASH_123456V, 1)
    fila("correo", uid, uid, correo)
    fila("telefono", uid, uid, tel)
    fila("recepcionista", uid, desde, turno, cli, princ)

V = VET     # alias: V[1] es 100@gmail.com ... V[11] es 110@gmail.com
T = TUT     # T[1] es 1@gmail.com ...
R = REC     # R[1] es 1000@gmail.com ...

# =====================================================================
# 4. Disponibilidad: solicitudes, servicios autorizados, horarios y ausencias
# =====================================================================
tabla("solicitud_disponibilidad", ["idSolicitud", "idVeterinario", "idVeterinaria", "tipo", "estado", "descripcion",
                                   "creadaEn", "revisadaPor", "revisadaEn", "observacionRevision"])
tabla("veterinario_servicio", ["idVeterinarioServicio", "idVeterinario", "idServicio", "estado", "idSolicitud", "creadoEn"])
tabla("horario_veterinario", ["idHorarioVeterinario", "idVeterinario", "diaSemana", "horaInicio", "horaFin", "estado",
                              "idSolicitud", "creadoEn"])
tabla("ausencia_veterinario", ["idAusencia", "idVeterinario", "tipo", "inicio", "fin", "motivo", "estado", "idSolicitud",
                               "creadoEn", "desbloqueadaPor", "desbloqueadaEn"])


def solicitud(vet, tipo, estado, desc, creada, revisor=None, revisada=None, obs=None):
    n = len(TABLAS["solicitud_disponibilidad"][1]) + 1
    fila("solicitud_disponibilidad", n, vet, VET_CLINICA[vet], tipo, estado, desc, creada, revisor, revisada, obs)
    return n


# Horarios aprobados (franjas pequeñas para que el total de cupos quepa en 500 registros).
# (veterinario, día ISO, inicio, fin)
HORARIOS_APROBADOS = [(V[1], 1, "14:00", "15:00"), (V[5], 2, "09:00", "10:00"),
                      (V[8], 3, "08:00", "09:00"), (V[10], 5, "10:00", "11:00")]
NOMBRE_DIA = ["", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"]
SOL_HORARIO = {}
for k, (vet, d, a, b) in enumerate(HORARIOS_APROBADOS):
    creada = momento(dia(-75 + 6 * k), "08:40")
    revisada = creada + dt.timedelta(hours=5, minutes=20)
    SOL_HORARIO[vet] = solicitud(vet, "HORARIO", "APROBADO",
                                 f"Atención por VetLink los {NOMBRE_DIA[d]} de {a} a {b}.",
                                 creada, PRINCIPAL[VET_CLINICA[vet]], revisada, "Aprobado según la agenda de la clínica.")
s_aus_vac = solicitud(V[2], "AUSENCIA", "APROBADO", "Vacaciones de fin de año.",
                      momento(dia(-20), "10:05"), PRINCIPAL[1], momento(dia(-19), "09:12"), "Aprobadas.")
s_serv_pend = solicitud(V[8], "SERVICIOS", "PENDIENTE",
                        "Solicito agregar Desparasitación a los servicios que atiendo.", momento(dia(-2), "16:25"))
s_hor_pend = solicitud(V[10], "HORARIO", "PENDIENTE",
                       "Agregar atención los jueves de 15:00 a 17:00.", momento(dia(-1), "11:10"))
s_aus_rech = solicitud(V[4], "AUSENCIA", "RECHAZADO", "Permiso personal para trámite bancario.",
                       momento(dia(-40), "08:15"), PRINCIPAL[2], momento(dia(-40), "12:30"),
                       "Ese día hay dos esterilizaciones programadas; por favor elige otra fecha.")

SERV_AUTORIZADOS = {V[1]: [S_CONSULTA, S_VACUNA, S_DESPAR], V[2]: [S_DERMA], V[3]: [S_ODONTO],
                    V[4]: [S_ESTERIL], V[5]: [S_CONSULTA, S_VACUNA, S_DESPAR], V[6]: [S_CONSULTA, S_VACUNA],
                    V[7]: [S_CONSULTA, S_URGENCIA], V[8]: [S_CONSULTA, S_VACUNA], V[9]: [S_CONSULTA, S_VACUNA],
                    V[10]: [S_CONSULTA, S_VACUNA, S_LAB], V[11]: [S_ESTERIL, S_ECO]}
for vet, servs in SERV_AUTORIZADOS.items():
    for s in servs:
        assert s in SERV_CLINICA[VET_CLINICA[vet]], (vet, s)
        fila("veterinario_servicio", len(TABLAS["veterinario_servicio"][1]) + 1, vet, s, "APROBADO", None,
             momento(dia(-180), "09:00"))
fila("veterinario_servicio", len(TABLAS["veterinario_servicio"][1]) + 1, V[8], S_DESPAR, "PENDIENTE", s_serv_pend,
     momento(dia(-2), "16:25"))

for vet, d, a, b in HORARIOS_APROBADOS:
    fila("horario_veterinario", len(TABLAS["horario_veterinario"][1]) + 1, vet, d, a + ":00", b + ":00", "APROBADO",
         SOL_HORARIO[vet], momento(dia(-75), "08:40"))
fila("horario_veterinario", len(TABLAS["horario_veterinario"][1]) + 1, V[10], 4, "15:00:00", "17:00:00", "PENDIENTE",
     s_hor_pend, momento(dia(-1), "11:10"))

inicio_vac = momento(proximo(1, dia(18)), "00:00")
fila("ausencia_veterinario", 1, V[2], "VACACIONES", inicio_vac, inicio_vac + dt.timedelta(days=12),
     "Vacaciones de fin de año.", "APROBADO", s_aus_vac, momento(dia(-20), "10:05"), None, None)
fila("ausencia_veterinario", 2, V[4], "PERMISO", momento(dia(-33), "08:00"), momento(dia(-33), "12:00"),
     "Permiso personal para trámite bancario.", "RECHAZADO", s_aus_rech, momento(dia(-40), "08:15"), None, None)
fila("ausencia_veterinario", 3, V[7], "BLOQUEO", momento(dia(-26), "13:00"), momento(dia(-26), "17:00"),
     "Capacitación interna en manejo de urgencias.", "APROBADO", None, momento(dia(-30), "09:30"), None, None)
fila("ausencia_veterinario", 4, V[3], "BLOQUEO", momento(dia(-12), "14:00"), momento(dia(-12), "18:00"),
     "Mantenimiento del equipo de odontología.", "ANULADO", None, momento(dia(-14), "10:00"),
     PRINCIPAL[1], momento(dia(-13), "15:40"))

# =====================================================================
# 5. Mascotas (estado = 1 en todas)
# =====================================================================
tabla("mascota", ["idMascota", "nombre", "idEspecie", "genero", "idRaza", "fechaNacimiento", "estado", "foto", "peso",
                  "Tutor_idUsuario", "situacion"])
MASCOTAS = [  # (nombre, especie, raza, género, nacimiento, peso kg, dueño)
    ("Luna", PERRO, "French Poodle", "H", "2019-03-14", 6.2, 1),
    ("Milo", GATO, "Criollo", "M", "2021-03-02", 4.8, 1),
    ("Max", PERRO, "Labrador Retriever", "M", "2018-05-20", 34.8, 2),
    ("Kira", PERRO, "Criollo", "H", "2022-01-09", 14.6, 2),
    ("Toby", PERRO, "Shih Tzu", "M", "2020-08-11", 7.4, 3),
    ("Nala", GATO, "Siamés", "H", "2023-02-27", 3.6, 3),
    ("Simón", PERRO, "Golden Retriever", "M", "2017-04-03", 34.1, 4),
    ("Mía", GATO, "Persa", "H", "2020-02-16", 4.1, 5),
    ("Coco", AVE, "Periquito Australiano", "M", "2024-05-08", 0.038, 5),
    ("Rocky", PERRO, "Schnauzer Miniatura", "M", "2016-06-25", 8.9, 6),
    ("Lola", PERRO, "Criollo", "H", "2021-07-30", 11.3, 7),
    ("Tom", GATO, "Criollo", "M", "2019-07-12", 4.3, 7),
    ("Bruno", PERRO, "Labrador Retriever", "M", "2022-10-04", 29.8, 8),
    ("Canela", CONEJO, "Cabeza de León", "H", "2024-03-19", 1.7, 9),
    ("Zeus", PERRO, "Criollo", "M", "2020-10-21", 22.5, 9),
    ("Pelusa", HAMSTER, "Sirio", "H", "2025-09-01", 0.135, 10),
]
M = {}
DUENO = {}
for i, (nom, esp, raza, gen, nac, peso, dueno) in enumerate(MASCOTAS, 1):
    fila("mascota", i, nom, esp, gen, RAZA[(raza, esp)], nac, 1, None, peso, T[dueno], "ACTIVA")
    M[nom] = i
    DUENO[i] = T[dueno]

# =====================================================================
# 6. Citas, historia clínica, vacunas y archivos
# =====================================================================
tabla("cita", ["idCita", "fecha", "hora", "estado", "Veterinario_idUsuario", "Servicio_idServicio",
               "Veterinaria_idVeterinaria", "idMascota", "duracionMin", "motivo", "observaciones", "indicacionesPrevias",
               "observacionesAdmin", "creadaEn", "reservadaEn", "expiraEn", "inicioAtencion", "finAtencion",
               "idCitaOrigen", "generadaAuto", "version", "respuestaDuenio", "respuestaDuenioEn"])
tabla("cita_historial_estado", ["idHistorialEstado", "idCita", "estadoAnterior", "estadoNuevo", "idUsuario",
                                "comentario", "fecha"])
tabla("emergencia", ["idEmergencia", "creadaEn", "detalle", "estado", "motivo", "idMascota", "idVeterinaria",
                     "idVeterinario", "idSolicitante", "atendidaEn"])
tabla("historial_clinico", ["numeroHistorial", "fechaConsulta", "motivoConsulta", "diagnostico", "observaciones",
                            "proximaCita", "Historial_Clinicocol", "Veterinario_idUsuario", "idMascota", "fechaRegistro",
                            "idCita", "idVeterinaria", "tipoRegistro", "antecedentes", "temperatura",
                            "frecuenciaCardiaca", "frecuenciaRespiratoria", "pesoKg", "evaluacionClinica", "hallazgos",
                            "tratamiento", "medicamentosIndicados", "recomendaciones", "idEmergencia"])
tabla("historial_medicamento", ["Historial_Clinico_numeroHistorial", "Medicamento_idMedicamento", "cantidad",
                                "instrucciones"])
tabla("historial_tratamiento", ["Tratamiento_idTratamiento", "Historial_Clinico_numeroHistorial",
                                "duracionTratamiento", "intrucciones"])
tabla("carnet_vacuna", ["idCarnet", "fechaAplicacion", "proximaDosis", "dosis", "observaciones", "idLote", "idUsuario",
                        "idVacuna", "idMascota", "idCita", "idVeterinaria"])
tabla("archivo_historial", ["idArchivo", "tipoArchivo", "numeroHistorial", "nombreArchivo", "rutaArchivo",
                            "tamanoBytes", "subidoEn", "idUsuario"])

OCUPADOS_VET, OCUPADOS_MASCOTA = set(), set()


def dia_abierto(clinica, offset, hora="09:00", duracion=30):
    """Retrocede desde HOY+offset hasta un día en que la clínica atiende a esa hora."""
    f = dia(offset)
    while True:
        franja = HORARIO_CLINICA[clinica].get(f.isoweekday())
        if franja and momento(f, franja[0]) <= momento(f, hora)                 and momento(f, hora) + dt.timedelta(minutes=duracion) <= momento(f, franja[1]):
            return f
        f -= dt.timedelta(days=1)


def nueva_cita(fecha, hora, estado, vet, servicio, mascota=None, motivo=None, *, observaciones=None,
               indicaciones=None, obs_admin=None, creada=None, reservada=None, expira=None, inicio_at=None,
               fin_at=None, origen=None, auto=0, version=0):
    clinica = VET_CLINICA[vet]
    dur = SERV_CLINICA[clinica][servicio][1]
    ini = momento(fecha, hora)
    a, b = HORARIO_CLINICA[clinica][fecha.isoweekday()]
    assert momento(fecha, a) <= ini and ini + dt.timedelta(minutes=dur) <= momento(fecha, b), (fecha, hora, vet)
    if estado in ("PENDIENTE", "CONFIRMADA", "EN_ATENCION", "ATENDIDA"):
        assert (vet, fecha, hora) not in OCUPADOS_VET, (vet, fecha, hora)
        OCUPADOS_VET.add((vet, fecha, hora))
        if mascota:
            assert (mascota, fecha, hora) not in OCUPADOS_MASCOTA
            OCUPADOS_MASCOTA.add((mascota, fecha, hora))
    if creada is None:
        creada = min(ini - dt.timedelta(days=10), AHORA - dt.timedelta(hours=3))
    n = len(TABLAS["cita"][1]) + 1
    fila("cita", n, fecha, hora + ":00", estado, vet, servicio, clinica, mascota, dur, motivo, observaciones,
         indicaciones, obs_admin, creada, reservada, expira, inicio_at, fin_at, origen, auto, version, None, None)
    return n


def estado_cita(cita, anterior, nuevo, usuario, comentario, fecha):
    n = len(TABLAS["cita_historial_estado"][1]) + 1
    fila("cita_historial_estado", n, cita, anterior, nuevo, usuario, comentario, fecha)


def cita_atendida(nombre_mascota, vet, servicio, offset, hora, motivo, origen=None):
    clinica = VET_CLINICA[vet]
    dur = SERV_CLINICA[clinica][servicio][1]
    fecha = dia_abierto(clinica, offset, hora, dur)
    ini = momento(fecha, hora)
    reservada = ini - dt.timedelta(days=rnd.randint(2, 6), hours=rnd.randint(0, 8))
    return nueva_cita(fecha, hora, "ATENDIDA", vet, servicio, M[nombre_mascota], motivo,
                      creada=reservada - dt.timedelta(days=3), reservada=reservada,
                      inicio_at=ini + dt.timedelta(minutes=rnd.randint(0, 8)),
                      fin_at=ini + dt.timedelta(minutes=dur - rnd.randint(0, 4)), origen=origen, version=3)


def fin_de(cita):
    return TABLAS["cita"][1][cita - 1][17]


def historial(cita, tipo, mascota, vet, datos, *, emergencia=None, clinica=None, registro=None, proxima=None,
              meds=(), trats=()):
    n = len(TABLAS["historial_clinico"][1]) + 1
    if cita:
        c = TABLAS["cita"][1][cita - 1]
        clinica, registro = c[6], c[17]
    (motivo, antecedentes, temp, fc, fr, peso, evaluacion, hallazgos, diagnostico, tratamiento, medicamentos,
     recomendaciones, observaciones) = datos
    fila("historial_clinico", n, registro.year, motivo, diagnostico, observaciones, proxima, None, vet, mascota,
         registro, cita, clinica, tipo, antecedentes, temp, fc, fr, peso, evaluacion, hallazgos, tratamiento,
         medicamentos, recomendaciones, emergencia)
    for med, cant, instr in meds:
        fila("historial_medicamento", n, med, cant, instr)
    for trat, dur, instr in trats:
        fila("historial_tratamiento", trat, n, dur, instr)
    return n


def carnet(cita, vacuna, dosis, proxima_dias, obs, *, mascota=None, vet=None, clinica=None, fecha=None):
    if cita:
        c = TABLAS["cita"][1][cita - 1]
        fecha, vet, clinica, mascota = c[1], c[4], c[6], c[7]
    n = len(TABLAS["carnet_vacuna"][1]) + 1
    fila("carnet_vacuna", n, fecha, fecha + dt.timedelta(days=proxima_dias), dosis, obs, LOTE[vacuna], vet, vacuna,
         mascota, cita, clinica)


# ---------------------------------------------------------------------
# 6.1 Historia (de ~6 meses atrás a la semana pasada)
# ---------------------------------------------------------------------
c = cita_atendida("Luna", V[1], S_VACUNA, -170, "09:00", "Refuerzo anual de la vacuna antirrábica.")
carnet(c, VAC_RABIA_C, "Refuerzo anual", 365, "Aplicación subcutánea en región interescapular. Sin reacciones tras 20 minutos de observación.")

c = cita_atendida("Milo", V[1], S_CONSULTA, -150, "10:30", "Vomita después de comer desde hace 4 días.")
historial(c, "CONSULTA", M["Milo"], V[1], (
    "Vómitos intermitentes desde hace 4 días, principalmente después de comer, con restos de pelo. Apetito conservado.",
    "Gato de interior, castrado. Vacunas y desparasitación al día. Alimento seco comercial.",
    38.6, 180, 28, 4.8,
    "Alerta e hidratado. Mucosas rosadas, TLLC menor a 2 segundos. Abdomen blando con leve molestia craneal, sin masas.",
    "Pelaje con abundante pelo muerto por muda. Sin signos de obstrucción intestinal.",
    "Gastritis leve asociada a tricobezoares (bolas de pelo).",
    "Antiemético por 3 días, pasta de malta y cepillado diario.",
    "Maropitant 1 mg/kg SC en consulta; luego 2 mg/kg VO cada 24 h por 3 días.",
    "Cepillado diario, alimento para control de bolas de pelo y comida fraccionada en 3 tomas. Volver si el vómito sigue después de 48 h.",
    "La propietaria entiende las indicaciones."),
    meds=[(M_MAROP, "1 frasco de 10 ml", "1 mg/kg SC en consulta y luego 2 mg/kg VO cada 24 h por 3 días.")],
    trats=[(T_DIETA, "10 días", "Alimento con fibra para control de bolas de pelo, en 3 tomas al día.")])

c_max1 = cita_atendida("Max", V[5], S_CONSULTA, -140, "11:00", "Cojea de la pata trasera izquierda, sobre todo en las mañanas.")

c = cita_atendida("Kira", V[4], S_ESTERIL, -125, "08:00", "Esterilización programada.")
historial(c, "PROCEDIMIENTO", M["Kira"], V[4], (
    "Ovariohisterectomía electiva programada.",
    "Paciente sana, vacunada y desparasitada. Ayuno de 10 horas. Hemograma y química prequirúrgicos normales.",
    38.5, 110, 24, 14.3,
    "ASA I. Premedicación con tramadol y acepromacina; anestesia general inhalada con isoflurano.",
    "Útero y ovarios sin alteraciones. Procedimiento sin complicaciones y sangrado mínimo. Sutura intradérmica.",
    "Paciente sana sometida a ovariohisterectomía electiva.",
    "Analgesia y antibiótico profiláctico posoperatorio. Collar isabelino por 10 días.",
    "Tramadol 2 mg/kg VO cada 12 h por 3 días; Meloxicam 0,1 mg/kg VO cada 24 h por 3 días; Amoxicilina-ácido clavulánico 12,5 mg/kg VO cada 12 h por 5 días.",
    "Reposo, no bañar y revisar la herida a diario. Control a los 10 días.",
    "Despertó sin complicaciones; egresó el mismo día a las 4:00 p. m."),
    meds=[(M_TRAM, "6 tabletas", "1 tableta cada 12 horas por 3 días."),
          (M_MELOX, "1 frasco de 10 ml", "0,1 mg/kg cada 24 horas con comida por 3 días."),
          (M_AMOXI, "10 tabletas", "1 tableta cada 12 horas por 5 días.")],
    trats=[(T_DOLOR, "3 días", "Tramadol y meloxicam según fórmula."),
           (T_ANTIBIO, "5 días", "Amoxicilina-ácido clavulánico profiláctico.")])

c = cita_atendida("Toby", V[8], S_CONSULTA, -118, "15:00", "Se rasca mucho y tiene las axilas rojas.")
historial(c, "CONSULTA", M["Toby"], V[8], (
    "Rascado intenso, lamido de patas y enrojecimiento en axilas desde hace 3 semanas.",
    "Episodios similares cada año al inicio de la temporada de lluvias. Control antipulgas irregular.",
    38.7, 120, 26, 7.4,
    "Eritema en axilas, región inguinal y espacios interdigitales, con liquenificación leve. Otitis ceruminosa bilateral leve.",
    "Citología de piel con levaduras escasas (Malassezia). Raspado negativo para ácaros.",
    "Dermatitis atópica canina con sobrecrecimiento de Malassezia.",
    "Control del prurito y baños medicados dos veces por semana durante 4 semanas.",
    "Oclacitinib 0,4-0,6 mg/kg VO cada 12 h por 14 días y luego cada 24 h; champú de clorhexidina 2 % y miconazol 2 %.",
    "Dieta hipoalergénica, antipulgas mensual y evitar el pasto húmedo. Control en 4 semanas.",
    None),
    proxima=dia_abierto(3, -90),
    meds=[(M_OCLA, "20 tabletas", "Media tableta cada 12 horas por 14 días y luego cada 24 horas.")],
    trats=[(T_DERMA, "4 semanas", "Baños con champú de clorhexidina y miconazol dos veces por semana, dejando actuar 10 minutos.")])

c = cita_atendida("Nala", V[9], S_VACUNA, -110, "09:30", "Refuerzo anual de vacunas.")
carnet(c, VAC_TRIPLE_F, "Refuerzo anual", 365, "Aplicación subcutánea en miembro posterior derecho. Paciente tranquila.")

c_simon_lab = cita_atendida("Simón", V[10], S_LAB, -95, "08:30", "Chequeo geriátrico con exámenes de sangre.")

c = cita_atendida("Mía", V[3], S_ODONTO, -88, "10:00", "Mal aliento y le cuesta comer el concentrado.")
historial(c, "PROCEDIMIENTO", M["Mía"], V[3], (
    "Mal aliento y dificultad para masticar alimento seco.",
    "Sin enfermedades previas. Nunca se le había hecho limpieza dental.",
    38.4, 170, 26, 4.1,
    "Gingivitis marcada en premolares y molares superiores, con cálculo dental moderado.",
    "Profilaxis bajo anestesia general: destartraje ultrasónico y pulido. Extracción del diente 207 por movilidad grado III.",
    "Enfermedad periodontal grado II-III.",
    "Profilaxis dental con extracción, analgesia y antibiótico.",
    "Amoxicilina-ácido clavulánico 12,5 mg/kg VO cada 12 h por 7 días; Meloxicam 0,05 mg/kg VO cada 24 h por 3 días.",
    "Alimento húmedo por 5 días y luego dieta dental. Cepillado con pasta enzimática 3 veces por semana.",
    "Recuperación anestésica sin novedad."),
    meds=[(M_AMOXI, "14 tabletas", "1/4 de tableta cada 12 horas por 7 días."),
          (M_MELOX, "1 frasco de 10 ml", "0,05 mg/kg cada 24 horas por 3 días.")],
    trats=[(T_ANTIBIO, "7 días", "Amoxicilina-ácido clavulánico cada 12 horas.")])

c = cita_atendida("Rocky", V[6], S_CONSULTA, -80, "16:00", "Vomita, está decaído y le duele la barriga.")
historial(c, "CONSULTA", M["Rocky"], V[6], (
    "Vómito, decaimiento y dolor abdominal después de comer restos de lechona el fin de semana.",
    "Schnauzer de 10 años con hipertrigliceridemia conocida. Sin cirugías previas.",
    39.4, 132, 30, 8.9,
    "Deshidratación del 6 %, posición antiálgica (de rezo) y dolor a la palpación abdominal craneal.",
    "SNAP cPL positivo. Ecografía: páncreas hipoecoico con mesenterio hiperecoico.",
    "Pancreatitis aguda leve a moderada.",
    "Fluidoterapia intravenosa, antiemético, protector gástrico y analgesia; control a las 48 horas.",
    "Lactato de Ringer 60 ml/kg/día IV; Maropitant 1 mg/kg SC cada 24 h por 3 días; Omeprazol 1 mg/kg VO cada 24 h por 7 días; Tramadol 2 mg/kg VO cada 8 h por 4 días.",
    "Dieta baja en grasa de por vida y nada de comida de la mesa. Volver de inmediato si el vómito persiste.",
    "Permaneció 8 horas en observación con líquidos intravenosos."),
    meds=[(M_MAROP, "1 frasco de 10 ml", "1 mg/kg SC cada 24 horas por 3 días."),
          (M_OMEP, "7 cápsulas", "Media cápsula en ayunas cada 24 horas por 7 días."),
          (M_TRAM, "6 tabletas", "1/4 de tableta cada 8 horas por 4 días.")],
    trats=[(T_FLUIDO, "8 horas", "Lactato de Ringer IV en observación."),
           (T_DIETA, "Permanente", "Alimento bajo en grasa (menos de 10 % en materia seca).")])

c = cita_atendida("Lola", V[10], S_VACUNA, -74, "14:00", "Refuerzo anual de la vacuna polivalente.")
carnet(c, VAC_POLI_C, "Refuerzo anual", 365, "Sin reacciones adversas. Próxima dosis en un año.")

f_bruno = dia_abierto(2, -66)
c = nueva_cita(f_bruno, "09:00", "CANCELADA", V[6], S_CONSULTA, M["Bruno"], "Revisión general y plan de vacunas.",
               creada=momento(f_bruno, "00:15") - dt.timedelta(days=12), reservada=momento(f_bruno, "08:00") - dt.timedelta(days=4),
               obs_admin="Cancelada por el dueño: viaje de trabajo.", version=2)
estado_cita(c, "PENDIENTE", "CANCELADA", T[8], "Cancelada por el dueño", momento(f_bruno, "07:10") - dt.timedelta(days=1))

c = cita_atendida("Tom", V[11], S_ECO, -60, "11:00", "Toma mucha agua y está bajando de peso.")
h_tom = historial(c, "LABORATORIO", M["Tom"], V[11], (
    "Aumento del consumo de agua y de la micción, con pérdida de peso progresiva en los últimos 2 meses.",
    "Gato de interior de 7 años. Alimento seco comercial. Sin enfermedades previas.",
    38.2, 190, 30, 4.3,
    "Condición corporal 4/9, mucosas levemente pálidas y riñones pequeños e irregulares a la palpación.",
    "Ecografía abdominal: riñones de 3,1 cm con pérdida de la diferenciación corticomedular. Creatinina 2,4 mg/dL, SDMA 22 µg/dL, densidad urinaria 1.018.",
    "Enfermedad renal crónica estadio IRIS 2.",
    "Dieta renal exclusiva, control de hidratación y seguimiento de presión arterial y proteinuria.",
    "Sin medicación por ahora.",
    "Varias fuentes de agua en la casa y alimento húmedo renal. Control de creatinina y SDMA en 3 meses.",
    "Se entrega el informe ecográfico a la propietaria."),
    proxima=dia_abierto(4, 30),
    trats=[(T_DIETA, "Permanente", "Alimento renal comercial seco y húmedo, de forma exclusiva.")])

f_canela = dia_abierto(3, -52)
c = nueva_cita(f_canela, "10:00", "NO_ASISTIO", V[8], S_CONSULTA, M["Canela"], "Revisión general y corte de uñas.",
               creada=momento(f_canela, "00:15") - dt.timedelta(days=10), reservada=momento(f_canela, "09:00") - dt.timedelta(days=3),
               obs_admin="No se presentó y no respondió las llamadas.", version=3)

# Urgencia de Zeus (ingestión de chocolate) y su control dos días después.
f_urg = dia(-47)
fila("emergencia", 1, momento(f_urg, "19:40"),
     "Comió una barra de chocolate amargo de 150 g hace 2 horas. Está muy inquieto, tiembla y jadea.",
     "ATENDIDA", "Intoxicación por chocolate", M["Zeus"], 3, V[7], T[9], momento(f_urg, "21:25"))
historial(None, "URGENCIA", M["Zeus"], V[7], (
    "Ingestión de aproximadamente 150 g de chocolate amargo 2 horas antes de la consulta.",
    "Sin enfermedades previas. Vacunas al día.",
    39.1, 168, 36, 22.6,
    "Paciente hiperexcitable, con temblores musculares leves, taquicardia y jadeo.",
    "Dosis estimada de teobromina cercana a 70 mg/kg (rango tóxico moderado). ECG con taquicardia sinusal.",
    "Intoxicación moderada por chocolate (metilxantinas).",
    "Inducción de emesis, carbón activado, fluidoterapia intravenosa y monitoreo cardíaco por 6 horas.",
    "Apomorfina 0,03 mg/kg IV; carbón activado 1 g/kg VO; Lactato de Ringer 5 ml/kg/h IV por 6 horas.",
    "Control en 48 horas. Volver si presenta convulsiones, vómito persistente o arritmias.",
    "Se recuperó gran parte del chocolate con la emesis."),
    emergencia=1, clinica=3, registro=momento(f_urg, "21:20"), proxima=dia_abierto(3, -45),
    trats=[(T_FLUIDO, "6 horas", "Lactato de Ringer 5 ml/kg/h IV con monitoreo cardíaco.")])
f_emerg2 = dia_abierto(1, -70)
fila("emergencia", 2, momento(f_emerg2, "07:20"),
     "Respira con el pico abierto y mueve la cola al respirar desde esta madrugada.",
     "CANCELADA", "Dificultad respiratoria", M["Coco"], 1, None, T[5], None)

c = cita_atendida("Zeus", V[7], S_CONSULTA, -45, "17:00", "Control después de la urgencia por chocolate.")
historial(c, "CONTROL", M["Zeus"], V[7], (
    "Control 48 horas después de la atención de urgencia por intoxicación con chocolate.",
    "Urgencia por ingestión de chocolate atendida hace 2 días.",
    38.5, 100, 22, 22.5,
    "Alerta, sin temblores ni taquicardia. Hidratación normal.",
    "Ritmo cardíaco regular. Sin vómito ni diarrea en las últimas 24 horas.",
    "Recuperación completa de la intoxicación por metilxantinas (teobromina).",
    "No requiere tratamiento adicional.",
    "Ninguno.",
    "Mantener chocolates y productos con cacao fuera de su alcance. Alta médica.",
    None))

c = cita_atendida("Coco", V[1], S_CONSULTA, -38, "11:30", "Tiene costras blancas en el pico y las patas.")
historial(c, "CONSULTA", M["Coco"], V[1], (
    "Costras blanquecinas en el pico y las patas; se rasca con frecuencia.",
    "Vive en jaula con otro periquito. Dieta de semillas y frutas.",
    41.0, None, None, 0.038,
    "Lesiones costrosas porosas en la cera, las comisuras del pico y los tarsos. Plumaje en buen estado.",
    "Raspado de la lesión con ácaros Knemidocoptes pilae.",
    "Sarna knemidocóptica (pico escamoso).",
    "Ivermectina tópica en dos aplicaciones; limpieza y desinfección de la jaula y las perchas.",
    "Ivermectina 0,2 mg/kg tópica en la piel del cuello; repetir a los 14 días.",
    "Separar del otro periquito hasta terminar el tratamiento y cambiar las perchas de madera.",
    None),
    meds=[(M_IVER, "1 frasco gotero de 10 ml", "1 gota diluida en la piel del cuello; repetir a los 14 días.")],
    trats=[(T_DESPAR, "14 días", "Dos aplicaciones de ivermectina tópica.")])

# Max: la cita de control se reprogramó (REPROGRAMADA -> nueva cita atendida).
f_rep = dia_abierto(2, -30)
c_rep = nueva_cita(f_rep, "09:00", "REPROGRAMADA", V[5], S_CONSULTA, M["Max"], "Control de la cojera.",
                   creada=momento(f_rep, "00:15") - dt.timedelta(days=14), reservada=momento(f_rep, "10:00") - dt.timedelta(days=20),
                   obs_admin="La veterinaria tuvo una cirugía de urgencia; se movió dos días después con aceptación del dueño.",
                   version=3)
c_max2 = cita_atendida("Max", V[5], S_CONSULTA, -28, "10:00", "Control de la cojera.", origen=c_rep)
estado_cita(c_rep, "CONFIRMADA", "REPROGRAMADA", R[4], "Reprogramada por recepción: cirugía de urgencia", momento(f_rep, "07:30") - dt.timedelta(days=1))
historial(c_max1, "CONSULTA", M["Max"], V[5], (
    "Cojera del miembro posterior izquierdo que empeora en las mañanas y después de paseos largos.",
    "Labrador de 8 años con sobrepeso. Sin cirugías previas.",
    38.4, 96, 22, 36.2,
    "Dolor a la extensión de la cadera izquierda con crepitación leve. Condición corporal 4/5 (sobrepeso).",
    "Radiografía traída por el propietario compatible con displasia de cadera bilateral con cambios degenerativos.",
    "Osteoartritis coxofemoral secundaria a displasia de cadera.",
    "AINE por 14 días, control de peso y ejercicio de bajo impacto.",
    "Meloxicam 0,1 mg/kg VO cada 24 h con comida por 14 días.",
    "Reducir 10 % la ración diaria, paseos cortos en superficie blanda y evitar escaleras. Control en 4 semanas.",
    None),
    proxima=f_rep,
    meds=[(M_MELOX, "1 frasco de 32 ml", "0,1 mg/kg cada 24 horas con comida por 14 días.")],
    trats=[(T_DOLOR, "14 días", "Meloxicam según fórmula; el plan de peso queda en recomendaciones.")])

c = cita_atendida("Pelusa", V[8], S_CONSULTA, -21, "15:30", "Babea y no come semillas duras.")
historial(c, "PROCEDIMIENTO", M["Pelusa"], V[8], (
    "Dejó de comer semillas duras y babea; los dientes delanteros se ven largos.",
    "Hámster de 1 año, dieta de mezcla de semillas.",
    None, None, None, 0.135,
    "Incisivos inferiores sobrecrecidos con desviación lateral y lesión leve en la encía superior.",
    "Maloclusión de incisivos sin compromiso de molares.",
    "Maloclusión y sobrecrecimiento de incisivos.",
    "Recorte y limado de incisivos con fresa de baja velocidad.",
    "Meloxicam 0,2 mg/kg VO cada 24 h por 3 días.",
    "Ofrecer bloques de madera para roer y heno. Revisión dental cada 6 a 8 semanas.",
    None),
    meds=[(M_MELOX, "1 frasco de 10 ml", "Una gota cada 24 horas por 3 días.")])

c = cita_atendida("Bruno", V[5], S_CONSULTA, -16, "14:00", "Diarrea y vómito desde ayer.")
historial(c, "CONSULTA", M["Bruno"], V[5], (
    "Diarrea líquida y dos episodios de vómito desde ayer; comió basura en el parque.",
    "Vacunas al día. Desparasitado hace 2 meses.",
    39.0, 110, 24, 29.8,
    "Deshidratación leve (5 %), borborigmos aumentados y dolor abdominal leve.",
    "Coprológico sin parásitos. Sin cuerpos extraños palpables.",
    "Gastroenteritis aguda por indiscreción alimentaria.",
    "Antiemético, antibiótico de espectro anaerobio, probiótico y dieta blanda por 5 días.",
    "Maropitant 1 mg/kg SC dosis única; Metronidazol 15 mg/kg VO cada 12 h por 5 días; probiótico cada 24 h por 7 días.",
    "Pollo cocido con arroz en porciones pequeñas y agua fresca. Volver si hay sangre en las heces.",
    None),
    meds=[(M_MAROP, "1 aplicación", "1 mg/kg SC en consulta."),
          (M_METRO, "10 tabletas", "1 tableta cada 12 horas por 5 días.")],
    trats=[(T_DIETA, "5 días", "Dieta blanda de pollo y arroz.")])

c = cita_atendida("Kira", V[6], S_VACUNA, -12, "10:30", "Refuerzo anual de la vacuna polivalente.")
carnet(c, VAC_POLI_C, "Refuerzo anual", 365, "Sin reacciones adversas.")

c_luna_derma = cita_atendida("Luna", V[2], S_DERMA, -9, "16:00", "Sacude la cabeza y le huele mal el oído.")

f_rech = dia_abierto(2, -6)
c = nueva_cita(f_rech, "08:30", "RECHAZADA", V[5], S_DESPAR, M["Rocky"], "Desparasitación trimestral.",
               creada=momento(f_rech, "00:15") - dt.timedelta(days=14), reservada=momento(f_rech, "08:30") - dt.timedelta(days=1, hours=16),
               version=2)
estado_cita(c, "DISPONIBLE", "PENDIENTE", T[6], "Reserva solicitada por el dueño", momento(f_rech, "16:30") - dt.timedelta(days=2))
estado_cita(c, "PENDIENTE", "RECHAZADA", None, "La reserva expiró sin confirmación", momento(f_rech, "08:00"))

c = cita_atendida("Simón", V[10], S_VACUNA, -4, "09:00", "Refuerzo anual de la antirrábica.")
carnet(c, VAC_RABIA_C, "Refuerzo anual", 365, "Aplicación subcutánea. Se recuerda control de laboratorio pendiente.")
c_simon_vac = c

# Registros que dependen de citas creadas más arriba.
h_simon = historial(c_simon_lab, "LABORATORIO", M["Simón"], V[10], (
    "Chequeo geriátrico anual con perfil de laboratorio.",
    "Golden de 9 años, vacunas al día. Sin enfermedades previas.",
    38.3, 88, 20, 33.5,
    "Buena condición general. Tártaro dental leve. Sin otros hallazgos al examen físico.",
    "Hemograma normal. Química: ALT 148 U/L (ref. 10-100), fosfatasa alcalina 210 U/L (ref. 20-150), creatinina 1,1 mg/dL, glucosa 92 mg/dL. Uroanálisis normal.",
    "Elevación leve de enzimas hepáticas, probable hepatopatía reactiva.",
    "Hepatoprotector por 30 días y control de laboratorio.",
    "S-adenosilmetionina + silibina 20 mg/kg VO cada 24 h en ayunas por 30 días.",
    "Repetir perfil hepático con 12 horas de ayuno. Evitar premios grasos.",
    "Resultados adjuntos en PDF."),
    meds=[(M_SAME, "30 tabletas", "1 tableta en ayunas cada 24 horas por 30 días.")])
historial(c_max2, "CONTROL", M["Max"], V[5], (
    "Control de osteoartritis después de 4 semanas de antiinflamatorio y plan de peso.",
    "Osteoartritis coxofemoral diagnosticada hace 4 meses.",
    38.3, 92, 20, 34.8,
    "Mejoría de la cojera (grado 1/5). Bajó 1,4 kg.",
    "Leve dolor residual a la extensión de la cadera izquierda.",
    "Osteoartritis coxofemoral en mejoría.",
    "Suspender meloxicam e iniciar condroprotector a largo plazo.",
    "Glucosamina + condroitina 1 tableta cada 24 h por 60 días.",
    "Continuar el plan de peso hasta 32 kg. Control en 2 meses.",
    None),
    trats=[(T_PESO, "8 semanas", "Mantener la ración reducida y los paseos de bajo impacto.")])
carnet(None, VAC_POLI_C, "Refuerzo anual", 365, "Registrada por la clínica antes de usar la agenda de VetLink.",
       mascota=M["Toby"], vet=V[8], clinica=3, fecha=dia(-352))

# ---------------------------------------------------------------------
# 6.2 Agenda vigente: cupos que cubren las franjas aprobadas en todo el horizonte
# ---------------------------------------------------------------------
LIMITE_RESERVA = AHORA + dt.timedelta(hours=3)
RESERVAS = {   # (veterinario, ocurrencia, hora) -> datos de la reserva
}
ocurrencias = {}
for vet, d, a, b in HORARIOS_APROBADOS:
    f = proximo(d, HOY)
    lista = []
    while f <= dia(HORIZONTE_DIAS):
        lista.append(f)
        f += dt.timedelta(days=7)
    ocurrencias[vet] = lista


def primera_futura(vet, hora, minimo_dias=0):
    for f in ocurrencias[vet]:
        if momento(f, hora) >= LIMITE_RESERVA and f >= dia(minimo_dias):
            return f
    raise AssertionError("sin ocurrencia futura")


f_v1 = primera_futura(V[1], "14:00")
f_v5 = primera_futura(V[5], "09:00")
f_v8 = primera_futura(V[8], "08:00")
f_v10 = primera_futura(V[10], "10:00")
f_v1p = primera_futura(V[1], "14:30", 2)
f_v8p = primera_futura(V[8], "08:30", 2)
f_v10p = primera_futura(V[10], "10:30", 2)
RESERVAS = {
    (V[1], f_v1, "14:00"): ("CONFIRMADA", "Luna", S_CONSULTA, "Control de la otitis.", None),
    (V[1], f_v1p, "14:30"): ("PENDIENTE", "Mía", S_DESPAR, "Desparasitación trimestral.", None),
    (V[5], f_v5, "09:00"): ("CONFIRMADA", "Max", S_CONSULTA, "Control de osteoartritis y peso.", None),
    (V[8], f_v8, "08:00"): ("CONFIRMADA", "Toby", S_VACUNA, "Refuerzo anual de la vacuna polivalente.", None),
    (V[8], f_v8p, "08:30"): ("PENDIENTE", "Canela", S_CONSULTA, "Revisión general y corte de uñas.", None),
    (V[10], f_v10, "10:00"): ("CONFIRMADA", "Simón", S_LAB, "Control del perfil hepático.",
                              "Ayuno de 12 horas. Traer muestra de orina de la mañana en frasco estéril."),
    (V[10], f_v10p, "10:30"): ("PENDIENTE", "Lola", S_CONSULTA, "Revisión de una masa pequeña en el costado.", None),
}
CANCELADA_FUTURA = (V[5], f_v5, "09:30", "Bruno")
CONFIRMADAS, PENDIENTES = [], []
for vet, d, a, b in HORARIOS_APROBADOS:
    clinica = VET_CLINICA[vet]
    servs = SERV_AUTORIZADOS[vet]
    for f in ocurrencias[vet]:
        t = momento(f, a)
        k = f.timetuple().tm_yday
        while t < momento(f, b):
            hora = t.strftime("%H:%M")
            creada = max(momento(f - dt.timedelta(days=HORIZONTE_DIAS), "00:15"), momento(dia(-HORIZONTE_DIAS), "00:15"))
            creada = min(creada, AHORA - dt.timedelta(hours=1))
            reserva = RESERVAS.get((vet, f, hora))
            if (vet, f, hora) == CANCELADA_FUTURA[:3]:
                # Reserva cancelada por el dueño: queda la cita CANCELADA y el cupo se libera (fila nueva).
                reservada = AHORA - dt.timedelta(days=2, hours=3)
                cx = nueva_cita(f, hora, "CANCELADA", vet, S_CONSULTA, M["Bruno"], "Control de la gastroenteritis.",
                                creada=creada, reservada=reservada, obs_admin="Cancelada por el dueño: Bruno ya está bien.",
                                auto=1, version=3)
                estado_cita(cx, "DISPONIBLE", "PENDIENTE", T[8], "Reserva solicitada por el dueño", reservada)
                estado_cita(cx, "PENDIENTE", "CANCELADA", T[8], "Cancelada por el dueño", AHORA - dt.timedelta(hours=20))
                CANCELADA_ID = cx
                creada = AHORA - dt.timedelta(hours=20)
            if reserva:
                est, masc, serv, motivo, indic = reserva
                if est == "CONFIRMADA":
                    reservada = AHORA - dt.timedelta(days=3, hours=rnd.randint(1, 6))
                    cid = nueva_cita(f, hora, est, vet, serv, M[masc], motivo, indicaciones=indic, creada=creada,
                                     reservada=reservada, auto=1, version=2)
                    estado_cita(cid, "DISPONIBLE", "PENDIENTE", DUENO[M[masc]], "Reserva solicitada por el dueño", reservada)
                    estado_cita(cid, "PENDIENTE", "CONFIRMADA", PRINCIPAL[clinica], "Confirmada por recepción",
                                reservada + dt.timedelta(hours=2, minutes=15))
                    CONFIRMADAS.append(cid)
                else:
                    reservada = AHORA - dt.timedelta(minutes=40 + 15 * len(PENDIENTES))
                    ini = momento(f, hora)
                    expira = min(reservada + dt.timedelta(hours=24), ini - dt.timedelta(minutes=30))
                    assert expira > AHORA + dt.timedelta(hours=20), "una reserva pendiente vencería muy pronto"
                    cid = nueva_cita(f, hora, est, vet, serv, M[masc], motivo, creada=creada, reservada=reservada,
                                     expira=expira, auto=1, version=1)
                    estado_cita(cid, "DISPONIBLE", "PENDIENTE", DUENO[M[masc]], "Reserva solicitada por el dueño", reservada)
                    PENDIENTES.append(cid)
            else:
                serv = servs[k % len(servs)]
                k += 1
                nueva_cita(f, hora, "DISPONIBLE", vet, serv, creada=creada, auto=1)
            t += dt.timedelta(minutes=30)

# Historial de Luna (dermatología): la próxima cita es el control ya confirmado.
historial(c_luna_derma, "CONSULTA", M["Luna"], V[2], (
    "Sacude la cabeza, se rasca las orejas y tiene mal olor en el oído derecho.",
    "Poodle de 7 años. Episodio similar hace 2 años.",
    38.8, 118, 24, 6.2,
    "Conducto auditivo derecho eritematoso con exudado marrón; el izquierdo con eritema leve.",
    "Citología ótica con cocos abundantes y levaduras moderadas. Membrana timpánica íntegra.",
    "Otitis externa bilateral bacteriana y por Malassezia.",
    "Limpieza ótica en consulta y tratamiento tópico por 10 días.",
    "Gentamicina + betametasona + clotrimazol ótico, 4 gotas en cada oído cada 12 h por 10 días; limpiador ótico cada 48 h.",
    "No mojar las orejas durante el tratamiento. Control al terminar.",
    None),
    proxima=f_v1,
    meds=[(M_OTICO, "1 frasco de 15 ml", "4 gotas en cada oído cada 12 horas por 10 días.")],
    trats=[(T_DERMA, "10 días", "Limpieza ótica cada 48 horas y gotas óticas.")])

# Archivos PDF adjuntos (se escriben en el volumen de uploads con --adjuntos).
def pdf_simple(titulo, lineas):
    texto = ["BT /F1 15 Tf 50 790 Td (" + titulo + ") Tj ET"]
    y = 760
    for ln in lineas:
        texto.append(f"BT /F1 10 Tf 50 {y} Td ({ln}) Tj ET")
        y -= 16
    contenido = "\n".join(texto).encode("latin-1")
    objs = [b"<< /Type /Catalog /Pages 2 0 R >>",
            b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
            b"<< /Length " + str(len(contenido)).encode() + b" >>\nstream\n" + contenido + b"\nendstream",
            b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"]
    out = bytearray(b"%PDF-1.4\n")
    pos = []
    for i, o in enumerate(objs, 1):
        pos.append(len(out))
        out += f"{i} 0 obj\n".encode() + o + b"\nendobj\n"
    xref = len(out)
    out += f"xref\n0 {len(objs) + 1}\n0000000000 65535 f \n".encode()
    for p in pos:
        out += f"{p:010d} 00000 n \n".encode()
    out += f"trailer\n<< /Size {len(objs) + 1} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n".encode()
    return bytes(out)


ADJUNTOS = [
    (h_simon, "Resultados_laboratorio_Simon.pdf", c_simon_lab, V[10], "Laboratorio Clinico - Clinica Veterinaria Manila", [
        "Paciente: Simon - Canino, Golden Retriever, macho, 9 anios", "Propietario: Juan David Martinez Ospina",
        "Hemograma: dentro de rangos de referencia.", "ALT: 148 U/L (10 - 100)   ALTO",
        "Fosfatasa alcalina: 210 U/L (20 - 150)   ALTO", "Creatinina: 1,1 mg/dL (0,5 - 1,5)",
        "Glucosa: 92 mg/dL (70 - 120)", "Uroanalisis: sin alteraciones.", "Bacteriologa: Laura Sierra - Reg. 1123"]),
    (h_tom, "Informe_ecografia_Tom.pdf", None, V[11], "Informe ecografico abdominal - Clinica Veterinaria Manila", [
        "Paciente: Tom - Felino, criollo, macho, 7 anios", "Propietaria: Daniela Vargas Londono",
        "Rinion izquierdo: 3,1 cm, contorno irregular, perdida de diferenciacion corticomedular.",
        "Rinion derecho: 3,2 cm, hallazgos similares.", "Vejiga: paredes normales, contenido anecoico.",
        "Higado y bazo: sin alteraciones.", "Conclusion: cambios compatibles con nefropatia cronica.",
        "M.V. Isabel Cristina Zuluaga Henao - MV-12459"]),
]
ARCHIVOS_BYTES = {}
for i, (hist, nombre, cita_ref, vet, titulo, lineas) in enumerate(ADJUNTOS, 1):
    datos = pdf_simple(titulo, lineas)
    ruta = f"historial/{hist}/{uuid.uuid5(uuid.NAMESPACE_URL, 'vetlink-semilla-500/' + nombre)}.pdf"
    ARCHIVOS_BYTES[ruta] = datos
    registro = TABLAS["historial_clinico"][1][hist - 1][9]
    fila("archivo_historial", i, "application/pdf", hist, nombre, ruta, len(datos),
         registro - dt.timedelta(minutes=5), vet)

# =====================================================================
# 7. Comunicación y actividad: notificaciones, chat, reseñas, pasaportes, reportes
# =====================================================================
tabla("notificacion", ["idNotificacion", "idUsuario", "tipo", "titulo", "mensaje", "enlace", "claveUnica", "leida",
                       "creadaEn"])


def notificacion(usuario, tipo, titulo, mensaje, enlace, clave, leida, creada):
    n = len(TABLAS["notificacion"][1]) + 1
    fila("notificacion", n, usuario, tipo, titulo, mensaje, enlace, clave, leida, creada)


MESES = ["", "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre",
         "noviembre", "diciembre"]


def describir(cid):
    c = TABLAS["cita"][1][cid - 1]
    f, h = c[1], c[2]
    return f"{f.day} de {MESES[f.month]} a las {h[:5]}"


CLINICA_NOMBRE = {c[0]: c[2] for c in CLINICAS}
MASCOTA_NOMBRE = {i: m[0] for i, m in enumerate(MASCOTAS, 1)}
for cid in CONFIRMADAS:
    c = TABLAS["cita"][1][cid - 1]
    notificacion(DUENO[c[7]], "CITA_CONFIRMADA", "Cita confirmada",
                 f"La clínica confirmó la cita de {MASCOTA_NOMBRE[c[7]]} el {describir(cid)}.",
                 f"/citas/{cid}", f"cita-{cid}-confirmada", 1, c[14] + dt.timedelta(hours=2, minutes=15))
    ini = momento(c[1], c[2][:5])
    if AHORA <= ini <= AHORA + dt.timedelta(hours=24):
        # La tarea de recordatorios la crearía ahora: se deja creada (misma claveUnica) para no duplicarla.
        notificacion(DUENO[c[7]], "CITA_RECORDATORIO", "Recordatorio de cita",
                     f"{MASCOTA_NOMBRE[c[7]]} tiene cita el {describir(cid)} en {CLINICA_NOMBRE[c[6]]}.",
                     f"/citas/{cid}", f"cita-{cid}-recordatorio", 0, AHORA - dt.timedelta(minutes=10))
for cid in PENDIENTES:
    c = TABLAS["cita"][1][cid - 1]
    notificacion(DUENO[c[7]], "CITA_RESERVADA", "Reserva enviada",
                 f"Tu solicitud para {MASCOTA_NOMBRE[c[7]]} el {describir(cid)} está pendiente de confirmación por la clínica.",
                 f"/citas/{cid}", f"cita-{cid}-reservada", 0, c[14])
notificacion(PRINCIPAL[2], "CITA_CANCELADA", "Cita cancelada por el dueño",
             f"Carlos canceló la cita de Bruno del {describir(CANCELADA_ID)}. El cupo quedó disponible.",
             f"/agenda/{CANCELADA_ID}", f"cita-{CANCELADA_ID}-cancelada", 1, AHORA - dt.timedelta(hours=20))
notificacion(T[9], "EMERGENCIA_ATENDIDA", "Emergencia atendida",
             "La Dra. Mónica Patiño atendió la emergencia de Zeus en Veterinaria Huellitas de Castilla.",
             "/emergencias/1", "emergencia-1-atendida", 1, momento(f_urg, "21:25"))
notificacion(V[1], "DISPONIBILIDAD_APROBADA", "Solicitud aprobada", "Horario laboral: tu solicitud fue aprobada.",
             "/disponibilidad", f"solicitud-{SOL_HORARIO[V[1]]}-resuelta", 1, momento(dia(-75), "14:00"))

tabla("conversacion", ["idConversacion", "creadaEn", "usuarioA", "usuarioB", "ultimoMensajeEn", "idVeterinaria"])
tabla("mensaje_chat", ["idMensaje", "contenido", "enviadoEn", "idConversacion", "idRemitente", "leidoEn"])
CHATS = [
    (T[1], PRINCIPAL[1], 1, [
        (T[1], "Buenas tardes. Las gotas de Luna se acaban mañana, ¿igual debo llevarla al control?", -2, "15:12", True),
        (PRINCIPAL[1], "Hola, Laura. Sí, termine las gotas y tráigala al control; la Dra. Paula la revisa.", -2, "15:40", True),
        (T[1], "Perfecto, muchas gracias.", -2, "15:43", True)]),
    (T[4], PRINCIPAL[4], 4, [
        (T[4], "Hola, ¿Simón necesita ayuno para el examen de control?", -1, "18:05", True),
        (PRINCIPAL[4], "Buenas noches. Sí, 12 horas de ayuno; agua sí puede tomar. Y traiga la orina de la mañana.", -1, "18:30", False)]),
    (T[9], V[7], None, [
        (T[9], "Doctora, Zeus sigue muy bien después de lo del chocolate. Gracias por todo.", -40, "09:15", True),
        (V[7], "¡Qué bueno, Natalia! Cualquier cosa me escribes.", -40, "11:02", True)]),
]
idm = 0
for i, (a, b, cli, msgs) in enumerate(CHATS, 1):
    primero = momento(dia(msgs[0][2]), msgs[0][3]) - dt.timedelta(minutes=1)
    ultimo = momento(dia(msgs[-1][2]), msgs[-1][3])
    fila("conversacion", i, primero, a, b, ultimo, cli)
    for (quien, texto, off, hora, leido) in msgs:
        idm += 1
        enviado = momento(dia(off), hora)
        fila("mensaje_chat", idm, texto, enviado, i, quien, enviado + dt.timedelta(minutes=7) if leido else None)
ult = CHATS[1][3][-1]
notificacion(T[4], "CHAT_MENSAJE", f"Nuevo mensaje de {NOMBRE[PRINCIPAL[4]]}", (ult[1] if len(ult[1]) <= 120 else ult[1][:119] + "…"), "/chat/2", "chat-2", 0,
             momento(dia(ult[2]), ult[3]))

tabla("reseña", ["idReseña", "fechaPublicacion", "comentario", "idCalificacion", "idVeterinaria", "Tutor_idUsuario"])
for r in [(1, dia(-136), "Muy buena atención con Max. La doctora explicó con calma el problema de la cadera y el plan de peso.", 1, 2, T[2]),
          (2, dia(-115), "Atendieron rápido a Toby y el tratamiento le funcionó. Los precios son justos.", 2, 3, T[3]),
          (3, dia(-92), "Excelente servicio y muy organizados. Me enviaron los resultados de laboratorio el mismo día.", 1, 4, T[4]),
          (4, dia(-78), "La atención médica fue buena, pero esperamos casi una hora aunque teníamos cita.", 3, 2, T[6])]:
    fila("reseña", *r)

tabla("pasaporte", ["idPasaporte", "token", "idMascota", "idUsuarioCreador", "creado_en", "expira_en", "revocado_en"])
tabla("pasaporte_acceso", ["idAcceso", "idPasaporte", "consultado_en", "ip", "user_agent"])
ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
tok = lambda: "".join(rnd.choice(ALFABETO) for _ in range(43))
p1 = AHORA - dt.timedelta(days=2, hours=4)
fila("pasaporte", 1, tok(), M["Simón"], T[4], p1, p1 + dt.timedelta(hours=168), None)
p2 = AHORA - dt.timedelta(days=21, hours=6)
fila("pasaporte", 2, tok(), M["Luna"], T[1], p2, p2 + dt.timedelta(hours=24), None)
fila("pasaporte_acceso", 1, 1, p1 + dt.timedelta(hours=1, minutes=12), "181.49.120.34",
     "Mozilla/5.0 (Linux; Android 14; SM-A546E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36")
fila("pasaporte_acceso", 2, 2, p2 + dt.timedelta(hours=3, minutes=40), "190.85.46.12",
     "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1")

tabla("reporte_generado", ["idReporte", "idUsuario", "rol", "tipo", "idMascota", "numeroHistorial", "parametros", "ip",
                           "generadoEn"])
reg_lab = TABLAS["historial_clinico"][1][h_simon - 1][9]
fila("reporte_generado", 1, V[10], "VETERINARIO", "REGISTRO", M["Simón"], h_simon, None, "190.85.46.12",
     reg_lab + dt.timedelta(minutes=12))
fila("reporte_generado", 2, T[4], "TUTOR", "HISTORIAL_COMPLETO", M["Simón"], None, None, "181.49.120.34",
     AHORA - dt.timedelta(days=3, hours=2))
fila("reporte_generado", 3, PRINCIPAL[3], "RECEPCIONISTA", "VACUNAS", M["Nala"], None, None, "186.31.22.140",
     AHORA - dt.timedelta(days=9, hours=5))

tabla("transferencia_mascota", ["idTransferencia", "idMascota", "idTutorOrigen", "idTutorDestino", "estado", "mensaje",
                                "creadaEn", "resueltaEn"])
tr = AHORA - dt.timedelta(days=1, hours=2)
fila("transferencia_mascota", 1, M["Canela"], T[9], T[3], "PENDIENTE",
     "María José, te paso a Canela: en el nuevo apartamento no permiten conejos.", tr, None)
notificacion(T[3], "TRANSFERENCIA_SOLICITADA", "Te quieren transferir una mascota",
             "Natalia Andrea Salazar Mejía quiere transferirte a Canela. Revisa la solicitud.",
             "/mascotas/transferencias", "transferencia-1", 0, tr)

tabla("mascota_evento", ["idEvento", "idMascota", "tipo", "detalle", "valorAnterior", "valorNuevo", "idUsuario", "fecha"])
fila("mascota_evento", 1, M["Canela"], "TRANSFERENCIA_SOLICITADA", "Solicitada a María José Herrera Castaño", None, None,
     T[9], tr)
fila("mascota_evento", 2, M["Max"], "PESO_ACTUALIZADO", "Peso actualizado por recepción", "36.2", "34.8", R[4],
     fin_de(c_max2) - dt.timedelta(minutes=25))
fila("mascota_evento", 3, M["Simón"], "PESO_ACTUALIZADO", "Peso actualizado por recepción", "33.5", "34.1", PRINCIPAL[4],
     fin_de(c_simon_vac) - dt.timedelta(minutes=20))

# =====================================================================
# 8. Auditoría: acciones reales sobre los registros anteriores. Completa el total exacto de 500.
# =====================================================================
tabla("auditoria", ["idAuditoria", "idUsuario", "accion", "entidad", "idEntidad", "detalle", "ip", "fecha"])
CANDIDATAS = []
for cid in CONFIRMADAS:
    c = TABLAS["cita"][1][cid - 1]
    CANDIDATAS.append((PRINCIPAL[c[6]], "CITA_CONFIRMAR", "cita", cid, None, c[14] + dt.timedelta(hours=2, minutes=15)))
for cid in PENDIENTES:
    c = TABLAS["cita"][1][cid - 1]
    CANDIDATAS.append((DUENO[c[7]], "CITA_RESERVAR", "cita", cid, f"estado=PENDIENTE mascota={c[7]}", c[14]))
CANDIDATAS.append((T[8], "CITA_CANCELAR", "cita", CANCELADA_ID, None, AHORA - dt.timedelta(hours=20)))
for vet, sol in SOL_HORARIO.items():
    s = TABLAS["solicitud_disponibilidad"][1][sol - 1]
    CANDIDATAS.append((s[7], "DISPONIBILIDAD_APROBAR", "solicitud_disponibilidad", sol, None, s[8]))
for h in TABLAS["historial_clinico"][1][-6:]:
    CANDIDATAS.append((h[7], "HISTORIAL_CREAR", "historial_clinico", h[0], f"cita={h[10]}" if h[10] else None, h[9]))
CANDIDATAS.append((T[9], "MASCOTA_TRANSFERENCIA_SOLICITAR", "mascota", M["Canela"], f"transferencia=1 destino={T[3]}", tr))
for cv in TABLAS["carnet_vacuna"][1]:
    if cv[9]:
        CANDIDATAS.append((cv[6], "VACUNA_REGISTRAR", "carnet_vacuna", cv[0], f"cita={cv[9]}",
                           fin_de(cv[9]) - dt.timedelta(minutes=3)))

ya = sum(len(f) for _, f in TABLAS.values())
faltan = TOTAL_OBJETIVO - ya
if not 1 <= faltan <= len(CANDIDATAS):
    for t in ORDEN: print(f"{t:28s} {len(TABLAS[t][1]):4d}", file=sys.stderr)
    sys.exit(f"ERROR: la semilla tiene {ya} filas sin auditoría; no se puede completar {TOTAL_OBJETIVO} "
             f"(candidatas de auditoría: {len(CANDIDATAS)}).")
IPS = ["190.85.46.12", "181.49.120.34", "186.31.22.140", "191.95.12.77"]
for i, (u, acc, ent, ident, det, fecha) in enumerate(sorted(CANDIDATAS[:faltan], key=lambda x: x[5]), 1):
    fila("auditoria", i, u, acc, ent, str(ident), det, IPS[i % len(IPS)], fecha)

TOTAL = sum(len(f) for _, f in TABLAS.values())
assert TOTAL == TOTAL_OBJETIVO, TOTAL


# =====================================================================
# Salida
# =====================================================================
def sql(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "1" if v else "0"
    if isinstance(v, (int, float)):
        return repr(v)
    if isinstance(v, dt.datetime):
        return "'" + v.strftime("%Y-%m-%d %H:%M:%S") + "'"
    if isinstance(v, dt.date):
        return "'" + v.isoformat() + "'"
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


if "--resumen" in args:
    for t in ORDEN:
        print(f"{t:28s} {len(TABLAS[t][1]):4d}")
    print(f"{'TOTAL':28s} {TOTAL:4d}   (referencia: {AHORA:%Y-%m-%d %H:%M} America/Bogota)")
elif "--adjuntos" in args:
    import os
    base = args[args.index("--adjuntos") + 1]
    for ruta, datos in ARCHIVOS_BYTES.items():
        destino = os.path.join(base, *ruta.split("/"))
        os.makedirs(os.path.dirname(destino), exist_ok=True)
        with open(destino, "wb") as fh:
            fh.write(datos)
        print(f"adjunto: {ruta} ({len(datos)} bytes)")
else:
    out = sys.stdout
    out.write(f"-- Semilla VetLink: {TOTAL} registros. Referencia {AHORA:%Y-%m-%d %H:%M} (America/Bogota).\n")
    out.write("SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS = 1;\nSTART TRANSACTION;\n")
    for t in ORDEN:
        cols, filas = TABLAS[t]
        if not filas:
            continue
        out.write(f"INSERT INTO `{t}` (" + ", ".join(f"`{c}`" for c in cols) + ") VALUES\n")
        out.write(",\n".join("(" + ", ".join(sql(v) for v in f) + ")" for f in filas) + ";\n")
    out.write("COMMIT;\n")
