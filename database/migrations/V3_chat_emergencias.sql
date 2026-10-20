-- VetLink V3: chat interno y gestión de emergencias.
CREATE TABLE conversacion (
  idConversacion INT AUTO_INCREMENT PRIMARY KEY,
  usuarioA INT NOT NULL,
  usuarioB INT NOT NULL,
  creadaEn DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_conversacion_a FOREIGN KEY (usuarioA) REFERENCES usuario(idUsuario),
  CONSTRAINT fk_conversacion_b FOREIGN KEY (usuarioB) REFERENCES usuario(idUsuario),
  UNIQUE KEY uk_conversacion_par (usuarioA, usuarioB)
);
CREATE TABLE mensaje_chat (
  idMensaje INT AUTO_INCREMENT PRIMARY KEY,
  idConversacion INT NOT NULL,
  idRemitente INT NOT NULL,
  contenido TEXT NOT NULL,
  enviadoEn DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_mensaje_conversacion FOREIGN KEY (idConversacion) REFERENCES conversacion(idConversacion),
  CONSTRAINT fk_mensaje_remitente FOREIGN KEY (idRemitente) REFERENCES usuario(idUsuario)
);
CREATE TABLE emergencia (
  idEmergencia INT AUTO_INCREMENT PRIMARY KEY,
  idMascota INT(10) UNSIGNED NOT NULL,
  idVeterinaria INT NOT NULL,
  idVeterinario INT NULL,
  motivo VARCHAR(120) NOT NULL,
  detalle TEXT NULL,
  estado ENUM('PENDIENTE','ASIGNADA','ATENDIDA','CANCELADA') NOT NULL DEFAULT 'PENDIENTE',
  creadaEn DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_emergencia_mascota FOREIGN KEY (idMascota) REFERENCES mascota(idMascota),
  CONSTRAINT fk_emergencia_veterinaria FOREIGN KEY (idVeterinaria) REFERENCES veterinaria(idVeterinaria),
  CONSTRAINT fk_emergencia_veterinario FOREIGN KEY (idVeterinario) REFERENCES usuario(idUsuario)
);
