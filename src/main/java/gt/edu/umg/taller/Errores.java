package gt.edu.umg.taller;

import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class Errores {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> negocio(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason() == null ? "Operación inválida" : e.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage()).findFirst().orElse("Datos inválidos");
        return ResponseEntity.badRequest().body(Map.of("error", mensaje));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> json() { return ResponseEntity.badRequest().body(Map.of("error", "Revise los tipos de datos y el formato JSON.")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> integridad() { return ResponseEntity.status(409).body(Map.of("error", "Registro duplicado o referencia inválida.")); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> inesperado(Exception e) {
        LoggerFactory.getLogger(Errores.class).error("Error al procesar la solicitud", e);
        return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo completar la operación."));
    }
}
