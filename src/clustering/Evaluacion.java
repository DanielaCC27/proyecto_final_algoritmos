package clustering;

import java.util.ArrayList;

/**
 * Metricas de evaluacion INTERNA (no necesitan etiquetas reales):
 *
 * 1. Coeficiente de correlacion cofenetica (CCC, Sokal y Rohlf, 1962): correlacion de Pearson
 *    entre la distancia original d(i,j) y la distancia cofenetica c(i,j) (altura de la fusion
 *    donde i y j quedan juntos por primera vez). Mide que tan fiel es el dendrograma a los datos.
 *    Cerca de 1 = el arbol conserva bien las distancias.
 *
 * 2. Coeficiente de silueta (Rousseeuw, 1987) al cortar el arbol en k grupos:
 *    a(i) = distancia promedio de i a los de su grupo
 *    b(i) = menor distancia promedio de i a otro grupo
 *    s(i) = (b(i) - a(i)) / max(a(i), b(i));  si el grupo de i tiene un solo elemento, s(i) = 0.
 *    El valor del agrupamiento es el promedio de s(i). Va de -1 a 1; mas alto = grupos mas coherentes.
 */
public class Evaluacion
{
    // Matriz n x n con la altura de la fusion donde cada par de hojas se junta por primera vez.
    // Orden de complejidad O(n^2): cada par se escribe una sola vez.
    public static double[][] matrizCofenetica(ArrayList<Fusion> fusiones, AgrupamientoJerarquico arbol)
    {
        int n = arbol.n;
        double[][] c = new double[n][n];
        for (int paso = 0; paso < fusiones.size(); paso++)
        {
            Fusion f = fusiones.get(paso);
            ArrayList<Integer> izq = arbol.hojas(f.izquierdo);
            ArrayList<Integer> der = arbol.hojas(f.derecho);
            for (int x = 0; x < izq.size(); x++)
            {
                for (int y = 0; y < der.size(); y++)
                {
                    c[izq.get(x)][der.get(y)] = f.altura;
                    c[der.get(y)][izq.get(x)] = f.altura;
                }
            }
        }
        return c;
    }

    // Correlacion de Pearson sobre los pares i < j: compara las distancias originales con las del arbol.
    // Orden de complejidad O(n^2).
    public static double correlacionCofenetica(double[][] d, double[][] c)
    {
        int n = d.length;
        int pares = n * (n - 1) / 2;
        double sumaD = 0;
        double sumaC = 0;
        for (int i = 0; i < n; i++)
        {
            for (int j = i + 1; j < n; j++)
            {
                sumaD = sumaD + d[i][j];
                sumaC = sumaC + c[i][j];
            }
        }
        // Primer recorrido: las medias
        double mediaD = sumaD / pares;
        double mediaC = sumaC / pares;
        // Segundo recorrido: covarianza (numerador) y varianzas
        double numerador = 0;
        double varD = 0;
        double varC = 0;
        for (int i = 0; i < n; i++)
        {
            for (int j = i + 1; j < n; j++)
            {
                numerador = numerador + (d[i][j] - mediaD) * (c[i][j] - mediaC);
                varD = varD + (d[i][j] - mediaD) * (d[i][j] - mediaD);
                varC = varC + (c[i][j] - mediaC) * (c[i][j] - mediaC);
            }
        }
        return numerador / Math.sqrt(varD * varC);
    }

    // Promedio de la silueta de los n abstracts para el corte dado. Orden de complejidad O(n^2).
    public static double silueta(double[][] d, int[] grupo, int k)
    {
        int n = d.length;
        double suma = 0;
        for (int i = 0; i < n; i++)
        {
            // Para el abstract i: suma de distancias hacia cada grupo y cuantos hay en cada uno
            double[] sumaPorGrupo = new double[k];
            int[] cuantosPorGrupo = new int[k];
            for (int j = 0; j < n; j++)
            {
                if (j != i)
                {
                    sumaPorGrupo[grupo[j]] = sumaPorGrupo[grupo[j]] + d[i][j];
                    cuantosPorGrupo[grupo[j]]++;
                }
            }
            if (cuantosPorGrupo[grupo[i]] == 0)
            {
                continue;
            }
            // a = cohesion (distancia promedio a su propio grupo)
            double a = sumaPorGrupo[grupo[i]] / cuantosPorGrupo[grupo[i]];
            // b = separacion (distancia promedio al grupo vecino mas cercano)
            double b = Double.MAX_VALUE;
            for (int g = 0; g < k; g++)
            {
                if (g != grupo[i] && cuantosPorGrupo[g] > 0)
                {
                    double promedio = sumaPorGrupo[g] / cuantosPorGrupo[g];
                    if (promedio < b)
                    {
                        b = promedio;
                    }
                }
            }
            suma = suma + (b - a) / Math.max(a, b);
        }
        return suma / n;
    }
}
