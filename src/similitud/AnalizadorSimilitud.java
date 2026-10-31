package similitud;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import datos.Corpus;
import modelo.Articulo;
import seguimiento.Preprocesador;

/**
 * Reune los 6 algoritmos del requerimiento 1 y calcula la similitud entre dos articulos.
 * Orden de los resultados: Levenshtein, Needleman-Wunsch, Coseno TF-IDF, Jaccard,
 * GloVe (coseno), Sentence-BERT (coseno). Tambien guarda el tiempo de cada uno en ms.
 */
public class AnalizadorSimilitud
{
    public static final String[] NOMBRES =
    {
        "Levenshtein", "Needleman-Wunsch", "Coseno TF-IDF", "Jaccard", "GloVe (coseno)", "Sentence-BERT (coseno)"
    };

    // Los 20 articulos
    public Corpus corpus;
    public Preprocesador preprocesador = new Preprocesador();
    // terminos.get(i): abstract i ya preprocesado (palabras limpias, sin vacias, con raiz)
    public ArrayList<String[]> terminos = new ArrayList<String[]>();
    // Modelo TF-IDF de todo el corpus
    public TfIdf tfidf;
    // Los dos modelos de IA
    public EmbeddingsGlove glove;
    public EmbeddingsSbert sbert;
    // Vector GloVe promedio de cada abstract (se calcula una vez)
    public double[][] vectoresGlove;
    // Milisegundos que tardo cada algoritmo en la ultima comparacion
    public double[] ultimosTiempos = new double[6];
    // Distancia euclidiana GloVe y SBERT de la ultima comparacion
    public double[] ultimasDistanciasEuclidianas = new double[2];

    // Prepara todo lo que se reutiliza: preprocesa los 20 abstracts, arma TF-IDF y carga los embeddings
    public AnalizadorSimilitud(Corpus corpus, String carpetaDatos) throws IOException
    {
        this.corpus = corpus;
        for (int i = 0; i < corpus.tamano(); i++)
        {
            terminos.add(preprocesador.terminos(corpus.obtener(i).resumen));
        }
        tfidf = new TfIdf(terminos);
        // Los vectores de GloVe y SBERT ya vienen calculados en datos/embeddings/
        File carpeta = new File(carpetaDatos, "embeddings");
        glove = new EmbeddingsGlove(new File(carpeta, "glove_corpus_100d.txt").getPath());
        sbert = new EmbeddingsSbert(new File(carpeta, "sbert_minilm_384d.csv").getPath());
        vectoresGlove = new double[corpus.tamano()][];
        for (int i = 0; i < corpus.tamano(); i++)
        {
            vectoresGlove[i] = glove.vectorAbstract(terminos.get(i));
        }
    }

    // Posicion (0..19) de un articulo a partir de su numero de PDF
    public int posicion(int numero)
    {
        int pos = -1;
        for (int i = 0; i < corpus.tamano(); i++)
        {
            if (corpus.obtener(i).numero == numero)
            {
                pos = i;
            }
        }
        return pos;
    }

    // Corre los 6 algoritmos sobre el mismo par y mide cuanto tarda cada uno con System.nanoTime().
    // Devuelve las 6 similitudes en el orden de NOMBRES.
    public double[] compararTodos(int numeroA, int numeroB)
    {
        int a = posicion(numeroA);
        int b = posicion(numeroB);
        String[] ta = terminos.get(a);
        String[] tb = terminos.get(b);
        double[] r = new double[6];
        long t0 = System.nanoTime();
        r[0] = new Levenshtein().similitud(ta, tb);
        long t1 = System.nanoTime();
        r[1] = new NeedlemanWunsch().similitud(ta, tb);
        long t2 = System.nanoTime();
        r[2] = tfidf.similitud(a, b);
        long t3 = System.nanoTime();
        r[3] = new Jaccard().similitud(ta, tb);
        long t4 = System.nanoTime();
        r[4] = Vectores.coseno(glove.vectorAbstract(ta), glove.vectorAbstract(tb));
        long t5 = System.nanoTime();
        Articulo artA = corpus.obtener(a);
        Articulo artB = corpus.obtener(b);
        double[] va = sbert.vectorArticulo(artA.numero);
        double[] vb = sbert.vectorArticulo(artB.numero);
        r[5] = Vectores.coseno(va, vb);
        long t6 = System.nanoTime();
        // De nanosegundos a milisegundos
        ultimosTiempos[0] = (t1 - t0) / 1e6;
        ultimosTiempos[1] = (t2 - t1) / 1e6;
        ultimosTiempos[2] = (t3 - t2) / 1e6;
        ultimosTiempos[3] = (t4 - t3) / 1e6;
        ultimosTiempos[4] = (t5 - t4) / 1e6;
        ultimosTiempos[5] = (t6 - t5) / 1e6;
        ultimasDistanciasEuclidianas[0] = Vectores.euclidiana(vectoresGlove[a], vectoresGlove[b]);
        ultimasDistanciasEuclidianas[1] = Vectores.euclidiana(va, vb);
        return r;
    }

    // tipo = "tfidf", "glove" o "sbert"
    // Vectores de los 20 abstracts segun la representacion elegida (para el clustering)
    public double[][] representacion(String tipo)
    {
        double[][] v = new double[corpus.tamano()][];
        for (int i = 0; i < corpus.tamano(); i++)
        {
            if (tipo.equals("sbert"))
            {
                v[i] = sbert.vectorArticulo(corpus.obtener(i).numero);
            }
            else if (tipo.equals("glove"))
            {
                v[i] = vectoresGlove[i];
            }
            else
            {
                v[i] = tfidf.pesos[i];
            }
        }
        return v;
    }
}
