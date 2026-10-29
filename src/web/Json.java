package web;

/**
 * Ayudas para escribir JSON a mano (sin librerias).
 */
public class Json
{
    // Convierte un String en un texto JSON valido: comillas, barras y saltos de linea se escapan.
    // Orden de complejidad O(L), con L = largo del texto.
    public static String texto(String s)
    {
        StringBuilder r = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (c == '"')
            {
                r.append("\\\"");
            }
            else if (c == '\\')
            {
                r.append("\\\\");
            }
            else if (c == '\n')
            {
                r.append("\\n");
            }
            else if (c == '\r')
            {
                r.append("\\r");
            }
            else if (c == '\t')
            {
                r.append("\\t");
            }
            else if (c < 32)
            {
                r.append(String.format("\\u%04x", (int) c));
            }
            else
            {
                r.append(c);
            }
        }
        return r.append("\"").toString();
    }

    // Numero con 6 decimales y punto (Locale.US); NaN o infinito no existen en JSON, se escriben como null
    public static String numero(double x)
    {
        if (Double.isNaN(x) || Double.isInfinite(x))
        {
            return "null";
        }
        return String.format(java.util.Locale.US, "%.6f", x);
    }

    // [1.0,2.0,...] a partir de un arreglo de numeros
    public static String arreglo(double[] v)
    {
        StringBuilder r = new StringBuilder("[");
        for (int i = 0; i < v.length; i++)
        {
            if (i > 0)
            {
                r.append(",");
            }
            r.append(numero(v[i]));
        }
        return r.append("]").toString();
    }

    // ["a","b",...] a partir de un arreglo de textos
    public static String arregloTextos(String[] v)
    {
        StringBuilder r = new StringBuilder("[");
        for (int i = 0; i < v.length; i++)
        {
            if (i > 0)
            {
                r.append(",");
            }
            r.append(texto(v[i]));
        }
        return r.append("]").toString();
    }
}
