# Taller mecánico

Proyecto II de Desarrollo Web. Aplicación Java con Spring Boot, MySQL y una interfaz HTML/JavaScript con Bootstrap 5.3.8, sin CSS personalizado. Bootstrap se carga desde CDN y requiere conexión a Internet en el navegador.

## Documentación

- [Manual de usuario en Word](documentacion/Manual_de_usuario_Taller_mecanico.docx): instalación, ejecución en IntelliJ y operación por roles.
- [Colección Bruno](bruno/README.md): 39 solicitudes para ejecutar toda la API con sesión, CSRF e IDs automáticos.
- [Guía para explicar el código](GUIA_EXPLICACION_CODIGO.md): directorios, archivos, métodos de ejemplo y recorrido sugerido para la exposición.
- [Requerimientos](REQUERIMIENTOS.md): alcance acordado y condición de no sobreingeniería.
- [Manual de instalación y usuario](MANUAL_USUARIO.md): requisitos, configuración y uso de los cuatro roles.
- [API REST](API.md): rutas, permisos y ejemplos.

## Inicio rápido

Necesita **JDK 21, Maven 3.9 y MySQL 8** en ejecución. Desde esta carpeta, en PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1' # Ajustar a su instalación
$env:DB_USER = 'root'
$env:DB_PASSWORD = Read-Host 'Contraseña de MySQL' -MaskInput
$env:ADMIN_PASSWORD = Read-Host 'Contraseña inicial de admin (8 a 72 caracteres)' -MaskInput
mvn spring-boot:run
```

Abrir `http://localhost:8081`. El puerto predeterminado es **8081** y puede cambiarse con la variable `PORT`. El usuario inicial es `admin`. La contraseña es la indicada en `ADMIN_PASSWORD` en la primera ejecución. La aplicación crea la base `taller_mecanico` y sus tablas si el usuario de MySQL tiene los permisos necesarios. El manual explica cómo crear la base previamente si se prefiere.

En la ejecución normal, el administrador crea los usuarios de recepción, mecánica y bodega desde la interfaz. Para preparar una presentación puede activar la carga opcional de datos ficticios descrita a continuación.

## Datos iniciales para la demostración

En IntelliJ, agregue **`DEMO_DATA=true`** en **Run → Edit Configurations → Environment variables**, y vuelva a ejecutar `Aplicacion`. Utiliza la misma conexión MySQL configurada para la aplicación; no cambia el puerto web `8081`.

La carga agrega:

- 10 clientes y 10 vehículos de diferentes marcas, con información ficticia de apariencia realista.
- 10 órdenes: 2 recibidas, 2 diagnosticadas, 3 en reparación, 1 terminada y 2 entregadas.
- 8 diagnósticos, 12 repuestos, 15 solicitudes y 21 movimientos de inventario.
- 3 comprobantes internos y el historial correspondiente.
- 4 usuarios operativos para mostrar las vistas de cada rol.

| Usuario de demostración | Rol |
| --- | --- |
| `demo_recepcion` | Recepción |
| `demo_mecanico1` | Mecánico: Carlos Estuardo Méndez |
| `demo_mecanico2` | Mecánico: José Andrés Vásquez |
| `demo_bodega` | Bodeguero |

La contraseña de estas cuatro cuentas ficticias es **`DemoTaller2026!`**. El administrador existente conserva su usuario y contraseña. Si es una base nueva, también necesita definir `ADMIN_PASSWORD` para crear primero al administrador.

La carga se guarda completa en una transacción y se marca en el historial como `CARGA_DEMO`. Reiniciar con `DEMO_DATA=true` no vuelve a insertar ni modifica lo cargado; tampoco rellena el inventario consumido durante la presentación. Puede quitar la variable después de la primera carga: los registros permanecen en MySQL.

El cargador no borra ni actualiza registros existentes. Si hay una coincidencia de usuario, placa o código único con los datos del script, la carga se revierte completa y la consola indica el conflicto. Los códigos de repuestos comienzan con `DEMO-` para distinguirlos. El SQL está en `src/main/resources/datos-demo.sql` y se ejecuta desde Java; no se importa directamente en Workbench porque el cargador genera las contraseñas BCrypt.

## Organización

```text
src/main/java/gt/edu/umg/taller/
  Aplicacion.java       Inicio de Spring Boot
  Seguridad.java        Sesión, contraseñas y administrador inicial
  DatosDemostracion.java Carga opcional de información ficticia
  Api.java              Endpoints REST
  TallerServicio.java   Consultas SQL, permisos y reglas del taller
  Datos.java            Datos de entrada y validaciones
  Errores.java          Respuestas de error
src/main/resources/
  application.properties  Configuración mediante variables de entorno
  schema.sql              Tablas y relaciones
  datos-demo.sql          Registros opcionales para presentación
  static/                 Interfaz HTML y JavaScript con Bootstrap
src/test/java/gt/edu/umg/taller/
  FlujoTallerTest.java     Pruebas de integración con MySQL real
```

Es una sola aplicación. La interfaz consume `/api` mediante `fetch`; el servicio valida el rol vigente y las reglas del proceso. JDBC usa consultas parametrizadas. Las contraseñas se almacenan con BCrypt y la sesión utiliza cookies HttpOnly y protección CSRF.

## Reglas principales

- Flujo: **Recibido → Diagnosticado → En reparación → Terminado → Entregado**.
- Un vehículo solo puede tener una orden sin entregar.
- El mecánico trabaja únicamente sus órdenes asignadas. El administrador tiene acceso global.
- Se consulta stock antes de solicitar un repuesto y se comprueba nuevamente al despacharlo.
- Una solicitud pendiente no reserva ni descuenta inventario. El despacho guarda el precio utilizado y descuenta stock una sola vez, dentro de una transacción con bloqueo de registros.
- No se termina una reparación con solicitudes pendientes.
- La factura interna suma mano de obra y repuestos despachados. Solo se emite una por orden; debe existir antes de entregar el vehículo.
- La salida registra fecha y hora. Las órdenes cerradas conservan su historial.
- La facturación es un comprobante interno; el proyecto no incluye FEL, cobros ni servicios tributarios externos.

## Compilar y ejecutar el JAR

```powershell
mvn package
java -jar target/taller-mecanico-1.0.0.jar
```

Las variables de configuración deben estar definidas en la terminal que ejecuta el JAR. `mvn package` compila y ejecuta las pruebas habilitadas; las de integración se omiten cuando no se define `TEST_DB_URL`.

## Pruebas de integración

Usar exclusivamente una base descartable cuyo nombre termine en `_pruebas`. Las pruebas borran los registros de esa base antes de cada caso.

```powershell
$env:TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3306/taller_mecanico_pruebas?createDatabaseIfNotExist=true&connectionTimeZone=UTC'
$env:DB_USER = 'root'
$env:DB_PASSWORD = Read-Host 'Contraseña de MySQL' -MaskInput
mvn test
Remove-Item Env:TEST_DB_URL
```

La suite cubre el flujo completo, autenticación y CSRF, permisos por rol, órdenes ajenas, validaciones de estados, insuficiencia de stock, rechazo de solicitudes, doble despacho, despachos concurrentes, facturación, entrega, configuración y cambios de rol en sesiones existentes.

Verificado el 17 de octubre de 2026 con Java 21 y MySQL 8.0.42: **5 pruebas ejecutadas, 0 fallos, 0 errores** y JAR compilado. Se recorrió el flujo en navegador y se revisó la vista móvil a 390 píxeles. La captura `documentacion/comprobante-prueba.jpg` corresponde a datos ficticios de esa verificación; esos datos no se cargan en las instalaciones nuevas.

La actualización del 24 de octubre de 2026 incorpora la carga opcional de demostración: **7 pruebas ejecutadas, 0 fallos y 0 errores** con MySQL 8.0.42. Se comprobaron además los saldos del inventario, los totales de los comprobantes, la conservación de registros existentes, la segunda carga sin duplicados y la reversión completa ante un conflicto.
