package similitud;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Modelo de IA 1: embeddings de palabras GloVe preentrenados (Pennington, Socher y Manning, 2014),
 * version glove-wiki-gigaword-100: 400 000 palabras, 100 dimensiones, entrenados con Wikipedia 2014
 * y Gigaword 5.
 *
 * El vector de un abstract es el PROMEDIO de los vectores de sus terminos (los que estan en el
 * vocabulario de GloVe). La similitud entre dos abstracts es el coseno entre sus vectores promedio;
 * tambien se reporta la distancia euclidiana.
 *
 * Para no cargar 400 000 palabras, PreparadorGlove deja en datos/embeddings/glove_corpus_100d.txt
 * solo las palabras que aparecen en los 20 abstracts. El formato es el de GloVe: "palabra v1 v2 ... v100".
 */
public class EmbeddingsGlove
{
    // palabra -> su vector de 100 numeros (HashMap: buscar una palabra es O(1))
    public HashMap<String, double[]> vectores = new HashMap<String, double[]>();
    // Largo de cada vector (100)
    public int dimension;
    // Del ultimo abstract vectorizado: cuantos terminos se encontraron en GloVe
    public int encontrados;
    public int total;

    // Lee el archivo linea por linea: la palabra, seguida de los numeros del vector. Orden O(P * d)
    public EmbeddingsGlove(String archivo) throws IOException
    {
        BufferedReader lector = new BufferedReader(new InputStreamReader(new FileInputStream(new File(archivo)),
            StandardCharsets.UTF_8));
        String linea = lector.readLine();
        while (linea != null)
        {
            String[] partes = linea.trim().split(" ");
            if (partes.length > 2)
            {
                double[] v = new double[partes.length - 1];
                for (int i = 1; i < partes.length; i++)
                {
                    v[i - 1] = Double.parseDouble(partes[i]);
                }
                vectores.put(partes[0], v);
                dimension = v.length;
            }
            linea = lector.readLine();
        }
        lector.close();
    }

    // Vector de una palabra; null si GloVe no la conoce
    public double[] vectorPalabra(String palabra)
    {
        return vectores.get(palabra);
    }

    // Promedio de los vectores de los terminos del abstract
    public double[] vectorAbstract(String[] terminos)
    {
        double[] suma = new double[dimension];
        encontrados = 0;
        total = terminos.length;
        for (int i = 0; i < terminos.length; i++)
        {
            double[] v = vectores.get(terminos[i]);
            if (v != null)
            {
                for (int k = 0; k < dimension; k++)
                {
                    suma[k] = suma[k] + v[k];
                }
                encontrados++;
            }
        }
        // Se divide por los terminos que si tenian vector (los desconocidos no cuentan). Orden O(T * d)
        if (encontrados > 0)
        {
            for (int k = 0; k < dimension; k++)
            {
                suma[k] = suma[k] / encontrados;
            }
        }
        return suma;
    }
}
