package busqueda;

import java.util.ArrayList;

/**
 * Knuth-Morris-Pratt.
 * pi[q] = longitud del prefijo propio mas largo de P[0..q] que tambien es sufijo de P[0..q].
 * Cuando hay un fallo despues de q coincidencias, el patron se corre usando pi y el indice
 * del texto nunca retrocede.
 * Preprocesamiento O(m), busqueda O(n).
 */
public class KMP extends AlgoritmoBusqueda
{
    // Tabla de prefijos: pi[q] = largo del prefijo mas largo del patron que tambien es sufijo de patron[0..q].
    // Ejemplo con el patron "abab": pi = [0, 0, 1, 2]
    public int[] pi;

    // Nombre para las tablas de resultados
    public String nombre()
    {
        return "KMP";
    }

    // Fase 1: construye la tabla pi. El patron se compara contra si mismo.
    // k es el largo del prefijo que ya coincide con el final de lo leido; cuando falla se retrocede con pi.
    // Orden de complejidad O(m): k sube a lo sumo una vez por cada q, y solo puede bajar tanto como subio.
    public void preprocesar(char[] patron)
    {
        this.patron = patron;
        m = patron.length;
        pi = new int[m];
        pi[0] = 0;
        int k = 0;
        for (int q = 1; q < m; q++)
        {
            // Mientras no coincida, se prueba con el prefijo mas corto que tambien sirve
            while (k > 0 && patron[k] != patron[q])
            {
                k = pi[k - 1];
            }
            // Si coincide, el prefijo reconocido crece en uno
            if (patron[k] == patron[q])
            {
                k = k + 1;
            }
            pi[q] = k;
        }
    }

    // Fase 2: recorre el texto una sola vez. q = cuantos caracteres del patron coinciden hasta ahora.
    // El indice i del texto nunca retrocede; al fallar solo se mueve q. Orden de complejidad O(n).
    public ArrayList<Integer> buscar(char[] texto)
    {
        ArrayList<Integer> posiciones = new ArrayList<Integer>();
        comparaciones = 0;
        int n = texto.length;
        int q = 0;
        for (int i = 0; i < n; i++)
        {
            comparaciones++;
            // Fallo: se corre el patron usando la tabla pi (cada correccion cuenta una comparacion mas)
            while (q > 0 && patron[q] != texto[i])
            {
                q = pi[q - 1];
                comparaciones++;
            }
            // Coincidencia: un caracter mas del patron reconocido
            if (patron[q] == texto[i])
            {
                q = q + 1;
            }
            // Se reconocio el patron completo: la aparicion empieza m - 1 posiciones atras
            if (q == m)
            {
                posiciones.add(i - m + 1);
                q = pi[q - 1];
            }
        }
        return posiciones;
    }
}
