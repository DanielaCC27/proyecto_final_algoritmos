package casos;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;
import busqueda.AlgoritmoBusqueda;
import busqueda.BoyerMoore;
import busqueda.KMP;
import busqueda.RabinKarp;
import clustering.AgrupamientoJerarquico;
import clustering.Dendrograma;
import clustering.Evaluacion;
import datos.Corpus;
import similitud.AnalizadorSimilitud;
import similitud.Vectores;

/**
 * Corre todos los experimentos y deja los resultados en la carpeta resultados/:
 *  - similitud_*.csv: matriz 20 x 20 de cada uno de los 6 algoritmos y su tiempo promedio por par.
 *  - dendrograma_*.svg y clustering.txt: los 3 criterios sobre 3 representaciones, con la
 *    correlacion cofenetica y la silueta.
 *  - busqueda.txt: tiempos de preprocesamiento y de busqueda de KMP, Boyer-Moore y Rabin-Karp.
 * Uso: java -cp bin casos.Experimentos
 */
public class Experimentos
{
    static final int REPETICIONES = 200;

    // Corre los tres experimentos en orden y deja los archivos en resultados/
    public static void main(String[] args) throws IOException
    {
        Locale.setDefault(Locale.US);
        Corpus corpus = new Corpus("datos");
        AnalizadorSimilitud an = new AnalizadorSimilitud(corpus, "datos");
        new File("resultados").mkdirs();
        similitud(corpus, an);
        clustering(corpus, an);
        busqueda(corpus);
    }

    // Matriz n x n de distancias d(i,j) = 1 - coseno(v[i], v[j]); la diagonal queda en 0. Orden O(n^2 * d)
    public static double[][] matrizDistancias(double[][] v)
    {
        int n = v.length;
        double[][] d = new double[n][n];
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                if (i != j)
                {
                    d[i][j] = 1.0 - Vectores.coseno(v[i], v[j]);
                }
            }
        }
        return d;
    }

    // Primera aparicion del patron en el texto usando nuestro KMP; -1 si no aparece
    public static int primeraPosicion(String texto, String patron)
    {
        KMP kmp = new KMP();
        kmp.preprocesar(patron.toCharArray());
        ArrayList<Integer> r = kmp.buscar(texto.toCharArray());
        if (r.isEmpty())
        {
            return -1;
        }
        return r.get(0);
    }

    // Experimento 1: matriz 20 x 20 por algoritmo (similitud_*.csv) y tiempo promedio por par
    static void similitud(Corpus corpus, AnalizadorSimilitud an) throws IOException
    {
        int n = corpus.tamano();
        double[][][] m = new double[6][n][n];
        double[] tiempos = new double[6];
        int pares = 0;
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                if (i == j)
                {
                    for (int k = 0; k < 6; k++)
                    {
                        m[k][i][j] = 1.0;
                    }
                }
                else if (i < j)
                {
                    // Se repite 5 veces para que el tiempo sea estable
                    double[] r = null;
                    double[] t = new double[6];
                    for (int rep = 0; rep < 5; rep++)
                    {
                        r = an.compararTodos(corpus.obtener(i).numero, corpus.obtener(j).numero);
                        for (int k = 0; k < 6; k++)
                        {
                            t[k] = t[k] + an.ultimosTiempos[k] / 5;
                        }
                    }
                    for (int k = 0; k < 6; k++)
                    {
                        m[k][i][j] = r[k];
                        m[k][j][i] = r[k];
                        tiempos[k] = tiempos[k] + t[k];
                    }
                    pares++;
                }
            }
        }
        String[] archivos = { "levenshtein", "needleman_wunsch", "coseno_tfidf", "jaccard", "glove", "sbert" };
        PrintWriter resumen = new PrintWriter(new File("resultados", "similitud_resumen.txt"), "UTF-8");
        resumen.println("algoritmo | minimo | promedio | maximo | tiempo promedio por par (ms)");
        for (int k = 0; k < 6; k++)
        {
            PrintWriter csv = new PrintWriter(new File("resultados", "similitud_" + archivos[k] + ".csv"), "UTF-8");
            StringBuilder enc = new StringBuilder("art");
            for (int j = 0; j < n; j++)
            {
                enc.append(";").append(corpus.obtener(j).numero);
            }
            csv.println(enc);
            double minimo = 2;
            double maximo = -2;
            double suma = 0;
            for (int i = 0; i < n; i++)
            {
                StringBuilder fila = new StringBuilder(String.valueOf(corpus.obtener(i).numero));
                for (int j = 0; j < n; j++)
                {
                    fila.append(";").append(String.format("%.4f", m[k][i][j]));
                    if (i < j)
                    {
                        minimo = Math.min(minimo, m[k][i][j]);
                        maximo = Math.max(maximo, m[k][i][j]);
                        suma = suma + m[k][i][j];
                    }
                }
                csv.println(fila);
            }
            csv.close();
            resumen.printf("%s | %.4f | %.4f | %.4f | %.4f%n", AnalizadorSimilitud.NOMBRES[k], minimo, suma / pares, maximo,
                tiempos[k] / pares);
        }
        // Correlacion entre los algoritmos (que tanto coinciden en el orden de los pares)
        resumen.println();
        resumen.println("Correlacion de Pearson entre las similitudes de los 190 pares:");
        for (int a = 0; a < 6; a++)
        {
            StringBuilder fila = new StringBuilder(String.format("%-24s", AnalizadorSimilitud.NOMBRES[a]));
            for (int b = 0; b < 6; b++)
            {
                fila.append(String.format(" %6.3f", Evaluacion.correlacionCofenetica(m[a], m[b])));
            }
            resumen.println(fila);
        }
        resumen.close();
        System.out.println("Similitud: listo (resultados/similitud_*.csv)");
    }

    // Experimento 2: 3 criterios x 3 representaciones; guarda los dendrogramas y las metricas en clustering.txt
    static void clustering(Corpus corpus, AnalizadorSimilitud an) throws IOException
    {
        String[] representaciones = { "tfidf", "glove", "sbert" };
        String[] metodos = { AgrupamientoJerarquico.SINGLE, AgrupamientoJerarquico.COMPLETE, AgrupamientoJerarquico.AVERAGE };
        String[] etiquetas = etiquetas(corpus);
        PrintWriter salida = new PrintWriter(new File("resultados", "clustering.txt"), "UTF-8");
        salida.println("representacion | metodo | correlacion cofenetica | silueta k=2..6 | mejor k | tiempo (ms)");
        for (int r = 0; r < representaciones.length; r++)
        {
            double[][] d = matrizDistancias(an.representacion(representaciones[r]));
            for (int mtd = 0; mtd < metodos.length; mtd++)
            {
                AgrupamientoJerarquico arbol = new AgrupamientoJerarquico();
                long t0 = System.nanoTime();
                for (int rep = 0; rep < REPETICIONES; rep++)
                {
                    arbol.agrupar(d, metodos[mtd]);
                }
                double ms = (System.nanoTime() - t0) / 1e6 / REPETICIONES;
                double ccc = Evaluacion.correlacionCofenetica(d, Evaluacion.matrizCofenetica(arbol.fusiones, arbol));
                StringBuilder sil = new StringBuilder();
                int mejorK = 2;
                double mejorS = -2;
                for (int k = 2; k <= 6; k++)
                {
                    double s = Evaluacion.silueta(d, arbol.cortar(k), k);
                    sil.append(String.format("%.4f ", s));
                    if (s > mejorS)
                    {
                        mejorS = s;
                        mejorK = k;
                    }
                }
                salida.printf("%s | %s | %.4f | %s| %d | %.4f%n", representaciones[r], metodos[mtd], ccc, sil, mejorK, ms);
                String svg = Dendrograma.generarSvg(arbol, etiquetas, "Dendrograma " + metodos[mtd] + " linkage ("
                    + representaciones[r] + ")", arbol.cortar(mejorK));
                PrintWriter archivo = new PrintWriter(new File("resultados", "dendrograma_" + representaciones[r] + "_"
                    + metodos[mtd] + ".svg"), "UTF-8");
                archivo.print(svg);
                archivo.close();
            }
        }
        salida.close();
        System.out.println("Clustering: listo (resultados/clustering.txt y dendrograma_*.svg)");
    }

    // Etiqueta corta de cada articulo (autor y numero) para las hojas del dendrograma
    public static String[] etiquetas(Corpus corpus)
    {
        String[] e = new String[corpus.tamano()];
        for (int i = 0; i < corpus.tamano(); i++)
        {
            String t = corpus.obtener(i).titulo;
            if (t.length() > 52)
            {
                t = t.substring(0, 50) + "...";
            }
            e[i] = corpus.obtener(i).numero + ". " + t;
        }
        return e;
    }

    // Experimento 3: tiempos de preprocesamiento y busqueda de los tres algoritmos para varios patrones
    static void busqueda(Corpus corpus) throws IOException
    {
        StringBuilder todo = new StringBuilder();
        for (int i = 0; i < corpus.tamano(); i++)
        {
            todo.append(corpus.obtener(i).resumen.toLowerCase()).append(' ');
        }
        String corpusTexto = todo.toString();
        String[] patrones = { "ai", "data", "learning", "assessment", "generative artificial intelligence",
            "artificial intelligence in education", "systematic literature review on opportunities" };
        int[] copias = { 1, 10, 100 };
        PrintWriter salida = new PrintWriter(new File("resultados", "busqueda.txt"), "UTF-8");
        salida.println("Tiempos promedio de " + REPETICIONES + " repeticiones (ms). Texto = los 20 abstracts en minuscula.");
        salida.println("copias | n | patron | m | algoritmo | ocurrencias | preprocesamiento (ms) | busqueda (ms) | comparaciones | comparaciones/n");
        for (int c = 0; c < copias.length; c++)
        {
            StringBuilder grande = new StringBuilder();
            for (int k = 0; k < copias[c]; k++)
            {
                grande.append(corpusTexto);
            }
            char[] texto = grande.toString().toCharArray();
            for (int p = 0; p < patrones.length; p++)
            {
                AlgoritmoBusqueda[] algoritmos = { new KMP(), new BoyerMoore(), new RabinKarp() };
                for (int a = 0; a < algoritmos.length; a++)
                {
                    double[] t = medir(algoritmos[a], patrones[p].toCharArray(), texto);
                    salida.printf("%d | %d | %s | %d | %s | %d | %.5f | %.5f | %d | %.3f%n", copias[c], texto.length,
                        patrones[p], patrones[p].length(), algoritmos[a].nombre(), (int) t[2], t[0], t[1],
                        algoritmos[a].comparaciones, algoritmos[a].comparaciones / (double) texto.length);
                }
            }
        }
        // Peor caso teorico: texto "aaa...a" y patron "aaaaaaaaaa": el patron aparece en TODAS las posiciones.
        // KMP sigue en O(n); Rabin-Karp verifica cada ventana (O(n*m)) y Boyer-Moore, sin la regla de Galil,
        // vuelve a comparar los m caracteres en cada alineacion (O(n*m)).
        salida.println();
        salida.println("Peor caso: texto de 100 000 letras 'a' y patron 'aaaaaaaaaa' (m = 10, n - m + 1 ocurrencias)");
        char[] texto = new char[100000];
        for (int i = 0; i < texto.length; i++)
        {
            texto[i] = 'a';
        }
        AlgoritmoBusqueda[] algoritmos = { new KMP(), new BoyerMoore(), new RabinKarp() };
        for (int a = 0; a < algoritmos.length; a++)
        {
            double[] t = medir(algoritmos[a], "aaaaaaaaaa".toCharArray(), texto);
            salida.printf("%s | preprocesamiento %.5f ms | busqueda %.5f ms | comparaciones %d | ocurrencias %d%n",
                algoritmos[a].nombre(), t[0], t[1], algoritmos[a].comparaciones, (int) t[2]);
        }
        // Efecto del primo q en Rabin-Karp: con q pequeno hay muchas colisiones que obligan a verificar
        salida.println();
        salida.println("Rabin-Karp con distintos q, patron 'assessment', los 20 abstracts (copias = 1)");
        char[] abstracts = corpusTexto.toCharArray();
        long[] primos = { 3, 13, 97, 1000000007L };
        for (int k = 0; k < primos.length; k++)
        {
            RabinKarp rk = new RabinKarp(256, primos[k]);
            double[] t = medir(rk, "assessment".toCharArray(), abstracts);
            salida.printf("q = %d | busqueda %.5f ms | colisiones %d | comparaciones %d | ocurrencias %d%n", primos[k], t[1],
                rk.colisiones, rk.comparaciones, (int) t[2]);
        }
        salida.close();
        System.out.println("Busqueda: listo (resultados/busqueda.txt)");
    }

    // Devuelve {ms de preprocesamiento, ms de busqueda, ocurrencias}
    public static double[] medir(AlgoritmoBusqueda algoritmo, char[] patron, char[] texto)
    {
        // Calentamiento de la maquina virtual de Java
        for (int r = 0; r < 20; r++)
        {
            algoritmo.preprocesar(patron);
            algoritmo.buscar(texto);
        }
        long tiempoPre = 0;
        long tiempoBus = 0;
        int ocurrencias = 0;
        for (int r = 0; r < REPETICIONES; r++)
        {
            long t0 = System.nanoTime();
            algoritmo.preprocesar(patron);
            long t1 = System.nanoTime();
            ocurrencias = algoritmo.buscar(texto).size();
            long t2 = System.nanoTime();
            tiempoPre = tiempoPre + (t1 - t0);
            tiempoBus = tiempoBus + (t2 - t1);
        }
        return new double[] { tiempoPre / 1e6 / REPETICIONES, tiempoBus / 1e6 / REPETICIONES, ocurrencias };
    }
}
