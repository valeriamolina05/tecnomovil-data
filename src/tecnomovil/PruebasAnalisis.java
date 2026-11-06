package tecnomovil;

import tecnomovil.modelo.EstadoRuta;
import tecnomovil.modelo.Registro;
import tecnomovil.modelo.ResumenDiario;
import tecnomovil.servicio.AnalisisTransporte;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Pruebas manuales (sin frameworks externos) con un conjunto pequeno cuyos resultados
 * se pueden calcular a mano. Uso: java -cp out tecnomovil.PruebasAnalisis
 */
public final class PruebasAnalisis {

    private static int fallos = 0;

    private static LocalDateTime t(int hora, int minuto) {
        return LocalDate.of(2026, 11, 2).atTime(hora, minuto);
    }

    private static Registro r(String u, String ruta, String est, String acc, LocalDateTime ts) {
        return new Registro(u, ruta, est, acc, ts);
    }

    public static void main(String[] args) {
        List<Registro> datos = List.of(
                r("U1", "T1", "Hospital", "entrada", t(7, 0)),
                r("U1", "T1", "Universidad", "salida", t(7, 10)),
                r("U2", "T1", "Hospital", "entrada", t(7, 30)),
                r("U2", "T1", "Universidad", "salida", t(7, 36)),
                r("U3", "T2", "Mercado", "entrada", t(18, 0)),
                r("U3", "T2", "Estadio", "salida", t(18, 12)),
                r("U1", "T1", "Universidad", "entrada", t(18, 5)),
                r("U1", "T1", "Hospital", "salida", t(18, 15)));
        int huella = datos.hashCode();

        for (boolean paralelo : new boolean[]{false, true}) {
            System.out.println("--- Modo " + (paralelo ? "PARALELO" : "SECUENCIAL") + " ---");

            verificar("a) afluencia por estacion",
                    AnalisisTransporte.afluenciaPorEstacion(datos, paralelo).equals(
                            Map.of("Hospital", 2L, "Universidad", 1L, "Mercado", 1L)));

            verificar("b) distribucion por hora",
                    AnalisisTransporte.distribucionPorHora(datos, paralelo).equals(
                            Map.of(7, 2L, 18, 2L)));

            verificar("b) hora pico (empate: gana orden alfabetico)",
                    AnalisisTransporte.horasPico(datos, 1, paralelo).get(0).clave()
                            .equals("07:00-07:59"));

            verificar("c) ruta mas utilizada = T1 con 3",
                    AnalisisTransporte.rutasMasUtilizadas(datos, 1, paralelo).get(0)
                            .equals(new tecnomovil.modelo.Conteo("T1", 3)));

            Map<String, List<String>> patrones =
                    AnalisisTransporte.patronesPorUsuario(datos, paralelo);
            verificar("d) patron U1 conserva el orden",
                    patrones.get("U1").equals(
                            List.of("Hospital", "Universidad", "Universidad", "Hospital")));
            verificar("d) patron U3",
                    patrones.get("U3").equals(List.of("Mercado", "Estadio")));

            Map<String, Double> tiempos =
                    AnalisisTransporte.tiempoPromedioEntreEstaciones(datos, paralelo);
            verificar("e) Hospital -> Universidad = (10 + 6) / 2 = 8 min",
                    tiempos.get("Hospital -> Universidad") == 8.0);
            verificar("e) Mercado -> Estadio = 12 min", tiempos.get("Mercado -> Estadio") == 12.0);
            verificar("e) Universidad -> Hospital = 10 min",
                    tiempos.get("Universidad -> Hospital") == 10.0);
            verificar("e) no se cuentan pares salida -> entrada", tiempos.size() == 3);

            List<EstadoRuta> umbral1 = AnalisisTransporte.detectarSobrecarga(datos, 1, paralelo);
            verificar("f) con umbral 1, T1 es CRITICA (pico 2 > 1) y T2 normal",
                    umbral1.get(0).ruta().equals("T1") && umbral1.get(0).critica()
                            && !umbral1.get(1).critica());
            verificar("f) con umbral 2, ninguna ruta es critica",
                    AnalisisTransporte.detectarSobrecarga(datos, 2, paralelo).stream()
                            .noneMatch(EstadoRuta::critica));
        }

        ResumenDiario seq = AnalisisTransporte.analizar(datos, 1, false);
        ResumenDiario par = AnalisisTransporte.analizar(datos, 1, true);
        verificar("secuencial y paralelo producen el mismo resumen", seq.equals(par));
        verificar("la lista de entrada no fue modificada", datos.hashCode() == huella);

        boolean inmutable;
        try {
            seq.horasPico().add(null);
            inmutable = false;
        } catch (UnsupportedOperationException e) {
            inmutable = true;
        }
        verificar("los resultados son inmutables", inmutable);

        System.out.println();
        System.out.println(fallos == 0 ? "TODAS LAS PRUEBAS PASARON"
                                       : "PRUEBAS FALLIDAS: " + fallos);
        System.exit(fallos == 0 ? 0 : 1);
    }

    private static void verificar(String nombre, boolean condicion) {
        System.out.println((condicion ? "  [OK]    " : "  [FALLO] ") + nombre);
        if (!condicion) {
            fallos++;
        }
    }
}
