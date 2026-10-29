# Cómo funciona cada proceso del proyecto

Esta guía explica, en el orden en que ocurre, qué hace cada parte del programa y cómo lo hace. Sirve para estudiar el código antes de la sustentación. Cada título dice en qué archivo está el proceso. Los números de ejemplo salen del caso de estudio del artículo 11 frente al artículo 16 (`resultados/casos_de_estudio.txt`).

## 0. Vista general

```
PDF  ->  abstract + título + autores  ->  términos  ->  6 similitudes  ->  matriz de distancias
                                                                              |
                                                          agrupamiento jerárquico -> dendrograma + métricas
PDF  ->  texto de los 20 abstracts  ->  KMP, Boyer-Moore, Rabin-Karp  ->  posiciones, comparaciones y tiempos
```

Todo corre en un solo programa Java. La página web (`web/`) solo llama a los servicios del servidor (`src/web/ServidorWeb.java`) y dibuja lo que reciben.

## 1. Datos: de los PDF a los abstracts

**Dónde:** `extraccion/ExtractorPdf.java`, `datos/Corpus.java`, `modelo/Articulo.java`.

1. `ExtractorPdf` abre cada PDF con PDFBox (es lo único que hace la librería: leer el texto).
2. Busca la palabra *Abstract* y corta en *Keywords*, *Introduction* o el símbolo ©. Las marcas se buscan con nuestro KMP, no con `indexOf`.
3. El título sale de las propiedades del PDF y los autores de las líneas que siguen al título en la primera página. Lo que el programa no puede sacar bien se marca "revisar a mano".
4. El resultado se revisó contra cada PDF y quedó en `datos/abstracts/NN.txt` y `datos/metadatos.csv`.
5. `Corpus` carga esos archivos y entrega 20 objetos `Articulo` (número, título, autores, abstract). Los algoritmos usan el abstract; el título y los autores solo identifican cada resultado.

## 2. Preprocesamiento

**Dónde:** `preprocesamiento/Preprocesador.java`.

Convierte un abstract en una lista de términos:

1. Pasa todo a minúsculas.
2. Recorre el texto carácter por carácter y corta cada vez que aparece algo que no es una letra de la a a la z (sin `split` ni expresiones regulares).
3. Descarta las palabras de menos de 2 letras y las palabras vacías (*the*, *of*, *and*…). Se conserva *ai*.

Es el mismo criterio del Seguimiento 2026-2. Ejemplo: el abstract 11 queda en 114 términos y el 16 en 93.

## 3. Requerimiento 1: seis formas de medir similitud

**Dónde:** paquete `similitud/`; `AnalizadorSimilitud` las reúne y mide el tiempo de cada una.

### 3.1 Levenshtein (`Levenshtein.java`)
Cuenta cuántas ediciones (borrar, insertar o cambiar un término) hacen falta para convertir un abstract en el otro. Usa programación dinámica: llena una matriz `D` de (n+1) x (m+1) celdas, donde

`D[i][j] = min( D[i-1][j] + 1, D[i][j-1] + 1, D[i-1][j-1] + costo )`

y `costo` es 0 si los términos son iguales y 1 si no. La similitud es `1 - D[n][m] / max(n, m)`. Caso 11 vs 16: `D = 108`, similitud 0,0526. Orden: O(n·m).

### 3.2 Needleman-Wunsch (`NeedlemanWunsch.java`)
Alinea las dos secuencias de términos de punta a punta. Puntajes: coincidencia +1, sustitución −1, hueco −1. Llena una matriz `F` parecida a la anterior, pero maximizando. Después recorre la matriz desde la última celda hasta la primera (*traceback*) para escribir el alineamiento. Como ese recorrido descubre el alineamiento de atrás hacia adelante, cada paso se apila en una `Pila` propia (`estructuras/Pila.java`) y al vaciarla sale en el orden correcto. Similitud = coincidencias / longitud del alineamiento = 8/116 = 0,0690. Orden: O(n·m).

### 3.3 Coseno con TF-IDF (`TfIdf.java`)
1. `tf(t, d)`: cuántas veces aparece el término `t` en el abstract `d`.
2. `df(t)`: en cuántos de los 20 abstracts aparece `t`. `idf(t) = log10(20 / df(t))`: un término que está en todos los abstracts vale 0 y no aporta.
3. Cada abstract es un vector con una posición por término del vocabulario (987 términos); `w = tf · idf`.
4. La similitud es el coseno entre los dos vectores: `A·B / (|A| |B|)`. Caso: 10,9763 / (9,8019 · 11,1837) = 0,1001.

### 3.4 Jaccard (`Jaccard.java`)
Trata cada abstract como un conjunto de términos distintos y mide cuánto se solapan: `|A ∩ B| / |A ∪ B|`. Caso: 12 / 138 = 0,0870. No cuenta repeticiones ni orden.

### 3.5 GloVe, modelo de IA 1 (`EmbeddingsGlove.java`, `PreparadorGlove.java`)
GloVe es un modelo preentrenado que da a cada palabra un vector de 100 números; palabras que se usan en contextos parecidos quedan cerca. Aquí no se entrena nada:
1. `PreparadorGlove` (se corrió una vez) dejó en `datos/embeddings/glove_corpus_100d.txt` solo las palabras que aparecen en los 20 abstracts.
2. `EmbeddingsGlove` promedia los vectores de los términos del abstract (los que tienen vector) y compara los dos promedios con el coseno. Caso: 0,9345.

El coseno de GloVe sale siempre muy alto (0,92 a 0,99) porque promediar muchas palabras deja vectores parecidos entre sí.

### 3.6 Sentence-BERT, modelo de IA 2 (`EmbeddingsSbert.java`, `embeddings/generar_embeddings_sbert.py`)
Sentence-BERT (modelo all-MiniLM-L6-v2) convierte el abstract completo en un vector de 384 números que resume su significado. El script de Python lo hizo una vez y guardó los vectores en `datos/embeddings/sbert_minilm_384d.csv`. Java solo los lee y calcula el coseno. Caso: 0,7355.

### 3.7 Cómo se leen los resultados
Los clásicos comparan palabras exactas, por eso dan valores bajos entre artículos que hablan del mismo tema con otras palabras. Los de IA captan el significado y dan valores más altos. `resultados/similitud_resumen.txt` resume mínimo, promedio, máximo, tiempo y la correlación de Pearson entre algoritmos para los 190 pares.

## 4. Requerimiento 2: agrupamiento jerárquico

**Dónde:** paquete `clustering/`.

### 4.1 Matriz de distancias
Para cada representación (TF-IDF, GloVe, SBERT) se arma una matriz de 20 x 20 con `distancia = 1 - similitud`.

### 4.2 Agrupar (`AgrupamientoJerarquico.agrupar`)
1. Cada artículo empieza como su propio grupo.
2. Se busca el par de grupos activos con menor distancia y se unen (una `Fusion`, con su altura y tamaño).
3. Se calcula la distancia del grupo nuevo a cada grupo que queda con la fórmula de Lance-Williams según el criterio:
   - *single*: el mínimo de las dos distancias;
   - *complete*: el máximo;
   - *average*: el promedio ponderado por el tamaño de cada grupo.
4. Se repite 19 veces hasta que queda un solo grupo. Cada paso recorre la matriz (O(n²)), en total O(n³).

Los grupos se numeran como en la literatura: 0 a 19 son los artículos y 20 a 38 los grupos nuevos; la raíz es el 38.

### 4.3 El dendrograma como árbol binario
Las 19 fusiones forman un árbol binario con 20 hojas. `hojas()` recorre el árbol en preorden para dar el orden de dibujo sin cruces; `altura()` calcula la altura del árbol con la definición del curso (mayor nivel de una hoja + 1). `cortar(k)` aplica solo las primeras `n - k` fusiones y devuelve el grupo de cada artículo.

### 4.4 Dibujo (`Dendrograma.java`)
Genera un archivo SVG (texto) con orientación **vertical**: los artículos quedan abajo y la distancia de fusión sube por el eje de la izquierda. Una fusión es una "U" invertida: dos líneas verticales hasta la altura de la unión y una horizontal que las conecta. El eje empieza en un valor cercano a la primera fusión para que se vean las diferencias. Las etiquetas se colorean según el grupo del corte.

Cómo leerlo: dos artículos unidos abajo son muy parecidos; los que solo se unen cerca de la raíz son muy distintos. La altura de la unión es la distancia entre los dos grupos en ese momento.

### 4.5 Métricas (`Evaluacion.java`)
- **Correlación cofenética (CCC):** compara las distancias originales con las alturas del dendrograma (Pearson). Cerca de 1 significa que el árbol respeta los datos. Con enlace promedio sale la más alta en las tres representaciones (0,80 a 0,82).
- **Silueta:** para un corte en k grupos mide qué tan separados y compactos quedan. Va de −1 a 1; se calcula para k de 2 a 6.

## 5. Requerimiento 3: búsqueda de patrones

**Dónde:** paquete `busqueda/`. Los tres heredan de `AlgoritmoBusqueda`, que separa el trabajo en `preprocesar(patron)` y `buscar(texto)` y cuenta las comparaciones.

### 5.1 KMP (`KMP.java`)
Antes de buscar construye `pi`: para cada posición del patrón, el largo del prefijo que también es sufijo. Al fallar, `pi` dice a qué posición del patrón retroceder sin volver atrás en el texto. Cada carácter del texto se mira una vez: O(n) más O(m) de preprocesamiento.

### 5.2 Boyer-Moore (`BoyerMoore.java`)
Compara el patrón de derecha a izquierda y, al fallar, salta lo más lejos que permiten dos reglas:
- **Carácter malo:** `ultimo[c]` es la última posición de `c` en el patrón; se alinea el carácter del texto con esa posición.
- **Sufijo bueno:** si ya coincidió un sufijo, se corre el patrón hasta que ese sufijo vuelva a calzar (se calcula con el arreglo `borde`).

Se toma el mayor de los dos saltos. En texto normal con patrones largos salta casi de a `m` posiciones: O(n/m) en el mejor caso, O(n·m) en el peor.

### 5.3 Rabin-Karp (`RabinKarp.java`)
Convierte cada ventana del texto en un número (hash) y lo compara con el hash del patrón. Al correr la ventana una posición no recalcula todo: quita el carácter que sale y agrega el que entra (*rolling hash*), `t(s+1) = (d·(t(s) − T[s]·d^(m−1)) + T[s+m]) mod q`. Si los hashes coinciden compara carácter por carácter, porque puede haber una colisión (hash igual con texto distinto). Esperado O(n + m); en el peor caso O(n·m).

### 5.4 Experimentos (`casos/Experimentos.java`)
Mide, sobre los 20 abstracts unidos (y repetidos 1, 10 y 100 veces), el tiempo de preprocesamiento y de búsqueda, las comparaciones y las ocurrencias de 7 patrones de 2 a 45 caracteres, más el peor caso (100 000 letras "a" y el patrón `aaaaaaaaaa`) y el efecto del módulo `q` en Rabin-Karp. El resultado queda en `resultados/busqueda.txt`.

## 6. Estructuras de datos propias

**Dónde:** `estructuras/Nodo.java`, `estructuras/Pila.java`.

Una pila enlazada con nodo cabecera, como en el núcleo 6 del curso. `push` pone un nodo después de la cabecera, `pop` lo saca (devuelve `null` si está vacía) y cada operación cuesta O(1). Se usa en el traceback de Needleman-Wunsch.

## 7. Servidor y página web

**Dónde:** `web/ServidorWeb.java`, `web/Json.java`, `web/index.html`, `web/app.js`, `web/estilos.css`.

El servidor usa el `HttpServer` que trae el JDK (sin frameworks) y escribe el JSON a mano. Servicios:

| Servicio | Qué hace |
|---|---|
| `GET /api/articulos` | Lista los 20 artículos con título, autores y abstract. |
| `GET /api/similitud?ids=11,16` | Calcula los 6 algoritmos para cada par de artículos elegidos. |
| `GET /api/clustering?metodo=average&rep=tfidf&k=2` | Devuelve el dendrograma SVG, la CCC, las siluetas y las fusiones. |
| `GET /api/busqueda?patron=learning&mayusculas=0` | Corre KMP, Boyer-Moore y Rabin-Karp y devuelve posiciones, comparaciones y tiempos. |

La página tiene una pestaña por requerimiento. Lee el puerto de la variable `PORT` (8080 por defecto), así sirve igual en el computador y en la nube.

## 8. Cómo se reproducen los resultados

1. `compilar.bat` compila todo a `bin/`.
2. `java -cp bin casos.CasosDeEstudio` imprime los casos de estudio paso a paso.
3. `java -cp bin casos.Experimentos` regenera matrices, dendrogramas, métricas y tiempos en `resultados/`. Los tiempos cambian de un equipo a otro; las comparaciones, las similitudes y las métricas no.
4. `docs/calculos_paso_a_paso.xlsx` repite los cálculos con fórmulas de Excel para verificarlos a mano.
