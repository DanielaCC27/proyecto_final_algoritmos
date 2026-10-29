package estructuras;

/**
 * Pila basada en nodos con nodo cabecera (nucleo 6). El tope es cabecera.siguiente:
 * el ultimo elemento que entra es el primero que sale.
 * Se usa en el traceback de Needleman-Wunsch, que descubre el alineamiento de atras hacia adelante.
 */
public class Pila
{
    // Nodo cabecera: no guarda dato; cabecera.siguiente es siempre el tope de la pila
    private Nodo cabecera;
    // Cuantos elementos hay en la pila
    private int cantidad;

    // Crea una pila vacia: solo existe la cabecera
    public Pila()
    {
        cabecera = new Nodo();                  // Orden de complejidad O(1)
        cantidad = 0;
    }

    // true si no hay ningun nodo despues de la cabecera
    public boolean estaVacia()
    {
        if (cabecera.siguiente == null)         // Orden de complejidad O(1)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    // Inserta el elemento como siguiente a la cabecera (nuevo tope)
    public void push(Object dato)
    {
        Nodo nuevo = new Nodo(dato);            // Orden de complejidad O(1)
        nuevo.siguiente = cabecera.siguiente;
        cabecera.siguiente = nuevo;
        cantidad++;
    }

    // Saca y retorna el elemento del tope; null si la pila esta vacia
    public Object pop()
    {
        Object dato = null;                     // Orden de complejidad O(1)
        if (!estaVacia())
        {
            dato = cabecera.siguiente.valor;
            cabecera.siguiente = cabecera.siguiente.siguiente;
            cantidad--;
        }
        return dato;
    }

    // Mira el tope sin sacarlo; null si la pila esta vacia
    public Object top()
    {
        if (estaVacia())                        // Orden de complejidad O(1)
        {
            return null;
        }
        return cabecera.siguiente.valor;
    }

    // Cantidad de elementos guardados
    public int tamano()
    {
        return cantidad;                        // Orden de complejidad O(1)
    }
}
