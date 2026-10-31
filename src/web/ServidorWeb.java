package web;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import busqueda.AlgoritmoBusqueda;
import busqueda.BoyerMoore;
import busqueda.KMP;
import busqueda.RabinKarp;
import casos.Experimentos;
import clustering.AgrupamientoJerarquico;
import clustering.Dendrograma;
import clustering.Evaluacion;
import clustering.Fusion;
import datos.Corpus;
import modelo.Articulo;
import seguimiento.Ordenamientos;
import seguimiento.Preprocesador;
import seguimiento.Termino;
import similitud.AnalizadorSimilitud;

/**
 * Aplicacion web del proyecto. Usa el servidor HTTP que trae el JDK (com.sun.net.httpserver),
 * sin frameworks. La pagina (carpeta web/) llama a estos servicios:
 *   GET /api/articulos                         lista de los 20 articulos (titulo, autores, abstract)
 *   GET /api/ranking                           base del seguimiento: top de terminos con Counting, Radix y Bucket Sort
 *   GET /api/similitud?ids=11,16,3             requerimiento 1: los 6 algoritmos para cada par
 *   GET /api/clustering?metodo=average&rep=tfidf   requerimiento 2: dendrograma SVG y metricas
 *   GET /api/busqueda?patron=learning&mayusculas=0 requerimiento 3: KMP, Boyer-Moore y Rabin-Karp
 * Uso: java -cp bin web.ServidorWeb   (puerto 8080 o el de la variable de entorno PORT)
 */
public class ServidorWeb
{
    // Se cargan una sola vez al arrancar y se comparten entre todas las peticiones
    static Corpus corpus;
    static AnalizadorSimilitud analizador;
    // Carpeta con index.html, estilos.css y app.js
    static File carpetaWeb;

    // Punto de entrada: carga los datos, registra una ruta por cada servicio y deja el servidor escuchando
    public static void main(String[] args) throws IOException
    {
        Locale.setDefault(Locale.US);
        corpus = new Corpus("datos");
        analizador = new AnalizadorSimilitud(corpus, "datos");
        carpetaWeb = new File("web");
        // Azure (y otros servicios) indican el puerto con la variable de entorno PORT; en el PC se usa 8080
        int puerto = 8080;
        String variable = System.getenv("PORT");
        if (variable != null && !variable.isEmpty())
        {
            puerto = Integer.parseInt(variable);
        }
        HttpServer servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/api/articulos", ServidorWeb::articulos);
        servidor.createContext("/api/ranking", ServidorWeb::ranking);
        servidor.createContext("/api/similitud", ServidorWeb::similitud);
        servidor.createContext("/api/clustering", ServidorWeb::clustering);
        servidor.createContext("/api/busqueda", ServidorWeb::busqueda);
        servidor.createContext("/", ServidorWeb::archivoEstatico);
        servidor.start();
        System.out.println("Servidor listo en http://localhost:" + puerto);
    }

    // Lee los parametros de la URL (?a=1&b=2) y los deja en un mapa nombre -> valor
    static HashMap<String, String> parametros(HttpExchange intercambio)
    {
        HashMap<String, String> mapa = new HashMap<String, String>();
        String consulta = intercambio.getRequestURI().getRawQuery();
        if (consulta == null)
        {
            return mapa;
        }
        String[] pares = consulta.split("&");
        for (int i = 0; i < pares.length; i++)
        {
            String[] kv = pares[i].split("=", 2);
            String valor = "";
            if (kv.length > 1)
            {
                valor = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
            mapa.put(kv[0], valor);
        }
        return mapa;
    }

    // Envia la respuesta HTTP: codigo, tipo de contenido y cuerpo
    static void responder(HttpExchange intercambio, int codigo, String tipo, byte[] cuerpo) throws IOException
    {
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.sendResponseHeaders(codigo, cuerpo.length);
        OutputStream salida = intercambio.getResponseBody();
        salida.write(cuerpo);
        salida.close();
    }

    // Respuesta 200 con un JSON
    static void responderJson(HttpExchange intercambio, String json) throws IOException
    {
        responder(intercambio, 200, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    // Respuesta 400 con {"error": mensaje} para que la pagina muestre el aviso
    static void error(HttpExchange intercambio, String mensaje) throws IOException
    {
        responder(intercambio, 400, "application/json; charset=utf-8",
            ("{\"error\":" + Json.texto(mensaje) + "}").getBytes(StandardCharsets.UTF_8));
    }

    // Entrega la pagina web (html, css, js, svg) desde la carpeta web/
    static void archivoEstatico(HttpExchange intercambio) throws IOException
    {
        String ruta = intercambio.getRequestURI().getPath();
        if (ruta.equals("/"))
        {
            ruta = "/index.html";
        }
        File archivo = new File(carpetaWeb, ruta.substring(1));
        // Solo se sirven archivos que esten dentro de la carpeta web/
        boolean dentro = archivo.getCanonicalPath().startsWith(carpetaWeb.getCanonicalPath() + File.separator);
        if (!dentro || !archivo.isFile())
        {
            responder(intercambio, 404, "text/plain; charset=utf-8", "No encontrado".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String tipo = "text/plain; charset=utf-8";
        if (ruta.endsWith(".html"))
        {
            tipo = "text/html; charset=utf-8";
        }
        else if (ruta.endsWith(".css"))
        {
            tipo = "text/css; charset=utf-8";
        }
        else if (ruta.endsWith(".js"))
        {
            tipo = "application/javascript; charset=utf-8";
        }
        else if (ruta.endsWith(".svg"))
        {
            tipo = "image/svg+xml";
        }
        responder(intercambio, 200, tipo, Files.readAllBytes(archivo.toPath()));
    }

    // Servicio /api/articulos: lista los 20 articulos con cuantos terminos tiene cada abstract
    static void articulos(HttpExchange intercambio) throws IOException
    {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < corpus.tamano(); i++)
        {
            Articulo a = corpus.obtener(i);
            if (i > 0)
            {
                json.append(",");
            }
            json.append("{\"numero\":").append(a.numero)
                .append(",\"titulo\":").append(Json.texto(a.titulo))
                .append(",\"autores\":").append(Json.texto(a.autores))
                .append(",\"resumen\":").append(Json.texto(a.resumen))
                .append(",\"terminos\":").append(analizador.terminos.get(i).length)
                .append("}");
        }
        responderJson(intercambio, json.append("]").toString());
    }

    // Servicio /api/ranking: es el Seguimiento 2026-2 dentro de la aplicacion. Cuenta los terminos de los 20 abstracts
    // con el mismo Preprocesador y los ordena con Counting, Radix y Bucket Sort (clases copiadas sin cambios).
    // Cada algoritmo se repite 200 veces y se entrega el tiempo promedio. Orden de complejidad O(n + k).
    static void ranking(HttpExchange intercambio) throws IOException
    {
        String todo = "";
        for (int i = 0; i < corpus.tamano(); i++)
        {
            todo = todo + corpus.obtener(i).resumen + " ";
        }
        Termino[] terminos = new Preprocesador().contarTerminos(todo);
        int n = terminos.length;
        Ordenamientos ord = new Ordenamientos();
        int k = ord.obtenerMayor(terminos, n);
        int repeticiones = 200;
        double[] ms = new double[3];
        for (int a = 0; a < 3; a++)
        {
            long total = 0;
            for (int r = 0; r < repeticiones; r++)
            {
                Termino[] copia = terminos.clone();
                long t0 = System.nanoTime();
                if (a == 0)
                {
                    ord.countingSort(copia, n);
                }
                else if (a == 1)
                {
                    ord.radixSort(copia, n);
                }
                else
                {
                    ord.bucketSort(copia, n);
                }
                total = total + (System.nanoTime() - t0);
            }
            ms[a] = total / (double) repeticiones / 1e6;
        }
        // Los tres dejan los terminos de menor a mayor frecuencia: el ranking se lee desde el final
        Termino[] ordenado = ord.countingSort(terminos, n);
        StringBuilder json = new StringBuilder("{\"n\":" + n + ",\"k\":" + k + ",\"tiempos\":" + Json.arreglo(ms));
        json.append(",\"top\":[");
        for (int i = 0; i < 15 && i < n; i++)
        {
            if (i > 0)
            {
                json.append(",");
            }
            json.append("{\"palabra\":").append(Json.texto(ordenado[n - 1 - i].palabra))
                .append(",\"frecuencia\":").append(ordenado[n - 1 - i].frecuencia).append("}");
        }
        responderJson(intercambio, json.append("]}").toString());
    }

    // Servicio /api/similitud: para cada par de articulos elegidos corre los 6 algoritmos.
    // Con k articulos hay k(k-1)/2 pares.
    static void similitud(HttpExchange intercambio) throws IOException
    {
        HashMap<String, String> p = parametros(intercambio);
        // ids llega como "11,16,3"; se descartan los repetidos y los que no existan
        String ids = p.getOrDefault("ids", "");
        ArrayList<Integer> numeros = new ArrayList<Integer>();
        String[] partes = ids.split(",");
        for (int i = 0; i < partes.length; i++)
        {
            try
            {
                int numero = Integer.parseInt(partes[i].trim());
                if (corpus.buscarPorNumero(numero) != null && !numeros.contains(numero))
                {
                    numeros.add(numero);
                }
            }
            catch (NumberFormatException e)
            {
                // se ignora lo que no sea un numero
            }
        }
        if (numeros.size() < 2)
        {
            error(intercambio, "Seleccione al menos dos articulos");
            return;
        }
        StringBuilder json = new StringBuilder("{\"algoritmos\":" + Json.arregloTextos(AnalizadorSimilitud.NOMBRES));
        json.append(",\"pares\":[");
        boolean primero = true;
        for (int i = 0; i < numeros.size(); i++)
        {
            for (int j = i + 1; j < numeros.size(); j++)
            {
                double[] valores = analizador.compararTodos(numeros.get(i), numeros.get(j));
                if (!primero)
                {
                    json.append(",");
                }
                primero = false;
                json.append("{\"a\":").append(numeros.get(i)).append(",\"b\":").append(numeros.get(j))
                    .append(",\"valores\":").append(Json.arreglo(valores))
                    .append(",\"tiempos\":").append(Json.arreglo(analizador.ultimosTiempos))
                    .append(",\"euclidianas\":").append(Json.arreglo(analizador.ultimasDistanciasEuclidianas))
                    .append("}");
            }
        }
        responderJson(intercambio, json.append("]}").toString());
    }

    // Servicio /api/clustering: arma el dendrograma con el criterio y la representacion elegidos
    // y calcula las metricas internas (correlacion cofenetica y silueta).
    static void clustering(HttpExchange intercambio) throws IOException
    {
        HashMap<String, String> p = parametros(intercambio);
        String metodo = p.getOrDefault("metodo", AgrupamientoJerarquico.AVERAGE);
        String rep = p.getOrDefault("rep", "tfidf");
        if (!metodo.equals(AgrupamientoJerarquico.SINGLE) && !metodo.equals(AgrupamientoJerarquico.COMPLETE)
            && !metodo.equals(AgrupamientoJerarquico.AVERAGE))
        {
            error(intercambio, "Metodo no valido");
            return;
        }
        // Distancias entre los 20 abstracts (1 - coseno) segun la representacion: tfidf, glove o sbert
        double[][] d = Experimentos.matrizDistancias(analizador.representacion(rep));
        AgrupamientoJerarquico arbol = new AgrupamientoJerarquico();
        long t0 = System.nanoTime();
        ArrayList<Fusion> fusiones = arbol.agrupar(d, metodo);
        double ms = (System.nanoTime() - t0) / 1e6;
        double ccc = Evaluacion.correlacionCofenetica(d, Evaluacion.matrizCofenetica(fusiones, arbol));
        // Silueta para k = 2..6; el k con mayor silueta se propone como mejor corte
        double[] siluetas = new double[5];
        int mejorK = 2;
        for (int k = 2; k <= 6; k++)
        {
            siluetas[k - 2] = Evaluacion.silueta(d, arbol.cortar(k), k);
            if (siluetas[k - 2] > siluetas[mejorK - 2])
            {
                mejorK = k;
            }
        }
        // Si el usuario pide un k propio (2..10) se usa ese; si no, el mejor
        int k = mejorK;
        if (p.containsKey("k"))
        {
            try
            {
                k = Math.max(2, Math.min(10, Integer.parseInt(p.get("k"))));
            }
            catch (NumberFormatException e)
            {
                k = mejorK;
            }
        }
        // Dibujo del dendrograma y respuesta con todas las fusiones para mostrarlas en la tabla
        String svg = Dendrograma.generarSvg(arbol, Experimentos.etiquetas(corpus), "Dendrograma " + metodo + " linkage ("
            + rep + ")", arbol.cortar(k));
        StringBuilder json = new StringBuilder("{");
        json.append("\"svg\":").append(Json.texto(svg));
        json.append(",\"ccc\":").append(Json.numero(ccc));
        json.append(",\"siluetas\":").append(Json.arreglo(siluetas));
        json.append(",\"mejorK\":").append(mejorK);
        json.append(",\"k\":").append(k);
        json.append(",\"tiempoMs\":").append(Json.numero(ms));
        json.append(",\"fusiones\":[");
        for (int i = 0; i < fusiones.size(); i++)
        {
            Fusion f = fusiones.get(i);
            if (i > 0)
            {
                json.append(",");
            }
            json.append("{\"paso\":").append(i + 1)
                .append(",\"grupo\":").append(arbol.n + i)
                .append(",\"izquierdo\":").append(Json.texto(nombreGrupo(arbol, f.izquierdo)))
                .append(",\"derecho\":").append(Json.texto(nombreGrupo(arbol, f.derecho)))
                .append(",\"altura\":").append(Json.numero(f.altura))
                .append(",\"tamano\":").append(f.tamano).append("}");
        }
        responderJson(intercambio, json.append("]}").toString());
    }

    // Texto de un grupo para la tabla de fusiones: "{3, 7}" o "G21 {3, 7, 9}" para los grupos nuevos
    static String nombreGrupo(AgrupamientoJerarquico arbol, int grupo)
    {
        ArrayList<Integer> hojas = arbol.hojas(grupo);
        StringBuilder r = new StringBuilder();
        if (grupo >= arbol.n)
        {
            r.append("G").append(grupo).append(" ");
        }
        r.append("{");
        for (int i = 0; i < hojas.size(); i++)
        {
            if (i > 0)
            {
                r.append(", ");
            }
            r.append(corpus.obtener(hojas.get(i)).numero);
        }
        return r.append("}").toString();
    }

    // Servicio /api/busqueda: corre KMP, Boyer-Moore y Rabin-Karp sobre los 20 abstracts.
    // Cada algoritmo se repite 50 veces para que el tiempo promedio no dependa de una sola medicion.
    static void busqueda(HttpExchange intercambio) throws IOException
    {
        HashMap<String, String> p = parametros(intercambio);
        String patron = p.getOrDefault("patron", "");
        boolean distinguir = p.getOrDefault("mayusculas", "0").equals("1");
        if (patron.isEmpty() || patron.length() > 200)
        {
            error(intercambio, "Escriba un patron de 1 a 200 caracteres");
            return;
        }
        // Sin distinguir mayusculas se pasa todo a minusculas, tanto el patron como los textos
        if (!distinguir)
        {
            patron = patron.toLowerCase();
        }
        char[] patronChars = patron.toCharArray();
        char[][] textos = new char[corpus.tamano()][];
        for (int i = 0; i < corpus.tamano(); i++)
        {
            String t = corpus.obtener(i).resumen;
            if (!distinguir)
            {
                t = t.toLowerCase();
            }
            textos[i] = t.toCharArray();
        }
        // Los tres algoritmos comparten la misma interfaz, asi que se tratan igual en un ciclo
        AlgoritmoBusqueda[] algoritmos = { new KMP(), new BoyerMoore(), new RabinKarp() };
        int repeticiones = 50;
        StringBuilder json = new StringBuilder("{\"patron\":" + Json.texto(patron) + ",\"algoritmos\":[");
        ArrayList<ArrayList<Integer>> posicionesKmp = new ArrayList<ArrayList<Integer>>();
        boolean coinciden = true;
        for (int a = 0; a < algoritmos.length; a++)
        {
            long tiempoPre = 0;
            long tiempoBus = 0;
            long comparaciones = 0;
            int total = 0;
            ArrayList<ArrayList<Integer>> posiciones = new ArrayList<ArrayList<Integer>>();
            for (int r = 0; r < repeticiones; r++)
            {
                comparaciones = 0;
                total = 0;
                posiciones.clear();
                long t0 = System.nanoTime();
                algoritmos[a].preprocesar(patronChars);
                long t1 = System.nanoTime();
                for (int i = 0; i < textos.length; i++)
                {
                    ArrayList<Integer> encontradas = algoritmos[a].buscar(textos[i]);
                    comparaciones = comparaciones + algoritmos[a].comparaciones;
                    total = total + encontradas.size();
                    posiciones.add(encontradas);
                }
                long t2 = System.nanoTime();
                tiempoPre = tiempoPre + (t1 - t0);
                tiempoBus = tiempoBus + (t2 - t1);
            }
            // Control de correccion: las posiciones de Boyer-Moore y Rabin-Karp deben ser las mismas de KMP
            if (a == 0)
            {
                posicionesKmp = posiciones;
            }
            else
            {
                for (int i = 0; i < textos.length; i++)
                {
                    if (!posiciones.get(i).equals(posicionesKmp.get(i)))
                    {
                        coinciden = false;
                    }
                }
            }
            if (a > 0)
            {
                json.append(",");
            }
            json.append("{\"nombre\":").append(Json.texto(algoritmos[a].nombre()))
                .append(",\"preprocesamientoMs\":").append(Json.numero(tiempoPre / 1e6 / repeticiones))
                .append(",\"busquedaMs\":").append(Json.numero(tiempoBus / 1e6 / repeticiones))
                .append(",\"comparaciones\":").append(comparaciones)
                .append(",\"ocurrencias\":").append(total).append("}");
        }
        // n = total de caracteres de los 20 textos, m = largo del patron (para comparar con la teoria)
        json.append("],\"coinciden\":").append(coinciden);
        int n = 0;
        for (int i = 0; i < textos.length; i++)
        {
            n = n + textos[i].length;
        }
        json.append(",\"n\":").append(n).append(",\"m\":").append(patronChars.length);
        json.append(",\"resultados\":[");
        boolean primero = true;
        for (int i = 0; i < textos.length; i++)
        {
            ArrayList<Integer> pos = posicionesKmp.get(i);
            if (pos.isEmpty())
            {
                continue;
            }
            Articulo art = corpus.obtener(i);
            if (!primero)
            {
                json.append(",");
            }
            primero = false;
            json.append("{\"numero\":").append(art.numero)
                .append(",\"titulo\":").append(Json.texto(art.titulo))
                .append(",\"autores\":").append(Json.texto(art.autores))
                .append(",\"posiciones\":").append(pos.toString())
                .append(",\"fragmentos\":[");
            // Fragmento: 50 caracteres antes y despues de cada aparicion, para mostrarlo resaltado
            for (int k = 0; k < pos.size(); k++)
            {
                int ini = Math.max(0, pos.get(k) - 50);
                int fin = Math.min(art.resumen.length(), pos.get(k) + patronChars.length + 50);
                if (k > 0)
                {
                    json.append(",");
                }
                json.append("{\"antes\":").append(Json.texto(art.resumen.substring(ini, pos.get(k))))
                    .append(",\"coincidencia\":").append(Json.texto(art.resumen.substring(pos.get(k), pos.get(k) + patronChars.length)))
                    .append(",\"despues\":").append(Json.texto(art.resumen.substring(pos.get(k) + patronChars.length, fin)))
                    .append("}");
            }
            json.append("]}");
        }
        responderJson(intercambio, json.append("]}").toString());
    }
}
