# Manual de instalación y uso

## 1. Qué necesita

- Un equipo con **JDK 21**, **Apache Maven 3.9** y **MySQL Server 8**.
- Un navegador actualizado.
- Conexión a Internet para descargar dependencias la primera vez que se compila.
- La carpeta completa `taller-mecanico`.

Java ejecuta el backend. MySQL guarda la información. Maven descarga las bibliotecas y compila el proyecto. No se necesita Apache, PHP ni Node.js para ejecutar la aplicación.

Compruebe las herramientas en una terminal:

```powershell
java -version
mvn -version
```

Ambos comandos deben utilizar Java 21. Si hay varias versiones, configure `JAVA_HOME` con la carpeta de JDK 21 y agregue su directorio `bin` al `PATH`. Maven también debe estar en el `PATH`.

## 2. Preparar MySQL

Inicie MySQL Server. Los valores predeterminados de conexión son:

| Dato | Valor |
| --- | --- |
| Servidor | `127.0.0.1` |
| Puerto | `3306` |
| Base de datos | `taller_mecanico` |
| Usuario | `root` |
| Contraseña | La de su instalación; configurar `DB_PASSWORD` |

Si el usuario tiene permiso para crear bases, la aplicación crea la base al conectarse. Para prepararla manualmente, abra MySQL Workbench, conecte con un usuario autorizado y ejecute:

```sql
CREATE DATABASE IF NOT EXISTS taller_mecanico
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Las tablas y relaciones se crean automáticamente al iniciar, a partir de `src/main/resources/schema.sql`. El script usa `CREATE TABLE IF NOT EXISTS`: reiniciar la aplicación no borra los registros.

Para una instalación compartida puede usar un usuario MySQL dedicado con permisos sobre esta base, indicando sus credenciales en `DB_USER` y `DB_PASSWORD`. El arranque necesita crear las tablas si aún no existen.

## 3. Configurar e iniciar por primera vez

Abra PowerShell en la carpeta que contiene `pom.xml` y defina las variables:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1' # Cambiar según la instalación
$env:DB_USER = 'root'
$env:DB_PASSWORD = Read-Host 'Contraseña de MySQL' -MaskInput
$env:ADMIN_PASSWORD = Read-Host 'Contraseña inicial del administrador' -MaskInput
mvn spring-boot:run
```

`Read-Host -MaskInput` está disponible en PowerShell 7. En otras terminales puede configurar las variables mediante la configuración de ejecución del IDE.

La contraseña inicial debe tener entre 8 y 72 caracteres y ocupar como máximo 72 bytes UTF-8. No hay una contraseña predeterminada. Cuando la base está vacía, se crea un único usuario administrador con esa contraseña. El nombre de usuario predeterminado es `admin`; puede cambiarlo con `ADMIN_USER` antes del primer arranque.

Espere el mensaje `Started Aplicacion`. Luego abra:

```text
http://localhost:8081
```

Inicie sesión con `admin` y la contraseña elegida. La primera descarga de dependencias puede tardar varios minutos. Para detener la aplicación, presione `Ctrl+C` en la terminal donde está ejecutándose.

### Otras configuraciones

| Variable | Uso |
| --- | --- |
| `DB_URL` | URL JDBC completa si cambian el servidor, puerto o nombre de base. |
| `DB_USER` | Usuario de MySQL. |
| `DB_PASSWORD` | Contraseña de MySQL. |
| `ADMIN_USER` | Nombre del administrador inicial; predeterminado `admin`. |
| `ADMIN_PASSWORD` | Contraseña del administrador inicial; obligatoria al crear la primera cuenta. |
| `PORT` | Puerto web; predeterminado `8081`. |
| `DEMO_DATA` | `true` carga los datos ficticios de presentación una sola vez; predeterminado `false`. |

Ejemplo para otro puerto de MySQL:

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3307/taller_mecanico?createDatabaseIfNotExist=true&connectionTimeZone=UTC'
```

Las variables se establecen antes de iniciar Java y se aplican en esa terminal. En siguientes ejecuciones no es necesario definir `ADMIN_PASSWORD` si ya hay usuarios: no se vuelve a crear el administrador ni se cambia su contraseña. Mantenga las variables de conexión a MySQL necesarias.

### Abrir en IntelliJ IDEA

1. Inicie **MySQL Server**. La conexión predeterminada utiliza `127.0.0.1:3306`; este puerto de base de datos es independiente del puerto web `8081`.
2. En IntelliJ IDEA, seleccione **File → Open** y abra la carpeta `taller-mecanico`, que contiene `pom.xml`. Si se solicita, cárguelo como proyecto Maven y espere la descarga de dependencias.
3. En **File → Project Structure → Project**, seleccione **JDK 21** como SDK. Si no aparece, agréguelo desde su carpeta de instalación.
4. Abra `src/main/java/gt/edu/umg/taller/Aplicacion.java` y use el triángulo verde junto al método `main` para crear o ejecutar su configuración. Si el primer intento falla por falta de contraseña inicial, configure las variables del paso siguiente y vuelva a ejecutarlo.
5. Entre en **Run → Edit Configurations** y seleccione la configuración de `Aplicacion`. También puede crear una de tipo **Application** con la clase principal `gt.edu.umg.taller.Aplicacion`, JDK 21 y el módulo `taller-mecanico` en el classpath.
6. En **Environment variables**, agregue estas variables como filas separadas. Si el campo está oculto, búsquelo en **Modify options → Environment variables**:

   | Variable | Valor que debe ingresar |
   | --- | --- |
   | `PORT` | `8081` |
   | `DB_USER` | Su usuario MySQL; por ejemplo, `root`. |
   | `DB_PASSWORD` | La contraseña de ese usuario MySQL. Si no tiene contraseña, puede omitir esta variable. |
   | `ADMIN_PASSWORD` | Una contraseña que usted elija para el primer administrador, con al menos 8 caracteres y máximo 72 bytes UTF-8. |

   No copie los textos explicativos de la tabla como contraseñas. `DB_PASSWORD` es la contraseña existente de MySQL; `ADMIN_PASSWORD` es la que usted elige para entrar a la aplicación. Las variables configuradas en PowerShell no se transfieren automáticamente a un IntelliJ que ya estaba abierto.

7. Guarde con **Apply → OK** y ejecute la configuración con **Run**. Espere que la consola indique `Tomcat started on port 8081` y `Started Aplicacion`.
8. Abra `http://localhost:8081`. Ingrese con el usuario `admin` y la contraseña elegida en `ADMIN_PASSWORD`. Esta variable solo crea la cuenta cuando la base está vacía; cambiarla después no cambia una contraseña ya guardada.

Si MySQL está en otro servidor o puerto, agregue también `DB_URL` con la URL JDBC correspondiente, como en el ejemplo anterior. El proyecto no necesita usar el puerto `8080`.

## 4. Preparación por el administrador

### Cargar información inicial para una presentación

Para mostrar las listas con datos, agregue `DEMO_DATA=true` en **Run → Edit Configurations → Environment variables** y reinicie la aplicación desde IntelliJ. Mantenga las variables de conexión a MySQL. En una base sin usuarios también debe indicar `ADMIN_PASSWORD` para crear al administrador antes de la carga.

Espere el mensaje `Datos ficticios cargados: 10 clientes, 10 vehículos, 10 órdenes y 12 repuestos.` Después ingrese con su administrador habitual y abra las listas.

Se incluyen clientes con nombres y teléfonos ficticios, vehículos de distintas marcas, órdenes en las cinco etapas, diagnósticos, repuestos con stock normal, bajo y agotado, solicitudes pendientes, despachadas y rechazadas, movimientos de bodega, tres comprobantes internos e historial de operaciones. Las fechas se calculan respecto al día de la carga.

Puede entrar a las vistas operativas usando `demo_recepcion`, `demo_mecanico1`, `demo_mecanico2` o `demo_bodega`. Las cuatro cuentas de demostración utilizan la contraseña `DemoTaller2026!`. Los dos mecánicos tienen cinco órdenes asignadas cada uno. La cuenta de administrador existente no se modifica.

La carga es opcional, no elimina datos existentes y no se repite al reiniciar. Puede retirar `DEMO_DATA` de la configuración después de cargar: la información permanece. Las operaciones que realice con esos registros funcionan normalmente y sus cambios se conservan. Por ejemplo, hay dos solicitudes pendientes para los mismos dos discos de freno: despachar una deja la otra sin stock y permite demostrar la validación de bodega.

Si aparece un conflicto por placa, usuario o código duplicado, no queda una carga parcial: la transacción se revierte. Revise la coincidencia antes de volver a intentarlo; no borre registros de trabajo para forzar la carga.

### Datos del taller

Abra **Configuración** y guarde el nombre del taller, teléfono y dirección. Estos datos aparecen en la aplicación y en la impresión de comprobantes.

### Crear usuarios y asignar permisos

Abra **Usuarios y roles → Crear usuario**. Registre nombre, usuario, contraseña y rol. El usuario debe tener de 3 a 60 letras, números, puntos, guiones o guiones bajos. La contraseña debe tener al menos 8 caracteres y un máximo de 72 bytes UTF-8.

Cree al menos un usuario por cada rol operativo:

| Rol | Funciones |
| --- | --- |
| Administrador | Todas las operaciones, usuarios, asignación de roles, configuración e historial. |
| Recepción | Clientes, vehículos, ingreso, asignación de mecánico, facturación y entrega. |
| Mecánico | Diagnósticos, consulta y solicitud de repuestos, y actualización de sus reparaciones asignadas. |
| Bodeguero | Repuestos, entradas, existencias, resolución de solicitudes y movimientos de inventario. |

Los permisos son fijos por rol. **Cambiar rol** modifica los permisos del usuario. No puede cambiar su propio rol. Para retirar el rol de un mecánico, primero reasigne o entregue sus órdenes abiertas. Después de cambiar un rol, el usuario debe cerrar sesión y volver a ingresar para actualizar el menú; el backend aplica los permisos vigentes inmediatamente.

## 5. Recepción: registrar el ingreso

1. En **Clientes**, seleccione **Registrar cliente** e indique nombre, NIT (o `CF`) y teléfono.
2. En **Vehículos**, seleccione **Registrar vehículo**, elija el cliente y registre placa, marca, modelo y año.
3. En **Órdenes de trabajo**, seleccione **Registrar ingreso**.
4. Seleccione el vehículo y un mecánico responsable e indique el motivo de ingreso.
5. Guarde. La orden queda en **Recibido**, con fecha y hora de ingreso.

Un vehículo no puede tener dos órdenes sin entregar. El cliente y el usuario mecánico deben existir antes del ingreso. Si necesita otro responsable, abra la orden y seleccione **Reasignar mecánico** antes de que la reparación termine.

## 6. Mecánico: diagnóstico y reparación

1. Inicie sesión con un usuario Mecánico. **Órdenes de trabajo** muestra únicamente sus órdenes asignadas.
2. Abra una orden y seleccione **Agregar diagnóstico**.
3. Describa las fallas detectadas y la reparación requerida. Al guardar el primer diagnóstico, la orden pasa a **Diagnosticado**.
4. Si necesita repuestos, seleccione **Solicitar repuesto**, elija un repuesto e indique la cantidad.
5. Pulse **Consultar existencia** para consultar el stock actual a través de la API. Al guardar, el sistema vuelve a comprobar que hay existencias suficientes.
6. Seleccione **Iniciar reparación**. La orden pasa a **En reparación**.
7. Al completar el trabajo, seleccione **Terminar reparación** y registre el importe final de mano de obra en quetzales. La orden pasa a **Terminado**.

Puede agregar diagnósticos mientras la orden esté abierta. No puede solicitar repuestos antes del diagnóstico, modificar órdenes de otro mecánico, saltar estados ni terminar con solicitudes pendientes. Los repuestos rechazados no se cobran; solo se suman los despachados.

## 7. Bodega: inventario y despacho

### Registrar repuestos y entradas

1. Abra **Repuestos → Registrar repuesto**.
2. Indique código único, nombre, precio unitario, existencia inicial y existencia mínima.
3. Para una compra o reposición posterior, pulse **Registrar entrada**, indique una cantidad positiva y un motivo o referencia.

Las existencias iguales o inferiores al mínimo aparecen como **Stock bajo**. Los importes y cantidades no pueden ser negativos. Las entradas y despachos quedan registrados en **Movimientos de bodega**.

### Resolver solicitudes

1. Abra **Despachos**.
2. Busque una solicitud pendiente y seleccione **Resolver solicitud**.
3. Seleccione **Despachar repuestos** o **Rechazar solicitud** y guarde.

Una solicitud pendiente no reserva stock. El sistema verifica otra vez las existencias al despachar y descuenta la cantidad de forma segura. Si otra operación agotó el inventario, el despacho se rechaza sin descontar nada. Puede registrar una entrada y reintentar, o rechazar la solicitud.

Una solicitud resuelta no se puede despachar otra vez. El precio se guarda al despachar para calcular el comprobante de la reparación.

## 8. Recepción: facturar y entregar

1. Abra una orden en estado **Terminado**.
2. Seleccione **Emitir factura** y confirme guardando el formulario.
3. El sistema suma mano de obra y repuestos despachados. La factura queda guardada con los datos del cliente y sus importes.
4. En **Facturación**, puede abrir el comprobante y seleccionar **Imprimir**; el navegador también permite guardarlo como PDF.
5. Regrese a la orden y seleccione **Registrar salida**. La orden pasa a **Entregado** y se guarda la fecha y hora de salida.

Se emite una sola factura por orden. No se puede registrar la salida antes de terminar y facturar la reparación. Una orden terminada o entregada conserva sus datos y ya no admite nuevos diagnósticos ni solicitudes.

La factura del proyecto es un **comprobante interno**, en quetzales. No incluye facturación electrónica FEL, pagos ni integración con SAT.

## 9. Historial y consultas

El administrador puede abrir **Historial** para ver usuario, fecha, acción y detalle de las operaciones. Las tablas con registros incluyen un campo de búsqueda. El detalle de cada orden conserva diagnósticos, solicitudes y referencia de factura.

Use **Cerrar sesión** al terminar. La sesión expira tras 30 minutos de inactividad. Si expira mientras trabaja, vuelva a iniciar sesión; un formulario que no llegó a guardarse debe completarse otra vez.

## 10. Compilar para otra computadora

Desde la carpeta del proyecto:

```powershell
mvn package
```

Se genera `target/taller-mecanico-1.0.0.jar`. En el equipo destino necesita Java 21 y acceso a MySQL. Configure las variables del apartado 3 y ejecute:

```powershell
java -jar taller-mecanico-1.0.0.jar
```

No necesita Maven en el equipo destino si utiliza el JAR ya compilado. El archivo SQL y la interfaz están incluidos en el JAR. Las pruebas de integración requieren una base descartable y se habilitan como indica el `README.md`; sin `TEST_DB_URL`, Maven las omite.

## 11. Solución de problemas

| Mensaje o situación | Solución |
| --- | --- |
| `java` o `mvn` no se reconoce | Instale la herramienta y agregue su carpeta `bin` al `PATH`; abra una terminal nueva. |
| Versión de Java incompatible | Configure JDK 21 en `JAVA_HOME` y en el IDE. |
| No se puede conectar a MySQL | Compruebe que MySQL está iniciado y revise host, puerto, usuario y contraseña. |
| `Access denied` en MySQL | Revise `DB_USER`, `DB_PASSWORD` y sus permisos sobre la base. |
| No se puede crear la base | Créela en Workbench como indica el apartado 2 y conceda los permisos necesarios. |
| Se solicita `ADMIN_PASSWORD` | Es el primer arranque sobre una base sin usuarios. Defina una contraseña válida y vuelva a iniciar. |
| Puerto web ocupado | El proyecto usa `8081`. Si también está ocupado, defina, por ejemplo, `PORT=8082` en la configuración de ejecución y abra `http://localhost:8082`. |
| Usuario o contraseña incorrectos | Use la cuenta creada por el administrador; en el primer arranque, `admin` y el valor que indicó en `ADMIN_PASSWORD`. |
| No hay mecánicos para asignar | El administrador debe crear un usuario con rol Mecánico. |
| El vehículo ya tiene una orden sin entregar | Complete y entregue la orden existente antes de registrar otro ingreso. |
| Stock insuficiente | Registre una entrada de bodega, reduzca la cantidad solicitada o rechace la solicitud según corresponda. |
| Hay solicitudes pendientes al terminar | Bodega debe despacharlas o rechazarlas. |
| Acceso denegado o sesión caducada | Revise el rol y vuelva a iniciar sesión; recargue si el formulario conserva un token de sesión anterior. |

## 12. Comprobación manual de instalación

Después de instalar, complete este recorrido con datos de prueba: cree los tres usuarios operativos, registre un repuesto, registre cliente y vehículo, abra una orden, diagnostique, solicite el repuesto, despáchelo, inicie y termine la reparación, emita el comprobante y registre la salida. Compruebe el stock restante y el historial desde la cuenta Administrador.
