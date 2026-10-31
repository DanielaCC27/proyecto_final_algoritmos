package similitud;

/**
 * Distancia de edicion de Levenshtein entre dos abstracts, tomando cada TERMINO
 * (palabra ya preprocesada) como un simbolo.
 * Programacion dinamica (tabulacion, Bottom Up):
 *   D[i][0] = i,  D[0][j] = j
 *   D[i][j] = min( D[i-1][j] + 1,            (borrar a_i)
 *                  D[i][j-1] + 1,            (insertar b_j)
 *                  D[i-1][j-1] + costo )     (sustituir; costo = 0 si a_i = b_j, 1 si no)
 * Similitud normalizada = 1 - D[n][m] / max(n, m).   Tiempo y espacio O(n*m).
 *
 * Como en las diapositivas de programacion dinamica del profesor, la misma recurrencia se escribe de tres formas:
 *   distanciaRecursiva  copia la definicion: cada llamada hace tres llamadas mas, O(3^(n+m)), solo sirve para textos cortos.
 *   distanciaMemo       Top Down (memorizacion): el recursivo consulta la tabla antes de calcular, O(n*m).
 *   distancia           Bottom Up (tabulacion): llena la tabla con dos ciclos, O(n*m). Es la que usa el programa.
 * Las tres dan el mismo numero (casos de estudio y pruebas de la clase CasosDeEstudio).
 */
public class Levenshtein
{
    // Matriz D de la programacion dinamica: D[i][j] = distancia entre los primeros i terminos de a y los primeros j de b
    public int[][] matriz;
    // Contador de llamadas para comparar la version recursiva con la memorizada
    public long llamadas;

    // Recursivo: D(i, j) con las tres opciones de la recurrencia. T(n) crece como 3^(n+m)
    public int distanciaRecursiva(String[] a, String[] b, int i, int j)
    {
        llamadas++;
        if (i == 0)
        {
            return j;
        }
        if (j == 0)
        {
            return i;
        }
        int costo = 1;
        if (a[i - 1].equals(b[j - 1]))
        {
            costo = 0;
        }
        return minimo(distanciaRecursiva(a, b, i - 1, j) + 1, distanciaRecursiva(a, b, i, j - 1) + 1,
            distanciaRecursiva(a, b, i - 1, j - 1) + costo);
    }

    // Memorizacion (Top Down): memo[i][j] = -1 significa "todavia no calculado". Orden O(n*m)
    public int distanciaMemo(String[] a, String[] b, int i, int j, int[][] memo)
    {
        llamadas++;
        if (i == 0)
        {
            return j;
        }
        if (j == 0)
        {
            return i;
        }
        if (memo[i][j] != -1)
        {
            return memo[i][j];
        }
        int costo = 1;
        if (a[i - 1].equals(b[j - 1]))
        {
            costo = 0;
        }
        memo[i][j] = minimo(distanciaMemo(a, b, i - 1, j, memo) + 1, distanciaMemo(a, b, i, j - 1, memo) + 1,
            distanciaMemo(a, b, i - 1, j - 1, memo) + costo);
        return memo[i][j];
    }

    // Prepara la tabla de memorizacion con -1 y llama a la version Top Down
    public int distanciaMemo(String[] a, String[] b)
    {
        int[][] memo = new int[a.length + 1][b.length + 1];
        for (int i = 0; i <= a.length; i++)
        {
            for (int j = 0; j <= b.length; j++)
            {
                memo[i][j] = -1;
            }
        }
        return distanciaMemo(a, b, a.length, b.length, memo);
    }

    // Llena la matriz de arriba hacia abajo y de izquierda a derecha; la respuesta queda en la esquina D[n][m].
    // Orden de complejidad O(n*m): dos ciclos anidados, cada celda se calcula en O(1).
    public int distancia(String[] a, String[] b)
    {
        int n = a.length;
        int m = b.length;
        matriz = new int[n + 1][m + 1];
        // Caso base: pasar de i terminos a la secuencia vacia cuesta i borrados
        for (int i = 0; i <= n; i++)
        {
            matriz[i][0] = i;
        }
        // Caso base: pasar de la secuencia vacia a j terminos cuesta j inserciones
        for (int j = 0; j <= m; j++)
        {
            matriz[0][j] = j;
        }
        for (int i = 1; i <= n; i++)
        {
            for (int j = 1; j <= m; j++)
            {
                // Sustituir cuesta 1, pero si los terminos son iguales no cuesta nada
                int costo = 1;
                if (a[i - 1].equals(b[j - 1]))
                {
                    costo = 0;
                }
                // Se elige la operacion mas barata: borrar, insertar o sustituir
                matriz[i][j] = minimo(matriz[i - 1][j] + 1, matriz[i][j - 1] + 1, matriz[i - 1][j - 1] + costo);
            }
        }
        return matriz[n][m];
    }

    // Menor de tres enteros, O(1)
    public int minimo(int x, int y, int z)
    {
        int menor = x;
        if (y < menor)
        {
            menor = y;
        }
        if (z < menor)
        {
            menor = z;
        }
        return menor;
    }

    // Normaliza la distancia entre 0 y 1: dos secuencias iguales dan 1, totalmente distintas dan 0
    public double similitud(String[] a, String[] b)
    {
        int mayor = Math.max(a.length, b.length);
        if (mayor == 0)
        {
            return 1.0;
        }
        return 1.0 - distancia(a, b) / (double) mayor;
    }
}
