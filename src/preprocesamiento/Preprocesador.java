package preprocesamiento;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Transforma un abstract en una lista de terminos:
 * 1. pasa a minusculas,
 * 2. separa por todo lo que no sea letra (a-z),
 * 3. quita las palabras de menos de 2 letras y las palabras vacias (stopwords).
 * Es el mismo criterio del seguimiento (se conserva "ai").
 * La separacion se hace recorriendo el texto caracter por caracter, sin split ni expresiones regulares.
 *
 * Ejemplo: "Generative AI, in the classroom!" -> [generative, ai, classroom]
 * ("in" y "the" son palabras vacias; la coma, el espacio y el signo se usan solo para cortar).
 */
public class Preprocesador
{
    // Palabras muy frecuentes que no ayudan a distinguir un abstract de otro (articulos, preposiciones, pronombres...)
    private static final String[] PALABRAS_VACIAS =
    {
        "a", "about", "above", "across", "after", "again", "against", "all", "also", "am", "among", "an", "and",
        "any", "are", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
        "by", "can", "could", "did", "do", "does", "doing", "down", "during", "each", "etc", "few", "for", "from",
        "further", "had", "has", "have", "having", "he", "her", "here", "hers", "him", "his", "how", "however",
        "if", "in", "into", "is", "it", "its", "itself", "just", "may", "me", "might", "more", "most", "much",
        "must", "my", "no", "nor", "not", "now", "of", "off", "on", "once", "one", "only", "or", "other", "our",
        "ours", "out", "over", "own", "same", "she", "should", "so", "some", "such", "than", "that", "the",
        "their", "theirs", "them", "then", "there", "these", "they", "this", "those", "through", "thus", "to",
        "too", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "when",
        "where", "whereas", "whether", "which", "while", "who", "whom", "why", "will", "with", "within",
        "without", "would", "yet", "you", "your", "i", "ie", "eg", "et", "al"
    };

    // Las mismas palabras vacias en una tabla hash: preguntar si una palabra esta cuesta O(1) en promedio
    private HashSet<String> palabrasVacias = new HashSet<String>();

    // Pasa la lista de palabras vacias al conjunto. Orden de complejidad O(P), con P = numero de palabras vacias.
    public Preprocesador()
    {
        for (int i = 0; i < PALABRAS_VACIAS.length; i++)
        {
            palabrasVacias.add(PALABRAS_VACIAS[i]);
        }
    }

    // true si la palabra esta en la lista de palabras vacias. O(1) en promedio.
    public boolean esPalabraVacia(String palabra)
    {
        return palabrasVacias.contains(palabra);
    }

    // Separa el texto en palabras (solo letras), en minusculas. No quita palabras vacias.
    // Recorre el texto una sola vez, caracter por caracter: O(L), con L = largo del texto.
    public ArrayList<String> separarPalabras(String texto)
    {
        // Palabras ya terminadas
        ArrayList<String> palabras = new ArrayList<String>();
        // Palabra que se va armando letra por letra
        StringBuilder actual = new StringBuilder();
        for (int i = 0; i < texto.length(); i++)
        {
            char c = Character.toLowerCase(texto.charAt(i));
            if (c >= 'a' && c <= 'z')
            {
                // Es una letra: se agrega a la palabra actual
                actual.append(c);
            }
            else if (actual.length() > 0)
            {
                // No es letra y habia una palabra en construccion: la palabra termino
                palabras.add(actual.toString());
                actual.setLength(0);
            }
        }
        // Si el texto termina en una letra, falta guardar la ultima palabra
        if (actual.length() > 0)
        {
            palabras.add(actual.toString());
        }
        return palabras;
    }

    // Terminos del abstract en el orden en que aparecen (para Levenshtein y Needleman-Wunsch).
    // Cada palabra se queda solo si tiene 2 o mas letras y no es una palabra vacia. O(L).
    public String[] terminos(String texto)
    {
        ArrayList<String> palabras = separarPalabras(texto);
        ArrayList<String> resultado = new ArrayList<String>();
        for (int i = 0; i < palabras.size(); i++)
        {
            String p = palabras.get(i);
            if (p.length() >= 2 && !esPalabraVacia(p))
            {
                resultado.add(p);
            }
        }
        return resultado.toArray(new String[0]);
    }
}
