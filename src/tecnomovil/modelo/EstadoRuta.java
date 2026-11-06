package tecnomovil.modelo;

/**
 * Resultado inmutable de la deteccion de sobrecarga para una ruta.
 *
 * @param ruta         identificador de la ruta
 * @param horaPico     hora del dia (0-23) con mas entradas en esa ruta
 * @param entradasPico cantidad de entradas en esa hora
 * @param umbral       umbral de ocupacion (simulado) usado en la comparacion
 * @param critica      {@code true} si entradasPico supera el umbral
 */
public record EstadoRuta(String ruta, int horaPico, long entradasPico,
                         long umbral, boolean critica) {

    public String etiqueta() {
        return critica ? "CRITICA" : "normal";
    }
}
