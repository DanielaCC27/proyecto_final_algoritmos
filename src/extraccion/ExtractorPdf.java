package extraccion;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import busqueda.KMP;

/**
 * Lee los 20 PDF con PDFBox y extrae el abstract, el titulo y los autores.
 * Uso: java -cp "bin;lib/pdfbox-app-3.0.3.jar" extraccion.ExtractorPdf articulos datos_extraidos
 *      (en Linux o Mac se cambia ";" por ":")
 *
 * - Abstract: la misma regla del seguimiento (desde "Abstract" hasta "Keywords", "Introduction" o ©).
 * - Titulo: el campo Title de las propiedades del PDF; si no sirve (vacio o nombre de archivo
 *   de la editorial) se marca para revisar a mano.
 * - Autores: las lineas que siguen al titulo en la primera pagina, hasta la primera linea de
 *   afiliacion o el abstract. Se quitan los numeros, letras y asteriscos de las afiliaciones.
 * Las marcas del texto se buscan con nuestro KMP (no con indexOf).
 * El resultado se compara a mano contra cada PDF y la version revisada queda en datos/.
 */
public class ExtractorPdf
{
    // Recorre los PDF en orden, extrae abstract, titulo y autores de cada uno y escribe abstracts/NN.txt y metadatos.csv. Los avisos [revisar a mano] marcan lo que no se pudo extraer bien
    public static void main(String[] args) throws IOException
    {
        String carpetaPdf = "articulos";
        String carpetaSalida = "datos_extraidos";
        if (args.length > 1)
        {
            carpetaPdf = args[0];
            carpetaSalida = args[1];
        }
        File[] archivos = new File(carpetaPdf).listFiles();
        if (archivos == null)
        {
            System.out.println("No encuentro la carpeta " + carpetaPdf);
            return;
        }
        Arrays.sort(archivos);
        File carpetaAbstracts = new File(carpetaSalida, "abstracts");
        carpetaAbstracts.mkdirs();
        ArrayList<String> filas = new ArrayList<String>();
        for (int i = 0; i < archivos.length; i++)
        {
            String nombre = archivos[i].getName();
            if (!nombre.toLowerCase().endsWith(".pdf"))
            {
                continue;
            }
            int punto = 0;
            while (nombre.charAt(punto) != '.')
            {
                punto++;
            }
            int numero = Integer.parseInt(nombre.substring(0, punto));
            PDDocument documento = Loader.loadPDF(archivos[i]);
            String texto = leerPaginas(documento, 2);
            String primeraPagina = leerPaginas(documento, 1);
            String titulo = documento.getDocumentInformation().getTitle();
            documento.close();

            String resumen = obtenerAbstract(texto);
            Files.writeString(new File(carpetaAbstracts, String.format("%02d.txt", numero)).toPath(), resumen,
                StandardCharsets.UTF_8);

            String aviso = "";
            if (!tituloValido(titulo))
            {
                titulo = "";
                aviso = aviso + " [titulo: revisar a mano]";
            }
            String autores = obtenerAutores(primeraPagina, titulo);
            if (autores.isEmpty())
            {
                aviso = aviso + " [autores: revisar a mano]";
            }
            int palabras = contarPalabras(resumen);
            if (palabras < 50 || palabras > 400)
            {
                aviso = aviso + " [abstract: revisar a mano]";
            }
            filas.add(numero + ";" + titulo.trim() + ";" + autores);
            System.out.println(numero + ": " + palabras + " palabras | " + titulo + " | " + autores + aviso);
        }
        PrintWriter salida = new PrintWriter(new File(carpetaSalida, "metadatos.csv"), "UTF-8");
        salida.println("numero;titulo;autores");
        for (int i = 0; i < filas.size(); i++)
        {
            salida.println(filas.get(i));
        }
        salida.close();
    }

    // Texto de las paginas 1 hasta ultimaPagina del PDF (PDFBox solo se usa para leer, la logica es nuestra)
    public static String leerPaginas(PDDocument documento, int ultimaPagina) throws IOException
    {
        PDFTextStripper lector = new PDFTextStripper();
        lector.setEndPage(ultimaPagina);
        lector.setLineSeparator("\n");
        return lector.getText(documento);
    }

    // Primera posicion de "marca" en "texto" desde "desde", buscada con KMP; -1 si no esta
    public static int posicion(String texto, String marca, int desde)
    {
        KMP kmp = new KMP();
        kmp.preprocesar(marca.toCharArray());
        ArrayList<Integer> todas = kmp.buscar(texto.toCharArray());
        int resultado = -1;
        for (int i = 0; i < todas.size() && resultado == -1; i++)
        {
            if (todas.get(i) >= desde)
            {
                resultado = todas.get(i);
            }
        }
        return resultado;
    }

    // Corta el texto entre el rotulo Abstract y la primera marca de fin (Keywords, Introduction o el simbolo de copyright)
    public static String obtenerAbstract(String texto)
    {
        String minusculas = texto.toLowerCase();
        int inicio = posicion(minusculas, "a b s t r a c t", 0);
        int largoTitulo = 15;
        if (inicio == -1)
        {
            inicio = posicion(minusculas, "abstract", 0);
            largoTitulo = 8;
        }
        if (inicio == -1)
        {
            return "";
        }
        inicio = inicio + largoTitulo;
        String[] marcasFin = { "\nkeywords", "key words", "introduction", "©" };
        int fin = texto.length();
        for (int i = 0; i < marcasFin.length; i++)
        {
            int p = posicion(minusculas, marcasFin[i], inicio);
            if (p != -1 && p < fin)
            {
                fin = p;
            }
        }
        String resultado = texto.substring(inicio, fin).trim();
        resultado = resultado.replace("-\n", "");
        resultado = resultado.replace("\n", " ");
        if (resultado.startsWith(":"))
        {
            resultado = resultado.substring(1).trim();
        }
        return resultado;
    }

    // Las editoriales a veces dejan en Title el nombre del archivo ("ET-04-2023-0117_proof 609..645")
    public static boolean tituloValido(String titulo)
    {
        if (titulo == null || titulo.trim().length() < 15)
        {
            return false;
        }
        String t = titulo.toLowerCase();
        return posicion(t, "proof", 0) == -1 && posicion(t, ".indd", 0) == -1 && posicion(t, ".pdf", 0) == -1;
    }

    // Deja solo letras y numeros en minuscula, separados por un espacio
    public static String normalizar(String texto)
    {
        StringBuilder r = new StringBuilder();
        boolean espacio = false;
        for (int i = 0; i < texto.length(); i++)
        {
            char c = Character.toLowerCase(texto.charAt(i));
            if (Character.isLetterOrDigit(c))
            {
                if (espacio && r.length() > 0)
                {
                    r.append(' ');
                }
                r.append(c);
                espacio = false;
            }
            else
            {
                espacio = true;
            }
        }
        return r.toString();
    }

    // true si el texto termina con el sufijo (se compara caracter por caracter). Orden O(s)
    public static boolean terminaCon(String texto, String sufijo)
    {
        if (sufijo.length() == 0 || sufijo.length() > texto.length())
        {
            return false;
        }
        int inicio = texto.length() - sufijo.length();
        boolean iguales = true;
        for (int i = 0; i < sufijo.length() && iguales; i++)
        {
            if (texto.charAt(inicio + i) != sufijo.charAt(i))
            {
                iguales = false;
            }
        }
        return iguales;
    }

    // Busca el titulo en la primera pagina y toma las lineas que le siguen como autores
    public static String obtenerAutores(String pagina, String titulo)
    {
        if (titulo.isEmpty())
        {
            return "";
        }
        String[] lineas = pagina.split("\n");
        String tituloNormal = normalizar(titulo);
        String finTitulo = tituloNormal.substring(Math.max(0, tituloNormal.length() - 25));
        // Candidatas a ultima linea del titulo: las lineas (antes del abstract) con las que termina el titulo.
        // Puede haber varias (una palabra clave como "Literature review" o el recuadro "Citation" de MDPI),
        // asi que se prueba cada una y se queda la que deja mas nombres de autor validos.
        String mejor = "";
        int mejorCantidad = 0;
        boolean llegoAlAbstract = false;
        for (int i = 0; i < lineas.length && !llegoAlAbstract; i++)
        {
            String linea = normalizar(lineas[i]);
            if (linea.startsWith("abstract") || posicion(linea, "a b s t r a c t", 0) != -1)
            {
                llegoAlAbstract = true;
            }
            else if ((linea.length() >= 2 && terminaCon(tituloNormal, linea))
                || (linea.length() >= 25 && terminaCon(linea, finTitulo)))
            {
                String autores = autoresDespuesDe(lineas, i);
                int cantidad = 0;
                if (!autores.isEmpty())
                {
                    cantidad = autores.split(",").length;
                }
                if (cantidad >= mejorCantidad && cantidad > 0)
                {
                    mejor = autores;
                    mejorCantidad = cantidad;
                }
            }
        }
        return mejor;
    }

    // Lineas que siguen a la linea "inicio" hasta la primera afiliacion (maximo 3 lineas)
    public static String autoresDespuesDe(String[] lineas, int inicio)
    {
        String[] marcasAfiliacion =
        {
            "university", "department", "institute", "faculty", "school", "centre", "center", "college",
            "correspondence", "abstract", "a r t i c l e", "received", "@", "academy", "hospital", "accepted",
            "published", "open access", "license", "licence", "editor", "citation", "doi", "http"
        };
        StringBuilder crudo = new StringBuilder();
        boolean seguir = true;
        for (int i = inicio + 1; i < lineas.length && i <= inicio + 3 && seguir; i++)
        {
            String linea = lineas[i].trim();
            String minus = linea.toLowerCase();
            boolean esAfiliacion = linea.isEmpty();
            for (int k = 0; k < marcasAfiliacion.length && !esAfiliacion; k++)
            {
                if (posicion(minus, marcasAfiliacion[k], 0) != -1)
                {
                    esAfiliacion = true;
                }
            }
            if (esAfiliacion)
            {
                seguir = false;
            }
            else
            {
                crudo.append(linea).append(", ");
            }
        }
        return limpiarAutores(crudo.toString());
    }

    // "Thomas K.F. Chiu a,*, Qi Xia b" -> "Thomas K.F. Chiu, Qi Xia"
    public static String limpiarAutores(String crudo)
    {
        String t = crudo.replace(" and ", ", ").replace("|", ",").replace("·", ",").replace("*", "")
            .replace("∗", "").replace("†", "").replace(" ", " ");
        String[] partes = t.split(",");
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < partes.length; i++)
        {
            String[] palabras = partes[i].trim().split(" ");
            StringBuilder nombre = new StringBuilder();
            for (int k = 0; k < palabras.length; k++)
            {
                // Quita los digitos pegados (Mikeladze1), los simbolos y las letras sueltas de afiliacion (a, b, c)
                StringBuilder sinDigitos = new StringBuilder();
                for (int c = 0; c < palabras[k].length(); c++)
                {
                    char letra = palabras[k].charAt(c);
                    if (Character.isLetter(letra) || letra == '.' || letra == '-' || letra == '\'')
                    {
                        sinDigitos.append(palabras[k].charAt(c));
                    }
                }
                String w = sinDigitos.toString().trim();
                boolean letraSuelta = w.length() == 1 && Character.isLowerCase(w.charAt(0));
                if (!w.isEmpty() && !letraSuelta)
                {
                    if (nombre.length() > 0)
                    {
                        nombre.append(' ');
                    }
                    nombre.append(w);
                }
            }
            // Un nombre tiene al menos dos palabras (nombre y apellido) y todas empiezan en mayuscula
            String[] partesNombre = nombre.toString().split(" ");
            boolean mayusculas = partesNombre.length >= 2;
            for (int k = 0; k < partesNombre.length && mayusculas; k++)
            {
                if (!Character.isUpperCase(partesNombre[k].charAt(0)))
                {
                    mayusculas = false;
                }
            }
            if (mayusculas)
            {
                if (r.length() > 0)
                {
                    r.append(", ");
                }
                r.append(nombre);
            }
        }
        return r.toString();
    }

    // Cuenta palabras separadas por espacios; sirve para detectar abstracts demasiado cortos o largos
    public static int contarPalabras(String texto)
    {
        int palabras = 0;
        boolean enPalabra = false;
        for (int i = 0; i < texto.length(); i++)
        {
            boolean blanco = Character.isWhitespace(texto.charAt(i));
            if (!blanco && !enPalabra)
            {
                palabras++;
            }
            enPalabra = !blanco;
        }
        return palabras;
    }
}
