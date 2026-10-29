package similitud;

/**
 * Operaciones entre vectores que usan el coseno TF-IDF y los dos modelos de embeddings.
 */
public class Vectores
{
    // u . v = suma de u[i]*v[i]. Orden de complejidad O(d), con d = dimension del vector
    public static double productoPunto(double[] u, double[] v)
    {
        double suma = 0;
        for (int i = 0; i < u.length; i++)
        {
            suma = suma + u[i] * v[i];
        }
        return suma;
    }

    // |u| = raiz de (u . u)
    public static double norma(double[] u)
    {
        return Math.sqrt(productoPunto(u, u));
    }

    // cos(theta) = (u . v) / (|u| |v|)
    public static double coseno(double[] u, double[] v)
    {
        double nu = norma(u);
        double nv = norma(v);
        // Un vector en ceros no tiene direccion: se define el coseno como 0 para no dividir por cero
        if (nu == 0 || nv == 0)
        {
            return 0;
        }
        return productoPunto(u, v) / (nu * nv);
    }

    // Angulo entre los vectores, en grados
    public static double anguloGrados(double[] u, double[] v)
    {
        double c = coseno(u, v);
        // Por redondeo el coseno puede pasar de 1 y acos fallaria
        if (c > 1)
        {
            c = 1;
        }
        return Math.toDegrees(Math.acos(c));
    }

    // Distancia en linea recta entre los dos puntos: raiz de la suma de (u[i]-v[i])^2
    public static double euclidiana(double[] u, double[] v)
    {
        double suma = 0;
        for (int i = 0; i < u.length; i++)
        {
            double dif = u[i] - v[i];
            suma = suma + dif * dif;
        }
        return Math.sqrt(suma);
    }

    // Convierte una distancia (0 a infinito) en similitud (1 a 0)
    public static double similitudDesdeDistancia(double distancia)
    {
        return 1.0 / (1.0 + distancia);
    }
}
