-- =====================================================================
-- Ingreso validado por dominio de correo (indicación del docente).
-- Una universidad puede tener varios dominios: en la UPN conviven
-- @upn.pe y @upn.edu.pe.
-- =====================================================================

CREATE TABLE dominio_permitido (
    dominio         VARCHAR(60) PRIMARY KEY,
    id_universidad  INTEGER     NOT NULL REFERENCES universidad (id_universidad),
    -- FALSE: la universidad está registrada pero la app aún no tiene sus sedes
    activo          BOOLEAN     NOT NULL DEFAULT FALSE
);

INSERT INTO dominio_permitido (dominio, id_universidad, activo)
SELECT dominio_correo, id_universidad, siglas = 'UPN' FROM universidad;

INSERT INTO dominio_permitido (dominio, id_universidad, activo)
SELECT '@upn.edu.pe', id_universidad, TRUE FROM universidad WHERE siglas = 'UPN';

-- El código sale de la parte del correo antes de la @; en docentes puede ser largo
-- (nombre.apellido.apellido) y no entraba en 20 caracteres
ALTER TABLE resena ALTER COLUMN id_usuario SET DATA TYPE VARCHAR(80);
ALTER TABLE usuario ALTER COLUMN codigo SET DATA TYPE VARCHAR(80);
