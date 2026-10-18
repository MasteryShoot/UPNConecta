-- Datos maestros: universidad, sedes y criterios de evaluación (los mismos de la app)

INSERT INTO universidad (nombre, siglas, dominio_correo)
VALUES ('Universidad Privada del Norte', 'UPN', '@upn.pe');

INSERT INTO sede (id_universidad, codigo, nombre, direccion) VALUES
(1, 'los_olivos', 'Los Olivos', 'Av. Alfredo Mendiola 6062'),
(1, 'brena', 'Breña', 'Av. Tingo María 1122'),
(1, 'comas', 'Comas', 'Av. Andrés Belaunde cdra. 10'),
(1, 'sjl', 'San Juan de Lurigancho', 'Av. El Sol 461'),
(1, 'chorrillos', 'Chorrillos', 'Av. Guardia Peruana 890'),
(1, 'el_molino', 'Trujillo El Molino', 'Av. El Ejército 920'),
(1, 'san_isidro', 'Trujillo San Isidro', 'Urb. Deán Saavedra, El Cortijo'),
(1, 'cajamarca', 'Cajamarca', 'Vía de Evitamiento Norte cdra. 15');

INSERT INTO criterio (area, nombre, orden) VALUES
('infraestructura', 'Pabellones', 1),
('infraestructura', 'Aulas', 2),
('infraestructura', 'Laboratorios', 3),
('limpieza', 'Baños', 1),
('limpieza', 'Áreas comunes', 2),
('limpieza', 'Cafeterías', 3),
('gestion', 'Secretaría', 1),
('gestion', 'Bienestar', 2),
('gestion', 'Soporte técnico', 3),
('sostenibilidad', 'Tachos de reciclaje', 1),
('sostenibilidad', 'Puntos de segregación', 2),
('sostenibilidad', 'Señalización ambiental', 3);
