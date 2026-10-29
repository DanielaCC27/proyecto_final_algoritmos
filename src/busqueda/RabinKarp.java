package busqueda;

import java.util.ArrayList;

/**
 * Rabin-Karp con rolling hash:
 *   h(P) = (P[0]*d^(m-1) + P[1]*d^(m-2) + ... + P[m-1]) mod q
 *   t(s+1) = (d * (t(s) - T[s] * d^(m-1)) + T[s+m]) mod q
 * Si los hashes coinciden se verifica caracter por caracter; si no coincide el texto
 * es una colision (falso positivo).
 * Preprocesamiento O(m), busqueda O(n + m) esperado y O(n*m) en el peor caso.
 */
public class RabinKarp extends AlgoritmoBusqueda
{
    // d: base del hash (tamano del alfabeto); q: primo del modulo
    public int d = 256;
    public long q = 1000000007L;
    // h = d^(m-1) mod q: peso del primer caracter de la ventana, sirve para quitarlo al deslizar
    public long h;
    // Hash del patron, se calcula una sola vez
    public long hashPatron;
    // Veces que los hashes coincidieron pero el texto era distinto (falsos positivos)
    public int colisiones;

    public RabinKarp()
    {
    }

    // Constructor para el caso de estudio: un primo pequeno (por ejemplo 101) produce colisiones
    public RabinKarp(int d, long q)
    {
        this.d = d;
        this.q = q;
    }

    // Nombre para las tablas de resultados
    public String nombre()
    {
        return "Rabin-Karp";
    }

    // Fase 1: calcula h y el hash del patron con la regla de Horner. Orden de complejidad O(m).
    public void preprocesar(char[] patron)
    {
        this.patron = patron;
        m = patron.length;
        // h = d^(m-1) mod q
        h = 1;
        for (int i = 0; i < m - 1; i++)
        {
            h = (h * d) % q;
        }
        hashPatron = 0;
        for (int i = 0; i < m; i++)
        {
            hashPatron = (d * hashPatron + patron[i]) % q;
        }
    }

    public ArrayList<Integer> buscar(char[] texto)
    {
        ArrayList<Integer> posiciones = new ArrayList<Integer>();
        comparaciones = 0;
        colisiones = 0;
        int n = texto.length;
        if (n < m)
        {
            return posiciones;
        }
        // Hash de la primera ventana del texto (los primeros m caracteres)
        long hashTexto = 0;
        for (int i = 0; i < m; i++)
        {
            hashTexto = (d * hashTexto + texto[i]) % q;
        }
        // s = inicio de la ventana actual. Se compara hash con hash (una comparacion por ventana)
        for (int s = 0; s <= n - m; s++)
        {
            comparaciones++;
            if (hashTexto == hashPatron)
            {
                // Hashes iguales: se verifica caracter por caracter porque puede ser una colision
                int j = 0;
                while (j < m && patron[j] == texto[s + j])
                {
                    j++;
                    comparaciones++;
                }
                if (j < m)
                {
                    comparaciones++;
                }
                if (j == m)
                {
                    posiciones.add(s);
                }
                else
                {
                    colisiones++;
                }
            }
            // Rolling hash: quita texto[s], mete texto[s+m] y obtiene el hash de la ventana siguiente en O(1)
            if (s < n - m)
            {
                hashTexto = (d * (hashTexto - texto[s] * h) + texto[s + m]) % q;
                // El modulo de un negativo en Java es negativo: se corrige sumando q
                if (hashTexto < 0)
                {
                    hashTexto = hashTexto + q;
                }
            }
        }
        return posiciones;
    }
}
