package similitud;

import java.util.ArrayList;

import estructuras.Pila;

/**
 * Alineamiento global de Needleman-Wunsch entre las secuencias de terminos de dos abstracts.
 * Puntajes: coincidencia +1, sustitucion -1, hueco (gap) -1.
 *   F[i][0] = -i,  F[0][j] = -j
 *   F[i][j] = max( F[i-1][j-1] + s(a_i, b_j),  F[i-1][j] - 1,  F[i][j-1] - 1 )
 * Despues se recorre la matriz desde F[n][m] hasta F[0][0] (traceback) para obtener el
 * alineamiento. Similitud = coincidencias / longitud del alineamiento.   O(n*m).
 */
public class NeedlemanWunsch
{
    // Sistema de puntaje del alineamiento
    public static final int COINCIDENCIA = 1;
    public static final int SUSTITUCION = -1;
    public static final int HUECO = -1;

    // Matriz F de la programacion dinamica (puntaje optimo de alinear prefijos)
    public int[][] matriz;
    // Puntaje total del alineamiento = F[n][m]
    public int puntaje;
    // Cuantos pares del alineamiento son terminos iguales
    public int coincidencias;
    // Cantidad de columnas del alineamiento (terminos + huecos)
    public int longitudAlineamiento;
    // Alineamiento resultante (para mostrarlo en el caso de estudio); "-" es un hueco
    public ArrayList<String> filaA = new ArrayList<String>();
    public ArrayList<String> filaB = new ArrayList<String>();

    // Fase 1: llena la matriz F. Orden de complejidad O(n*m).
    public int calcularMatriz(String[] a, String[] b)
    {
        int n = a.length;
        int m = b.length;
        matriz = new int[n + 1][m + 1];
        // Caso base: alinear i terminos contra nada son i huecos
        for (int i = 0; i <= n; i++)
        {
            matriz[i][0] = i * HUECO;
        }
        for (int j = 0; j <= m; j++)
        {
            matriz[0][j] = j * HUECO;
        }
        for (int i = 1; i <= n; i++)
        {
            for (int j = 1; j <= m; j++)
            {
                // Tres caminos: alinear los dos terminos (diagonal), hueco en b (arriba) o hueco en a (izquierda)
                int diagonal = matriz[i - 1][j - 1] + puntos(a[i - 1], b[j - 1]);
                int arriba = matriz[i - 1][j] + HUECO;
                int izquierda = matriz[i][j - 1] + HUECO;
                matriz[i][j] = maximo(diagonal, arriba, izquierda);
            }
        }
        puntaje = matriz[n][m];
        return puntaje;
    }

    // +1 si los terminos son iguales, -1 si son distintos
    public int puntos(String x, String y)
    {
        int valor = SUSTITUCION;
        if (x.equals(y))
        {
            valor = COINCIDENCIA;
        }
        return valor;
    }

    // Mayor de tres enteros, O(1)
    public int maximo(int x, int y, int z)
    {
        int mayor = x;
        if (y > mayor)
        {
            mayor = y;
        }
        if (z > mayor)
        {
            mayor = z;
        }
        return mayor;
    }

    // Traceback: prefiere diagonal, luego arriba, luego izquierda. O(n + m)
    public void reconstruir(String[] a, String[] b)
    {
        filaA.clear();
        filaB.clear();
        // El recorrido va de atras hacia adelante: cada paso se apila y al final se desapila,
        // asi las filas quedan en orden (push y pop O(1), todo el traceback O(n + m))
        Pila pilaA = new Pila();
        Pila pilaB = new Pila();
        coincidencias = 0;
        int i = a.length;
        int j = b.length;
        while (i > 0 || j > 0)
        {
            if (i > 0 && j > 0 && matriz[i][j] == matriz[i - 1][j - 1] + puntos(a[i - 1], b[j - 1]))
            {
                if (a[i - 1].equals(b[j - 1]))
                {
                    coincidencias++;
                }
                pilaA.push(a[i - 1]);
                pilaB.push(b[j - 1]);
                i--;
                j--;
            }
            else if (i > 0 && matriz[i][j] == matriz[i - 1][j] + HUECO)
            {
                pilaA.push(a[i - 1]);
                pilaB.push("-");
                i--;
            }
            else
            {
                pilaA.push("-");
                pilaB.push(b[j - 1]);
                j--;
            }
        }
        while (!pilaA.estaVacia())
        {
            filaA.add((String) pilaA.pop());
            filaB.add((String) pilaB.pop());
        }
        longitudAlineamiento = filaA.size();
    }

    // Calcula la matriz, reconstruye el alineamiento y devuelve coincidencias / columnas (entre 0 y 1)
    public double similitud(String[] a, String[] b)
    {
        calcularMatriz(a, b);
        reconstruir(a, b);
        if (longitudAlineamiento == 0)
        {
            return 1.0;
        }
        return coincidencias / (double) longitudAlineamiento;
    }
}
