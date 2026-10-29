package modelo;

/**
 * Un articulo del corpus: numero del PDF, titulo, autores y abstract.
 * El titulo y los autores se usan para mostrar a que articulo pertenece
 * cada resultado (cruce de informacion); los algoritmos trabajan sobre el abstract.
 *
 * Es una clase de datos: solo guarda informacion, no hace calculos.
 */
public class Articulo
{
    // Numero con el que viene el PDF (1 a 20); tambien es el nombre del abstract (01.txt ... 20.txt)
    public int numero;
    // Titulo y autores revisados a mano contra el PDF (datos/metadatos.csv)
    public String titulo;
    public String autores;
    // Texto del abstract, tal como se extrajo; sobre este texto trabajan todos los algoritmos
    public String resumen;

    // Constructor: guarda los cuatro datos del articulo
    public Articulo(int numero, String titulo, String autores, String resumen)
    {
        this.numero = numero;
        this.titulo = titulo;
        this.autores = autores;
        this.resumen = resumen;
    }

    // "Laupichler et al. (art. 1)" para etiquetas cortas.
    // Toma el primer autor (lo que hay antes de la primera coma) y se queda con su apellido.
    // Orden de complejidad O(L), con L = largo del texto de autores.
    public String etiquetaCorta()
    {
        String primerAutor = autores;
        // Se busca a mano la posicion de la primera coma (-1 = no hay coma, un solo autor)
        int coma = -1;
        for (int i = 0; i < autores.length() && coma == -1; i++)
        {
            if (autores.charAt(i) == ',')
            {
                coma = i;
            }
        }
        if (coma != -1)
        {
            primerAutor = autores.substring(0, coma);
        }
        // El apellido es la ultima palabra del nombre del primer autor
        String[] partes = primerAutor.trim().split(" ");
        String apellido = partes[partes.length - 1];
        // Si hay mas de un autor se agrega "et al."
        if (coma != -1)
        {
            apellido = apellido + " et al.";
        }
        return apellido + " (art. " + numero + ")";
    }
}
