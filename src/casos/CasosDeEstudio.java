package casos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import busqueda.BoyerMoore;
import busqueda.KMP;
import busqueda.RabinKarp;
import clustering.AgrupamientoJerarquico;
import clustering.Evaluacion;
import clustering.Fusion;
import datos.Corpus;
import modelo.Articulo;
import similitud.AnalizadorSimilitud;
import similitud.Jaccard;
import similitud.Levenshtein;
import similitud.NeedlemanWunsch;
import similitud.TfIdf;
import similitud.Vectores;

/**
 * Imprime, paso a paso, los numeros de los casos de estudio del documento tecnico:
 *  - Requerimiento 1: abstracts 11 y 16 (los dos tratan de IA generativa y evaluacion).
 *  - Requerimiento 2: matriz de distancias y fusiones de los tres criterios.
 *  - Requerimiento 3: patron "intelligence in" en la primera oracion del abstract 20.
 * Uso: java -cp bin casos.CasosDeEstudio > resultados/casos_de_estudio.txt
 */
public class CasosDeEstudio
{
    static final int ART_A = 11;
    static final int ART_B = 16;
    static final int SUBCASO = 7;
    static final int ART_BUSQUEDA = 20;
    static final long Q_CASO = 97;

    // Imprime los tres casos de estudio, uno despues del otro
    public static void main(String[] args) throws IOException
    {
        Locale.setDefault(Locale.US);
        Corpus corpus = new Corpus("datos");
        AnalizadorSimilitud analizador = new AnalizadorSimilitud(corpus, "datos");
        requerimiento1(corpus, analizador);
        requerimiento2(corpus, analizador);
        requerimiento3(corpus);
    }

    // Imprime un encabezado separado por lineas
    static void titulo(String texto)
    {
        System.out.println();
        System.out.println("==================================================================");
        System.out.println(texto);
        System.out.println("==================================================================");
    }

    // Caso 1: los 6 algoritmos de similitud calculados paso a paso entre dos articulos
    static void requerimiento1(Corpus corpus, AnalizadorSimilitud an)
    {
        Articulo a = corpus.buscarPorNumero(ART_A);
        Articulo b = corpus.buscarPorNumero(ART_B);
        String[] ta = an.terminos.get(an.posicion(ART_A));
        String[] tb = an.terminos.get(an.posicion(ART_B));
        titulo("REQUERIMIENTO 1. Caso de estudio: articulo " + ART_A + " vs articulo " + ART_B);
        System.out.println("A = " + a.titulo + " (" + a.autores + ")");
        System.out.println("B = " + b.titulo + " (" + b.autores + ")");
        System.out.println("Terminos de A (n = " + ta.length + "): " + String.join(" ", ta));
        System.out.println("Terminos de B (m = " + tb.length + "): " + String.join(" ", tb));

        // ---------- Levenshtein ----------
        String[] sa = Arrays.copyOf(ta, SUBCASO);
        String[] sb = Arrays.copyOf(tb, SUBCASO);
        titulo("1.1 Levenshtein: subcaso con los " + SUBCASO + " primeros terminos");
        Levenshtein lev = new Levenshtein();
        int dSub = lev.distancia(sa, sb);
        imprimirMatriz(lev.matriz, sa, sb);
        System.out.println("Distancia del subcaso = " + dSub + "; similitud = 1 - " + dSub + "/" + SUBCASO + " = "
            + f(1 - dSub / (double) SUBCASO));
        int dTotal = lev.distancia(ta, tb);
        System.out.println("Abstracts completos: D[" + ta.length + "][" + tb.length + "] = " + dTotal
            + "; similitud = 1 - " + dTotal + "/" + Math.max(ta.length, tb.length) + " = "
            + f(1 - dTotal / (double) Math.max(ta.length, tb.length)));
        System.out.println("Celdas llenadas: " + ((ta.length + 1) * (tb.length + 1)));

        // ---------- Needleman-Wunsch ----------
        titulo("1.2 Needleman-Wunsch (+1, -1, -1): subcaso con los " + SUBCASO + " primeros terminos");
        NeedlemanWunsch nw = new NeedlemanWunsch();
        nw.calcularMatriz(sa, sb);
        imprimirMatriz(nw.matriz, sa, sb);
        nw.reconstruir(sa, sb);
        System.out.println("Alineamiento A: " + String.join(" | ", nw.filaA));
        System.out.println("Alineamiento B: " + String.join(" | ", nw.filaB));
        System.out.println("Puntaje = " + nw.puntaje + "; coincidencias = " + nw.coincidencias + "; longitud = "
            + nw.longitudAlineamiento + "; similitud = " + f(nw.coincidencias / (double) nw.longitudAlineamiento));
        double simNw = nw.similitud(ta, tb);
        System.out.println("Abstracts completos: puntaje = " + nw.puntaje + ", coincidencias = " + nw.coincidencias
            + ", longitud del alineamiento = " + nw.longitudAlineamiento + ", similitud = " + f(simNw));
        StringBuilder emparejadas = new StringBuilder();
        for (int i = 0; i < nw.filaA.size(); i++)
        {
            if (nw.filaA.get(i).equals(nw.filaB.get(i)))
            {
                emparejadas.append(nw.filaA.get(i)).append(' ');
            }
        }
        System.out.println("Terminos emparejados en orden: " + emparejadas);

        // ---------- TF-IDF ----------
        titulo("1.3 Coseno con TF-IDF (idf = log10(20 / df))");
        TfIdf tfidf = an.tfidf;
        int pa = an.posicion(ART_A);
        int pb = an.posicion(ART_B);
        System.out.println("Vocabulario del corpus V = " + tfidf.vocabulario.size());
        System.out.println("termino | tfA | tfB | df | idf | wA | wB | wA*wB");
        double punto = 0;
        for (String t : tfidf.vocabulario.keySet())
        {
            int k = tfidf.indice(t);
            if (tfidf.tf[pa][k] > 0 && tfidf.tf[pb][k] > 0)
            {
                double prod = tfidf.pesos[pa][k] * tfidf.pesos[pb][k];
                punto = punto + prod;
                System.out.println(t + " | " + tfidf.tf[pa][k] + " | " + tfidf.tf[pb][k] + " | " + tfidf.df[k] + " | "
                    + f(tfidf.idf[k]) + " | " + f(tfidf.pesos[pa][k]) + " | " + f(tfidf.pesos[pb][k]) + " | " + f(prod));
            }
        }
        double na = Vectores.norma(tfidf.pesos[pa]);
        double nb = Vectores.norma(tfidf.pesos[pb]);
        System.out.println("Producto punto A.B = " + f(punto));
        System.out.println("|A| = " + f(na) + "   |B| = " + f(nb));
        System.out.println("Terminos de A con mayor peso:");
        imprimirMayoresPesos(tfidf, pa, 6);
        System.out.println("Terminos de B con mayor peso:");
        imprimirMayoresPesos(tfidf, pb, 6);
        double cos = Vectores.coseno(tfidf.pesos[pa], tfidf.pesos[pb]);
        System.out.println("cos(theta) = " + f(punto) + " / (" + f(na) + " * " + f(nb) + ") = " + f(cos));
        System.out.println("theta = " + f(Vectores.anguloGrados(tfidf.pesos[pa], tfidf.pesos[pb])) + " grados");

        // ---------- Jaccard ----------
        titulo("1.4 Jaccard");
        Jaccard jac = new Jaccard();
        double j = jac.similitud(ta, tb);
        System.out.println("|A| = " + jac.tamanoA + "  |B| = " + jac.tamanoB + "  |A n B| = " + jac.interseccion.size()
            + "  |A u B| = " + jac.tamanoUnion);
        System.out.println("A n B = " + jac.interseccion);
        System.out.println("J = " + jac.interseccion.size() + " / " + jac.tamanoUnion + " = " + f(j));

        // ---------- GloVe ----------
        titulo("1.5 GloVe 100d: promedio de vectores de palabra");
        System.out.println("Primeras 5 dimensiones de los 3 primeros terminos de A:");
        for (int i = 0; i < 3; i++)
        {
            System.out.println(ta[i] + " = " + primeras(an.glove.vectorPalabra(ta[i]), 5));
        }
        double[] ga = an.glove.vectorAbstract(ta);
        System.out.println("A: terminos con vector = " + an.glove.encontrados + " de " + an.glove.total);
        System.out.println("Faltan en GloVe (A): " + faltantes(an, ta));
        double[] gb = an.glove.vectorAbstract(tb);
        System.out.println("B: terminos con vector = " + an.glove.encontrados + " de " + an.glove.total);
        System.out.println("Faltan en GloVe (B): " + faltantes(an, tb));
        System.out.println("vector promedio A (5 primeras) = " + primeras(ga, 5));
        System.out.println("vector promedio B (5 primeras) = " + primeras(gb, 5));
        System.out.println("A.B = " + f(Vectores.productoPunto(ga, gb)) + "  |A| = " + f(Vectores.norma(ga)) + "  |B| = "
            + f(Vectores.norma(gb)));
        System.out.println("cos = " + f(Vectores.coseno(ga, gb)) + "   angulo = " + f(Vectores.anguloGrados(ga, gb))
            + " grados   euclidiana = " + f(Vectores.euclidiana(ga, gb)));
        System.out.println("Rango de coseno GloVe en los 190 pares: ver resultados/similitud_glove.csv");

        // ---------- Sentence-BERT ----------
        titulo("1.6 Sentence-BERT all-MiniLM-L6-v2 (384d)");
        double[] va = an.sbert.vectorArticulo(ART_A);
        double[] vb = an.sbert.vectorArticulo(ART_B);
        System.out.println("A (5 primeras) = " + primeras(va, 5));
        System.out.println("B (5 primeras) = " + primeras(vb, 5));
        double suma5 = 0;
        for (int k = 0; k < 5; k++)
        {
            suma5 = suma5 + va[k] * vb[k];
            System.out.println("  a" + (k + 1) + "*b" + (k + 1) + " = " + f6(va[k]) + " * " + f6(vb[k]) + " = "
                + f6(va[k] * vb[k]));
        }
        System.out.println("Suma de los 5 primeros productos = " + f6(suma5));
        System.out.println("A.B (384 productos) = " + f(Vectores.productoPunto(va, vb)) + "  |A| = " + f(Vectores.norma(va))
            + "  |B| = " + f(Vectores.norma(vb)));
        System.out.println("cos = " + f(Vectores.coseno(va, vb)) + "   angulo = " + f(Vectores.anguloGrados(va, vb))
            + " grados   euclidiana = " + f(Vectores.euclidiana(va, vb)) + " (= raiz(2 - 2cos) = "
            + f(Math.sqrt(2 - 2 * Vectores.coseno(va, vb))) + ")");

        titulo("1.7 Resumen del caso de estudio");
        double[] r = an.compararTodos(ART_A, ART_B);
        for (int k = 0; k < 6; k++)
        {
            System.out.println(AnalizadorSimilitud.NOMBRES[k] + " = " + f(r[k]));
        }
    }

    static String faltantes(AnalizadorSimilitud an, String[] t)
    {
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < t.length; i++)
        {
            if (an.glove.vectorPalabra(t[i]) == null)
            {
                r.append(t[i]).append(' ');
            }
        }
        return r.toString();
    }

    static void imprimirMayoresPesos(TfIdf tfidf, int doc, int cuantos)
    {
        int V = tfidf.vocabulario.size();
        String[] palabras = tfidf.vocabulario.keySet().toArray(new String[0]);
        boolean[] usado = new boolean[V];
        for (int c = 0; c < cuantos; c++)
        {
            int mejor = -1;
            for (int k = 0; k < V; k++)
            {
                if (!usado[k] && (mejor == -1 || tfidf.pesos[doc][k] > tfidf.pesos[doc][mejor]))
                {
                    mejor = k;
                }
            }
            usado[mejor] = true;
            System.out.println("  " + palabras[mejor] + ": tf = " + tfidf.tf[doc][mejor] + ", df = " + tfidf.df[mejor]
                + ", w = " + f(tfidf.pesos[doc][mejor]));
        }
    }

    // Caso 2: matriz de distancias y fusiones de los tres criterios de enlace
    static void requerimiento2(Corpus corpus, AnalizadorSimilitud an)
    {
        titulo("REQUERIMIENTO 2. Matriz de distancias d = 1 - coseno(TF-IDF)");
        double[][] v = an.representacion("tfidf");
        double[][] d = Experimentos.matrizDistancias(v);
        System.out.print("     ");
        for (int j = 0; j < d.length; j++)
        {
            System.out.printf("%5d", corpus.obtener(j).numero);
        }
        System.out.println();
        for (int i = 0; i < d.length; i++)
        {
            System.out.printf("%5d", corpus.obtener(i).numero);
            for (int j = 0; j < d.length; j++)
            {
                System.out.printf("%5.2f", d[i][j]);
            }
            System.out.println();
        }
        String[] metodos = { AgrupamientoJerarquico.SINGLE, AgrupamientoJerarquico.COMPLETE, AgrupamientoJerarquico.AVERAGE };
        for (int mtd = 0; mtd < metodos.length; mtd++)
        {
            titulo("Fusiones con " + metodos[mtd] + " linkage");
            AgrupamientoJerarquico arbol = new AgrupamientoJerarquico();
            ArrayList<Fusion> fusiones = arbol.agrupar(d, metodos[mtd]);
            for (int p = 0; p < fusiones.size(); p++)
            {
                Fusion fu = fusiones.get(p);
                System.out.println("Paso " + (p + 1) + ": G" + (arbol.n + p) + " = " + nombreGrupo(arbol, fu.izquierdo, corpus)
                    + " + " + nombreGrupo(arbol, fu.derecho, corpus) + "  a distancia " + f(fu.altura) + "  (tamano "
                    + fu.tamano + ")");
            }
            double ccc = Evaluacion.correlacionCofenetica(d, Evaluacion.matrizCofenetica(fusiones, arbol));
            System.out.println("Correlacion cofenetica = " + f(ccc));
            for (int k = 2; k <= 6; k++)
            {
                System.out.println("  silueta k=" + k + ": " + f(Evaluacion.silueta(d, arbol.cortar(k), k)));
            }
        }
    }

    static String nombreGrupo(AgrupamientoJerarquico arbol, int grupo, Corpus corpus)
    {
        if (grupo < arbol.n)
        {
            return "{" + corpus.obtener(grupo).numero + "}";
        }
        ArrayList<Integer> hojas = arbol.hojas(grupo);
        StringBuilder r = new StringBuilder("G" + grupo + "{");
        for (int i = 0; i < hojas.size(); i++)
        {
            if (i > 0)
            {
                r.append(",");
            }
            r.append(corpus.obtener(hojas.get(i)).numero);
        }
        return r.append("}").toString();
    }

    // Caso 3: KMP, Boyer-Moore y Rabin-Karp con trazas de cada paso sobre un texto corto
    static void requerimiento3(Corpus corpus)
    {
        String resumen = corpus.buscarPorNumero(ART_BUSQUEDA).resumen.toLowerCase();
        String inicioFrase = "according to";
        String finFrase = "educational technology.";
        int ini = Experimentos.primeraPosicion(resumen, inicioFrase);
        int fin = Experimentos.primeraPosicion(resumen, finFrase) + finFrase.length();
        String texto = resumen.substring(ini, fin);
        String patron = "intelligence in";
        char[] t = texto.toCharArray();
        char[] p = patron.toCharArray();
        titulo("REQUERIMIENTO 3. Patron \"" + patron + "\" (m = " + p.length + ") en texto de n = " + t.length);
        System.out.println("Texto: " + texto);
        System.out.println("Posiciones (indice: caracter):");
        StringBuilder regla = new StringBuilder();
        for (int i = 0; i < t.length; i++)
        {
            regla.append(i).append(":").append(t[i] == ' ' ? "_" : String.valueOf(t[i])).append(' ');
        }
        System.out.println(regla);

        // ---------- KMP ----------
        titulo("3.1 KMP");
        KMP kmp = new KMP();
        kmp.preprocesar(p);
        System.out.println("q   : " + indices(p.length));
        System.out.println("P[q]: " + caracteres(p));
        System.out.println("pi  : " + Arrays.toString(kmp.pi));
        ArrayList<Integer> rk = trazaKmp(kmp, t, p);
        System.out.println("Ocurrencias = " + rk + "; comparaciones = " + kmp.comparaciones);

        // ---------- Boyer-Moore ----------
        titulo("3.2 Boyer-Moore");
        BoyerMoore bm = new BoyerMoore();
        bm.preprocesar(p);
        System.out.println("Caracter malo (ultima posicion en el patron):");
        boolean[] visto = new boolean[256];
        for (int j = 0; j < p.length; j++)
        {
            if (!visto[p[j]])
            {
                visto[p[j]] = true;
                System.out.println("  ultimo['" + p[j] + "'] = " + bm.ultimo[p[j]]);
            }
        }
        System.out.println("  cualquier otro caracter = -1");
        System.out.println("j            : " + indices(p.length + 1));
        System.out.println("borde[j]     : " + Arrays.toString(bm.borde));
        System.out.println("desplaz.[j]  : " + Arrays.toString(bm.desplazamiento));
        trazaBoyerMoore(bm, t, p);

        // ---------- Rabin-Karp ----------
        titulo("3.3 Rabin-Karp (d = 256, q = " + Q_CASO + ")");
        RabinKarp rab = new RabinKarp(256, Q_CASO);
        rab.preprocesar(p);
        System.out.println("h = 256^" + (p.length - 1) + " mod " + Q_CASO + " = " + rab.h);
        long hp = 0;
        for (int i = 0; i < p.length; i++)
        {
            long antes = hp;
            hp = (256 * hp + p[i]) % Q_CASO;
            System.out.println("  paso " + i + ": (256*" + antes + " + " + (int) p[i] + " ['" + p[i] + "']) mod " + Q_CASO
                + " = " + hp);
        }
        System.out.println("hash(P) = " + rab.hashPatron);
        trazaRabinKarp(rab, t, p);
        ArrayList<Integer> res = rab.buscar(t);
        System.out.println("Ocurrencias = " + res + "; colisiones = " + rab.colisiones + "; comparaciones = "
            + rab.comparaciones);
        long[] otrosQ = { 13, 101, 1000000007L };
        for (int k = 0; k < otrosQ.length; k++)
        {
            RabinKarp otro = new RabinKarp(256, otrosQ[k]);
            otro.preprocesar(p);
            otro.buscar(t);
            System.out.println("Con q = " + otrosQ[k] + ": colisiones = " + otro.colisiones + "; comparaciones = "
                + otro.comparaciones);
        }
    }

    // Igual que KMP.buscar pero imprimiendo cada comparacion
    static ArrayList<Integer> trazaKmp(KMP kmp, char[] t, char[] p)
    {
        // Misma logica de KMP.buscar, pero imprime cada vez que el estado q cambia por un fallo o un acierto completo
        int q = 0;
        int m = p.length;
        for (int i = 0; i < t.length; i++)
        {
            while (q > 0 && p[q] != t[i])
            {
                System.out.println("  i=" + i + " T[i]='" + t[i] + "' falla con P[" + q + "]='" + p[q] + "' -> q = pi["
                    + (q - 1) + "] = " + kmp.pi[q - 1]);
                q = kmp.pi[q - 1];
            }
            if (p[q] == t[i])
            {
                q = q + 1;
            }
            if (q == m)
            {
                System.out.println("  i=" + i + " q=" + m + " -> OCURRENCIA en " + (i - m + 1) + "; q = pi[" + (m - 1)
                    + "] = " + kmp.pi[m - 1]);
                q = kmp.pi[q - 1];
            }
        }
        return kmp.buscar(t);
    }

    // Igual que BoyerMoore.buscar pero imprimiendo cada alineamiento
    static void trazaBoyerMoore(BoyerMoore bm, char[] t, char[] p)
    {
        int n = t.length;
        int m = p.length;
        int s = 0;
        int alineaciones = 0;
        long comparaciones = 0;
        while (s <= n - m)
        {
            alineaciones++;
            int j = m - 1;
            comparaciones++;
            while (j >= 0 && p[j] == t[s + j])
            {
                j--;
                if (j >= 0)
                {
                    comparaciones++;
                }
            }
            String ventana = new String(t, s, m).replace(' ', '_');
            if (j < 0)
            {
                System.out.println("  s=" + s + " [" + ventana + "] coincide todo -> OCURRENCIA; s += desplaz.[0] = "
                    + bm.desplazamiento[0]);
                s = s + bm.desplazamiento[0];
            }
            else
            {
                char c = t[s + j];
                int malo = j - bm.ultimo[BoyerMoore.codigo(c)];
                int bueno = bm.desplazamiento[j + 1];
                System.out.println("  s=" + s + " [" + ventana + "] falla en j=" + j + " (T='" + (c == ' ' ? "_" : c)
                    + "', P='" + p[j] + "'): caracter malo = " + j + " - (" + bm.ultimo[BoyerMoore.codigo(c)] + ") = "
                    + malo + ", sufijo bueno = desplaz.[" + (j + 1) + "] = " + bueno + " -> s += " + Math.max(malo, bueno));
                s = s + Math.max(malo, bueno);
            }
        }
        ArrayList<Integer> r = bm.buscar(t);
        System.out.println("Ocurrencias = " + r + "; alineaciones = " + alineaciones + "; comparaciones = "
            + bm.comparaciones + " (traza: " + comparaciones + ")");
    }

    // Igual que RabinKarp.buscar pero imprimiendo hashes y colisiones
    static void trazaRabinKarp(RabinKarp rab, char[] t, char[] p)
    {
        int m = p.length;
        long ht = 0;
        for (int i = 0; i < m; i++)
        {
            ht = (256 * ht + t[i]) % Q_CASO;
        }
        System.out.println("s | ventana | hash | resultado");
        for (int s = 0; s <= t.length - m; s++)
        {
            String ventana = new String(t, s, m).replace(' ', '_');
            String resultado = "";
            if (ht == rab.hashPatron)
            {
                boolean igual = true;
                for (int j = 0; j < m; j++)
                {
                    if (p[j] != t[s + j])
                    {
                        igual = false;
                    }
                }
                resultado = igual ? "hash igual -> verificar -> OCURRENCIA" : "hash igual -> verificar -> COLISION";
            }
            if (s < 6 || !resultado.isEmpty())
            {
                System.out.println(s + " | " + ventana + " | " + ht + " | " + resultado);
            }
            if (s < t.length - m)
            {
                long anterior = ht;
                ht = (256 * (ht - t[s] * rab.h) + t[s + m]) % Q_CASO;
                if (ht < 0)
                {
                    ht = ht + Q_CASO;
                }
                if (s < 3)
                {
                    System.out.println("    t(" + (s + 1) + ") = (256 * (" + anterior + " - " + (int) t[s] + " * " + rab.h
                        + ") + " + (int) t[s + m] + ") mod " + Q_CASO + " = " + ht);
                }
            }
        }
    }

    static String indices(int m)
    {
        StringBuilder r = new StringBuilder("[");
        for (int i = 0; i < m; i++)
        {
            r.append(i);
            if (i < m - 1)
            {
                r.append(", ");
            }
        }
        return r.append("]").toString();
    }

    static String caracteres(char[] p)
    {
        StringBuilder r = new StringBuilder("[");
        for (int i = 0; i < p.length; i++)
        {
            r.append(p[i]);
            if (i < p.length - 1)
            {
                r.append(", ");
            }
        }
        return r.append("]").toString();
    }

    // Imprime la matriz de programacion dinamica con los terminos en los bordes
    static void imprimirMatriz(int[][] mat, String[] a, String[] b)
    {
        StringBuilder enc = new StringBuilder(String.format("%-14s%6s", "", "-"));
        for (int j = 0; j < b.length; j++)
        {
            enc.append(String.format("%14s", b[j]));
        }
        System.out.println(enc);
        for (int i = 0; i < mat.length; i++)
        {
            StringBuilder fila = new StringBuilder(String.format("%-14s", i == 0 ? "-" : a[i - 1]));
            for (int j = 0; j < mat[i].length; j++)
            {
                fila.append(String.format(j == 0 ? "%6d" : "%14d", mat[i][j]));
            }
            System.out.println(fila);
        }
    }

    static String primeras(double[] v, int k)
    {
        StringBuilder r = new StringBuilder("(");
        for (int i = 0; i < k; i++)
        {
            r.append(f(v[i]));
            if (i < k - 1)
            {
                r.append(", ");
            }
        }
        return r.append(", ...)").toString();
    }

    // Formato de un numero con 4 decimales
    static String f(double x)
    {
        return String.format(Locale.US, "%.4f", x);
    }

    // Formato de un numero con 6 decimales
    static String f6(double x)
    {
        return String.format(Locale.US, "%.6f", x);
    }
}
