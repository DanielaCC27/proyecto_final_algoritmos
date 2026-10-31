# Análisis de algoritmos en el contexto de la bibliometría

Proyecto final de Análisis de algoritmos, Universidad del Quindío (Ingeniería de Sistemas y Computación, docente Sergio Augusto Cardona Torres). Integrantes: Daniela Castañeda Cartagena y David Esteban Perez Arevalo. Continúa el Seguimiento 2026-2 (ranking de términos sin comparaciones) sobre el mismo corpus (repositorio del seguimiento: https://github.com/DanielaCC27/seguimiento_analisis_algoritmos): 20 artículos en PDF sobre inteligencia artificial en educación. Todo el procesamiento se hace sobre el abstract de cada artículo; el título y los autores se usan para identificar cada resultado.

Código del proyecto: https://github.com/DanielaCC27/proyecto_final_algoritmos

La aplicación web tiene cinco pestañas: *Corpus*, *Ranking de términos* (el seguimiento) y los tres módulos de los requerimientos:

1. **Similitud textual**: compara dos o más abstracts con 4 algoritmos clásicos (Levenshtein, Needleman-Wunsch, coseno con TF-IDF y Jaccard) y 2 modelos de IA (promedio de vectores GloVe y Sentence-BERT all-MiniLM-L6-v2).
2. **Agrupamiento jerárquico**: single, complete y average linkage implementados desde cero, con dendrograma en SVG, correlación cofenética y coeficiente de silueta.
3. **Búsqueda de patrones**: KMP, Boyer-Moore (carácter malo y sufijo bueno) y Rabin-Karp (rolling hash), sin `indexOf` ni `find`, con tiempos de preprocesamiento y de búsqueda por separado.

El documento técnico (arquitectura, casos de estudio paso a paso, T(n) y declaración de uso de IA) se entrega aparte. Para entender qué hace cada proceso y cómo funciona, lee `docs/como_funciona.md`; los cálculos paso a paso están en `docs/calculos_paso_a_paso.xlsx`.

## Estructura

```
proyecto-final/
├── src/
│   ├── modelo/            Articulo (número, título, autores, abstract)
│   ├── datos/             Corpus: carga datos/metadatos.csv y datos/abstracts/
│   ├── extraccion/        ExtractorPdf: abstract, título y autores con PDFBox
│   ├── seguimiento/       Clases del Seguimiento 2026-2 (Preprocesador, Termino, NodoTermino, ColaEnlazada, Ordenamientos): línea base
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
├── .github/workflows/     publicar-imagen.yml (construye y publica la imagen en ghcr.io)
├── docs/                  arquitectura, Excel de cálculos y guías
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
- **GloVe**: descargar `glove-wiki-gigaword-100.gz` (128 MB) de https://github.com/RaRe-Technologies/gensim-data/releases/tag/glove-wiki-gigaword-100 y correr `java -cp bin similitud.PreparadorGlove glove-wiki-gigaword-100.gz datos`. Deja solo las 973 palabras del corpus que tienen vector.
- **Sentence-BERT** (Python 3.9 o más nuevo): `pip install -r embeddings/requirements.txt` y `python embeddings/generar_embeddings_sbert.py`. Descarga el modelo all-MiniLM-L6-v2 (unos 90 MB) la primera vez.

## Despliegue en Azure

La aplicación está publicada en **Azure Container Apps**: https://bibliometria-uq.yellowmushroom-3a86c65b.eastus.azurecontainerapps.io/

Es un solo proceso Java que lee el puerto de la variable `PORT`. El `Dockerfile` compila el código (imagen `eclipse-temurin:21-jdk`) y deja una imagen liviana (`eclipse-temurin:21-jre`) con `bin/`, `datos/` y `web/`; no lleva los PDF, PDFBox ni Python.

### Cómo funciona

```
git push a main  ->  GitHub Actions construye la imagen  ->  ghcr.io  ->  Azure Container Apps
```

1. **Repositorio.** El código vive en https://github.com/DanielaCC27/proyecto_final_algoritmos (público, sin los PDF).
2. **Imagen.** El flujo `.github/workflows/publicar-imagen.yml` se ejecuta en cada *push* a `main`: construye la imagen con el `Dockerfile` y la publica en GitHub Container Registry como `ghcr.io/danielacc27/proyecto_final_algoritmos:latest`. Se usa ghcr.io porque la suscripción de estudiante de Azure no permite *ACR Tasks* ni crear *service principals*.
3. **Contenedor.** La aplicación `bibliometria-uq` (grupo de recursos `rg-bibliometria`, región `eastus`) toma esa imagen.

### Primera vez (ya hecho)

```powershell
az login
az extension add --name containerapp --upgrade
az provider register --namespace Microsoft.App
az provider register --namespace Microsoft.OperationalInsights
az group create --name rg-bibliometria --location eastus
az containerapp env create --name bibliometria-uq-env --resource-group rg-bibliometria --location eastus
az containerapp create --name bibliometria-uq --resource-group rg-bibliometria --environment bibliometria-uq-env `
  --image ghcr.io/danielacc27/proyecto_final_algoritmos:latest --ingress external --target-port 8080
```

### Actualizar después de un cambio en el código

1. Subir los cambios a `main` (`git add .`, `git commit -m "mensaje"`, `git push`).
2. Esperar a que termine la acción en la pestaña *Actions* de GitHub (marca verde).
3. Publicar la imagen nueva en Azure, con un sufijo de revisión distinto cada vez (`v2`, `v3`, …):

```powershell
az containerapp update --name bibliometria-uq --resource-group rg-bibliometria `
  --image ghcr.io/danielacc27/proyecto_final_algoritmos:latest --revision-suffix v2
```

### Probar y revisar

- Abrir la URL: debe cargar la página con las cinco pestañas (Corpus, Ranking de términos, Similitud, Agrupamiento y Búsqueda). `<URL>/api/articulos` devuelve los 20 artículos en JSON.
- Ver errores: `az containerapp logs show --name bibliometria-uq --resource-group rg-bibliometria --follow`
- Al terminar la sustentación se puede borrar todo con `az group delete --name rg-bibliometria --yes`.

La guía con más detalle está en `docs/despliegue_azure.md`.

**Con Docker en cualquier equipo:** `docker build -t bibliometria .` y `docker run -p 8080:8080 bibliometria`.

## Reglas del curso que cumple

- Los algoritmos pedidos están escritos a mano con arreglos, ciclos y condicionales. No se usa `indexOf`, `String.contains` ni `find`, ni librerías de similitud, clustering o búsqueda.
- Las librerías externas son solo PDFBox (leer los PDF) y, en Python, `sentence-transformers` para obtener los vectores preentrenados; la similitud entre esos vectores se calcula en Java.
