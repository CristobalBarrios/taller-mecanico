package gt.edu.umg.taller;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

/** Cuerpos de las solicitudes REST. Los identificadores se validan además contra la BD. */
public final class Datos {
    private Datos() {}
    public record Cliente(@NotBlank @Size(max=100) String nombre,
        @NotBlank @Size(max=30) String nit, @NotBlank @Size(max=30) String telefono) {}
    public record Vehiculo(@Positive long clienteId, @NotBlank @Size(max=20) String placa,
        @NotBlank @Size(max=60) String marca, @NotBlank @Size(max=60) String modelo,
        @Min(1900) @Max(2100) int anio) {}
    public record Orden(@Positive long vehiculoId, @Positive long mecanicoId,
        @NotBlank @Size(max=1000) String motivo) {}
    public record Diagnostico(@NotBlank @Size(max=2000) String descripcion,
        @NotBlank @Size(max=2000) String reparacion) {}
    public record Estado(@NotBlank String estado,
        @NotNull @DecimalMin("0.00") @Digits(integer=8, fraction=2) BigDecimal manoObra) {}
    public record Repuesto(@NotBlank @Size(max=40) String codigo, @NotBlank @Size(max=100) String nombre,
        @NotNull @DecimalMin("0.00") @Digits(integer=8,fraction=2) BigDecimal precio,
        @Min(0) int stock, @Min(0) int minimo) {}
    public record Entrada(@Positive int cantidad, @NotBlank @Size(max=300) String motivo) {}
    public record Solicitud(@Positive long repuestoId, @Positive int cantidad) {}
    public record Despacho(@NotBlank @Pattern(regexp="DESPACHADO|RECHAZADO") String estado) {}
    public record Usuario(@NotBlank @Size(max=100) String nombre,
        @NotBlank @Pattern(regexp="[a-zA-Z0-9._-]{3,60}") String usuario,
        @NotBlank @Size(min=8,max=72) String clave,
        @NotBlank @Pattern(regexp="ADMINISTRADOR|RECEPCION|MECANICO|BODEGUERO") String rol) {}
    public record Rol(@NotBlank @Pattern(regexp="ADMINISTRADOR|RECEPCION|MECANICO|BODEGUERO") String rol) {}
    public record Asignacion(@Positive long mecanicoId) {}
    public record Configuracion(@NotBlank @Size(max=100) String nombre,
        @NotNull @Size(max=30) String telefono, @NotNull @Size(max=200) String direccion) {}
}
