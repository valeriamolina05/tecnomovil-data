package tecnomovil.modelo;

import java.util.List;
import java.util.Map;

/**
 * Resultado completo e inmutable del analisis diario. Al ser un record, dos resumenes
 * calculados con los mismos datos son iguales ({@code equals}), lo que permite verificar
 * que la version secuencial y la paralela producen exactamente el mismo resultado.
 */
public record ResumenDiario(
        Map<String, Long> afluenciaPorEstacion,
        List<Conteo> horasPico,
        List<Conteo> rutasMasUtilizadas,
        Map<String, List<String>> patronesPorUsuario,
        Map<String, Double> minutosPromedioPorTramo,
        List<EstadoRuta> estadoRutas) { }
