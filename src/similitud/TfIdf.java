package similitud;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Modelo de espacio vectorial TF-IDF sobre los 20 abstracts.
 *   tf(t, d)  = veces que aparece el termino t en el abstract d
 *   df(t)     = numero de abstracts que contienen t
 *   idf(t)    = log10( N / df(t) ),   N = 20
 *   w(t, d)   = tf(t, d) * idf(t)
 * Cada abstract queda como un vector de tamano V (vocabulario del corpus).
 * La similitud es el coseno del angulo entre los dos vectores.
 * Construir el modelo cuesta O(N * L + N * V) (L = terminos por abstract).
 */
public class TfIdf
{
    // Vocabulario del corpus: termino -> columna del vector (LinkedHashMap conserva el orden de aparicion)
    public LinkedHashMap<String, Integer> vocabulario = new LinkedHashMap<String, Integer>();
    // df[t]: en cuantos abstracts aparece el termino t
    public int[] df;
    // idf[t]: que tan raro es el termino t en el corpus
    public double[] idf;
    // pesos[d][t] = tf * idf: el vector TF-IDF de cada abstract
    public double[][] pesos;
    // tf[d][t]: veces que el termino t aparece en el abstract d
    public int[][] tf;
    // Cantidad de documentos
    public int N;

    // Recibe los abstracts ya preprocesados (un arreglo de terminos por documento) y arma las tablas en 4 pasos
    public TfIdf(ArrayList<String[]> documentos)
    {
        N = documentos.size();
        // 1. Vocabulario: cada termino distinto recibe un indice
        for (int d = 0; d < N; d++)
        {
            String[] terminos = documentos.get(d);
            for (int i = 0; i < terminos.length; i++)
            {
                if (!vocabulario.containsKey(terminos[i]))
                {
                    vocabulario.put(terminos[i], vocabulario.size());
                }
            }
        }
        int V = vocabulario.size();
        // 2. Frecuencia de cada termino en cada documento
        tf = new int[N][V];
        for (int d = 0; d < N; d++)
        {
            String[] terminos = documentos.get(d);
            for (int i = 0; i < terminos.length; i++)
            {
                tf[d][vocabulario.get(terminos[i])]++;
            }
        }
        // 3. df e idf
        df = new int[V];
        idf = new double[V];
        for (int t = 0; t < V; t++)
        {
            for (int d = 0; d < N; d++)
            {
                if (tf[d][t] > 0)
                {
                    df[t]++;
                }
            }
            idf[t] = Math.log10(N / (double) df[t]);
        }
        // 4. Pesos
        pesos = new double[N][V];
        for (int d = 0; d < N; d++)
        {
            for (int t = 0; t < V; t++)
            {
                pesos[d][t] = tf[d][t] * idf[t];
            }
        }
    }

    // Coseno entre los vectores de dos abstracts: 1 = mismo tema, 0 = nada en comun
    public double similitud(int documentoA, int documentoB)
    {
        return Vectores.coseno(pesos[documentoA], pesos[documentoB]);
    }

    // Columna del termino en el vocabulario; -1 si no existe
    public int indice(String termino)
    {
        Integer pos = vocabulario.get(termino);
        if (pos == null)
        {
            return -1;
        }
        return pos;
    }
}
