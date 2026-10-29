package datos;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import modelo.Articulo;

/**
 * Carga los 20 articulos: el abstract de datos/abstracts/NN.txt y el titulo
 * y los autores de datos/metadatos.csv (separado por punto y coma).
 * Los dos archivos los genera ExtractorPdf y se revisaron a mano contra cada PDF.
 *
 * Es el primer paso de todo el programa: el resto de las clases le piden los articulos a Corpus.
 */
public class Corpus
{
    // Los 20 articulos en el orden en que aparecen en metadatos.csv
    private ArrayList<Articulo> articulos = new ArrayList<Articulo>();

    // Lee metadatos.csv y, por cada fila, el abstract que le corresponde.
    // Orden de complejidad O(N * L): N articulos y L caracteres por abstract (todo se lee una vez).
    public Corpus(String carpetaDatos) throws IOException
    {
        File carpeta = new File(carpetaDatos);
        if (!carpeta.isDirectory())
        {
            // Por si se ejecuta desde la carpeta de arriba (VS Code abierto un nivel antes)
            carpeta = new File("proyecto-final", carpetaDatos);
        }
        File archivoMetadatos = new File(carpeta, "metadatos.csv");
        List<String> lineas = Files.readAllLines(archivoMetadatos.toPath(), StandardCharsets.UTF_8);
        // La primera linea es el encabezado: numero;titulo;autores
        for (int i = 1; i < lineas.size(); i++)
        {
            String linea = lineas.get(i);
            if (linea.trim().isEmpty())
            {
                continue;
            }
            // Cada fila: numero;titulo;autores
            String[] campos = linea.split(";");
            int numero = Integer.parseInt(campos[0].trim());
            // El abstract se llama como el numero con dos cifras: 01.txt, 02.txt, ...
            String nombreAbstract = String.format("%02d.txt", numero);
            String resumen = Files.readString(new File(new File(carpeta, "abstracts"), nombreAbstract).toPath(),
                StandardCharsets.UTF_8).trim();
            articulos.add(new Articulo(numero, campos[1].trim(), campos[2].trim(), resumen));
        }
    }

    // Cantidad de articulos cargados (20)
    public int tamano()
    {
        return articulos.size();
    }

    // Articulo en la posicion 0..19 de la lista (no es lo mismo que su numero de PDF)
    public Articulo obtener(int posicion)
    {
        return articulos.get(posicion);
    }

    // Busca por numero de PDF (1..20). Devuelve null si no existe.
    // Orden de complejidad O(N): recorre la lista completa.
    public Articulo buscarPorNumero(int numero)
    {
        Articulo encontrado = null;
        for (int i = 0; i < articulos.size(); i++)
        {
            if (articulos.get(i).numero == numero)
            {
                encontrado = articulos.get(i);
            }
        }
        return encontrado;
    }

    // La lista completa, para quien necesite recorrerla
    public ArrayList<Articulo> todos()
    {
        return articulos;
    }
}
