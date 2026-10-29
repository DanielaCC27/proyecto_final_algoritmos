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
 */
public class Levenshtein
{
    // Matriz D de la programacion dinamica: D[i][j] = distancia entre los primeros i terminos de a y los primeros j de b
    public int[][] matriz;

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
