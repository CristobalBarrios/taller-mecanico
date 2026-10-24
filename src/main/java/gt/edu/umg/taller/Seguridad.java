package gt.edu.umg.taller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class Seguridad {
    @Bean PasswordEncoder claves() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService usuarios(JdbcTemplate db) {
        return nombre -> {
            var filas = db.queryForList("SELECT * FROM usuarios WHERE usuario=? AND activo=TRUE", nombre);
            if (filas.isEmpty()) throw new UsernameNotFoundException("Usuario no encontrado");
            Map<String, Object> u = filas.getFirst();
            return User.withUsername(nombre).password((String) u.get("clave"))
                .roles((String) u.get("rol")).build();
        };
    }

    @Bean SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a
            .requestMatchers("/", "/index.html", "/app.js", "/api/csrf").permitAll()
            .anyRequest().authenticated())
            .formLogin(f -> f.loginProcessingUrl("/api/login")
                .successHandler((req, res, auth) -> { res.setContentType("application/json"); res.getWriter().write("{\"ok\":true}"); })
                .failureHandler((req, res, ex) -> res.sendError(401, "Credenciales inválidas")))
            .logout(l -> l.logoutUrl("/api/logout").logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> res.sendError(401))
                .accessDeniedHandler((req, res, ex) -> res.sendError(403)));
        return http.build();
    }

    @Bean @Order(0) CommandLineRunner administrador(JdbcTemplate db, PasswordEncoder encoder,
            @Value("${taller.admin.usuario}") String usuario, @Value("${taller.admin.clave}") String clave) {
        return args -> {
            if (db.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class) == 0) {
                if (!usuario.matches("[a-zA-Z0-9._-]{3,60}"))
                    throw new IllegalStateException("ADMIN_USER debe tener de 3 a 60 letras, números, puntos, guiones o guiones bajos.");
                if (clave.length() < 8 || clave.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
                    throw new IllegalStateException("Primera ejecución: configure ADMIN_PASSWORD con al menos 8 caracteres y máximo 72 bytes UTF-8.");
                db.update("INSERT INTO usuarios(nombre,usuario,clave,rol) VALUES(?,?,?,'ADMINISTRADOR')",
                    "Administrador", usuario, encoder.encode(clave));
            }
        };
    }
}
