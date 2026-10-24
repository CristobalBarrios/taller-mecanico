# API REST del taller

Base: `http://localhost:8081/api`. Las respuestas son JSON, salvo las operaciones sin contenido. La interfaz usa esta misma API.

Para ejecutar el recorrido completo, abra la [colección Bruno](bruno/README.md): incluye las rutas siguientes, autenticación, CSRF, captura de IDs y comprobaciones de resultados.

## Autenticación

Se usa una sesión del servidor con cookie `JSESSIONID`, no JWT. Conserve las cookies entre solicitudes.

1. `GET /csrf`: devuelve `{ "token": "...", "header": "X-CSRF-TOKEN" }` y establece la sesión.
2. `POST /login`: enviar `application/x-www-form-urlencoded` con `username` y `password`, además de la cabecera CSRF indicada en el paso anterior.
3. Después del login, consultar nuevamente `GET /csrf`: el token cambia al autenticarse.
4. Enviar la cookie de sesión y la cabecera CSRF en cada `POST` y `PUT`.
5. `POST /logout`: cierra la sesión, también requiere CSRF.

No desactive CSRF para usar Postman: conserve su almacén de cookies y obtenga el token como se describe arriba.

## Rutas y permisos

El Administrador puede realizar todas las operaciones. En la tabla se indican los demás roles autorizados. El Mecánico solo tiene acceso a las órdenes que tiene asignadas.

| Método | Ruta | Rol operativo / función |
| --- | --- | --- |
| GET | `/csrf` | Público: token CSRF. |
| POST | `/login` | Público con CSRF: iniciar sesión. |
| POST | `/logout` | Cerrar sesión. |
| GET | `/yo` | Cualquier usuario autenticado: sus datos y rol vigente. |
| GET, POST | `/clientes` | Recepción: listar / registrar. |
| GET, POST | `/vehiculos` | Recepción: listar / registrar. |
| GET | `/mecanicos` | Recepción: mecánicos disponibles para asignación. |
| GET, POST | `/ordenes` | Recepción: listar / ingresar. Mecánico: solo listar sus órdenes. |
| GET | `/ordenes/{id}` | Recepción, Mecánico: detalle con diagnósticos, solicitudes y factura. |
| PUT | `/ordenes/{id}/mecanico` | Recepción: reasignar responsable mientras esté abierta. |
| POST | `/ordenes/{id}/diagnosticos` | Mecánico: registrar diagnóstico. |
| PUT | `/ordenes/{id}/estado` | Mecánico: iniciar o terminar reparación. |
| POST | `/ordenes/{id}/solicitudes` | Mecánico: solicitar repuesto tras comprobar stock. |
| POST | `/ordenes/{id}/factura` | Recepción: emitir comprobante de una reparación terminada. |
| POST | `/ordenes/{id}/entrega` | Recepción: registrar salida de una orden terminada y facturada. |
| GET | `/repuestos` | Mecánico, Bodeguero: inventario actual. |
| POST | `/repuestos` | Bodeguero: registrar repuesto. |
| GET | `/repuestos/{id}/existencia?cantidad=1` | Mecánico, Bodeguero: comprobar cantidad disponible en tiempo real. |
| POST | `/repuestos/{id}/entradas` | Bodeguero: entrada positiva de inventario. |
| GET | `/solicitudes` | Bodeguero: solicitudes de todas las órdenes. |
| PUT | `/solicitudes/{id}` | Bodeguero: despachar o rechazar una solicitud pendiente. |
| GET | `/movimientos` | Bodeguero: entradas y despachos de inventario. |
| GET | `/facturas` | Recepción: comprobantes emitidos. |
| GET, POST | `/usuarios` | Solo Administrador: listar / crear usuarios. |
| PUT | `/usuarios/{id}/rol` | Solo Administrador: asignar rol. |
| GET | `/configuracion` | Cualquier usuario autenticado: datos del taller. |
| PUT | `/configuracion` | Solo Administrador: actualizar datos del taller. |
| GET | `/historial` | Solo Administrador: historial de operaciones. |

## Ejemplos de cuerpos JSON

Los IDs de ejemplo deben sustituirse por los devueltos al registrar los datos.

### Crear usuario

```json
{"nombre":"Mecánico de ejemplo","usuario":"mecanico1","clave":"UnaClaveDeEjemplo123","rol":"MECANICO"}
```

Roles válidos: `ADMINISTRADOR`, `RECEPCION`, `MECANICO`, `BODEGUERO`.

### Cliente y vehículo

```json
{"nombre":"Cliente de ejemplo","nit":"CF","telefono":"55550000"}
```

```json
{"clienteId":1,"placa":"P123ABC","marca":"Toyota","modelo":"Corolla","anio":2020}
```

### Ingreso y reasignación

```json
{"vehiculoId":1,"mecanicoId":2,"motivo":"Ruido al frenar"}
```

```json
{"mecanicoId":3}
```

### Diagnóstico y cambio de estado

```json
{"descripcion":"Pastillas de freno desgastadas","reparacion":"Cambiar pastillas y revisar discos"}
```

```json
{"estado":"EN_REPARACION","manoObra":0}
```

```json
{"estado":"TERMINADO","manoObra":150.00}
```

### Repuesto, entrada, solicitud y despacho

```json
{"codigo":"FRE-001","nombre":"Pastillas de freno","precio":75.50,"stock":10,"minimo":2}
```

```json
{"cantidad":5,"motivo":"Compra a proveedor"}
```

```json
{"repuestoId":1,"cantidad":2}
```

```json
{"estado":"DESPACHADO"}
```

Para rechazar: `{"estado":"RECHAZADO"}`. La consulta previa no reserva stock. El despacho vuelve a verificar existencias en una transacción y conserva el precio unitario aplicado.

### Configuración y rol

```json
{"nombre":"Taller mecánico","telefono":"55550000","direccion":"Guatemala"}
```

```json
{"rol":"RECEPCION"}
```

Facturación y entrega usan `POST` sin cuerpo.

## Respuestas

- `200`: consulta exitosa o login.
- `201`: registro creado; normalmente devuelve `{"id":1}`. Diagnóstico no devuelve cuerpo y factura devuelve sus datos.
- `204`: actualización, entrega, entrada, despacho o logout sin cuerpo.
- `400`: datos inválidos.
- `401`: falta de sesión o credenciales incorrectas.
- `403`: rol no autorizado, orden de otro mecánico o token CSRF inválido.
- `404`: registro o referencia inexistente.
- `409`: duplicado, falta de existencias o regla del proceso incumplida.
- `500`: error inesperado, registrado en el servidor sin exponer detalles internos.

Los errores de validación y negocio devuelven `{"error":"Descripción del problema"}`. Los errores generados por el filtro de seguridad usan el formato de error HTTP de Spring.
