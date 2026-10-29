package similitud;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

/**
 * Modelo de IA 2: embeddings de oracion con Sentence-BERT (Reimers y Gurevych, 2019), modelo
 * all-MiniLM-L6-v2 (transformer de 6 capas, 384 dimensiones).
 *
 * El script embeddings/generar_embeddings_sbert.py pasa cada abstract COMPLETO (sin preprocesar,
 * porque el modelo usa el contexto y las palabras vacias) por el modelo y guarda el vector en
 * datos/embeddings/sbert_minilm_384d.csv con el formato "numero;v1;v2;...;v384".
 * El modelo ya entrega los vectores normalizados (norma 1).
 * Aqui solo se cargan los vectores y se calculan, de forma explicita, el coseno y la distancia euclidiana.
 */
public class EmbeddingsSbert
{
    // numero de articulo -> su vector de 384 numeros
    public HashMap<Integer, double[]> vectores = new HashMap<Integer, double[]>();
    // Largo de cada vector (384)
    public int dimension;

    // Lee el CSV: cada fila es el numero del articulo y sus 384 valores separados por punto y coma
    public EmbeddingsSbert(String archivo) throws IOException
    {
        List<String> lineas = Files.readAllLines(new File(archivo).toPath(), StandardCharsets.UTF_8);
        for (int i = 0; i < lineas.size(); i++)
        {
            String linea = lineas.get(i).trim();
            if (linea.isEmpty() || linea.startsWith("numero"))
            {
                continue;
            }
            String[] partes = linea.split(";");
            double[] v = new double[partes.length - 1];
            for (int k = 1; k < partes.length; k++)
            {
                v[k - 1] = Double.parseDouble(partes[k]);
            }
            vectores.put(Integer.parseInt(partes[0]), v);
            dimension = v.length;
        }
    }

    // Vector del articulo con ese numero de PDF
    public double[] vectorArticulo(int numero)
    {
        return vectores.get(numero);
    }
}
