#!/bin/sh
# Compila el proyecto (requiere JDK 17 o superior; probado con JDK 21).
rm -rf out && mkdir out
javac -encoding UTF-8 -d out $(find src -name "*.java") && echo "Compilacion exitosa."
