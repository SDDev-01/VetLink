#!/usr/bin/env bash
set -Eeuo pipefail

DATABASE_DIR="/database"

: "${MYSQL_DATABASE:?MYSQL_DATABASE no está definida}"
: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD no está definida}"
: "${MYSQL_SPRING_PASSWORD:?MYSQL_SPRING_PASSWORD no está definida}"
: "${MYSQL_FAST_PASSWORD:?MYSQL_FAST_PASSWORD no está definida}"

# Validar los valores que se interpolarán en sentencias SQL.
if [[ ! "${MYSQL_DATABASE}" =~ ^[A-Za-z0-9_]+$ ]]; then
    echo "ERROR: MYSQL_DATABASE contiene caracteres no permitidos."
    exit 1
fi

# Usamos contraseñas hexadecimales generadas con openssl rand -hex 32.
if [[ ! "${MYSQL_SPRING_PASSWORD}" =~ ^[a-fA-F0-9]{64}$ ]] ||
   [[ ! "${MYSQL_FAST_PASSWORD}" =~ ^[a-fA-F0-9]{64}$ ]]; then
    echo "ERROR: Las contraseñas de aplicación deben tener 64 caracteres hexadecimales."
    exit 1
fi

# Evitar repetir credenciales en los argumentos del proceso.
export MYSQL_PWD="${MYSQL_ROOT_PASSWORD}"

mysql_exec() {
    mysql --protocol=socket -uroot "$MYSQL_DATABASE" "$@"
}

echo "======================================"
echo " Inicializando base de datos VetLink"
echo "======================================"

echo "[1/5] Importando estructura y datos iniciales..."

mysql_exec < "${DATABASE_DIR}/mydb.sql"

echo "[2/5] Aplicando correcciones V5..."

mysql_exec < "${DATABASE_DIR}/migrations/V5_correcciones.sql"

echo "[3/5] Creando tablas del pasaporte (V7)..."

mysql_exec < "${DATABASE_DIR}/migrations/V7_pasaporte.sql"

echo "[4/5] Creando usuarios y asignando permisos..."

mysql_exec --execute="
    CREATE USER IF NOT EXISTS 'vetlink_spring'@'%'
    IDENTIFIED BY '${MYSQL_SPRING_PASSWORD}';

    CREATE USER IF NOT EXISTS 'vetlink_fast'@'%'
    IDENTIFIED BY '${MYSQL_FAST_PASSWORD}';
"

echo "Asignando permisos a vetlink_spring..."

# Spring recibe permisos DML únicamente sobre las tablas existentes.
# Las tablas reservadas para FastAPI quedan excluidas.
while IFS= read -r table; do
    [[ -z "${table}" ]] && continue

    mysql_exec --execute="
        GRANT SELECT, INSERT, UPDATE, DELETE
        ON \`${MYSQL_DATABASE}\`.\`${table}\`
        TO 'vetlink_spring'@'%';
    "
done < <(
    mysql_exec --batch --skip-column-names --execute="
        SELECT TABLE_NAME
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = '${MYSQL_DATABASE}'
          AND TABLE_TYPE = 'BASE TABLE'
          AND TABLE_NAME NOT IN ('pasaporte', 'pasaporte_acceso')
        ORDER BY TABLE_NAME;
    "
)

echo "Asignando permisos a vetlink_fast..."

# FastAPI escribe solo en sus dos tablas (sin DELETE) y lee únicamente lo que
# muestra el pasaporte. Columnas leídas: docs/contrato-documentos.md, sección 5.
# GRANT falla si una tabla no existe, así que también sirve de comprobación.
mysql_exec --execute="
    GRANT SELECT, INSERT, UPDATE ON \`${MYSQL_DATABASE}\`.\`pasaporte\` TO 'vetlink_fast'@'%';
    GRANT SELECT, INSERT, UPDATE ON \`${MYSQL_DATABASE}\`.\`pasaporte_acceso\` TO 'vetlink_fast'@'%';

    GRANT SELECT ON \`${MYSQL_DATABASE}\`.\`mascota\` TO 'vetlink_fast'@'%';
    GRANT SELECT ON \`${MYSQL_DATABASE}\`.\`especie\` TO 'vetlink_fast'@'%';
    GRANT SELECT ON \`${MYSQL_DATABASE}\`.\`raza\` TO 'vetlink_fast'@'%';
    GRANT SELECT ON \`${MYSQL_DATABASE}\`.\`carnet_vacuna\` TO 'vetlink_fast'@'%';
    GRANT SELECT ON \`${MYSQL_DATABASE}\`.\`vacuna\` TO 'vetlink_fast'@'%';
    GRANT SELECT ON \`${MYSQL_DATABASE}\`.\`historial_clinico\` TO 'vetlink_fast'@'%';
    GRANT SELECT ON \`${MYSQL_DATABASE}\`.\`historial_medicamento\` TO 'vetlink_fast'@'%';
    GRANT SELECT ON \`${MYSQL_DATABASE}\`.\`medicamento\` TO 'vetlink_fast'@'%';
    GRANT SELECT ON \`${MYSQL_DATABASE}\`.\`veterinaria\` TO 'vetlink_fast'@'%';

    -- Solo el nombre del veterinario; idUsuario hace falta para unirlo con historial_clinico.
    GRANT SELECT (idUsuario, nombre, apellido) ON \`${MYSQL_DATABASE}\`.\`usuario\` TO 'vetlink_fast'@'%';
"

echo "[5/5] Comprobando datos de demostración..."

if [[ "${RUN_DEMO_SEED:-false}" == "true" ]]; then
    echo "Aplicando V6 y pasaportes DEMO..."
    mysql_exec < "${DATABASE_DIR}/migrations/V6_datos_demo.sql"
    mysql_exec < "${DATABASE_DIR}/migrations/V7_pasaporte_demo.sql"
else
    echo "Datos de demostración omitidos."
fi

unset MYSQL_PWD

echo "======================================"
echo " Inicialización finalizada"
echo "======================================"
