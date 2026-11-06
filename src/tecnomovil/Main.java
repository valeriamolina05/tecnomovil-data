package tecnomovil;

import tecnomovil.datos.GeneradorDatos;
import tecnomovil.modelo.Conteo;
import tecnomovil.modelo.EstadoRuta;
import tecnomovil.modelo.Promedio;
import tecnomovil.modelo.Registro;
import tecnomovil.modelo.ResumenDiario;
import tecnomovil.servicio.AnalisisTransporte;

import java.util.List;
import java.util.function.Supplier;

/**
 * Punto de entrada. Es la unica clase con efectos secundarios (imprimir y medir tiempo);
 * el nucleo funcional (AnalisisTransporte) permanece puro.
 *
 * Uso: java -cp out tecnomovil.Main [numeroDeUsuarios] [umbralOcupacion]
 */
public final class Main {

    private static final long SEMILLA = 2026L;

    public static void main(String[] args) {
        int usuarios = args.length > 0 ? Integer.parseInt(args[0]) : 250_000;
        long umbral = args.length > 1 ? Long.parseLong(args[1]) : 22_000L;

        titulo("TECNOMOVIL DATA - Procesamiento funcional de datos (TecnoValle)");
        System.out.println("Fecha simulada : " + GeneradorDatos.FECHA);
        System.out.println("Usuarios       : " + usuarios);
        System.out.println("Umbral critico : " + umbral + " entradas/hora por ruta (simulado)");

        List<Registro> registros = GeneradorDatos.generar(usuarios, SEMILLA);
        System.out.println("Registros      : " + registros.size());
        System.out.println("Procesadores   : " + Runtime.getRuntime().availableProcessors());

        int tamanoAntes = registros.size();
        int huellaAntes = registros.hashCode();

        // Primera pasada sin medir (calentamiento de la JVM) y luego mediciones.
        AnalisisTransporte.analizar(registros, umbral, false);
        long t0 = System.nanoTime();
        ResumenDiario secuencial = AnalisisTransporte.analizar(registros, umbral, false);
        long msSecuencial = (System.nanoTime() - t0) / 1_000_000;

        long t1 = System.nanoTime();
        ResumenDiario paralelo = AnalisisTransporte.analizar(registros, umbral, true);
        long msParalelo = (System.nanoTime() - t1) / 1_000_000;

        titulo("a) Afluencia por estacion (entradas del dia)");
        secuencial.afluenciaPorEstacion().entrySet().stream()
                .sorted((x, y) -> Long.compare(y.getValue(), x.getValue()))
                .forEach(e -> System.out.printf("  %-18s %,10d%n", e.getKey(), e.getValue()));

        titulo("b) Horas pico (top 3 por entradas)");
        imprimirConteos(secuencial.horasPico());
        System.out.println("  Distribucion completa por hora:");
        AnalisisTransporte.distribucionPorHora(registros, true).forEach(
                (hora, cantidad) -> System.out.printf("    %02d:00  %,9d  %s%n",
                        hora, cantidad, "#".repeat((int) (cantidad / 4_000))));

        titulo("c) Rutas mas utilizadas");
        imprimirConteos(secuencial.rutasMasUtilizadas());

        titulo("d) Patrones de viaje por usuario (primeros 5 usuarios)");
        secuencial.patronesPorUsuario().entrySet().stream().limit(5)
                .forEach(e -> System.out.println("  " + e.getKey() + " -> " + e.getValue()));
        System.out.println("  Usuarios con patron calculado: "
                + secuencial.patronesPorUsuario().size());

        titulo("e) Tiempo promedio por tramo (los 8 mas lentos)");
        List<Promedio> lentos = AnalisisTransporte.tramosMasLentos(
                secuencial.minutosPromedioPorTramo(), 8);
        lentos.forEach(p -> System.out.printf("  %-38s %6.2f min%n", p.clave(), p.valor()));
        System.out.println("  Tramos distintos analizados: "
                + secuencial.minutosPromedioPorTramo().size());

        titulo("f) Deteccion de sobrecarga en rutas");
        System.out.printf("  %-6s %-12s %12s %10s  %s%n",
                "Ruta", "Hora pico", "Entradas", "Umbral", "Estado");
        secuencial.estadoRutas().forEach(Main::imprimirEstado);

        titulo("Verificaciones de programacion funcional");
        System.out.println("  Secuencial == Paralelo (resultado identico): "
                + secuencial.equals(paralelo));
        System.out.println("  Tiempo secuencial : " + msSecuencial + " ms");
        System.out.println("  Tiempo paralelo   : " + msParalelo + " ms");
        System.out.println("  Lista original sin cambios (tamano y huella): "
                + (registros.size() == tamanoAntes && registros.hashCode() == huellaAntes));
        intentar("registros.add(...)", () -> registros.add(registros.get(0)));
        intentar("afluencia.put(...)",
                () -> secuencial.afluenciaPorEstacion().put("Estacion X", 1L));
        intentar("horasPico.clear()", () -> { secuencial.horasPico().clear(); return true; });
    }

    /** Intenta mutar una coleccion y muestra que la inmutabilidad lo impide. */
    private static void intentar(String operacion, Supplier<Object> accion) {
        try {
            accion.get();
            System.out.println("  " + operacion + " -> PERMITIDO (inesperado)");
        } catch (UnsupportedOperationException e) {
            System.out.println("  " + operacion + " -> UnsupportedOperationException (inmutable)");
        }
    }

    private static void imprimirConteos(List<Conteo> conteos) {
        conteos.forEach(c -> System.out.printf("  %-14s %,10d%n", c.clave(), c.valor()));
    }

    private static void imprimirEstado(EstadoRuta e) {
        System.out.printf("  %-6s %02d:00-%02d:59 %,12d %,10d  %s%n",
                e.ruta(), e.horaPico(), e.horaPico(), e.entradasPico(), e.umbral(),
                e.etiqueta());
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("=== " + texto + " ===");
    }
}
