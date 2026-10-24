package gt.edu.umg.taller;

import java.util.Map;
import java.util.concurrent.Executors;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Se ejecuta solo con una URL de MySQL destinada a pruebas; limpia sus tablas antes de cada caso. */
@EnabledIfEnvironmentVariable(named="TEST_DB_URL", matches=".+")
@SpringBootTest(properties={"spring.datasource.url=${TEST_DB_URL}",
    "taller.admin.usuario=prueba_admin", "taller.admin.clave=PruebaAdmin123", "taller.demo=false"})
@AutoConfigureMockMvc
class FlujoTallerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder claves;
    @Autowired PlatformTransactionManager transacciones;
    MockHttpSession admin, recepcion, mecanico, otroMecanico, bodega;
    long mecanicoId;

    @BeforeEach void preparar() throws Exception {
        // Impide borrar datos si por error se proporciona una base que no es de pruebas.
        assertThat(db.queryForObject("SELECT DATABASE()",String.class)).endsWith("_pruebas");
        for (String tabla : new String[]{"historial","facturas","movimientos","solicitudes","diagnosticos","ordenes","vehiculos","clientes","repuestos"}) db.update("DELETE FROM "+tabla);
        db.update("DELETE FROM usuarios WHERE usuario<>'prueba_admin'");
        for(String[] u : new String[][]{{"recepcion","RECEPCION"},{"mecanico","MECANICO"},{"otro","MECANICO"},{"bodega","BODEGUERO"}})
            db.update("INSERT INTO usuarios(nombre,usuario,clave,rol) VALUES(?,?,?,?)",u[0],u[0],claves.encode("PruebaUsuario123"),u[1]);
        mecanicoId=db.queryForObject("SELECT id FROM usuarios WHERE usuario='mecanico'",Long.class);
        admin=login("prueba_admin","PruebaAdmin123");
        recepcion=login("recepcion","PruebaUsuario123"); mecanico=login("mecanico","PruebaUsuario123");
        otroMecanico=login("otro","PruebaUsuario123"); bodega=login("bodega","PruebaUsuario123");
    }
    MockHttpSession login(String usuario,String clave) throws Exception {
        return (MockHttpSession)mvc.perform(post("/api/login").with(csrf()).param("username",usuario).param("password",clave))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    MvcResult enviar(MockHttpSession sesion, String metodo, String ruta, Object cuerpo, int estado) throws Exception {
        MockHttpServletRequestBuilder req=switch(metodo){case "POST"->post(ruta);case "PUT"->put(ruta);default->get(ruta);};
        req.session(sesion).with(csrf());
        if(cuerpo!=null)req.contentType("application/json").content(json.writeValueAsBytes(cuerpo));
        return mvc.perform(req).andExpect(status().is(estado)).andReturn();
    }
    long crear(MockHttpSession sesion,String ruta,Object datos) throws Exception {
        return json.readTree(enviar(sesion,"POST",ruta,datos,201).getResponse().getContentAsString()).get("id").asLong();
    }
    long nuevaOrden(String placa) throws Exception {
        long c=crear(recepcion,"/api/clientes",Map.of("nombre","Cliente prueba","nit","CF","telefono","55550000"));
        long v=crear(recepcion,"/api/vehiculos",Map.of("clienteId",c,"placa",placa,"marca","Toyota","modelo","Corolla","anio",2020));
        return crear(recepcion,"/api/ordenes",Map.of("vehiculoId",v,"mecanicoId",mecanicoId,"motivo","Revisar frenos"));
    }
    void diagnosticar(long o) throws Exception {
        enviar(mecanico,"POST","/api/ordenes/"+o+"/diagnosticos",Map.of("descripcion","Pastillas gastadas","reparacion","Cambiar pastillas"),201);
    }
    long repuesto(int stock) throws Exception {
        return crear(bodega,"/api/repuestos",Map.of("codigo","FRE-01","nombre","Pastillas de freno","precio",75.50,"stock",stock,"minimo",2));
    }

    void cargarDemostracion() {
        new TransactionTemplate(transacciones).executeWithoutResult(t -> new DatosDemostracion(db,claves).run());
    }

    @Test void demostracionCoherenteSinDuplicadosNiCambiosEnDatosExistentes() throws Exception {
        long existente=nuevaOrden("P777PRU");
        var configuracion=db.queryForMap("SELECT * FROM configuracion WHERE id=1");
        cargarDemostracion();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM clientes",Integer.class)).isEqualTo(11);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM vehiculos",Integer.class)).isEqualTo(11);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM ordenes",Integer.class)).isEqualTo(11);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM repuestos",Integer.class)).isEqualTo(12);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM solicitudes",Integer.class)).isEqualTo(15);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM movimientos",Integer.class)).isEqualTo(21);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM facturas",Integer.class)).isEqualTo(3);
        assertThat(db.queryForObject("SELECT SUM(total) FROM facturas",java.math.BigDecimal.class)).isEqualByComparingTo("3085.00");
        assertThat(db.queryForObject("SELECT COUNT(*) FROM (SELECT r.id FROM repuestos r JOIN movimientos m ON m.repuesto_id=r.id GROUP BY r.id,r.stock HAVING SUM(m.cantidad)<>r.stock) saldos",Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM ordenes o LEFT JOIN facturas f ON f.orden_id=o.id WHERE o.estado='ENTREGADO' AND (o.salida IS NULL OR f.id IS NULL)",Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM ordenes WHERE estado IN ('TERMINADO','ENTREGADO') AND id IN (SELECT orden_id FROM solicitudes WHERE estado='PENDIENTE')",Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM facturas WHERE total<>mano_obra+repuestos",Integer.class)).isZero();
        assertThat(db.queryForMap("SELECT * FROM configuracion WHERE id=1")).isEqualTo(configuracion);
        assertThat(db.queryForObject("SELECT estado FROM ordenes WHERE id=?",String.class,existente)).isEqualTo("RECIBIDO");
        MockHttpSession sesion=login("demo_mecanico1","DemoTaller2026!");
        var filas=enviar(sesion,"GET","/api/ordenes",null,200);
        assertThat(json.readTree(filas.getResponse().getContentAsString()).size()).isEqualTo(5);
        int eventos=db.queryForObject("SELECT COUNT(*) FROM historial",Integer.class);
        cargarDemostracion();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM clientes",Integer.class)).isEqualTo(11);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM ordenes",Integer.class)).isEqualTo(11);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM historial",Integer.class)).isEqualTo(eventos);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM usuarios WHERE usuario LIKE 'demo_%'",Integer.class)).isEqualTo(4);
    }

    @Test void conflictoEnDemostracionRevierteTodaLaCarga() throws Exception {
        crear(bodega,"/api/repuestos",Map.of("codigo","DEMO-ACE5W30","nombre","Repuesto que ya existía","precio",10,"stock",1,"minimo",0));
        assertThatThrownBy(this::cargarDemostracion).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM usuarios WHERE usuario LIKE 'demo_%'",Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM clientes",Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM vehiculos",Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM historial WHERE accion='CARGA_DEMO'",Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT nombre FROM repuestos WHERE codigo='DEMO-ACE5W30'",String.class)).isEqualTo("Repuesto que ya existía");
    }

    @Test void flujoCompletoConFacturaYEntrega() throws Exception {
        long o=nuevaOrden("P001ABC"), r=repuesto(5); diagnosticar(o);
        enviar(mecanico,"GET","/api/repuestos/"+r+"/existencia?cantidad=2",null,200);
        long s=crear(mecanico,"/api/ordenes/"+o+"/solicitudes",Map.of("repuestoId",r,"cantidad",2));
        enviar(mecanico,"PUT","/api/ordenes/"+o+"/estado",Map.of("estado","EN_REPARACION","manoObra",100),204);
        enviar(mecanico,"PUT","/api/ordenes/"+o+"/estado",Map.of("estado","TERMINADO","manoObra",100),409);
        enviar(bodega,"PUT","/api/solicitudes/"+s,Map.of("estado","DESPACHADO"),204);
        assertThat(db.queryForObject("SELECT stock FROM repuestos WHERE id=?",Integer.class,r)).isEqualTo(3);
        enviar(bodega,"PUT","/api/solicitudes/"+s,Map.of("estado","DESPACHADO"),409);
        enviar(mecanico,"PUT","/api/ordenes/"+o+"/estado",Map.of("estado","TERMINADO","manoObra",100),204);
        enviar(recepcion,"POST","/api/ordenes/"+o+"/entrega",null,409);
        var f=enviar(recepcion,"POST","/api/ordenes/"+o+"/factura",null,201);
        assertThat(json.readTree(f.getResponse().getContentAsString()).get("total").decimalValue()).isEqualByComparingTo("251.00");
        enviar(recepcion,"POST","/api/ordenes/"+o+"/factura",null,409);
        enviar(recepcion,"POST","/api/ordenes/"+o+"/entrega",null,204);
        assertThat(db.queryForMap("SELECT estado,salida FROM ordenes WHERE id=?",o)).containsEntry("estado","ENTREGADO").doesNotContainValue(null);
        enviar(mecanico,"POST","/api/ordenes/"+o+"/diagnosticos",Map.of("descripcion","x","reparacion","y"),409);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM historial",Integer.class)).isGreaterThanOrEqualTo(10);
        enviar(recepcion,"GET","/api/facturas",null,200);
        enviar(admin,"GET","/api/historial",null,200);
    }

    @Test void autenticacionPermisosYValidacion() throws Exception {
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/login").with(csrf()).param("username","prueba_admin").param("password","incorrecta")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/clientes").session(recepcion).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        enviar(recepcion,"POST","/api/clientes",Map.of("nombre"," ","nit","CF","telefono","123"),400);
        enviar(bodega,"GET","/api/clientes",null,403);
        enviar(mecanico,"GET","/api/usuarios",null,403);
        enviar(recepcion,"POST","/api/usuarios",Map.of("nombre","x","usuario","nuevo","clave","12345678","rol","ADMINISTRADOR"),403);
        long o=nuevaOrden("P002ABC");
        enviar(otroMecanico,"GET","/api/ordenes/"+o,null,403);
        var lista=enviar(otroMecanico,"GET","/api/ordenes",null,200);
        assertThat(json.readTree(lista.getResponse().getContentAsString()).size()).isZero();
        enviar(recepcion,"POST","/api/ordenes/"+o+"/diagnosticos",Map.of("descripcion","x","reparacion","y"),403);
        enviar(mecanico,"PUT","/api/ordenes/"+o+"/estado",Map.of("estado","TERMINADO","manoObra",100),409);
        enviar(mecanico,"PUT","/api/ordenes/"+o+"/estado",Map.of("estado","EN_REPARACION","manoObra",-1),400);
        enviar(recepcion,"POST","/api/ordenes/"+o+"/entrega",null,409);
        long v=db.queryForObject("SELECT vehiculo_id FROM ordenes WHERE id=?",Long.class,o);
        enviar(recepcion,"POST","/api/ordenes",Map.of("vehiculoId",v,"mecanicoId",mecanicoId,"motivo","Duplicado"),409);
        enviar(recepcion,"POST","/api/vehiculos",Map.of("clienteId",9999999,"placa","PXX","marca","X","modelo","Y","anio",2020),404);
    }

    @Test void stockInsuficienteNoDescuentaYRechazoPermiteCerrar() throws Exception {
        long o=nuevaOrden("P003ABC"), r=repuesto(2); diagnosticar(o);
        enviar(mecanico,"POST","/api/ordenes/"+o+"/solicitudes",Map.of("repuestoId",r,"cantidad",3),409);
        long s1=crear(mecanico,"/api/ordenes/"+o+"/solicitudes",Map.of("repuestoId",r,"cantidad",2));
        long s2=crear(mecanico,"/api/ordenes/"+o+"/solicitudes",Map.of("repuestoId",r,"cantidad",2));
        enviar(bodega,"PUT","/api/solicitudes/"+s1,Map.of("estado","DESPACHADO"),204);
        enviar(bodega,"PUT","/api/solicitudes/"+s2,Map.of("estado","DESPACHADO"),409);
        assertThat(db.queryForObject("SELECT stock FROM repuestos WHERE id=?",Integer.class,r)).isZero();
        assertThat(db.queryForObject("SELECT estado FROM solicitudes WHERE id=?",String.class,s2)).isEqualTo("PENDIENTE");
        enviar(bodega,"PUT","/api/solicitudes/"+s2,Map.of("estado","RECHAZADO"),204);
        enviar(mecanico,"PUT","/api/ordenes/"+o+"/estado",Map.of("estado","EN_REPARACION","manoObra",0),204);
        enviar(mecanico,"PUT","/api/ordenes/"+o+"/estado",Map.of("estado","TERMINADO","manoObra",0),204);
        enviar(bodega,"POST","/api/repuestos/"+r+"/entradas",Map.of("cantidad",4,"motivo","Compra"),204);
        assertThat(db.queryForObject("SELECT stock FROM repuestos WHERE id=?",Integer.class,r)).isEqualTo(4);
        enviar(bodega,"POST","/api/repuestos/"+r+"/entradas",Map.of("cantidad",-1,"motivo","Inválido"),400);
    }

    @Test void despachosConcurrentesNoGeneranStockNegativo() throws Exception {
        long o1=nuevaOrden("P004ABC"), o2=nuevaOrden("P005ABC"), r=repuesto(1);
        diagnosticar(o1); diagnosticar(o2);
        long s1=crear(mecanico,"/api/ordenes/"+o1+"/solicitudes",Map.of("repuestoId",r,"cantidad",1));
        long s2=crear(mecanico,"/api/ordenes/"+o2+"/solicitudes",Map.of("repuestoId",r,"cantidad",1));
        try(var ejecutor=Executors.newFixedThreadPool(2)) {
            var a=ejecutor.submit(()->mvc.perform(put("/api/solicitudes/"+s1).session(bodega).with(csrf()).contentType("application/json").content("{\"estado\":\"DESPACHADO\"}")).andReturn().getResponse().getStatus());
            var b=ejecutor.submit(()->mvc.perform(put("/api/solicitudes/"+s2).session(bodega).with(csrf()).contentType("application/json").content("{\"estado\":\"DESPACHADO\"}")).andReturn().getResponse().getStatus());
            assertThat(java.util.List.of(a.get(),b.get())).containsExactlyInAnyOrder(204,409);
        }
        assertThat(db.queryForObject("SELECT stock FROM repuestos WHERE id=?",Integer.class,r)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM solicitudes WHERE estado='DESPACHADO'",Integer.class)).isEqualTo(1);
    }

    @Test void administracionRolVigenteYConfiguracion() throws Exception {
        long u=crear(admin,"/api/usuarios",Map.of("nombre","Nuevo","usuario","nuevo","clave","UsuarioNuevo123","rol","RECEPCION"));
        MockHttpSession sesion=login("nuevo","UsuarioNuevo123");
        enviar(sesion,"GET","/api/clientes",null,200);
        enviar(admin,"PUT","/api/usuarios/"+u+"/rol",Map.of("rol","BODEGUERO"),204);
        enviar(sesion,"GET","/api/clientes",null,403);
        enviar(sesion,"GET","/api/repuestos",null,200);
        nuevaOrden("P006ABC");
        enviar(admin,"PUT","/api/usuarios/"+mecanicoId+"/rol",Map.of("rol","BODEGUERO"),409);
        enviar(admin,"PUT","/api/configuracion",Map.of("nombre","Taller prueba","telefono","1234","direccion","Zona 1"),204);
        var c=enviar(recepcion,"GET","/api/configuracion",null,200);
        assertThat(json.readTree(c.getResponse().getContentAsString()).get("nombre").asText()).isEqualTo("Taller prueba");
        enviar(recepcion,"PUT","/api/configuracion",Map.of("nombre","Otro","telefono","","direccion",""),403);
        mvc.perform(post("/api/logout").session(sesion).with(csrf())).andExpect(status().isNoContent());
        assertThat(sesion.isInvalid()).isTrue();
    }
}
