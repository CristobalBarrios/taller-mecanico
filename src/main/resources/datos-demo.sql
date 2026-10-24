-- Datos ficticios de presentación. Se ejecuta mediante DatosDemostracion con DEMO_DATA=true.
-- No ejecutarlo por separado: @demo_clave la establece Java con BCrypt.
-- La transacción del cargador revierte todas las inserciones si hay algún conflicto.
-- Las existencias ya representan el saldo después de los despachos incluidos.
SET @ahora_demo = UTC_TIMESTAMP();

INSERT INTO usuarios(nombre,usuario,clave,rol) VALUES
 ('María Fernanda López','demo_recepcion',@demo_clave,'RECEPCION'),
 ('Carlos Estuardo Méndez','demo_mecanico1',@demo_clave,'MECANICO'),
 ('José Andrés Vásquez','demo_mecanico2',@demo_clave,'MECANICO'),
 ('Luis Alberto Estrada','demo_bodega',@demo_clave,'BODEGUERO');
SET @recepcion_demo = (SELECT id FROM usuarios WHERE usuario='demo_recepcion');
SET @mecanico1_demo = (SELECT id FROM usuarios WHERE usuario='demo_mecanico1');
SET @mecanico2_demo = (SELECT id FROM usuarios WHERE usuario='demo_mecanico2');
SET @bodega_demo = (SELECT id FROM usuarios WHERE usuario='demo_bodega');

INSERT INTO clientes(nombre,nit,telefono) VALUES ('Ana Lucía Morales','CF','5550-0101');
SET @c1_demo = LAST_INSERT_ID();
INSERT INTO clientes(nombre,nit,telefono) VALUES ('Jorge Luis Méndez','8364251-7','5550-0102');
SET @c2_demo = LAST_INSERT_ID();
INSERT INTO clientes(nombre,nit,telefono) VALUES ('Sofía Alejandra Castillo','CF','5550-0103');
SET @c3_demo = LAST_INSERT_ID();
INSERT INTO clientes(nombre,nit,telefono) VALUES ('Ricardo Antonio Pérez','7241683-5','5550-0104');
SET @c4_demo = LAST_INSERT_ID();
INSERT INTO clientes(nombre,nit,telefono) VALUES ('Mariana Isabel Ramírez','CF','5550-0105');
SET @c5_demo = LAST_INSERT_ID();
INSERT INTO clientes(nombre,nit,telefono) VALUES ('José Manuel Aguilar','9157342-8','5550-0106');
SET @c6_demo = LAST_INSERT_ID();
INSERT INTO clientes(nombre,nit,telefono) VALUES ('Andrea Paola Cifuentes','CF','5550-0107');
SET @c7_demo = LAST_INSERT_ID();
INSERT INTO clientes(nombre,nit,telefono) VALUES ('Luis Fernando Herrera','6824519-3','5550-0108');
SET @c8_demo = LAST_INSERT_ID();
INSERT INTO clientes(nombre,nit,telefono) VALUES ('Gabriela Beatriz López','CF','5550-0109');
SET @c9_demo = LAST_INSERT_ID();
INSERT INTO clientes(nombre,nit,telefono) VALUES ('Óscar Daniel Fuentes','CF','5550-0110');
SET @c10_demo = LAST_INSERT_ID();

INSERT INTO vehiculos(cliente_id,placa,marca,modelo,anio) VALUES (@c1_demo,'P472KLM','Toyota','Corolla',2018);
SET @v1_demo = LAST_INSERT_ID();
INSERT INTO vehiculos(cliente_id,placa,marca,modelo,anio) VALUES (@c2_demo,'P183JRS','Honda','Civic',2016);
SET @v2_demo = LAST_INSERT_ID();
INSERT INTO vehiculos(cliente_id,placa,marca,modelo,anio) VALUES (@c3_demo,'P625MBD','Toyota','Hilux',2021);
SET @v3_demo = LAST_INSERT_ID();
INSERT INTO vehiculos(cliente_id,placa,marca,modelo,anio) VALUES (@c4_demo,'P394HGT','Mazda','3',2017);
SET @v4_demo = LAST_INSERT_ID();
INSERT INTO vehiculos(cliente_id,placa,marca,modelo,anio) VALUES (@c5_demo,'P751LPN','Hyundai','Tucson',2019);
SET @v5_demo = LAST_INSERT_ID();
INSERT INTO vehiculos(cliente_id,placa,marca,modelo,anio) VALUES (@c6_demo,'P268NFC','Kia','Rio',2020);
SET @v6_demo = LAST_INSERT_ID();
INSERT INTO vehiculos(cliente_id,placa,marca,modelo,anio) VALUES (@c7_demo,'P519GVM','Nissan','Sentra',2015);
SET @v7_demo = LAST_INSERT_ID();
INSERT INTO vehiculos(cliente_id,placa,marca,modelo,anio) VALUES (@c8_demo,'P846FDR','Mitsubishi','Lancer',2014);
SET @v8_demo = LAST_INSERT_ID();
INSERT INTO vehiculos(cliente_id,placa,marca,modelo,anio) VALUES (@c9_demo,'P307PBT','Suzuki','Swift',2022);
SET @v9_demo = LAST_INSERT_ID();
INSERT INTO vehiculos(cliente_id,placa,marca,modelo,anio) VALUES (@c10_demo,'P692DKS','Chevrolet','Aveo',2013);
SET @v10_demo = LAST_INSERT_ID();

INSERT INTO repuestos(codigo,nombre,precio,stock,minimo) VALUES
 ('DEMO-ACE5W30','Aceite sintético 5W-30, litro',85.00,24,8),
 ('DEMO-FILTACE','Filtro de aceite',65.00,7,4),
 ('DEMO-FILTAIRE','Filtro de aire de motor',110.00,3,4),
 ('DEMO-PASTDEL','Juego de pastillas de freno delanteras',325.00,2,3),
 ('DEMO-DISCFRE','Disco de freno delantero',450.00,2,2),
 ('DEMO-BUJIA','Bujía de encendido',65.00,18,8),
 ('DEMO-BATERIA','Batería de 12 V, 60 Ah',975.00,3,2),
 ('DEMO-FAJALT','Faja de alternador',185.00,0,3),
 ('DEMO-REFRI','Refrigerante premezclado, galón',135.00,10,4),
 ('DEMO-TERMOST','Termostato de motor',245.00,1,2),
 ('DEMO-AMORT','Amortiguador delantero',625.00,4,2),
 ('DEMO-FILTAC','Filtro de aire acondicionado',145.00,6,3);
SET @aceite_demo = (SELECT id FROM repuestos WHERE codigo='DEMO-ACE5W30');
SET @filtroace_demo = (SELECT id FROM repuestos WHERE codigo='DEMO-FILTACE');
SET @filtroaire_demo = (SELECT id FROM repuestos WHERE codigo='DEMO-FILTAIRE');
SET @pastillas_demo = (SELECT id FROM repuestos WHERE codigo='DEMO-PASTDEL');
SET @discos_demo = (SELECT id FROM repuestos WHERE codigo='DEMO-DISCFRE');
SET @bujias_demo = (SELECT id FROM repuestos WHERE codigo='DEMO-BUJIA');
SET @bateria_demo = (SELECT id FROM repuestos WHERE codigo='DEMO-BATERIA');
SET @faja_demo = (SELECT id FROM repuestos WHERE codigo='DEMO-FAJALT');
SET @refrigerante_demo = (SELECT id FROM repuestos WHERE codigo='DEMO-REFRI');
SET @termostato_demo = (SELECT id FROM repuestos WHERE codigo='DEMO-TERMOST');
SET @amortiguador_demo = (SELECT id FROM repuestos WHERE codigo='DEMO-AMORT');
SET @filtroac_demo = (SELECT id FROM repuestos WHERE codigo='DEMO-FILTAC');

INSERT INTO ordenes(vehiculo_id,mecanico_id,motivo,estado,ingreso,mano_obra) VALUES
 (@v1_demo,@mecanico1_demo,'Servicio de mantenimiento preventivo de 80,000 km y revisión de luz de motor.','RECIBIDO',DATE_SUB(@ahora_demo,INTERVAL 1 HOUR),0);
SET @o1_demo = LAST_INSERT_ID();
INSERT INTO ordenes(vehiculo_id,mecanico_id,motivo,estado,ingreso,mano_obra) VALUES
 (@v2_demo,@mecanico2_demo,'Vibración en el volante al frenar en carretera.','RECIBIDO',DATE_SUB(@ahora_demo,INTERVAL 2 HOUR),0);
SET @o2_demo = LAST_INSERT_ID();
INSERT INTO ordenes(vehiculo_id,mecanico_id,motivo,estado,ingreso,mano_obra) VALUES
 (@v3_demo,@mecanico1_demo,'Ruido metálico al frenar y mayor distancia de frenado.','DIAGNOSTICADO',DATE_SUB(@ahora_demo,INTERVAL 8 HOUR),0);
SET @o3_demo = LAST_INSERT_ID();
INSERT INTO ordenes(vehiculo_id,mecanico_id,motivo,estado,ingreso,mano_obra) VALUES
 (@v4_demo,@mecanico2_demo,'Golpeteo en suspensión delantera y arranque lento por las mañanas.','DIAGNOSTICADO',DATE_SUB(@ahora_demo,INTERVAL 24 HOUR),0);
SET @o4_demo = LAST_INSERT_ID();
INSERT INTO ordenes(vehiculo_id,mecanico_id,motivo,estado,ingreso,mano_obra) VALUES
 (@v5_demo,@mecanico1_demo,'Chirrido al frenar y desgaste irregular de pastillas delanteras.','EN_REPARACION',DATE_SUB(@ahora_demo,INTERVAL 30 HOUR),200);
SET @o5_demo = LAST_INSERT_ID();
INSERT INTO ordenes(vehiculo_id,mecanico_id,motivo,estado,ingreso,mano_obra) VALUES
 (@v6_demo,@mecanico2_demo,'Cambio de aceite y revisión general antes de viaje.','EN_REPARACION',DATE_SUB(@ahora_demo,INTERVAL 48 HOUR),150);
SET @o6_demo = LAST_INSERT_ID();
INSERT INTO ordenes(vehiculo_id,mecanico_id,motivo,estado,ingreso,mano_obra) VALUES
 (@v7_demo,@mecanico1_demo,'Temperatura del motor elevada durante el tráfico.','EN_REPARACION',DATE_SUB(@ahora_demo,INTERVAL 60 HOUR),300);
SET @o7_demo = LAST_INSERT_ID();
INSERT INTO ordenes(vehiculo_id,mecanico_id,motivo,estado,ingreso,mano_obra) VALUES
 (@v8_demo,@mecanico2_demo,'Pérdida de potencia y mantenimiento de sistema de encendido.','TERMINADO',DATE_SUB(@ahora_demo,INTERVAL 72 HOUR),350);
SET @o8_demo = LAST_INSERT_ID();
INSERT INTO ordenes(vehiculo_id,mecanico_id,motivo,estado,ingreso,salida,mano_obra) VALUES
 (@v9_demo,@mecanico1_demo,'Servicio de cambio de aceite y filtro de motor.','ENTREGADO',DATE_SUB(@ahora_demo,INTERVAL 120 HOUR),DATE_SUB(@ahora_demo,INTERVAL 113 HOUR),250);
SET @o9_demo = LAST_INSERT_ID();
INSERT INTO ordenes(vehiculo_id,mecanico_id,motivo,estado,ingreso,salida,mano_obra) VALUES
 (@v10_demo,@mecanico2_demo,'Pulsación del pedal de freno y ruido en faja de alternador.','ENTREGADO',DATE_SUB(@ahora_demo,INTERVAL 168 HOUR),DATE_SUB(@ahora_demo,INTERVAL 161 HOUR),480);
SET @o10_demo = LAST_INSERT_ID();

INSERT INTO diagnosticos(orden_id,usuario_id,descripcion,reparacion,fecha) VALUES
 (@o3_demo,@mecanico1_demo,'Discos delanteros con desgaste por debajo del espesor recomendado.','Sustituir ambos discos delanteros y verificar el sistema de frenado.',DATE_SUB(@ahora_demo,INTERVAL 7 HOUR)),
 (@o4_demo,@mecanico2_demo,'Amortiguadores delanteros con fuga. Se verificó batería y se descartó su reemplazo después de la prueba de carga.','Cambiar amortiguadores delanteros y limpiar terminales de batería.',DATE_SUB(@ahora_demo,INTERVAL 23 HOUR)),
 (@o5_demo,@mecanico1_demo,'Pastillas delanteras gastadas y discos con superficie irregular.','Cambiar juego de pastillas y discos delanteros.',DATE_SUB(@ahora_demo,INTERVAL 29 HOUR)),
 (@o6_demo,@mecanico2_demo,'Aceite de motor al final de su intervalo de servicio. Sin fugas visibles.','Drenar aceite usado, rellenar con aceite 5W-30 y realizar inspección general.',DATE_SUB(@ahora_demo,INTERVAL 47 HOUR)),
 (@o7_demo,@mecanico1_demo,'Termostato atascado y refrigerante deteriorado. Sin evidencia de fuga externa.','Sustituir termostato, renovar refrigerante y purgar el circuito.',DATE_SUB(@ahora_demo,INTERVAL 59 HOUR)),
 (@o8_demo,@mecanico2_demo,'Bujías desgastadas y filtros de aire de motor y cabina saturados.','Reemplazar cuatro bujías y ambos filtros; comprobar marcha mínima.',DATE_SUB(@ahora_demo,INTERVAL 71 HOUR)),
 (@o9_demo,@mecanico1_demo,'Servicio periódico de lubricación. Filtro de aceite requiere sustitución.','Cambiar cuatro litros de aceite y filtro. Verificar nivel y ausencia de fugas.',DATE_SUB(@ahora_demo,INTERVAL 119 HOUR)),
 (@o10_demo,@mecanico2_demo,'Discos delanteros deformados y faja de alternador cuarteada.','Cambiar discos delanteros y faja; realizar prueba de frenado y carga.',DATE_SUB(@ahora_demo,INTERVAL 167 HOUR));

-- Las solicitudes pendientes no reservan inventario: ambas órdenes pueden pedir
-- los mismos dos discos, pero despachar una obligará a reponer stock para la otra.
INSERT INTO solicitudes(orden_id,repuesto_id,cantidad,precio_unitario,estado,fecha) VALUES
 (@o3_demo,@discos_demo,2,NULL,'PENDIENTE',DATE_SUB(@ahora_demo,INTERVAL 6 HOUR)),
 (@o4_demo,@bateria_demo,1,NULL,'RECHAZADO',DATE_SUB(@ahora_demo,INTERVAL 22 HOUR)),
 (@o4_demo,@amortiguador_demo,2,NULL,'PENDIENTE',DATE_SUB(@ahora_demo,INTERVAL 22 HOUR)),
 (@o5_demo,@pastillas_demo,1,325.00,'DESPACHADO',DATE_SUB(@ahora_demo,INTERVAL 28 HOUR)),
 (@o5_demo,@discos_demo,2,NULL,'PENDIENTE',DATE_SUB(@ahora_demo,INTERVAL 28 HOUR)),
 (@o6_demo,@aceite_demo,4,NULL,'PENDIENTE',DATE_SUB(@ahora_demo,INTERVAL 46 HOUR)),
 (@o7_demo,@termostato_demo,1,245.00,'DESPACHADO',DATE_SUB(@ahora_demo,INTERVAL 58 HOUR)),
 (@o7_demo,@refrigerante_demo,2,NULL,'PENDIENTE',DATE_SUB(@ahora_demo,INTERVAL 58 HOUR)),
 (@o8_demo,@filtroaire_demo,1,110.00,'DESPACHADO',DATE_SUB(@ahora_demo,INTERVAL 70 HOUR)),
 (@o8_demo,@bujias_demo,4,65.00,'DESPACHADO',DATE_SUB(@ahora_demo,INTERVAL 70 HOUR)),
 (@o8_demo,@filtroac_demo,1,145.00,'DESPACHADO',DATE_SUB(@ahora_demo,INTERVAL 70 HOUR)),
 (@o9_demo,@aceite_demo,4,85.00,'DESPACHADO',DATE_SUB(@ahora_demo,INTERVAL 118 HOUR)),
 (@o9_demo,@filtroace_demo,1,65.00,'DESPACHADO',DATE_SUB(@ahora_demo,INTERVAL 118 HOUR)),
 (@o10_demo,@discos_demo,2,450.00,'DESPACHADO',DATE_SUB(@ahora_demo,INTERVAL 166 HOUR)),
 (@o10_demo,@faja_demo,1,185.00,'DESPACHADO',DATE_SUB(@ahora_demo,INTERVAL 166 HOUR));

INSERT INTO movimientos(repuesto_id,usuario_id,cantidad,motivo,fecha)
 SELECT r.id,@bodega_demo,r.stock+COALESCE(d.consumidos,0),'Inventario inicial de demostración',DATE_SUB(@ahora_demo,INTERVAL 240 HOUR)
 FROM repuestos r
 LEFT JOIN (SELECT repuesto_id,SUM(cantidad) consumidos FROM solicitudes
   WHERE orden_id IN (@o1_demo,@o2_demo,@o3_demo,@o4_demo,@o5_demo,@o6_demo,@o7_demo,@o8_demo,@o9_demo,@o10_demo)
   AND estado='DESPACHADO' GROUP BY repuesto_id) d ON d.repuesto_id=r.id
 WHERE r.codigo IN ('DEMO-ACE5W30','DEMO-FILTACE','DEMO-FILTAIRE','DEMO-PASTDEL','DEMO-DISCFRE','DEMO-BUJIA','DEMO-BATERIA','DEMO-FAJALT','DEMO-REFRI','DEMO-TERMOST','DEMO-AMORT','DEMO-FILTAC');
INSERT INTO movimientos(repuesto_id,usuario_id,cantidad,motivo,fecha)
 SELECT repuesto_id,@bodega_demo,-cantidad,CONCAT('Despacho solicitud #',id),DATE_ADD(fecha,INTERVAL 1 HOUR)
 FROM solicitudes WHERE orden_id IN (@o5_demo,@o7_demo,@o8_demo,@o9_demo,@o10_demo) AND estado='DESPACHADO';

INSERT INTO facturas(orden_id,cliente_nombre,cliente_nit,mano_obra,repuestos,total,fecha)
 SELECT o.id,c.nombre,c.nit,o.mano_obra,SUM(s.cantidad*s.precio_unitario),o.mano_obra+SUM(s.cantidad*s.precio_unitario),DATE_ADD(o.ingreso,INTERVAL 5 HOUR)
 FROM ordenes o JOIN vehiculos v ON v.id=o.vehiculo_id JOIN clientes c ON c.id=v.cliente_id
 JOIN solicitudes s ON s.orden_id=o.id AND s.estado='DESPACHADO'
 WHERE o.id IN (@o8_demo,@o9_demo,@o10_demo)
 GROUP BY o.id,c.nombre,c.nit,o.mano_obra,o.ingreso;

INSERT INTO historial(usuario_id,accion,detalle,fecha)
 SELECT @recepcion_demo,'INGRESO',CONCAT('Orden #',id,' · ingreso de demostración'),ingreso
 FROM ordenes WHERE id IN (@o1_demo,@o2_demo,@o3_demo,@o4_demo,@o5_demo,@o6_demo,@o7_demo,@o8_demo,@o9_demo,@o10_demo);
INSERT INTO historial(usuario_id,accion,detalle,fecha)
 SELECT usuario_id,'DIAGNOSTICO',CONCAT('Orden #',orden_id),fecha FROM diagnosticos
 WHERE orden_id IN (@o3_demo,@o4_demo,@o5_demo,@o6_demo,@o7_demo,@o8_demo,@o9_demo,@o10_demo);
INSERT INTO historial(usuario_id,accion,detalle,fecha)
 SELECT o.mecanico_id,'SOLICITUD_REPUESTO',CONCAT('Solicitud #',s.id,', orden #',o.id),s.fecha
 FROM solicitudes s JOIN ordenes o ON o.id=s.orden_id
 WHERE o.id IN (@o3_demo,@o4_demo,@o5_demo,@o6_demo,@o7_demo,@o8_demo,@o9_demo,@o10_demo);
INSERT INTO historial(usuario_id,accion,detalle,fecha)
 SELECT @bodega_demo,estado,CONCAT('Solicitud #',id),DATE_ADD(fecha,INTERVAL 1 HOUR) FROM solicitudes
 WHERE orden_id IN (@o4_demo,@o5_demo,@o7_demo,@o8_demo,@o9_demo,@o10_demo) AND estado<>'PENDIENTE';
INSERT INTO historial(usuario_id,accion,detalle,fecha)
 SELECT mecanico_id,'ESTADO_REPARACION',CONCAT('Orden #',id,': EN_REPARACION'),DATE_ADD(ingreso,INTERVAL 2 HOUR)
 FROM ordenes WHERE id IN (@o5_demo,@o6_demo,@o7_demo,@o8_demo,@o9_demo,@o10_demo);
INSERT INTO historial(usuario_id,accion,detalle,fecha)
 SELECT mecanico_id,'ESTADO_REPARACION',CONCAT('Orden #',id,': TERMINADO'),DATE_ADD(ingreso,INTERVAL 4 HOUR)
 FROM ordenes WHERE id IN (@o8_demo,@o9_demo,@o10_demo);
INSERT INTO historial(usuario_id,accion,detalle,fecha)
 SELECT @recepcion_demo,'FACTURA_EMITIDA',CONCAT('Factura #',id,', orden #',orden_id),fecha
 FROM facturas WHERE orden_id IN (@o8_demo,@o9_demo,@o10_demo);
INSERT INTO historial(usuario_id,accion,detalle,fecha)
 SELECT @recepcion_demo,'SALIDA',CONCAT('Orden #',id),salida FROM ordenes WHERE id IN (@o9_demo,@o10_demo);
INSERT INTO historial(usuario_id,accion,detalle,fecha)
 VALUES (@recepcion_demo,'CARGA_DEMO','Datos ficticios de demostración v1',@ahora_demo);
