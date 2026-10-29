package busqueda;

import java.util.ArrayList;

/**
 * Boyer-Moore con las dos heuristicas:
 *  - caracter malo: ultimo[c] = ultima posicion de c en el patron (-1 si no esta).
 *  - sufijo bueno: desplazamiento[j] = cuanto correr el patron cuando P[j..m-1] ya coincidio
 *    y fallo P[j-1]. Se calcula con el arreglo borde[] (borde = prefijo que tambien es sufijo).
 * El patron se compara de DERECHA a IZQUIERDA y se corre el maximo de las dos reglas.
 * Preprocesamiento O(m + sigma), busqueda O(n/m) en el mejor caso y O(n*m) en el peor.
 */
public class BoyerMoore extends AlgoritmoBusqueda
{
    // Alfabeto: los 256 primeros caracteres; los demas (comillas tipograficas, guiones largos)
    // se agrupan en la casilla 255. Agruparlos solo puede dar un salto MAS CORTO, nunca uno incorrecto.
    public static final int SIGMA = 256;
    // Tabla del caracter malo: ultimo[c] = ultima posicion de c en el patron (-1 si c no esta)
    public int[] ultimo;
    // Tabla del sufijo bueno: desplazamiento[j] = cuanto correr el patron si P[j..m-1] ya coincidio y fallo P[j-1]
    public int[] desplazamiento;
    // Bordes del patron: auxiliar para calcular la tabla anterior
    public int[] borde;

    // Nombre para las tablas de resultados
    public String nombre()
    {
        return "Boyer-Moore";
    }

    // Convierte un caracter en el indice de la tabla (0..255). O(1)
    public static int codigo(char c)
    {
        int codigo = c;
        if (codigo >= SIGMA)
        {
            codigo = SIGMA - 1;
        }
        return codigo;
    }

    // Fase 1: calcula las dos tablas. Orden de complejidad O(m + SIGMA).
    public void preprocesar(char[] patron)
    {
        this.patron = patron;
        m = patron.length;
        calcularCaracterMalo();
        calcularSufijoBueno();
    }

    // Regla del caracter malo
    // Si el texto tiene un caracter c que no coincide, el patron puede correrse hasta alinear la ultima c que tenga.
    // Orden de complejidad O(SIGMA + m).
    public void calcularCaracterMalo()
    {
        ultimo = new int[SIGMA];
        // Al principio ningun caracter esta en el patron
        for (int c = 0; c < SIGMA; c++)
        {
            ultimo[c] = -1;
        }
        // Recorriendo de izquierda a derecha, la ultima asignacion deja la ultima aparicion de cada caracter
        for (int j = 0; j < m; j++)
        {
            ultimo[codigo(patron[j])] = j;
        }
    }

    // Regla del sufijo bueno (version fuerte). Orden de complejidad O(m).
    public void calcularSufijoBueno()
    {
        desplazamiento = new int[m + 1];
        borde = new int[m + 1];
        // Caso 1: el sufijo que coincidio aparece otra vez en el patron precedido de otro caracter
        int i = m;
        int j = m + 1;
        borde[i] = j;
        while (i > 0)
        {
            while (j <= m && patron[i - 1] != patron[j - 1])
            {
                if (desplazamiento[j] == 0)
                {
                    desplazamiento[j] = j - i;
                }
                j = borde[j];
            }
            i--;
            j--;
            borde[i] = j;
        }
        // Caso 2: solo una parte del sufijo coincide con un prefijo del patron
        j = borde[0];
        for (i = 0; i <= m; i++)
        {
            if (desplazamiento[i] == 0)
            {
                desplazamiento[i] = j;
            }
            if (i == j)
            {
                j = borde[j];
            }
        }
    }

    // Fase 2: s es la posicion del texto donde se alinea el inicio del patron.
    // En el mejor caso salta casi m posiciones por vuelta: O(n/m); en el peor, O(n*m).
    public ArrayList<Integer> buscar(char[] texto)
    {
        ArrayList<Integer> posiciones = new ArrayList<Integer>();
        comparaciones = 0;
        int n = texto.length;
        int s = 0;
        while (s <= n - m)
        {
            // j empieza en el ultimo caracter del patron: se compara de derecha a izquierda
            int j = m - 1;
            comparaciones++;
            while (j >= 0 && patron[j] == texto[s + j])
            {
                j--;
                if (j >= 0)
                {
                    comparaciones++;
                }
            }
            if (j < 0)
            {
                // Coincidio todo el patron: se registra y se corre lo que indique la tabla del sufijo bueno
                posiciones.add(s);
                s = s + desplazamiento[0];
            }
            else
            {
                // Fallo en j: se corre el maximo entre las dos reglas (nunca menos de 1 porque el sufijo bueno es >= 1)
                int saltoMalo = j - ultimo[codigo(texto[s + j])];
                int saltoBueno = desplazamiento[j + 1];
                s = s + Math.max(saltoMalo, saltoBueno);
            }
        }
        return posiciones;
    }
}
