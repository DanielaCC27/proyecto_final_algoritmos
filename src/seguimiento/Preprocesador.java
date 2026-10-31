package seguimiento;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Limpia el texto de los abstracts y cuenta cuantas veces aparece cada termino.
 * Es la clase del Seguimiento 2026-2 sin cambios en contarTerminos; el proyecto final solo le
 * agrega terminos(), que devuelve las mismas palabras EN ORDEN y con repeticiones, porque
 * Levenshtein y Needleman-Wunsch comparan secuencias y TF-IDF necesita el tf de cada abstract.
 */
public class Preprocesador
{
    private String[] palabrasVacias =
    {
        "of", "in", "to", "on", "as", "at", "by", "is", "an", "be", "or", "it", "we", "if", "no", "so", "do",
        "the", "and", "for", "are", "was", "were", "with", "that", "this", "these", "those",
        "from", "into", "their", "its", "has", "have", "had", "been", "can", "may", "not",
        "but", "such", "than", "then", "also", "which", "while", "when", "where", "who",
        "how", "what", "our", "they", "them", "there", "both", "each", "more", "most", "other",
        "some", "only", "very", "between", "through", "using", "used", "use", "based", "via",
        "show", "shows", "results", "paper", "study", "propose", "proposed", "present", "well",
        "new", "many", "about", "among", "across", "however", "thus", "will", "would", "could", "should"
    };

    public boolean esPalabraVacia(String palabra)
    {
        boolean encontrada = false;
        for (int i = 0; i < palabrasVacias.length; i++)
        {
            if (palabrasVacias[i].equals(palabra))
            {
                encontrada = true;
            }
        }
        return encontrada;
    }

    public Termino[] contarTerminos(String texto)
    {
        texto = texto.toLowerCase();
        texto = texto.replaceAll("[^a-z]+", " ");
        String[] palabras = texto.trim().split(" ");
        LinkedHashMap<String, Integer> frecuencias = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < palabras.length; i++)
        {
            if (palabras[i].length() >= 2 && !esPalabraVacia(palabras[i]))
            {
                frecuencias.put(palabras[i], frecuencias.getOrDefault(palabras[i], 0) + 1);
            }
        }
        Termino[] terminos = new Termino[frecuencias.size()];
        int pos = 0;
        for (String palabra : frecuencias.keySet())
        {
            terminos[pos] = new Termino(palabra, frecuencias.get(palabra));
            pos++;
        }
        return terminos;
    }

    // Las palabras de un abstract que sobreviven a la limpieza, en el orden en que aparecen.
    // Mismo criterio de contarTerminos: minusculas, solo letras a-z, 2 o mas letras, sin palabras vacias.
    // Orden de complejidad O(L * v), con L palabras y v palabras vacias
    public String[] terminos(String texto)
    {
        texto = texto.toLowerCase();
        texto = texto.replaceAll("[^a-z]+", " ");
        String[] palabras = texto.trim().split(" ");
        ArrayList<String> lista = new ArrayList<String>();
        for (int i = 0; i < palabras.length; i++)
        {
            if (palabras[i].length() >= 2 && !esPalabraVacia(palabras[i]))
            {
                lista.add(palabras[i]);
            }
        }
        return lista.toArray(new String[0]);
    }
}
