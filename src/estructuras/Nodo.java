package estructuras;

/**
 * Nodo de la pila enlazada (nucleo 6): guarda un valor y la referencia al siguiente nodo.
 */
public class Nodo
{
    // Dato que guarda el nodo (puede ser cualquier objeto, por eso es Object)
    public Object valor;
    // Nodo que sigue en la cadena (null si es el ultimo)
    public Nodo siguiente;

    // Nodo vacio: se usa como cabecera de la pila (no guarda ningun dato)
    public Nodo()
    {
        this.valor = null;
        this.siguiente = null;
    }

    // Nodo con un dato y sin siguiente todavia
    public Nodo(Object valor)
    {
        this.valor = valor;
        this.siguiente = null;
    }

    // Devuelve el dato guardado. O(1)
    public Object getValor()
    {
        return valor;
    }

    // Devuelve el nodo que sigue. O(1)
    public Nodo getSiguiente()
    {
        return siguiente;
    }
}
