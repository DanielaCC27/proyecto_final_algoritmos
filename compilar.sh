#!/bin/sh
# Compila todo el proyecto en la carpeta bin (Linux o Mac). Se ejecuta desde la carpeta proyecto-final.
mkdir -p bin
javac -encoding UTF-8 -cp lib/pdfbox-app-3.0.3.jar -d bin src/modelo/*.java src/datos/*.java src/seguimiento/*.java src/busqueda/*.java src/similitud/*.java src/clustering/*.java src/extraccion/*.java src/casos/*.java src/web/*.java src/estructuras/*.java
echo "Compilado en bin/"
