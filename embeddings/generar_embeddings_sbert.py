"""
Genera los embeddings de Sentence-BERT (modelo all-MiniLM-L6-v2) de los 20 abstracts.

Uso (desde la carpeta proyecto-final):
    pip install -r embeddings/requirements.txt
    python embeddings/generar_embeddings_sbert.py

Lee datos/abstracts/01.txt ... 20.txt y escribe datos/embeddings/sbert_minilm_384d.csv
con una fila por articulo: numero;v1;v2;...;v384.

El modelo es preentrenado (no se entrena nada aqui). Recibe el abstract COMPLETO,
lo divide en tokens WordPiece (maximo 256; lo que pase de 256 tokens se corta),
lo pasa por 6 capas de transformer y promedia los vectores de los tokens (mean pooling).
normalize_embeddings=True deja cada vector con norma 1.
La similitud (coseno y distancia euclidiana) NO se calcula aqui: la calcula el programa en Java
(similitud/Vectores.java).
"""
import os
import time
from sentence_transformers import SentenceTransformer

CARPETA_ABSTRACTS = os.path.join("datos", "abstracts")
SALIDA = os.path.join("datos", "embeddings", "sbert_minilm_384d.csv")

modelo = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
print("Longitud maxima en tokens:", modelo.max_seq_length)

numeros = list(range(1, 21))
textos = []
for n in numeros:
    with open(os.path.join(CARPETA_ABSTRACTS, f"{n:02d}.txt"), encoding="utf-8") as archivo:
        textos.append(archivo.read().strip())

inicio = time.perf_counter()
vectores = modelo.encode(textos, normalize_embeddings=True)
duracion = time.perf_counter() - inicio
print(f"Tiempo de inferencia: {duracion * 1000:.1f} ms para 20 abstracts ({duracion * 50:.1f} ms por abstract)")
for n, texto in zip(numeros, textos):
    print(f"Abstract {n:02d}: {len(modelo.tokenizer(texto)['input_ids'])} tokens (se usan hasta {modelo.max_seq_length})")

os.makedirs(os.path.dirname(SALIDA), exist_ok=True)
with open(SALIDA, "w", encoding="utf-8") as salida:
    salida.write("numero;" + ";".join(f"v{i + 1}" for i in range(vectores.shape[1])) + "\n")
    for n, v in zip(numeros, vectores):
        salida.write(str(n) + ";" + ";".join(f"{x:.8f}" for x in v) + "\n")

print("Listo:", SALIDA, vectores.shape)
