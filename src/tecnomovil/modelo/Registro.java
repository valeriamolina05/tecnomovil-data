package tecnomovil.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro de validacion capturado por validadores, sensores o aplicaciones moviles.
 *
 * <p>Es un {@code record}: todos sus campos son finales y no existen metodos que los
 * modifiquen, por lo tanto cada instancia es inmutable y puede compartirse entre hilos
 * sin sincronizacion (requisito de inmutabilidad y de paralelizacion del caso).</p>
 *
 * @param idUsuario identificador del usuario (tarjeta o aplicacion)
 * @param ruta      identificador de la ruta (por ejemplo "T1")
 * @param estacion  nombre de la estacion donde ocurre el evento
 * @param accion    "entrada" o "salida"
 * @param timestamp fecha y hora exacta del evento
 */
public record Registro(String idUsuario, String ruta, String estacion,
                       String accion, LocalDateTime timestamp) {

    public static final String ENTRADA = "entrada";
    public static final String SALIDA = "salida";

    /** Constructor compacto: valida los datos una unica vez, al crear el objeto. */
    public Registro {
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(ruta, "ruta");
        Objects.requireNonNull(estacion, "estacion");
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(timestamp, "timestamp");
        if (!ENTRADA.equals(accion) && !SALIDA.equals(accion)) {
            throw new IllegalArgumentException("accion invalida: " + accion);
        }
    }

    /** Funcion pura: depende solo del estado (inmutable) del registro. */
    public boolean esEntrada() {
        return ENTRADA.equals(accion);
    }
}
