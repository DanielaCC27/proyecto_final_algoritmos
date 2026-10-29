package clustering;

import java.util.ArrayList;

/**
 * Agrupamiento jerarquico aglomerativo implementado desde cero.
 * 1. Cada abstract empieza como un grupo.
 * 2. Se buscan los dos grupos activos mas cercanos y se unen (una Fusion).
 * 3. Se actualiza la distancia del grupo nuevo a cada grupo activo k con el criterio elegido
 *    (formula de Lance-Williams):
 *      single   (enlace simple):   d(AB, k) = min( d(A,k), d(B,k) )
 *      complete (enlace completo): d(AB, k) = max( d(A,k), d(B,k) )
 *      average  (enlace promedio, UPGMA): d(AB, k) = ( |A| d(A,k) + |B| d(B,k) ) / ( |A| + |B| )
 * 4. Se repite hasta que queda un solo grupo (n - 1 fusiones).
 * Cada paso recorre la matriz para buscar el minimo: O(n^2) por paso, O(n^3) en total.
 */
public class AgrupamientoJerarquico
{
    // Criterios de enlace que se pueden elegir
    public static final String SINGLE = "single";
    public static final String COMPLETE = "complete";
    public static final String AVERAGE = "average";

    // Las n-1 uniones, en el orden en que ocurrieron; juntas forman el dendrograma
    public ArrayList<Fusion> fusiones = new ArrayList<Fusion>();
    // Cantidad de abstracts (hojas del arbol)
    public int n;

    // Recibe la matriz de distancias n x n y el criterio de enlace; devuelve la lista de fusiones.
    // Los grupos nuevos se numeran n, n+1, ..., 2n-2 (el primero que se crea es n).
    public ArrayList<Fusion> agrupar(double[][] distanciasOriginales, String metodo)
    {
        n = distanciasOriginales.length;
        // Copia de trabajo de tamano 2n-1 para guardar tambien los grupos nuevos
        double[][] d = new double[2 * n - 1][2 * n - 1];
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                d[i][j] = distanciasOriginales[i][j];
            }
        }
        // activo[g]: el grupo g todavia no se ha unido a otro; tamano[g]: cuantos abstracts tiene
        boolean[] activo = new boolean[2 * n - 1];
        int[] tamano = new int[2 * n - 1];
        for (int i = 0; i < n; i++)
        {
            activo[i] = true;
            tamano[i] = 1;
        }
        fusiones.clear();
        for (int paso = 0; paso < n - 1; paso++)
        {
            // 2. Par de grupos activos mas cercano
            int mejorA = -1;
            int mejorB = -1;
            double menor = Double.MAX_VALUE;
            for (int a = 0; a < n + paso; a++)
            {
                if (activo[a])
                {
                    for (int b = a + 1; b < n + paso; b++)
                    {
                        if (activo[b] && d[a][b] < menor)
                        {
                            menor = d[a][b];
                            mejorA = a;
                            mejorB = b;
                        }
                    }
                }
            }
            // Se crea el grupo nuevo con los dos mas cercanos; los dos viejos dejan de estar activos
            int nuevo = n + paso;
            tamano[nuevo] = tamano[mejorA] + tamano[mejorB];
            fusiones.add(new Fusion(mejorA, mejorB, menor, tamano[nuevo]));
            activo[mejorA] = false;
            activo[mejorB] = false;
            // 3. Distancia del grupo nuevo a los demas grupos activos
            for (int k = 0; k < nuevo; k++)
            {
                if (activo[k])
                {
                    // Formula de Lance-Williams segun el criterio
                    double valor;
                    if (metodo.equals(SINGLE))
                    {
                        valor = Math.min(d[mejorA][k], d[mejorB][k]);
                    }
                    else if (metodo.equals(COMPLETE))
                    {
                        valor = Math.max(d[mejorA][k], d[mejorB][k]);
                    }
                    else
                    {
                        valor = (tamano[mejorA] * d[mejorA][k] + tamano[mejorB] * d[mejorB][k])
                            / (tamano[mejorA] + tamano[mejorB]);
                    }
                    // La matriz es simetrica: se guarda en las dos posiciones
                    d[nuevo][k] = valor;
                    d[k][nuevo] = valor;
                }
            }
            activo[nuevo] = true;
        }
        return fusiones;
    }

    // Hojas (abstracts) que contiene un grupo, en el orden en que se dibujan en el dendrograma.
    // El dendrograma es un arbol binario: hojas 0..n-1, nodos internos n..2n-2, raiz 2n-2.
    // Recorrido en preorden: visita cada uno de los 2n-1 nodos una vez, O(n); pila de llamadas O(altura).
    public ArrayList<Integer> hojas(int grupo)
    {
        ArrayList<Integer> lista = new ArrayList<Integer>();
        agregarHojas(grupo, lista);
        return lista;
    }

    // Recursion: si es hoja se agrega; si es un grupo, se baja primero por la izquierda y luego por la derecha
    private void agregarHojas(int grupo, ArrayList<Integer> lista)
    {
        if (grupo < n)
        {
            lista.add(grupo);
        }
        else
        {
            Fusion f = fusiones.get(grupo - n);
            agregarHojas(f.izquierdo, lista);
            agregarHojas(f.derecho, lista);
        }
    }

    // Altura del dendrograma con la definicion del nucleo 6: maximo nivel de una hoja + 1
    // (la raiz tiene nivel 0). Visita cada nodo una vez: O(n).
    public int altura()
    {
        return alturaDesde(2 * n - 2);
    }

    // Recursion sobre el arbol: una hoja vale 1; un nodo vale 1 + el mayor de sus dos hijos
    private int alturaDesde(int grupo)
    {
        if (grupo < n)
        {
            return 1;
        }
        Fusion f = fusiones.get(grupo - n);
        int izquierda = alturaDesde(f.izquierdo);
        int derecha = alturaDesde(f.derecho);
        if (izquierda > derecha)
        {
            return izquierda + 1;
        }
        return derecha + 1;
    }

    // Corta el arbol para dejar k grupos: se aplican solo las primeras n - k fusiones.
    // Devuelve, para cada abstract, el numero de grupo (0..k-1).
    // Subir desde cada hoja cuesta a lo sumo la altura h del arbol: O(n*h + n*k), O(n^2) si el arbol es una cadena.
    public int[] cortar(int k)
    {
        // padre[g]: grupo al que se unio g (si no se ha unido, es el mismo)
        int[] padre = new int[2 * n - 1];
        for (int i = 0; i < 2 * n - 1; i++)
        {
            padre[i] = i;
        }
        for (int paso = 0; paso < n - k; paso++)
        {
            Fusion f = fusiones.get(paso);
            padre[f.izquierdo] = n + paso;
            padre[f.derecho] = n + paso;
        }
        // Cada hoja sube por los padres hasta la raiz de su grupo
        int[] raiz = new int[n];
        for (int i = 0; i < n; i++)
        {
            int r = i;
            while (padre[r] != r)
            {
                r = padre[r];
            }
            raiz[i] = r;
        }
        // Renumera las raices como 0, 1, ..., k-1
        int[] etiqueta = new int[n];
        int[] raicesVistas = new int[k];
        int cantidad = 0;
        for (int i = 0; i < n; i++)
        {
            int encontrada = -1;
            for (int g = 0; g < cantidad; g++)
            {
                if (raicesVistas[g] == raiz[i])
                {
                    encontrada = g;
                }
            }
            if (encontrada == -1)
            {
                raicesVistas[cantidad] = raiz[i];
                encontrada = cantidad;
                cantidad++;
            }
            etiqueta[i] = encontrada;
        }
        return etiqueta;
    }
}
