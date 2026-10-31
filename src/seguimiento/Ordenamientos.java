package seguimiento;

/**
 * Ordenamientos que no comparan un elemento contra otro.
 * Los tres dejan los terminos de MENOR a MAYOR frecuencia, como en el
 * material de clase; el ranking se lee desde el final del arreglo.
 */
public class Ordenamientos
{
    private ColaEnlazada[] Q =
    {
        new ColaEnlazada(),
        new ColaEnlazada(),
        new ColaEnlazada(),
        new ColaEnlazada(),
        new ColaEnlazada(),
        new ColaEnlazada(),
        new ColaEnlazada(),
        new ColaEnlazada(),
        new ColaEnlazada(),
        new ColaEnlazada()
    };

    // Frecuencia mayor del arreglo. T(n) = 4n + 6
    public int obtenerMayor(Termino[] arreglo, int n)
    {
        int mayor = 0;
        for (int i = 0; i < n; i++)
        {
            if (arreglo[i].frecuencia > mayor)
            {
                mayor = arreglo[i].frecuencia;
            }
        }
        return mayor;
    }

    // Counting Sort. T(n, k) = 11n + 3k + 17 -> O(n + k)
    public Termino[] countingSort(Termino[] arreglo, int n)
    {
        int k = obtenerMayor(arreglo, n);
        int[] conteo = new int[k + 1];
        Termino[] salida = new Termino[n];
        for (int i = 0; i < n; i++)
        {
            conteo[arreglo[i].frecuencia]++;
        }
        for (int i = 1; i <= k; i++)
        {
            conteo[i] = conteo[i] + conteo[i - 1];
        }
        for (int i = n - 1; i >= 0; i--)
        {
            conteo[arreglo[i].frecuencia]--;
            salida[conteo[arreglo[i].frecuencia]] = arreglo[i];
        }
        return salida;
    }

    // Radix Sort del taller. T(n, d) = 20dn + 4n + 37d + 11 -> O(d * n)
    public void radixSort(Termino[] arreglo, int n)
    {
        int max = obtenerMayor(arreglo, n);
        int numeroDigitos = String.valueOf(max).length();
        int posArreglo;
        for (int i = 1; i <= numeroDigitos; i++)
        {
            posArreglo = 0;
            for (int j = 0; j < n; j++)
            {
                Q[obtenerRadical(arreglo[j].frecuencia, i)].encolar(arreglo[j]);
            }
            for (int j = 0; j < Q.length; j++)
            {
                while (!Q[j].estaVacia())
                {
                    arreglo[posArreglo] = Q[j].decolar();
                    posArreglo++;
                }
            }
        }
    }

    public int obtenerRadical(int numero, int radical)
    {
        return (int) (numero / Math.pow(10, radical - 1)) % 10;
    }

    // Bucket Sort con un casillero por frecuencia (como en el libro).
    // T(n, k) = 21n + 6k + 24 -> O(n + k)
    public Termino[] bucketSort(Termino[] arreglo, int n)
    {
        int k = obtenerMayor(arreglo, n);
        ColaEnlazada[] casilleros = new ColaEnlazada[k + 1];
        Termino[] salida = new Termino[n];
        int pos = 0;
        for (int i = 0; i <= k; i++)
        {
            casilleros[i] = new ColaEnlazada();
        }
        for (int i = 0; i < n; i++)
        {
            casilleros[arreglo[i].frecuencia].encolar(arreglo[i]);
        }
        for (int i = 0; i <= k; i++)
        {
            while (!casilleros[i].estaVacia())
            {
                salida[pos] = casilleros[i].decolar();
                pos++;
            }
        }
        return salida;
    }
}
