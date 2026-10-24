-- =====================================================================
-- T2: registro e inicio de sesión desde la API.
-- La cuenta puede crearse con contraseña propia (CORREO) o entrar con la
-- cuenta institucional de Microsoft 365 (MICROSOFT), que ya trae el correo.
-- =====================================================================

ALTER TABLE usuario ADD COLUMN clave_hash VARCHAR(100);
ALTER TABLE usuario ADD COLUMN sede_codigo VARCHAR(30);
ALTER TABLE usuario ADD COLUMN proveedor VARCHAR(10) NOT NULL DEFAULT 'CORREO';

-- Universidades de Lima con correo institucional en Microsoft 365.
-- Por ahora la app solo muestra las sedes de la UPN; estas filas dejan lista
-- la base para abrir la app a otras universidades sin cambiar el modelo.
INSERT INTO universidad (nombre, siglas, dominio_correo) VALUES
('Universidad Peruana de Ciencias Aplicadas', 'UPC', '@upc.edu.pe'),
('Universidad Tecnológica del Perú', 'UTP', '@utp.edu.pe'),
('Universidad de Lima', 'ULIMA', '@aloe.ulima.edu.pe');
