package seguimiento;

/**
 * Cola enlazada del taller de RadixSort del profesor, adaptada a Termino.
 */
public class ColaEnlazada
{
    private NodoTermino inicio;
    private NodoTermino fin;
    private int tamano;

    public ColaEnlazada()
    {
        inicio = null;
        fin = null;
        tamano = 0;
    }

    public void encolar(Termino num)
    {
        tamano++;
        NodoTermino temp = new NodoTermino(num);
        if (inicio == null)
        {
            inicio = temp;
            fin = inicio;
        }
        else
        {
            fin.siguiente = temp;
            fin = temp;
        }
        temp = null;
    }

    public Termino decolar()
    {
        tamano--;
        Termino temp = inicio.valor;
        NodoTermino nodoTemp;
        nodoTemp = inicio;
        inicio = inicio.siguiente;
        nodoTemp = null;
        return temp;
    }

    public boolean estaVacia()
    {
        return (tamano == 0);
    }
}
