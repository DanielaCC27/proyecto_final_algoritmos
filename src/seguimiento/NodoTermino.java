package seguimiento;

/**
 * Nodo de la cola enlazada (igual al nodoEntero del taller de RadixSort,
 * pero guarda un Termino en lugar de un int).
 */
public class NodoTermino
{
    public Termino valor;
    public NodoTermino siguiente;

    public NodoTermino(Termino a)
    {
        valor = a;
        siguiente = null;
    }
}
