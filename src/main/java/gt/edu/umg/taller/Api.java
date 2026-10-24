package gt.edu.umg.taller;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class Api {
    private final TallerServicio taller;
    public Api(TallerServicio taller) { this.taller = taller; }
    @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token) {
        return Map.of("token",token.getToken(),"header",token.getHeaderName());
    }
    @GetMapping("/yo") Map<String,Object> yo() { return taller.actual(); }
    @GetMapping("/clientes") List<Map<String,Object>> clientes() { return taller.clientes(); }
    @PostMapping("/clientes") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Long> cliente(@Valid @RequestBody Datos.Cliente c) { return Map.of("id",taller.crearCliente(c)); }
    @GetMapping("/vehiculos") List<Map<String,Object>> vehiculos() { return taller.vehiculos(); }
    @PostMapping("/vehiculos") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Long> vehiculo(@Valid @RequestBody Datos.Vehiculo v) { return Map.of("id",taller.crearVehiculo(v)); }
    @GetMapping("/mecanicos") List<Map<String,Object>> mecanicos() { return taller.mecanicos(); }
    @GetMapping("/ordenes") List<Map<String,Object>> ordenes() { return taller.ordenes(); }
    @PostMapping("/ordenes") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Long> orden(@Valid @RequestBody Datos.Orden o) { return Map.of("id",taller.crearOrden(o)); }
    @GetMapping("/ordenes/{id}") Map<String,Object> detalle(@PathVariable long id) { return taller.detalle(id); }
    @PutMapping("/ordenes/{id}/mecanico") @ResponseStatus(HttpStatus.NO_CONTENT)
    void asignar(@PathVariable long id, @Valid @RequestBody Datos.Asignacion a) { taller.asignar(id,a); }
    @PostMapping("/ordenes/{id}/diagnosticos") @ResponseStatus(HttpStatus.CREATED)
    void diagnostico(@PathVariable long id, @Valid @RequestBody Datos.Diagnostico d) { taller.diagnosticar(id,d); }
    @PutMapping("/ordenes/{id}/estado") @ResponseStatus(HttpStatus.NO_CONTENT)
    void estado(@PathVariable long id, @Valid @RequestBody Datos.Estado e) { taller.estado(id,e); }
    @PostMapping("/ordenes/{id}/entrega") @ResponseStatus(HttpStatus.NO_CONTENT)
    void entrega(@PathVariable long id) { taller.entregar(id); }
    @PostMapping("/ordenes/{id}/solicitudes") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Long> solicitud(@PathVariable long id, @Valid @RequestBody Datos.Solicitud s) { return Map.of("id",taller.solicitar(id,s)); }
    @GetMapping("/repuestos") List<Map<String,Object>> repuestos() { return taller.repuestos(); }
    @GetMapping("/repuestos/{id}/existencia")
    Map<String,Object> existencia(@PathVariable long id, @RequestParam(defaultValue="1") int cantidad) { return taller.existencia(id,cantidad); }
    @PostMapping("/repuestos") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Long> repuesto(@Valid @RequestBody Datos.Repuesto r) { return Map.of("id",taller.crearRepuesto(r)); }
    @PostMapping("/repuestos/{id}/entradas") @ResponseStatus(HttpStatus.NO_CONTENT)
    void entrada(@PathVariable long id, @Valid @RequestBody Datos.Entrada e) { taller.entrada(id,e); }
    @GetMapping("/solicitudes") List<Map<String,Object>> solicitudes() { return taller.solicitudes(); }
    @PutMapping("/solicitudes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void despacho(@PathVariable long id, @Valid @RequestBody Datos.Despacho d) { taller.despachar(id,d); }
    @GetMapping("/movimientos") List<Map<String,Object>> movimientos() { return taller.movimientos(); }
    @PostMapping("/ordenes/{id}/factura") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> facturar(@PathVariable long id) { return taller.facturar(id); }
    @GetMapping("/facturas") List<Map<String,Object>> facturas() { return taller.facturas(); }
    @GetMapping("/usuarios") List<Map<String,Object>> usuarios() { return taller.usuarios(); }
    @PostMapping("/usuarios") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Long> usuario(@Valid @RequestBody Datos.Usuario u) { return Map.of("id",taller.crearUsuario(u)); }
    @PutMapping("/usuarios/{id}/rol") @ResponseStatus(HttpStatus.NO_CONTENT)
    void rol(@PathVariable long id, @Valid @RequestBody Datos.Rol r) { taller.rol(id,r); }
    @GetMapping("/configuracion") Map<String,Object> configuracion() { return taller.configuracion(); }
    @PutMapping("/configuracion") @ResponseStatus(HttpStatus.NO_CONTENT)
    void configurar(@Valid @RequestBody Datos.Configuracion c) { taller.configurar(c); }
    @GetMapping("/historial") List<Map<String,Object>> historial() { return taller.historial(); }
}
