// Interfaz del proyecto: llama a los servicios del ServidorWeb (Java) y muestra los resultados.
let articulos = [];
const $ = id => document.getElementById(id);

function escapar(t) {
  return String(t).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
}

function f(x, d = 4) {
  return x === null || x === undefined ? '-' : Number(x).toFixed(d);
}

async function pedir(url) {
  const r = await fetch(url);
  const datos = await r.json();
  if (!r.ok) throw new Error(datos.error || 'Error del servidor');
  return datos;
}

function cargando(destino, texto) {
  $(destino).innerHTML = `<div class="cargando">${texto}</div>`;
}

function mostrarError(destino, e) {
  $(destino).innerHTML = `<p class="error">${escapar(e.message)}</p>`;
}

// Un campo de texto con su botón ✕: aparece solo cuando hay texto y lo borra al pulsarlo
function campoConBorrar(idCampo, idBorrar, alCambiar) {
  const campo = $(idCampo);
  const contenedor = campo.parentElement;
  const actualizar = () => {
    contenedor.classList.toggle('con-texto', campo.value.length > 0);
    if (alCambiar) alCambiar();
  };
  campo.addEventListener('input', actualizar);
  $(idBorrar).addEventListener('click', () => { campo.value = ''; actualizar(); campo.focus(); });
  actualizar();
}

// ---------- Pestañas ----------
function irA(vista) {
  document.querySelectorAll('.pestana').forEach(x => x.classList.toggle('activa', x.dataset.vista === vista));
  document.querySelectorAll('.vista').forEach(x => x.classList.toggle('activa', x.id === vista));
  window.scrollTo({ top: 0, behavior: 'smooth' });
  try { history.replaceState(null, '', '#' + vista); } catch (e) { /* sin historial */ }
}
document.querySelectorAll('.pestana').forEach(b => b.addEventListener('click', () => irA(b.dataset.vista)));

// ---------- Corpus ----------
async function cargarArticulos() {
  articulos = await pedir('/api/articulos');
  dibujarArticulos();
  $('seleccionArticulos').innerHTML = articulos.map(a => `
    <label><input type="checkbox" value="${a.numero}" ${a.numero === 11 || a.numero === 16 ? 'checked' : ''}>
      <span><strong>${a.numero}.</strong> ${escapar(a.titulo)}</span></label>`).join('');
  $('seleccionArticulos').addEventListener('change', contarSeleccion);
  contarSeleccion();
}

function dibujarArticulos() {
  const filtro = $('filtroArticulos').value.trim().toLowerCase();
  const visibles = articulos.filter(a => !filtro ||
    (a.titulo + ' ' + a.autores + ' ' + a.resumen).toLowerCase().includes(filtro));
  $('contadorArticulos').textContent = `${visibles.length} de ${articulos.length} artículos`;
  $('listaArticulos').innerHTML = visibles.length ? visibles.map(a => `
    <div class="articulo" onclick="this.classList.toggle('abierto')">
      <div><span class="num">${a.numero}</span> <strong>${escapar(a.titulo)}</strong></div>
      <div class="autores">${escapar(a.autores)} · ${a.terminos} términos después del preprocesamiento</div>
      <div class="resumen">${escapar(a.resumen)}</div>
    </div>`).join('') : '<div class="vacio">Ningún artículo coincide con el filtro.</div>';
}
campoConBorrar('filtroArticulos', 'borrarFiltro', () => { if (articulos.length) dibujarArticulos(); });
$('abrirTodos').addEventListener('click', () => document.querySelectorAll('.articulo').forEach(x => x.classList.add('abierto')));
$('cerrarTodos').addEventListener('click', () => document.querySelectorAll('.articulo').forEach(x => x.classList.remove('abierto')));

// ---------- Ranking de términos (seguimiento) ----------
$('botonRanking').addEventListener('click', async () => {
  cargando('resultadoRanking', 'Ordenando los términos…');
  try {
    const d = await pedir('/api/ranking');
    const nombres = ['Counting Sort', 'Radix Sort', 'Bucket Sort'];
    let html = `<div class="tarjetas">
      <div class="tarjeta"><div class="valor">${d.n}</div><div class="etiqueta">Términos distintos (n)</div></div>
      <div class="tarjeta"><div class="valor">${d.k}</div><div class="etiqueta">Frecuencia mayor (k)</div></div>
      <div class="tarjeta"><div class="valor">${f(d.tiempos[0], 4)} ms</div><div class="etiqueta">Counting Sort</div></div>
    </div>
    <div class="tabla-scroll"><table><tr>${nombres.map(x => `<th>${x}</th>`).join('')}</tr>
      <tr>${d.tiempos.map(t => `<td>${f(t, 4)} ms</td>`).join('')}</tr></table></div>
    <h3>Los 15 términos más frecuentes</h3>`;
    const mayor = d.top[0].frecuencia;
    html += d.top.map((t, i) => `<div class="ranking-fila"><span class="pos">${i + 1}</span><span>${escapar(t.palabra)}</span>
      <span class="barra-fondo"><span class="barra-valor" style="width:${(100 * t.frecuencia / mayor).toFixed(1)}%"></span></span>
      <span class="frec">${t.frecuencia}</span></div>`).join('');
    html += '<p class="ayuda">Los tres algoritmos dejan el mismo orden. Tiempo promedio de 200 ejecuciones en el servidor.</p>';
    $('resultadoRanking').innerHTML = html;
  } catch (e) {
    mostrarError('resultadoRanking', e);
  }
});
$('limpiarRanking').addEventListener('click', () => { $('resultadoRanking').innerHTML = ''; });

// ---------- Requerimiento 1: similitud ----------
function nombreCorto(n) {
  const a = articulos.find(x => x.numero === n);
  return a ? `${n}. ${escapar(a.titulo.slice(0, 60))}${a.titulo.length > 60 ? '…' : ''}` : n;
}

function contarSeleccion() {
  const n = document.querySelectorAll('#seleccionArticulos input:checked').length;
  const pares = n > 1 ? n * (n - 1) / 2 : 0;
  $('contadorSeleccion').textContent = `${n} seleccionado${n === 1 ? '' : 's'} · ${pares} par${pares === 1 ? '' : 'es'}`;
  $('botonSimilitud').disabled = n < 2;
}
$('seleccionarTodos').addEventListener('click', () => {
  document.querySelectorAll('#seleccionArticulos input').forEach(x => { x.checked = true; });
  contarSeleccion();
});
$('limpiarSeleccion').addEventListener('click', () => {
  document.querySelectorAll('#seleccionArticulos input').forEach(x => { x.checked = false; });
  contarSeleccion();
});
$('limpiarSimilitud').addEventListener('click', () => { $('resultadoSimilitud').innerHTML = ''; });

$('botonSimilitud').addEventListener('click', async () => {
  const ids = [...document.querySelectorAll('#seleccionArticulos input:checked')].map(x => x.value);
  if (ids.length < 2) { $('resultadoSimilitud').innerHTML = '<p class="error">Seleccione al menos dos artículos.</p>'; return; }
  cargando('resultadoSimilitud', 'Calculando los 6 algoritmos…');
  try {
    const d = await pedir('/api/similitud?ids=' + ids.join(','));
    // El valor más alto de cada par se resalta; la barrita de cada celda mide el valor de 0 a 1
    let html = '<p class="ayuda">El valor más alto de cada fila está resaltado. Todas las similitudes van de 0 (nada en común) a 1 (idénticos).</p>' +
      '<div class="tabla-scroll"><table><tr><th class="izq">Par</th>' + d.algoritmos.map(a => `<th>${escapar(a)}</th>`).join('') + '</tr>';
    for (const p of d.pares) {
      const mayor = Math.max(...p.valores);
      html += `<tr><td class="izq">${nombreCorto(p.a)}<br>vs ${nombreCorto(p.b)}</td>` +
        p.valores.map(v => `<td class="celda ${v === mayor ? 'max' : ''}">${f(v)}<span class="barrita" style="width:${Math.max(0, Math.min(100, v * 100))}%"></span></td>`).join('') + '</tr>';
    }
    html += '</table></div>';
    html += '<h3>Tiempo de cada algoritmo (ms) y distancia euclidiana de los embeddings</h3><div class="tabla-scroll"><table><tr><th class="izq">Par</th>' +
      d.algoritmos.map(a => `<th>${escapar(a)}</th>`).join('') + '<th>Euclidiana GloVe</th><th>Euclidiana SBERT</th></tr>';
    for (const p of d.pares) {
      html += `<tr><td class="izq">${p.a} vs ${p.b}</td>` + p.tiempos.map(v => `<td>${f(v, 4)}</td>`).join('') +
        `<td>${f(p.euclidianas[0])}</td><td>${f(p.euclidianas[1])}</td></tr>`;
    }
    html += '</table></div><p class="ayuda">Los vectores de Sentence-BERT se calculan una sola vez con el script de Python; aquí solo se mide el coseno. GloVe sí promedia los vectores de palabra en cada comparación.</p>';
    $('resultadoSimilitud').innerHTML = html;
  } catch (e) {
    mostrarError('resultadoSimilitud', e);
  }
});

// ---------- Requerimiento 2: agrupamiento ----------
async function agrupar() {
  cargando('resultadoClustering', 'Armando el dendrograma…');
  const metodo = $('metodo').value;
  const rep = $('representacion').value;
  const k = $('k').value;
  try {
    const d = await pedir(`/api/clustering?metodo=${metodo}&rep=${rep}` + (k ? `&k=${k}` : ''));
    let html = `<div class="tarjetas">
      <div class="tarjeta"><div class="valor">${f(d.ccc)}</div><div class="etiqueta">Correlación cofenética</div></div>
      <div class="tarjeta"><div class="valor">${f(d.siluetas[d.mejorK - 2])}</div><div class="etiqueta">Mejor silueta (k = ${d.mejorK})</div></div>
      <div class="tarjeta"><div class="valor">${f(d.tiempoMs, 3)} ms</div><div class="etiqueta">Tiempo del agrupamiento</div></div>
    </div>`;
    html += `<div class="svg-contenedor">${d.svg}</div>`;
    html += '<h3>Silueta según el número de grupos</h3><table><tr>' + d.siluetas.map((_, i) => `<th>k = ${i + 2}</th>`).join('') +
      '</tr><tr>' + d.siluetas.map(s => `<td>${f(s)}</td>`).join('') + '</tr></table>';
    html += '<h3>Fusiones</h3><div class="tabla-scroll"><table><tr><th>Paso</th><th class="izq">Grupo nuevo</th><th class="izq">Se unen</th><th>Distancia</th><th>Tamaño</th></tr>' +
      d.fusiones.map(x => `<tr><td>${x.paso}</td><td class="izq">G${x.grupo}</td><td class="izq">${escapar(x.izquierdo)} + ${escapar(x.derecho)}</td><td>${f(x.altura)}</td><td>${x.tamano}</td></tr>`).join('') +
      '</table></div>';
    $('resultadoClustering').innerHTML = html;
  } catch (e) {
    mostrarError('resultadoClustering', e);
  }
}
$('botonClustering').addEventListener('click', agrupar);
// Al cambiar una opción, si ya hay un dendrograma en pantalla se vuelve a calcular solo
['metodo', 'representacion', 'k'].forEach(id => $(id).addEventListener('change', () => {
  if ($('resultadoClustering').innerHTML.trim() !== '') agrupar();
}));
$('limpiarClustering').addEventListener('click', () => {
  $('metodo').value = 'average';
  $('representacion').value = 'tfidf';
  $('k').value = '';
  $('resultadoClustering').innerHTML = '';
});

// ---------- Requerimiento 3: búsqueda ----------
async function buscar() {
  const patron = $('patron').value;
  if (patron.trim() === '') { $('resultadoBusqueda').innerHTML = '<div class="vacio">Escriba un patrón para buscar.</div>'; return; }
  const may = $('mayusculas').checked ? 1 : 0;
  cargando('resultadoBusqueda', 'Buscando con los tres algoritmos…');
  try {
    const d = await pedir(`/api/busqueda?patron=${encodeURIComponent(patron)}&mayusculas=${may}`);
    const total = d.algoritmos[0].ocurrencias;
    let html = `<div class="tarjetas">
      <div class="tarjeta"><div class="valor">${total}</div><div class="etiqueta">Ocurrencias en ${d.resultados.length} artículo${d.resultados.length === 1 ? '' : 's'}</div></div>
      <div class="tarjeta"><div class="valor">${d.m}</div><div class="etiqueta">Largo del patrón (m)</div></div>
      <div class="tarjeta"><div class="valor">${d.n}</div><div class="etiqueta">Caracteres del corpus (n)</div></div>
    </div>
    <p>Los tres algoritmos dan las mismas posiciones: <strong>${d.coinciden ? 'sí ✔' : 'no ✘'}</strong>.</p>`;
    html += '<div class="tabla-scroll"><table><tr><th class="izq">Algoritmo</th><th>Preprocesamiento (ms)</th><th>Búsqueda (ms)</th><th>Comparaciones</th><th>Comparaciones / n</th><th>Ocurrencias</th></tr>' +
      d.algoritmos.map(a => `<tr><td class="izq">${a.nombre}</td><td>${f(a.preprocesamientoMs, 5)}</td><td>${f(a.busquedaMs, 5)}</td><td>${a.comparaciones}</td><td>${f(a.comparaciones / d.n, 3)}</td><td>${a.ocurrencias}</td></tr>`).join('') +
      '</table></div>';
    if (d.resultados.length === 0) {
      html += '<div class="vacio">No hay ocurrencias de ese patrón. Pruebe con otra palabra o desmarque «Distinguir mayúsculas».</div>';
    }
    for (const r of d.resultados) {
      html += `<div class="resultado"><strong>${r.numero}. ${escapar(r.titulo)}</strong><div class="autores">${escapar(r.autores)} · posiciones ${r.posiciones.join(', ')}</div>` +
        r.fragmentos.map(x => `<div class="fragmento">…${escapar(x.antes)}<mark>${escapar(x.coincidencia)}</mark>${escapar(x.despues)}…</div>`).join('') + '</div>';
    }
    $('resultadoBusqueda').innerHTML = html;
  } catch (e) {
    mostrarError('resultadoBusqueda', e);
  }
}
campoConBorrar('patron', 'borrarPatron');
$('botonBusqueda').addEventListener('click', buscar);
$('patron').addEventListener('keydown', e => { if (e.key === 'Enter') buscar(); });
$('limpiarBusqueda').addEventListener('click', () => {
  $('patron').value = '';
  $('patron').parentElement.classList.remove('con-texto');
  $('mayusculas').checked = false;
  $('resultadoBusqueda').innerHTML = '';
  $('patron').focus();
});
document.querySelectorAll('.chip').forEach(c => c.addEventListener('click', () => {
  $('patron').value = c.dataset.p;
  $('patron').parentElement.classList.add('con-texto');
  buscar();
}));

cargarArticulos();
const inicial = location.hash.slice(1);
if (inicial && document.getElementById(inicial) && document.querySelector(`.pestana[data-vista="${inicial}"]`)) irA(inicial);
