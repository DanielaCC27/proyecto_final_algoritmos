# Cómo compilar y ejecutar el proyecto

## Requisitos
- **Java 17 o superior** (JDK, no solo JRE). Se comprueba con `java -version` y `javac -version` en la terminal.
- Nada más: el servidor web usa el que trae el JDK y el JSON se escribe a mano. PDFBox (`lib/pdfbox-app-3.0.3.jar`) solo se usa para leer los PDF y viene incluido en `lib/`.

## 1. Compilar
Abre la carpeta `proyecto-final` en VS Code, abre la terminal (Ctrl + ñ) y ejecuta:

| Sistema | Comando |
|---|---|
| Windows | `compilar.bat` |
| Linux o Mac | `sh compilar.sh` |

Esto convierte los `.java` de `src/` en archivos `.class` dentro de la carpeta `bin/`. La carpeta `bin/` se genera sola; no se sube a git. Si aparece "Compilado en bin/" sin errores, está listo. Hay que volver a compilar cada vez que se cambie un `.java`.

## 2. Ejecutar
- **Aplicación web:** `ejecutar.bat` (o `sh ejecutar.sh`) y abrir http://localhost:8080. Para detenerla, Ctrl + C en la terminal.
- **Casos de estudio** (los números paso a paso del informe): `java -cp bin casos.CasosDeEstudio`
- **Experimentos** (matrices, dendrogramas y tiempos en `resultados/`): `java -cp bin casos.Experimentos`

Si el puerto 8080 está ocupado, se puede usar otro: en Windows `set PORT=9090` y luego `ejecutar.bat`.

## 3. Dónde mirar el código
El recorrido recomendado para entenderlo está en `docs/como_funciona.md`. Cada archivo de `src/` tiene arriba un comentario con lo que hace y, en cada método, el orden de complejidad en el estilo del profesor.

| Carpeta | Qué contiene |
|---|---|
| `modelo`, `datos` | `Articulo` y `Corpus`: los 20 artículos |
| `seguimiento` | clases del Seguimiento 2026-2: limpieza del texto (minúsculas, solo letras, 90 palabras vacías) y ranking de términos con Counting, Radix y Bucket Sort |
| `similitud` | Levenshtein, Needleman-Wunsch, TF-IDF, Jaccard, GloVe y Sentence-BERT (requerimiento 1) |
| `clustering` | agrupamiento jerárquico, dendrograma y métricas (requerimiento 2) |
| `busqueda` | KMP, Boyer-Moore y Rabin-Karp (requerimiento 3) |
| `estructuras` | `Nodo` y `Pila` del núcleo 6 |
| `web` | servidor y JSON escrito a mano |
| `casos` | casos de estudio y experimentos |
| `extraccion` | lectura de los PDF con PDFBox (se usó una vez) |

## Problemas frecuentes
- *`javac` no se reconoce:* falta instalar el JDK o agregarlo al PATH.
- *Error de caracteres raros:* el script ya usa `-encoding UTF-8`; no compilar con otro comando.
- *No encuentra `datos/`:* ejecutar siempre desde la carpeta `proyecto-final`.
