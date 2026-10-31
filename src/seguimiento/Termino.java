package seguimiento;

/**
 * Un termino del corpus con su frecuencia (numero de apariciones
 * en todos los abstracts). La frecuencia es la CLAVE que se ordena.
 */
public class Termino
{
    public String palabra;
    public int frecuencia;

    public Termino(String palabra, int frecuencia)
    {
        this.palabra = palabra;
        this.frecuencia = frecuencia;
    }
}
