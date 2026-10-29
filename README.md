# Análisis de algoritmos en el contexto de la bibliometría

Proyecto final de Análisis de algoritmos de la Universidad del Quindío. 
Programa: Ingeniería de Sistemas y Computación
Docente: Sergio Augusto Cardona Torres
Integrantes: Daniela Castañeda Cartagena y David Esteban Perez Arevalo. 

La aplicación web tiene tres módulos:

1. **Similitud textual**: compara dos o más abstracts con 4 algoritmos clásicos (Levenshtein, Needleman-Wunsch, coseno con TF-IDF y Jaccard) y 2 modelos de IA (promedio de vectores GloVe y Sentence-BERT all-MiniLM-L6-v2).
2. **Agrupamiento jerárquico**: single, complete y average linkage implementados desde cero, con dendrograma en SVG, correlación cofenética y coeficiente de silueta.
3. **Búsqueda de patrones**: KMP, Boyer-Moore (carácter malo y sufijo bueno) y Rabin-Karp (rolling hash), sin `indexOf` ni `find`, con tiempos de preprocesamiento y de búsqueda por separado.

El documento técnico se entrega aparte. Para entender qué hace cada proceso y cómo funciona, lee `docs/como_funciona.md`; los cálculos paso a paso están en `docs/calculos_paso_a_paso.xlsx`.

## Estructura

```
proyecto-final/
├── src/
│   ├── modelo/            Articulo (número, título, autores, abstract)
│   ├── datos/             Corpus: carga datos/metadatos.csv y datos/abstracts/
│   ├── extraccion/        ExtractorPdf: abstract, título y autores con PDFBox
│   ├── preprocesamiento/  Preprocesador: minúsculas, solo letras, sin palabras vacías
│   ├── similitud/         Levenshtein, NeedlemanWunsch, TfIdf, Jaccard, EmbeddingsGlove, EmbeddingsSbert, Vectores
│   ├── clustering/        AgrupamientoJerarquico, Evaluacion, Dendrograma, Fusion
│   ├── busqueda/          KMP, BoyerMoore, RabinKarp
│   ├── estructuras/       Nodo y Pila enlazada (estilo núcleo 6), usada en el traceback de Needleman-Wunsch
│   ├── casos/             CasosDeEstudio (trazas del documento) y Experimentos (tiempos y métricas)
│   └── web/               ServidorWeb (servidor HTTP del JDK) y Json
├── web/                   index.html, app.js, estilos.css
├── datos/
│   ├── abstracts/         01.txt … 20.txt (revisados a mano contra cada PDF)
│   ├── metadatos.csv      número;título;autores (revisado a mano)
│   └── embeddings/        glove_corpus_100d.txt y sbert_minilm_384d.csv
├── embeddings/            generar_embeddings_sbert.py y requirements.txt
├── resultados/            salidas de CasosDeEstudio y Experimentos (CSV, TXT, SVG, PNG)
├── lib/                   pdfbox-app-3.0.3.jar (solo para la extracción)
├── articulos/             aquí van los 20 PDF (no se suben al repositorio)
├── Dockerfile
└── compilar.bat / compilar.sh / ejecutar.bat / ejecutar.sh
```

## Cómo correrlo (Java 17 o más nuevo)

Desde la carpeta `proyecto-final` (en VS Code: *File > Open Folder…*, terminal integrada):

1. Compilar: `compilar.bat` (Windows) o `sh compilar.sh` (Linux o Mac).
2. Iniciar la aplicación: `ejecutar.bat` o `sh ejecutar.sh`, y abrir http://localhost:8080
3. Casos de estudio del documento: `java -cp bin casos.CasosDeEstudio > resultados\casos_de_estudio.txt`
4. Experimentos (matrices de similitud, clustering, tiempos de búsqueda): `java -cp bin casos.Experimentos`

Los datos de `datos/` ya están listos, así que los pasos 1 a 4 no necesitan los PDF ni Python.

### Volver a generar los datos (opcional)

- **Extraer de los PDF** (copiar los 20 PDF en `articulos/`):
  `java -cp "bin;lib/pdfbox-app-3.0.3.jar" extraccion.ExtractorPdf articulos datos_extraidos` (en Linux o Mac `;` se cambia por `:`). El programa marca con "revisar a mano" lo que no pudo sacar bien; la versión revisada es la de `datos/`.
- **GloVe**: descargar `glove-wiki-gigaword-100.gz` (128 MB) de https://github.com/RaRe-Technologies/gensim-data/releases/tag/glove-wiki-gigaword-100 y correr `java -cp bin similitud.PreparadorGlove glove-wiki-gigaword-100.gz datos`. Deja solo las 970 palabras del corpus que tienen vector.
- **Sentence-BERT** (Python 3.9 o más nuevo): `pip install -r embeddings/requirements.txt` y `python embeddings/generar_embeddings_sbert.py`. Descarga el modelo all-MiniLM-L6-v2 (unos 90 MB) la primera vez.

## Despliegue

La aplicación es un solo proceso Java que lee el puerto de la variable `PORT`. El `Dockerfile` compila el código y deja la imagen lista.
