-- Estados finales de la cita y metadatos de adjuntos clínicos.
ALTER TABLE cita MODIFY estado ENUM('DISPONIBLE','AGENDADA','CANCELADA','PENDIENTE','ATENDIDA') NOT NULL DEFAULT 'DISPONIBLE';
ALTER TABLE archivo_historial ADD COLUMN nombreArchivo VARCHAR(255) NULL;
ALTER TABLE archivo_historial ADD COLUMN rutaArchivo VARCHAR(500) NULL;
ALTER TABLE archivo_historial MODIFY COLUMN tipoArchivo VARCHAR(255) NOT NULL;
