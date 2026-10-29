// Interfaz del proyecto: llama a los servicios del ServidorWeb (Java) y muestra los resultados.
let articulos = [];

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

document.querySelectorAll('.pestana').forEach(b => b.addEventListener('click', () => {
  document.querySelectorAll('.pestana').forEach(x => x.classList.remove('activa'));
  document.querySelectorAll('.vista').forEach(x => x.classList.remove('activa'));
  b.classList.add('activa');
  document.getElementById(b.dataset.vista).classList.add('activa');
}));

async function cargarArticulos() {
  articulos = await pedir('/api/articulos');
  document.getElementById('listaArticulos').innerHTML = articulos.map(a => `
    <div class="articulo" onclick="this.classList.toggle('abierto')">
      <div><span class="num">${a.numero}.</span> <strong>${escapar(a.titulo)}</strong></div>
      <div class="autores">${escapar(a.autores)} · ${a.terminos} términos después del preprocesamiento</div>
      <div class="resumen">${escapar(a.resumen)}</div>
    </div>`).join('');
  document.getElementById('seleccionArticulos').innerHTML = articulos.map(a => `
    <label><input type="checkbox" value="${a.numero}" ${a.numero === 11 || a.numero === 16 ? 'checked' : ''}>
      <span><strong>${a.numero}.</strong> ${escapar(a.titulo)}</span></label>`).join('');
}

function nombreCorto(n) {
  const a = articulos.find(x => x.numero === n);
  return a ? `${n}. ${escapar(a.titulo.slice(0, 60))}${a.titulo.length > 60 ? '…' : ''}` : n;
}

document.getElementById('botonSimilitud').addEventListener('click', async () => {
  const ids = [...document.querySelectorAll('#seleccionArticulos input:checked')].map(x => x.value);
  const salida = document.getElementById('resultadoSimilitud');
  if (ids.length < 2) { salida.innerHTML = '<p class="error">Seleccione al menos dos artículos.</p>'; return; }
  salida.innerHTML = '<p>Calculando…</p>';
  try {
    const d = await pedir('/api/similitud?ids=' + ids.join(','));
    let html = '<div class="tabla-scroll"><table><tr><th class="izq">Par</th>' +
      d.algoritmos.map(a => `<th>${escapar(a)}</th>`).join('') + '</tr>';
    for (const p of d.pares) {
      html += `<tr><td class="izq">${nombreCorto(p.a)}<br>vs ${nombreCorto(p.b)}</td>` +
        p.valores.map(v => `<td>${f(v)}</td>`).join('') + '</tr>';
    }
    html += '</table></div>';
    html += '<h3>Tiempo de cada algoritmo (ms) y distancia euclidiana de los embeddings</h3><div class="tabla-scroll"><table><tr><th class="izq">Par</th>' +
      d.algoritmos.map(a => `<th>${escapar(a)}</th>`).join('') + '<th>Euclidiana GloVe</th><th>Euclidiana SBERT</th></tr>';
    for (const p of d.pares) {
      html += `<tr><td class="izq">${p.a} vs ${p.b}</td>` + p.tiempos.map(v => `<td>${f(v, 4)}</td>`).join('') +
        `<td>${f(p.euclidianas[0])}</td><td>${f(p.euclidianas[1])}</td></tr>`;
    }
    html += '</table></div><p class="ayuda">Los vectores de Sentence-BERT se calculan una sola vez con el script de Python; aquí solo se mide el coseno. GloVe sí promedia los vectores de palabra en cada comparación.</p>';
    salida.innerHTML = html;
  } catch (e) {
    salida.innerHTML = `<p class="error">${escapar(e.message)}</p>`;
  }
});

document.getElementById('botonClustering').addEventListener('click', async () => {
  const salida = document.getElementById('resultadoClustering');
  salida.innerHTML = '<p>Agrupando…</p>';
  const metodo = document.getElementById('metodo').value;
  const rep = document.getElementById('representacion').value;
  const k = document.getElementById('k').value;
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
    salida.innerHTML = html;
  } catch (e) {
    salida.innerHTML = `<p class="error">${escapar(e.message)}</p>`;
  }
});

document.getElementById('botonBusqueda').addEventListener('click', buscar);
document.getElementById('patron').addEventListener('keydown', e => { if (e.key === 'Enter') buscar(); });

async function buscar() {
  const salida = document.getElementById('resultadoBusqueda');
  const patron = document.getElementById('patron').value;
  const may = document.getElementById('mayusculas').checked ? 1 : 0;
  salida.innerHTML = '<p>Buscando…</p>';
  try {
    const d = await pedir(`/api/busqueda?patron=${encodeURIComponent(patron)}&mayusculas=${may}`);
    let html = `<p>Patrón de m = ${d.m} caracteres en n = ${d.n} caracteres (20 abstracts). Los tres algoritmos dan las mismas posiciones: <strong>${d.coinciden ? 'sí' : 'no'}</strong>.</p>`;
    html += '<table><tr><th class="izq">Algoritmo</th><th>Preprocesamiento (ms)</th><th>Búsqueda (ms)</th><th>Comparaciones</th><th>Comparaciones / n</th><th>Ocurrencias</th></tr>' +
      d.algoritmos.map(a => `<tr><td class="izq">${a.nombre}</td><td>${f(a.preprocesamientoMs, 5)}</td><td>${f(a.busquedaMs, 5)}</td><td>${a.comparaciones}</td><td>${f(a.comparaciones / d.n, 3)}</td><td>${a.ocurrencias}</td></tr>`).join('') +
      '</table>';
    if (d.resultados.length === 0) {
      html += '<p>No hay ocurrencias.</p>';
    }
    for (const r of d.resultados) {
      html += `<div class="resultado"><strong>${r.numero}. ${escapar(r.titulo)}</strong><div class="autores">${escapar(r.autores)} · posiciones ${r.posiciones.join(', ')}</div>` +
        r.fragmentos.map(x => `<div class="fragmento">…${escapar(x.antes)}<mark>${escapar(x.coincidencia)}</mark>${escapar(x.despues)}…</div>`).join('') + '</div>';
    }
    salida.innerHTML = html;
  } catch (e) {
    salida.innerHTML = `<p class="error">${escapar(e.message)}</p>`;
  }
}

cargarArticulos();
