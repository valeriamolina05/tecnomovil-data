#!/bin/sh
# Uso: ./ejecutar.sh [numeroDeUsuarios] [umbral]   (por defecto 250000 usuarios y umbral 22000)
java -Dstdout.encoding=UTF-8 -cp out tecnomovil.PruebasAnalisis && \
java -Dstdout.encoding=UTF-8 -cp out tecnomovil.Main "$@"
