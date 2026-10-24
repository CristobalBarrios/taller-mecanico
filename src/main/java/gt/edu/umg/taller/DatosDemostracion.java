package gt.edu.umg.taller;

import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Carga opcional de información ficticia para presentar el proyecto. */
@Component
@Order(1)
@ConditionalOnProperty(name="taller.demo", havingValue="true")
public class DatosDemostracion implements CommandLineRunner {
    private final JdbcTemplate db;
    private final PasswordEncoder claves;
    public DatosDemostracion(JdbcTemplate db, PasswordEncoder claves) {
        this.db = db; this.claves = claves;
    }

    @Override @Transactional
    public void run(String... args) {
        if (db.queryForObject("SELECT COUNT(*) FROM historial WHERE accion='CARGA_DEMO'", Integer.class)>0) {
            LoggerFactory.getLogger(getClass()).info("Los datos de demostración ya se cargaron; se conservan sin cambios.");
            return;
        }
        // La variable SQL y el script usan la misma conexión dentro de esta transacción.
        db.update("SET @demo_clave = ?", claves.encode("DemoTaller2026!"));
        var script = new ResourceDatabasePopulator(new ClassPathResource("datos-demo.sql"));
        script.setSqlScriptEncoding("UTF-8");
        script.execute(db.getDataSource());
        LoggerFactory.getLogger(getClass()).info("Datos ficticios cargados: 10 clientes, 10 vehículos, 10 órdenes y 12 repuestos.");
    }
}
