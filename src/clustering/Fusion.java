package clustering;

/**
 * Un paso del agrupamiento: se unen los grupos "izquierdo" y "derecho" a la distancia "altura".
 * Los grupos se numeran como en la literatura: 0..n-1 son las hojas (los abstracts) y
 * n, n+1, ..., 2n-2 son los grupos que se van formando (el paso k crea el grupo n + k).
 */
public class Fusion
{
    // Numero de los dos grupos que se unen (0..n-1 = abstract suelto, n o mas = grupo formado antes)
    public int izquierdo;
    public int derecho;
    // Distancia entre los dos grupos en el momento de unirlos: es la altura de la "U" en el dendrograma
    public double altura;
    // Cuantos abstracts quedan en el grupo nuevo
    public int tamano;

    // Guarda los cuatro datos de la fusion
    public Fusion(int izquierdo, int derecho, double altura, int tamano)
    {
        this.izquierdo = izquierdo;
        this.derecho = derecho;
        this.altura = altura;
        this.tamano = tamano;
    }
}
