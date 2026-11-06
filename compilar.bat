@echo off
rem Compila el proyecto (requiere JDK 17 o superior).
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > fuentes.txt
javac -encoding UTF-8 -d out @fuentes.txt && echo Compilacion exitosa.
del fuentes.txt
