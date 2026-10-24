package gt.edu.umg.taller;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Reglas del taller. Las escrituras y su historial se guardan en la misma transacción. */
@Service
@Transactional(readOnly = true)
public class TallerServicio {
    private final JdbcTemplate db;
    private final PasswordEncoder claves;
    public TallerServicio(JdbcTemplate db, PasswordEncoder claves) { this.db = db; this.claves = claves; }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
    private Map<String,Object> uno(String sql, Object... args) {
        var filas = db.queryForList(sql, args);
        if (filas.isEmpty()) throw error(HttpStatus.NOT_FOUND, "Registro no encontrado.");
        return filas.getFirst();
    }
    private long numero(Map<String,Object> fila, String campo) { return ((Number) fila.get(campo)).longValue(); }
    private long insertar(String sql, Object... args) {
        var llave = new GeneratedKeyHolder();
        db.update(con -> {
            var ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i=0; i<args.length; i++) ps.setObject(i+1, args[i]);
            return ps;
        }, llave);
        return llave.getKey().longValue();
    }
    public Map<String,Object> actual() {
        String nombre = SecurityContextHolder.getContext().getAuthentication().getName();
        // Consultar el rol vigente evita conservar permisos antiguos en una sesión abierta.
        var filas = db.queryForList("SELECT id,nombre,usuario,rol FROM usuarios WHERE usuario=? AND activo=TRUE", nombre);
        if (filas.isEmpty()) throw error(HttpStatus.UNAUTHORIZED, "Inicie sesión nuevamente.");
        return filas.getFirst();
    }
    private Map<String,Object> permitir(String... roles) {
        var u = actual();
        if (!u.get("rol").equals("ADMINISTRADOR") && !List.of(roles).contains(u.get("rol")))
            throw error(HttpStatus.FORBIDDEN, "Su rol no tiene permiso para esta operación.");
        return u;
    }
    private void registrar(Map<String,Object> u, String accion, String detalle) {
        db.update("INSERT INTO historial(usuario_id,accion,detalle) VALUES(?,?,?)", u.get("id"), accion, detalle);
    }
    private Map<String,Object> orden(long id, Map<String,Object> u) {
        var o = uno("SELECT * FROM ordenes WHERE id=? FOR UPDATE", id);
        if (u.get("rol").equals("MECANICO") && numero(o,"mecanico_id") != numero(u,"id"))
            throw error(HttpStatus.FORBIDDEN, "La orden está asignada a otro mecánico.");
        return o;
    }
    private void abierta(Map<String,Object> o) {
        if (List.of("TERMINADO","ENTREGADO").contains(o.get("estado")))
            throw error(HttpStatus.CONFLICT, "La reparación ya está cerrada.");
    }

    public List<Map<String,Object>> clientes() {
        permitir("RECEPCION");
        return db.queryForList("SELECT * FROM clientes ORDER BY id DESC");
    }
    @Transactional public long crearCliente(Datos.Cliente c) {
        var u = permitir("RECEPCION");
        long id = insertar("INSERT INTO clientes(nombre,nit,telefono) VALUES(?,?,?)", c.nombre().trim(), c.nit().trim(), c.telefono().trim());
        registrar(u,"CLIENTE_CREADO","Cliente #"+id);
        return id;
    }
    public List<Map<String,Object>> vehiculos() {
        permitir("RECEPCION");
        return db.queryForList("SELECT v.*,c.nombre cliente FROM vehiculos v JOIN clientes c ON c.id=v.cliente_id ORDER BY v.id DESC");
    }
    @Transactional public long crearVehiculo(Datos.Vehiculo v) {
        var u = permitir("RECEPCION");
        uno("SELECT id FROM clientes WHERE id=?",v.clienteId());
        long id = insertar("INSERT INTO vehiculos(cliente_id,placa,marca,modelo,anio) VALUES(?,?,?,?,?)",
            v.clienteId(),v.placa().trim().toUpperCase(java.util.Locale.ROOT),v.marca().trim(),v.modelo().trim(),v.anio());
        registrar(u,"VEHICULO_CREADO","Vehículo #"+id);
        return id;
    }
    public List<Map<String,Object>> mecanicos() {
        permitir("RECEPCION");
        return db.queryForList("SELECT id,nombre FROM usuarios WHERE rol='MECANICO' AND activo=TRUE ORDER BY nombre");
    }
    public List<Map<String,Object>> ordenes() {
        var u = permitir("RECEPCION","MECANICO");
        String filtro = u.get("rol").equals("MECANICO") ? " WHERE o.mecanico_id=?" : "";
        Object[] args = filtro.isEmpty() ? new Object[]{} : new Object[]{u.get("id")};
        return db.queryForList("""
            SELECT o.*,v.placa,v.marca,v.modelo,c.nombre cliente,u.nombre mecanico
            FROM ordenes o JOIN vehiculos v ON v.id=o.vehiculo_id
            JOIN clientes c ON c.id=v.cliente_id JOIN usuarios u ON u.id=o.mecanico_id
            """ + filtro + " ORDER BY o.id DESC",args);
    }
    @Transactional public long crearOrden(Datos.Orden o) {
        var u = permitir("RECEPCION");
        // Bloquear el vehículo serializa ingresos simultáneos del mismo vehículo.
        uno("SELECT id FROM vehiculos WHERE id=? FOR UPDATE",o.vehiculoId());
        uno("SELECT id FROM usuarios WHERE id=? AND rol='MECANICO' AND activo=TRUE FOR UPDATE",o.mecanicoId());
        if (db.queryForObject("SELECT COUNT(*) FROM ordenes WHERE vehiculo_id=? AND estado<>'ENTREGADO'",Integer.class,o.vehiculoId()) > 0)
            throw error(HttpStatus.CONFLICT,"El vehículo ya tiene una orden sin entregar.");
        long id = insertar("INSERT INTO ordenes(vehiculo_id,mecanico_id,motivo) VALUES(?,?,?)",o.vehiculoId(),o.mecanicoId(),o.motivo().trim());
        registrar(u,"INGRESO","Orden #"+id);
        return id;
    }
    @Transactional public Map<String,Object> detalle(long id) {
        var u = permitir("RECEPCION","MECANICO");
        var o = orden(id,u);
        var datos = new java.util.LinkedHashMap<String,Object>(o);
        datos.put("diagnosticos",db.queryForList("SELECT d.*,u.nombre mecanico FROM diagnosticos d JOIN usuarios u ON u.id=d.usuario_id WHERE orden_id=? ORDER BY d.id",id));
        datos.put("solicitudes",db.queryForList("SELECT s.*,r.nombre repuesto FROM solicitudes s JOIN repuestos r ON r.id=s.repuesto_id WHERE orden_id=? ORDER BY s.id",id));
        datos.put("facturas",db.queryForList("SELECT * FROM facturas WHERE orden_id=?",id));
        return datos;
    }
    @Transactional public void asignar(long id, Datos.Asignacion a) {
        var u = permitir("RECEPCION");
        var o = orden(id,u); abierta(o);
        uno("SELECT id FROM usuarios WHERE id=? AND rol='MECANICO' AND activo=TRUE FOR UPDATE",a.mecanicoId());
        db.update("UPDATE ordenes SET mecanico_id=? WHERE id=?",a.mecanicoId(),id);
        registrar(u,"MECANICO_ASIGNADO","Orden #"+id+", mecánico #"+a.mecanicoId());
    }
    @Transactional public void diagnosticar(long id, Datos.Diagnostico d) {
        var u = permitir("MECANICO");
        var o = orden(id,u); abierta(o);
        insertar("INSERT INTO diagnosticos(orden_id,usuario_id,descripcion,reparacion) VALUES(?,?,?,?)",id,u.get("id"),d.descripcion().trim(),d.reparacion().trim());
        if (o.get("estado").equals("RECIBIDO")) db.update("UPDATE ordenes SET estado='DIAGNOSTICADO' WHERE id=?",id);
        registrar(u,"DIAGNOSTICO","Orden #"+id);
    }
    @Transactional public void estado(long id, Datos.Estado e) {
        var u = permitir("MECANICO");
        var o = orden(id,u);
        boolean valido = (o.get("estado").equals("DIAGNOSTICADO") && e.estado().equals("EN_REPARACION"))
            || (o.get("estado").equals("EN_REPARACION") && e.estado().equals("TERMINADO"));
        if (!valido) throw error(HttpStatus.CONFLICT,"Transición inválida. El flujo es: recibido, diagnosticado, en reparación, terminado, entregado.");
        if (e.estado().equals("TERMINADO") && db.queryForObject("SELECT COUNT(*) FROM solicitudes WHERE orden_id=? AND estado='PENDIENTE'",Integer.class,id)>0)
            throw error(HttpStatus.CONFLICT,"Resuelva las solicitudes de repuestos pendientes antes de terminar.");
        db.update("UPDATE ordenes SET estado=?,mano_obra=? WHERE id=?",e.estado(),e.manoObra(),id);
        registrar(u,"ESTADO_REPARACION","Orden #"+id+": "+e.estado());
    }
    @Transactional public void entregar(long id) {
        var u = permitir("RECEPCION");
        var o = orden(id,u);
        if (!o.get("estado").equals("TERMINADO")) throw error(HttpStatus.CONFLICT,"Solo se entrega una reparación terminada.");
        if (db.queryForObject("SELECT COUNT(*) FROM facturas WHERE orden_id=?",Integer.class,id)==0)
            throw error(HttpStatus.CONFLICT,"Emita la factura antes de registrar la salida.");
        db.update("UPDATE ordenes SET estado='ENTREGADO',salida=CURRENT_TIMESTAMP WHERE id=?",id);
        registrar(u,"SALIDA","Orden #"+id);
    }

    public List<Map<String,Object>> repuestos() {
        permitir("MECANICO","BODEGUERO");
        return db.queryForList("SELECT * FROM repuestos ORDER BY nombre");
    }
    public Map<String,Object> existencia(long id, int cantidad) {
        permitir("MECANICO","BODEGUERO");
        if (cantidad<1) throw error(HttpStatus.BAD_REQUEST,"La cantidad debe ser positiva.");
        var r = uno("SELECT id,codigo,nombre,stock FROM repuestos WHERE id=?",id);
        return Map.of("repuesto",r,"cantidad",cantidad,"disponible",numero(r,"stock")>=cantidad);
    }
    @Transactional public long crearRepuesto(Datos.Repuesto r) {
        var u = permitir("BODEGUERO");
        long id = insertar("INSERT INTO repuestos(codigo,nombre,precio,stock,minimo) VALUES(?,?,?,?,?)",r.codigo().trim(),r.nombre().trim(),r.precio(),r.stock(),r.minimo());
        if (r.stock()>0) db.update("INSERT INTO movimientos(repuesto_id,usuario_id,cantidad,motivo) VALUES(?,?,?,'Inventario inicial')",id,u.get("id"),r.stock());
        registrar(u,"REPUESTO_CREADO","Repuesto #"+id);
        return id;
    }
    @Transactional public void entrada(long id, Datos.Entrada e) {
        var u = permitir("BODEGUERO");
        var r = uno("SELECT * FROM repuestos WHERE id=? FOR UPDATE",id);
        if (numero(r,"stock")+e.cantidad()>Integer.MAX_VALUE) throw error(HttpStatus.BAD_REQUEST,"La cantidad excede el límite de inventario.");
        db.update("UPDATE repuestos SET stock=stock+? WHERE id=?",e.cantidad(),id);
        db.update("INSERT INTO movimientos(repuesto_id,usuario_id,cantidad,motivo) VALUES(?,?,?,?)",id,u.get("id"),e.cantidad(),e.motivo().trim());
        registrar(u,"ENTRADA_BODEGA","Repuesto #"+id+", cantidad "+e.cantidad());
    }
    @Transactional public long solicitar(long id, Datos.Solicitud s) {
        var u = permitir("MECANICO");
        var o = orden(id,u); abierta(o);
        if (o.get("estado").equals("RECIBIDO")) throw error(HttpStatus.CONFLICT,"Registre primero el diagnóstico.");
        var r = uno("SELECT stock FROM repuestos WHERE id=? FOR UPDATE",s.repuestoId());
        if (numero(r,"stock")<s.cantidad()) throw error(HttpStatus.CONFLICT,"Existencias insuficientes para solicitar el repuesto.");
        long sid = insertar("INSERT INTO solicitudes(orden_id,repuesto_id,cantidad) VALUES(?,?,?)",id,s.repuestoId(),s.cantidad());
        registrar(u,"SOLICITUD_REPUESTO","Solicitud #"+sid+", orden #"+id);
        return sid;
    }
    public List<Map<String,Object>> solicitudes() {
        permitir("BODEGUERO");
        return db.queryForList("""
            SELECT s.*,r.codigo,r.nombre repuesto,v.placa,u.nombre mecanico
            FROM solicitudes s JOIN repuestos r ON r.id=s.repuesto_id
            JOIN ordenes o ON o.id=s.orden_id JOIN vehiculos v ON v.id=o.vehiculo_id
            JOIN usuarios u ON u.id=o.mecanico_id ORDER BY s.id DESC
            """);
    }
    @Transactional public void despachar(long id, Datos.Despacho d) {
        var u = permitir("BODEGUERO");
        // Mantener el orden de bloqueo: orden, solicitud, repuesto.
        var referencia = uno("SELECT orden_id FROM solicitudes WHERE id=?",id);
        var o = orden(numero(referencia,"orden_id"),u); abierta(o);
        var s = uno("SELECT * FROM solicitudes WHERE id=? FOR UPDATE",id);
        if (!s.get("estado").equals("PENDIENTE")) throw error(HttpStatus.CONFLICT,"La solicitud ya fue resuelta.");
        if (d.estado().equals("DESPACHADO")) {
            var r = uno("SELECT * FROM repuestos WHERE id=? FOR UPDATE",s.get("repuesto_id"));
            // Se verifica nuevamente al despachar: una consulta previa no reserva existencias.
            if (numero(r,"stock")<numero(s,"cantidad")) throw error(HttpStatus.CONFLICT,"Existencias insuficientes. Registre una entrada o rechace la solicitud.");
            db.update("UPDATE repuestos SET stock=stock-? WHERE id=?",s.get("cantidad"),s.get("repuesto_id"));
            db.update("UPDATE solicitudes SET precio_unitario=? WHERE id=?",r.get("precio"),id);
            db.update("INSERT INTO movimientos(repuesto_id,usuario_id,cantidad,motivo) VALUES(?,?,?,?)",s.get("repuesto_id"),u.get("id"),-numero(s,"cantidad"),"Despacho solicitud #"+id);
        }
        db.update("UPDATE solicitudes SET estado=? WHERE id=?",d.estado(),id);
        registrar(u,d.estado(),"Solicitud #"+id);
    }
    public List<Map<String,Object>> movimientos() {
        permitir("BODEGUERO");
        return db.queryForList("SELECT m.*,r.nombre repuesto,u.nombre usuario FROM movimientos m JOIN repuestos r ON r.id=m.repuesto_id JOIN usuarios u ON u.id=m.usuario_id ORDER BY m.id DESC");
    }

    @Transactional public Map<String,Object> facturar(long id) {
        var u = permitir("RECEPCION");
        var o = orden(id,u);
        if (!o.get("estado").equals("TERMINADO")) throw error(HttpStatus.CONFLICT,"Solo se factura una reparación terminada.");
        if (db.queryForObject("SELECT COUNT(*) FROM facturas WHERE orden_id=?",Integer.class,id)>0)
            throw error(HttpStatus.CONFLICT,"Esta orden ya tiene factura.");
        var c = uno("SELECT c.nombre,c.nit FROM clientes c JOIN vehiculos v ON v.cliente_id=c.id WHERE v.id=?",o.get("vehiculo_id"));
        BigDecimal repuestos = db.queryForObject("SELECT COALESCE(SUM(cantidad*precio_unitario),0) FROM solicitudes WHERE orden_id=? AND estado='DESPACHADO'",BigDecimal.class,id);
        BigDecimal mano = (BigDecimal) o.get("mano_obra");
        long fid = insertar("INSERT INTO facturas(orden_id,cliente_nombre,cliente_nit,mano_obra,repuestos,total) VALUES(?,?,?,?,?,?)",id,c.get("nombre"),c.get("nit"),mano,repuestos,mano.add(repuestos));
        registrar(u,"FACTURA_EMITIDA","Factura #"+fid+", orden #"+id);
        return uno("SELECT * FROM facturas WHERE id=?",fid);
    }
    public List<Map<String,Object>> facturas() {
        permitir("RECEPCION");
        return db.queryForList("SELECT f.*,v.placa FROM facturas f JOIN ordenes o ON o.id=f.orden_id JOIN vehiculos v ON v.id=o.vehiculo_id ORDER BY f.id DESC");
    }
    public List<Map<String,Object>> usuarios() {
        permitir();
        return db.queryForList("SELECT id,nombre,usuario,rol FROM usuarios ORDER BY id");
    }
    @Transactional public long crearUsuario(Datos.Usuario n) {
        var u = permitir();
        if (n.clave().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw error(HttpStatus.BAD_REQUEST,"La contraseña debe ocupar como máximo 72 bytes UTF-8.");
        long id = insertar("INSERT INTO usuarios(nombre,usuario,clave,rol) VALUES(?,?,?,?)",n.nombre().trim(),n.usuario(),claves.encode(n.clave()),n.rol());
        registrar(u,"USUARIO_CREADO","Usuario #"+id+", rol "+n.rol());
        return id;
    }
    @Transactional public void rol(long id, Datos.Rol r) {
        var u = permitir();
        if (id==numero(u,"id")) throw error(HttpStatus.CONFLICT,"No puede cambiar su propio rol.");
        uno("SELECT id FROM usuarios WHERE id=? FOR UPDATE",id);
        if (!r.rol().equals("MECANICO") && db.queryForObject("SELECT COUNT(*) FROM ordenes WHERE mecanico_id=? AND estado<>'ENTREGADO'",Integer.class,id)>0)
            throw error(HttpStatus.CONFLICT,"Reasigne o entregue las órdenes de este mecánico antes de cambiar su rol.");
        db.update("UPDATE usuarios SET rol=? WHERE id=?",r.rol(),id);
        registrar(u,"ROL_ASIGNADO","Usuario #"+id+": "+r.rol());
    }
    public Map<String,Object> configuracion() {
        actual();
        return uno("SELECT nombre,telefono,direccion FROM configuracion WHERE id=1");
    }
    @Transactional public void configurar(Datos.Configuracion c) {
        var u = permitir();
        db.update("UPDATE configuracion SET nombre=?,telefono=?,direccion=? WHERE id=1",c.nombre().trim(),c.telefono().trim(),c.direccion().trim());
        registrar(u,"CONFIGURACION","Datos del taller actualizados");
    }
    public List<Map<String,Object>> historial() {
        permitir();
        return db.queryForList("SELECT h.*,u.nombre usuario FROM historial h JOIN usuarios u ON u.id=h.usuario_id ORDER BY h.id DESC");
    }
}
