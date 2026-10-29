package similitud;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Coeficiente de Jaccard entre los CONJUNTOS de terminos de dos abstracts:
 *   J(A, B) = |A interseccion B| / |A union B|
 * Construir los conjuntos es O(n + m) y la interseccion O(min(|A|, |B|)) con tabla hash.
 */
public class Jaccard
{
    // Terminos compartidos por los dos abstracts (se muestran en el caso de estudio)
    public ArrayList<String> interseccion = new ArrayList<String>();
    public int tamanoA;
    public int tamanoB;
    public int tamanoUnion;

    // Usa HashSet para que cada termino cuente una sola vez, sin importar cuantas veces se repita
    public double similitud(String[] a, String[] b)
    {
        HashSet<String> conjuntoA = new HashSet<String>();
        HashSet<String> conjuntoB = new HashSet<String>();
        // Cada abstract se convierte en conjunto: los repetidos se descartan solos
        for (int i = 0; i < a.length; i++)
        {
            conjuntoA.add(a[i]);
        }
        for (int j = 0; j < b.length; j++)
        {
            conjuntoB.add(b[j]);
        }
        // Interseccion: terminos de A que tambien estan en B (contains en tabla hash es O(1))
        interseccion.clear();
        for (String termino : conjuntoA)
        {
            if (conjuntoB.contains(termino))
            {
                interseccion.add(termino);
            }
        }
        tamanoA = conjuntoA.size();
        tamanoB = conjuntoB.size();
        // |A union B| = |A| + |B| - |A interseccion B|
        tamanoUnion = tamanoA + tamanoB - interseccion.size();
        if (tamanoUnion == 0)
        {
            return 1.0;
        }
        return interseccion.size() / (double) tamanoUnion;
    }
}
