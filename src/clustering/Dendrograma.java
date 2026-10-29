package clustering;

import java.util.ArrayList;

/**
 * Dibuja el dendrograma como una imagen SVG (texto), sin librerias de graficos.
 *
 * Orientacion VERTICAL: las hojas (abstracts) quedan abajo, una al lado de otra, con su etiqueta
 * escrita hacia abajo, y la distancia de fusion va en el eje y (a mas altura, mas distintos).
 * Cada fusion se dibuja como una "U" invertida que une a sus dos hijos.
 *
 * Como funciona, paso a paso:
 *  1. Se pide el orden de las hojas con un recorrido en preorden del arbol (AgrupamientoJerarquico.hojas),
 *     asi ningun cruce de lineas aparece en el dibujo.
 *  2. Cada hoja recibe su posicion x (una columna) y su y en la base.
 *  3. Cada fusion tiene x = promedio de las x de sus hijos y y = su altura reescalada al alto del dibujo.
 *  4. Se dibujan dos lineas verticales (de cada hijo hasta la altura de la fusion) y una horizontal que las une.
 *  5. Si se pasa un corte en k grupos, cada etiqueta se colorea segun su grupo.
 * Costo: se visita cada uno de los 2n - 1 nodos una vez, O(n).
 */
public class Dendrograma
{
    // Verde institucional de la Universidad del Quindio y colores armonicos para los grupos
    public static final String VERDE = "#00663F";
    public static final String VERDE_OSCURO = "#0B3D2E";
    private static final String[] COLORES =
    {
        "#00663F", "#C9892B", "#2B6F9E", "#8E3B6B", "#5C8A1F", "#B5483A", "#4A5A6A", "#00867D"
    };

    public static String generarSvg(AgrupamientoJerarquico arbol, String[] etiquetas, String titulo, int[] grupos)
    {
        ArrayList<Fusion> fusiones = arbol.fusiones;
        int n = arbol.n;
        int raiz = 2 * n - 2;
        ArrayList<Integer> orden = arbol.hojas(raiz);
        // El eje empieza un poco antes de la primera fusion para que las diferencias se vean
        // (con textos cortos casi todas las distancias quedan entre 0,8 y 1).
        double alturaMaxima = fusiones.get(fusiones.size() - 1).altura;
        double alturaMinima = Math.floor((fusiones.get(0).altura - 0.05) * 20) / 20.0;
        if (alturaMinima < 0)
        {
            alturaMinima = 0;
        }
        if (alturaMaxima <= alturaMinima)
        {
            alturaMaxima = alturaMinima + 1;
        }
        double rango = alturaMaxima - alturaMinima;
        int margenIzquierdo = 70;
        int paso = 34;
        int altoArbol = 420;
        int margenSuperior = 56;
        int altoEtiquetas = 330;
        int ancho = margenIzquierdo + n * paso + 30;
        int yBase = margenSuperior + altoArbol;
        int altoTotal = yBase + altoEtiquetas;

        // Posicion (x, y) de cada nodo: hojas en la base, fusiones a la altura que les toca
        double[] x = new double[2 * n - 1];
        double[] y = new double[2 * n - 1];
        for (int p = 0; p < orden.size(); p++)
        {
            int hoja = orden.get(p);
            x[hoja] = margenIzquierdo + p * paso + paso / 2.0;
            y[hoja] = yBase;
        }
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + ancho + "\" height=\"" + altoTotal
            + "\" viewBox=\"0 0 " + ancho + " " + altoTotal + "\" font-family=\"Arial, sans-serif\">\n");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");
        svg.append("<text x=\"10\" y=\"24\" font-size=\"16\" font-weight=\"bold\" fill=\"" + VERDE_OSCURO + "\">"
            + escapar(titulo) + "</text>\n");
        // Eje de distancias a la izquierda, con lineas guia suaves
        for (int t = 0; t <= 5; t++)
        {
            double valor = alturaMinima + rango * t / 5.0;
            double yt = yBase - altoArbol * t / 5.0;
            svg.append(linea(margenIzquierdo - 5, yt, margenIzquierdo, yt, "#555", 1.0));
            svg.append(linea(margenIzquierdo, yt, ancho - 20, yt, "#e3ece7", 1.0));
            svg.append("<text x=\"" + (margenIzquierdo - 9) + "\" y=\"" + (yt + 4) + "\" font-size=\"10\" "
                + "text-anchor=\"end\" fill=\"#444\">" + String.format(java.util.Locale.US, "%.3f", valor)
                + "</text>\n");
        }
        svg.append(linea(margenIzquierdo, margenSuperior, margenIzquierdo, yBase, "#555", 1.0));
        for (int paso2 = 0; paso2 < fusiones.size(); paso2++)
        {
            Fusion f = fusiones.get(paso2);
            int nodo = n + paso2;
            y[nodo] = yBase - ((f.altura - alturaMinima) / rango) * altoArbol;
            x[nodo] = (x[f.izquierdo] + x[f.derecho]) / 2.0;
            // Una linea vertical por cada hijo hasta la altura de la fusion y una horizontal que los une
            svg.append(linea(x[f.izquierdo], y[f.izquierdo], x[f.izquierdo], y[nodo], VERDE, 1.6));
            svg.append(linea(x[f.derecho], y[f.derecho], x[f.derecho], y[nodo], VERDE, 1.6));
            svg.append(linea(x[f.izquierdo], y[nodo], x[f.derecho], y[nodo], VERDE, 1.6));
        }
        // Etiquetas de las hojas, escritas hacia abajo
        for (int p = 0; p < orden.size(); p++)
        {
            int hoja = orden.get(p);
            String color = "#222";
            if (grupos != null)
            {
                color = COLORES[grupos[hoja] % COLORES.length];
            }
            double xe = x[hoja] + 4;
            double ye = yBase + 8;
            svg.append("<text x=\"" + xe + "\" y=\"" + ye + "\" font-size=\"11\" fill=\"" + color
                + "\" transform=\"rotate(90 " + xe + " " + ye + ")\">" + escapar(etiquetas[hoja]) + "</text>\n");
        }
        svg.append("<text x=\"14\" y=\"" + (margenSuperior - 14) + "\" font-size=\"11\" fill=\"#444\">"
            + "Distancia de fusion (1 - similitud); el eje empieza en "
            + String.format(java.util.Locale.US, "%.2f", alturaMinima) + "</text>\n");
        svg.append("</svg>\n");
        return svg.toString();
    }

    private static String linea(double x1, double y1, double x2, double y2, String color, double grosor)
    {
        return String.format(java.util.Locale.US,
            "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"%s\" stroke-width=\"%.1f\"/>\n",
            x1, y1, x2, y2, color, grosor);
    }

    public static String escapar(String texto)
    {
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < texto.length(); i++)
        {
            char c = texto.charAt(i);
            if (c == '&')
            {
                r.append("&amp;");
            }
            else if (c == '<')
            {
                r.append("&lt;");
            }
            else if (c == '>')
            {
                r.append("&gt;");
            }
            else if (c == '"')
            {
                r.append("&quot;");
            }
            else
            {
                r.append(c);
            }
        }
        return r.toString();
    }
}
