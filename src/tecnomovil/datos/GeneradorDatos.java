package tecnomovil.datos;

import tecnomovil.modelo.Registro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Genera un conjunto grande y reproducible de registros simulados para TecnoValle.
 *
 * <p>La generacion es una funcion deterministica: con los mismos parametros
 * (numero de usuarios y semilla) siempre produce la misma lista. Cada usuario usa su
 * propio generador pseudoaleatorio local derivado de la semilla, de modo que no hay
 * estado compartido entre usuarios y la generacion tambien podria paralelizarse.</p>
 *
 * <p>El resultado es una lista inmutable ({@link Stream#toList()}).</p>
 */
public final class GeneradorDatos {

    private GeneradorDatos() { }

    /** Fecha simulada del dia a procesar. */
    public static final LocalDate FECHA = LocalDate.of(2026, 11, 2);

    private record RutaSimulada(String id, int peso, List<String> estaciones) { }

    private record HoraPeso(int hora, int peso) { }

    private static final List<RutaSimulada> RUTAS = List.of(
            new RutaSimulada("T1", 35, List.of("Terminal Norte", "Hospital", "Universidad",
                    "Parque Central", "Plaza Mayor", "Terminal Sur")),
            new RutaSimulada("T2", 30, List.of("Aeropuerto", "Zona Franca", "Mercado",
                    "Parque Central", "Estadio", "Biblioteca")),
            new RutaSimulada("A1", 15, List.of("Barrio Esperanza", "Colegio", "Mercado",
                    "Universidad")),
            new RutaSimulada("A2", 12, List.of("Alto Verde", "Clinica", "Plaza Mayor",
                    "Centro Comercial")),
            new RutaSimulada("A3", 8, List.of("Los Pinos", "Parque Industrial", "Estadio",
                    "Terminal Sur")));

    /** Distribucion de viajes de la manana: pico alrededor de las 7:00. */
    private static final List<HoraPeso> FRANJA_MANANA = List.of(
            new HoraPeso(5, 2), new HoraPeso(6, 12), new HoraPeso(7, 25),
            new HoraPeso(8, 18), new HoraPeso(9, 8), new HoraPeso(10, 5));

    /** Distribucion de viajes de la tarde: pico alrededor de las 18:00. */
    private static final List<HoraPeso> FRANJA_TARDE = List.of(
            new HoraPeso(13, 6), new HoraPeso(14, 5), new HoraPeso(15, 7),
            new HoraPeso(16, 12), new HoraPeso(17, 22), new HoraPeso(18, 25),
            new HoraPeso(19, 14), new HoraPeso(20, 6), new HoraPeso(21, 3));

    /**
     * Genera los registros del dia.
     *
     * @param numUsuarios cantidad de usuarios simulados
     * @param semilla     semilla para reproducibilidad
     * @return lista inmutable de registros
     */
    public static List<Registro> generar(int numUsuarios, long semilla) {
        return IntStream.range(0, numUsuarios)
                .boxed()
                .flatMap(indice -> registrosDeUsuario(indice, semilla))
                .toList();
    }

    /** Un usuario hace (con 90 % de probabilidad cada uno) un viaje de ida y uno de regreso. */
    private static Stream<Registro> registrosDeUsuario(int indice, long semilla) {
        SplittableRandom aleatorio = new SplittableRandom(semilla + indice * 7919L);
        String id = String.format("U%06d", indice);
        Stream<Registro> manana = aleatorio.nextInt(100) < 90
                ? viaje(id, aleatorio, FRANJA_MANANA) : Stream.empty();
        Stream<Registro> tarde = aleatorio.nextInt(100) < 90
                ? viaje(id, aleatorio, FRANJA_TARDE) : Stream.empty();
        return Stream.concat(manana, tarde);
    }

    /** Un viaje = un registro de entrada y uno de salida en la misma ruta. */
    private static Stream<Registro> viaje(String id, SplittableRandom aleatorio,
                                          List<HoraPeso> franja) {
        RutaSimulada ruta = muestrear(aleatorio, RUTAS, RutaSimulada::peso);
        int hora = muestrear(aleatorio, franja, HoraPeso::peso).hora();
        LocalDateTime inicio = FECHA.atTime(hora, aleatorio.nextInt(60), aleatorio.nextInt(60));

        int n = ruta.estaciones().size();
        int origen = aleatorio.nextInt(n - 1);
        int paradas = 1 + aleatorio.nextInt(Math.min(4, n - 1 - origen));
        int destino = origen + paradas;
        int minutos = paradas * (3 + aleatorio.nextInt(3));

        return Stream.of(
                new Registro(id, ruta.id(), ruta.estaciones().get(origen),
                        Registro.ENTRADA, inicio),
                new Registro(id, ruta.id(), ruta.estaciones().get(destino),
                        Registro.SALIDA, inicio.plusMinutes(minutos)));
    }

    /** Seleccion aleatoria ponderada por peso. */
    private static <T> T muestrear(SplittableRandom aleatorio, List<T> opciones,
                                   ToIntFunction<T> peso) {
        int total = opciones.stream().mapToInt(peso).sum();
        int objetivo = aleatorio.nextInt(total);
        int acumulado = 0;
        for (T opcion : opciones) {
            acumulado += peso.applyAsInt(opcion);
            if (objetivo < acumulado) {
                return opcion;
            }
        }
        throw new IllegalStateException("Pesos inconsistentes");
    }
}
