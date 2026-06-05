-- Datos de ejemplo para demostrar los endpoints durante el video
INSERT INTO guias_despacho (numero_guia, transportista, destinatario, direccion_destino, descripcion_carga, peso_carga, fecha_despacho, estado)
VALUES ('GD-2024-001', 'TransportistaX', 'Empresa ABC Ltda.', 'Av. Providencia 1234, Santiago', 'Cajas de electrodomésticos (10 unidades)', 150.5, '2024-03-15', 'PENDIENTE');

INSERT INTO guias_despacho (numero_guia, transportista, destinatario, direccion_destino, descripcion_carga, peso_carga, fecha_despacho, estado)
VALUES ('GD-2024-002', 'TransportistaX', 'Comercial XYZ SpA', 'O''Higgins 567, Valparaíso', 'Pallets de materiales de construcción', 800.0, '2024-03-15', 'EN_TRANSITO');

INSERT INTO guias_despacho (numero_guia, transportista, destinatario, direccion_destino, descripcion_carga, peso_carga, fecha_despacho, estado)
VALUES ('GD-2024-003', 'TransportistaY', 'Distribuidora Norte', 'Av. Independencia 890, Antofagasta', 'Equipos de refrigeración (3 unidades)', 320.0, '2024-03-20', 'PENDIENTE');

INSERT INTO guias_despacho (numero_guia, transportista, destinatario, direccion_destino, descripcion_carga, peso_carga, fecha_despacho, estado)
VALUES ('GD-2024-004', 'TransportistaY', 'Retail Sur Ltda.', 'Los Carrera 123, Concepción', 'Ropa y textiles temporada', 95.0, '2024-03-20', 'ENTREGADO');
