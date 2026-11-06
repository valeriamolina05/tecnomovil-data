@echo off
rem Uso: ejecutar.bat [numeroDeUsuarios] [umbral]
chcp 65001 > nul
java -Dstdout.encoding=UTF-8 -cp out tecnomovil.PruebasAnalisis
java -Dstdout.encoding=UTF-8 -cp out tecnomovil.Main %*
