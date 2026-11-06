# TecnoMovil Data - Procesamiento funcional de datos de transporte urbano

Caso de estudio de la Unidad 2 - Tecnologia en Desarrollo de Software (IU Digital de Antioquia).
Java puro (sin frameworks externos): Streams, lambdas, funciones puras y colecciones inmutables.

## Estructura
- `src/tecnomovil/modelo/`    records inmutables (Registro, Conteo, Promedio, EstadoRuta, ResumenDiario)
- `src/tecnomovil/datos/`     GeneradorDatos: datos simulados reproducibles
- `src/tecnomovil/servicio/`  AnalisisTransporte: las 6 tareas funcionales (a-f)
- `src/tecnomovil/Main.java`  ejecucion completa con datos simulados
- `src/tecnomovil/PruebasAnalisis.java` pruebas con resultados calculados a mano
- `evidencias/`              capturas de la ejecucion

## Requisitos
JDK 17 o superior (desarrollado y probado con JDK 21).

## Compilar y ejecutar
Linux / macOS:  `./compilar.sh`  y luego  `./ejecutar.sh`
Windows:        `compilar.bat`   y luego  `ejecutar.bat`

Parametros opcionales: `ejecutar.sh 1000000 88000` (1 millon de usuarios, umbral de 88000 entradas/hora).
