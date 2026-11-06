package tecnomovil.servicio;

import tecnomovil.modelo.Conteo;
import tecnomovil.modelo.EstadoRuta;
import tecnomovil.modelo.Promedio;
import tecnomovil.modelo.Registro;
import tecnomovil.modelo.ResumenDiario;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Modulo funcional de TecnoMovil Data.
 *
 * <p>Todas las operaciones son funciones puras: reciben una lista de registros y devuelven
 * un resultado nuevo e inmutable, sin modificar la entrada, sin variables compartidas y
 * sin efectos secundarios (no imprimen, no escriben archivos, no usan reloj ni azar).
 * Cada operacion recibe un indicador {@code paralelo} que solo cambia la forma de
 * recorrer el flujo ({@code stream()} o {@code parallelStream()}); el resultado es el mismo.</p>
 */
public final class AnalisisTransporte {

    private AnalisisTransporte() { }

    // ------------------------------------------------------------------
    // Predicados y funciones reutilizables (lambdas y referencias a metodos)
    // ------------------------------------------------------------------

    public static final Predicate<Registro> ES_ENTRADA = Registro::esEntrada;
    public static final Predicate<Registro> ES_SALIDA = ES_ENTRADA.negate();

    public static final Function<Registro, String> POR_ESTACION = Registro::estacion;
    public static final Function<Registro, String> POR_RUTA = Registro::ruta;
    public static final Function<Registro, Integer> POR_HORA = r -> r.timestamp().getHour();

    // ------------------------------------------------------------------
    // Operacion integradora
    // ------------------------------------------------------------------

    /** Ejecuta las seis tareas del caso y devuelve un resumen inmutable. */
    public static ResumenDiario analizar(List<Registro> registros, long umbralOcupacion,
                                         boolean paralelo) {
        return new ResumenDiario(
                afluenciaPorEstacion(registros, paralelo),
                horasPico(registros, 3, paralelo),
                rutasMasUtilizadas(registros, 5, paralelo),
                patronesPorUsuario(registros, paralelo),
                tiempoPromedioEntreEstaciones(registros, paralelo),
                detectarSobrecarga(registros, umbralOcupacion, paralelo));
    }

    // ------------------------------------------------------------------
    // a) Afluencia por estacion
    // ------------------------------------------------------------------

    /** Cuenta cuantos usuarios ingresan a cada estacion durante el dia. */
    public static Map<String, Long> afluenciaPorEstacion(List<Registro> registros,
                                                         boolean paralelo) {
        return contarPor(registros, ES_ENTRADA, POR_ESTACION, paralelo);
    }

    // ------------------------------------------------------------------
    // b) Horas pico
    // ------------------------------------------------------------------

    /** Agrupa las entradas por hora del dia (0-23) y cuenta cada grupo. */
    public static Map<Integer, Long> distribucionPorHora(List<Registro> registros,
                                                         boolean paralelo) {
        return contarPor(registros, ES_ENTRADA, POR_HORA, paralelo);
    }

    /** Devuelve las {@code cuantas} horas con mayor flujo de entradas. */
    public static List<Conteo> horasPico(List<Registro> registros, int cuantas,
                                         boolean paralelo) {
        return ranking(distribucionPorHora(registros, paralelo),
                hora -> String.format("%02d:00-%02d:59", hora, hora), cuantas);
    }

    // ------------------------------------------------------------------
    // c) Rutas mas utilizadas
    // ------------------------------------------------------------------

    /** Devuelve las {@code cuantas} rutas con mayor volumen de entradas. */
    public static List<Conteo> rutasMasUtilizadas(List<Registro> registros, int cuantas,
                                                  boolean paralelo) {
        return ranking(contarPor(registros, ES_ENTRADA, POR_RUTA, paralelo), cuantas);
    }

    // ------------------------------------------------------------------
    // d) Patrones de viaje por usuario
    // ------------------------------------------------------------------

    /**
     * Para cada usuario, lista de estaciones en el orden en que aparecen en los registros.
     * Un flujo ordenado conserva el orden de encuentro dentro de cada grupo, tambien en
     * modo paralelo, porque {@code groupingBy} no es un colector concurrente.
     */
    public static Map<String, List<String>> patronesPorUsuario(List<Registro> registros,
                                                               boolean paralelo) {
        return flujo(registros, paralelo)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Registro::idUsuario, TreeMap::new,
                                Collectors.mapping(Registro::estacion,
                                        Collectors.toUnmodifiableList())),
                        Collections::unmodifiableMap));
    }

    // ------------------------------------------------------------------
    // e) Tiempo promedio entre estaciones
    // ------------------------------------------------------------------

    /** Par de registros consecutivos (ordenados por tiempo) de un mismo usuario. */
    private record Desplazamiento(Registro origen, Registro destino) {

        /** Es un desplazamiento real si va de una entrada a una salida de la misma ruta. */
        boolean esViaje() {
            return origen.esEntrada()
                    && !destino.esEntrada()
                    && origen.ruta().equals(destino.ruta())
                    && !origen.estacion().equals(destino.estacion());
        }

        long segundos() {
            return Duration.between(origen.timestamp(), destino.timestamp()).toSeconds();
        }

        String tramo() {
            return origen.estacion() + " -> " + destino.estacion();
        }
    }

    /** Pares de registros consecutivos de un usuario, sin modificar la lista recibida. */
    private static Stream<Desplazamiento> paresConsecutivos(List<Registro> delUsuario) {
        List<Registro> ordenados = delUsuario.stream()
                .sorted(Comparator.comparing(Registro::timestamp))
                .toList();
        return IntStream.range(0, ordenados.size() - 1)
                .mapToObj(i -> new Desplazamiento(ordenados.get(i), ordenados.get(i + 1)));
    }

    /**
     * Minutos promedio que tardan los usuarios en cada tramo "origen -> destino".
     * Se promedian segundos enteros ({@code averagingLong}) y luego se convierten a minutos;
     * asi el resultado no depende del orden de combinacion de los hilos.
     */
    public static Map<String, Double> tiempoPromedioEntreEstaciones(List<Registro> registros,
                                                                    boolean paralelo) {
        Collection<List<Registro>> porUsuario = flujo(registros, paralelo)
                .collect(Collectors.groupingBy(Registro::idUsuario))
                .values();
        Stream<List<Registro>> grupos = paralelo ? porUsuario.parallelStream()
                                                 : porUsuario.stream();
        return grupos
                .flatMap(AnalisisTransporte::paresConsecutivos)
                .filter(Desplazamiento::esViaje)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Desplazamiento::tramo, TreeMap::new,
                                Collectors.collectingAndThen(
                                        Collectors.averagingLong(Desplazamiento::segundos),
                                        segundos -> segundos / 60.0)),
                        Collections::unmodifiableMap));
    }

    /** Los {@code cuantos} tramos con mayor tiempo promedio. */
    public static List<Promedio> tramosMasLentos(Map<String, Double> promedios, int cuantos) {
        return promedios.entrySet().stream()
                .map(e -> new Promedio(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(Promedio::valor).reversed()
                        .thenComparing(Promedio::clave))
                .limit(cuantos)
                .toList();
    }

    // ------------------------------------------------------------------
    // f) Deteccion de sobrecarga en rutas
    // ------------------------------------------------------------------

    /**
     * Marca como critica toda ruta cuya hora de mayor ocupacion (entradas por hora)
     * supera el umbral simulado.
     */
    public static List<EstadoRuta> detectarSobrecarga(List<Registro> registros, long umbral,
                                                      boolean paralelo) {
        var porRutaYHora = flujo(registros, paralelo)
                .filter(ES_ENTRADA)
                .collect(Collectors.groupingBy(Registro::ruta, TreeMap::new,
                        Collectors.groupingBy(POR_HORA, TreeMap::new, Collectors.counting())));

        return porRutaYHora.entrySet().stream()
                .map(e -> estadoDeRuta(e.getKey(), e.getValue(), umbral))
                .sorted(Comparator.comparingLong(EstadoRuta::entradasPico).reversed()
                        .thenComparing(EstadoRuta::ruta))
                .toList();
    }

    private static EstadoRuta estadoDeRuta(String ruta, Map<Integer, Long> entradasPorHora,
                                           long umbral) {
        // En empate gana la hora mas temprana: el mapa esta ordenado y max() conserva el primero.
        Map.Entry<Integer, Long> pico = entradasPorHora.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();
        return new EstadoRuta(ruta, pico.getKey(), pico.getValue(), umbral,
                pico.getValue() > umbral);
    }

    // ------------------------------------------------------------------
    // Funciones de orden superior genericas
    // ------------------------------------------------------------------

    /**
     * Funcion de orden superior: recibe un predicado y una funcion clasificadora y cuenta
     * cuantos registros caen en cada clase. Devuelve un mapa inmutable ordenado por clave.
     */
    public static <K extends Comparable<? super K>> Map<K, Long> contarPor(
            List<Registro> registros, Predicate<Registro> filtro,
            Function<Registro, K> clasificador, boolean paralelo) {
        return flujo(registros, paralelo)
                .filter(filtro)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(clasificador, TreeMap::new, Collectors.counting()),
                        Collections::unmodifiableMap));
    }

    /** Ranking descendente por cantidad; en empate, orden alfabetico de la etiqueta. */
    public static <K> List<Conteo> ranking(Map<K, Long> conteos,
                                           Function<K, String> etiqueta, int limite) {
        return conteos.entrySet().stream()
                .map(e -> new Conteo(etiqueta.apply(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingLong(Conteo::valor).reversed()
                        .thenComparing(Conteo::clave))
                .limit(limite)
                .toList();
    }

    public static <K> List<Conteo> ranking(Map<K, Long> conteos, int limite) {
        return ranking(conteos, String::valueOf, limite);
    }

    /** Unico punto donde se decide entre flujo secuencial y paralelo. */
    private static Stream<Registro> flujo(List<Registro> registros, boolean paralelo) {
        return paralelo ? registros.parallelStream() : registros.stream();
    }
}
