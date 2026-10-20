-- Nota: idUsuario 4 (carlos@gmail.com) no tiene fila en tutor/veterinario/
-- recepcionista en el dump original, asi que no tiene un rol asignado y
-- no puede iniciar sesion aunque se le cambie la contrasena (el backend
-- exige que todo usuario tenga uno de los 3 roles).
-- =====================================================================

UPDATE `usuario` SET `password` = '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.' WHERE `idUsuario` = 1;
UPDATE `usuario` SET `password` = '$2b$10$7GnTAv5AtJYrf7NOKmaAvekpN.H/x7kHclDN0OPxrvLxkyENfG..y' WHERE `idUsuario` = 2;
UPDATE `usuario` SET `password` = '$2b$10$cEfDAPYOlpRT2tSotc.CY.wbNz4EZBCSSuWiD1o3yl3xHKP8QAdv2' WHERE `idUsuario` = 3;
