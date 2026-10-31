package similitud;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import datos.Corpus;
import seguimiento.Preprocesador;

/**
 * Deja en un archivo pequeno solo los vectores GloVe de las palabras que aparecen en los abstracts.
 * Uso (una sola vez):
 *   java -cp bin similitud.PreparadorGlove glove-wiki-gigaword-100.gz datos
 * Acepta el .gz de gensim-data o el glove.6B.100d.txt de Stanford (los dos tienen "palabra v1 ... v100").
 */
public class PreparadorGlove
{
    // Herramienta de una sola vez: se corre aparte, no hace parte de la aplicacion web.
    // Orden de complejidad O(P): lee una vez cada linea del archivo GloVe grande.
    public static void main(String[] args) throws IOException
    {
        String archivoGlove = args[0];
        String carpetaDatos = "datos";
        if (args.length > 1)
        {
            carpetaDatos = args[1];
        }
        Corpus corpus = new Corpus(carpetaDatos);
        Preprocesador pre = new Preprocesador();
        // Conjunto de todos los terminos que usan los 20 abstracts
        HashSet<String> necesarias = new HashSet<String>();
        for (int i = 0; i < corpus.tamano(); i++)
        {
            String[] terminos = pre.terminos(corpus.obtener(i).resumen);
            for (int k = 0; k < terminos.length; k++)
            {
                necesarias.add(terminos[k]);
            }
        }
        InputStream entrada = new FileInputStream(archivoGlove);
        if (archivoGlove.endsWith(".gz"))
        {
            entrada = new GZIPInputStream(entrada);
        }
        BufferedReader lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        new File(carpetaDatos, "embeddings").mkdirs();
        PrintWriter salida = new PrintWriter(new File(new File(carpetaDatos, "embeddings"), "glove_corpus_100d.txt"),
            "UTF-8");
        // Se recorre el GloVe completo y solo se copian las lineas de las palabras necesarias
        int escritas = 0;
        String linea = lector.readLine();
        while (linea != null)
        {
            // La palabra es todo lo que hay antes del primer espacio
            int espacio = 0;
            while (espacio < linea.length() && linea.charAt(espacio) != ' ')
            {
                espacio++;
            }
            String palabra = linea.substring(0, espacio);
            // La primera linea del formato word2vec de gensim es "400000 100": no es una palabra
            if (necesarias.contains(palabra))
            {
                salida.println(linea.trim());
                escritas++;
            }
            linea = lector.readLine();
        }
        lector.close();
        salida.close();
        System.out.println("Terminos distintos en los abstracts: " + necesarias.size());
        System.out.println("Terminos con vector GloVe: " + escritas);
    }
}
