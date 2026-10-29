package busqueda;

import java.util.ArrayList;

/**
 * Molde comun de los tres algoritmos de coincidencia exacta.
 * Cada algoritmo separa su trabajo en dos fases para poder medirlas por aparte:
 *   preprocesar(patron): construye las estructuras auxiliares (tabla de prefijos,
 *                        tablas heuristicas o hash del patron);
 *   buscar(texto):       recorre el texto y devuelve las posiciones donde empieza el patron.
 * El contador "comparaciones" suma cada comparacion de un caracter del patron contra
 * uno del texto (en Rabin-Karp tambien cuenta cada comparacion de hashes).
 */
public abstract class AlgoritmoBusqueda
{
    // Texto que se busca (se guarda en preprocesar) y su largo m
    protected char[] patron;
    protected int m;
    // Cuantas comparaciones hizo la ultima busqueda; sirve para comparar los algoritmos sin depender de la velocidad del PC
    public long comparaciones;

    // Nombre que se muestra en las tablas ("KMP", "Boyer-Moore", "Rabin-Karp")
    public abstract String nombre();

    // Fase 1: calcula las tablas que necesita el algoritmo; solo depende del patron
    public abstract void preprocesar(char[] patron);

    // Fase 2: recorre el texto y devuelve la posicion (empezando en 0) de cada aparicion del patron
    public abstract ArrayList<Integer> buscar(char[] texto);
}
