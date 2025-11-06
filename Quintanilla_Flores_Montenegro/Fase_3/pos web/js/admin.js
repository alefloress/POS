// js/admin.js — KPIs y paneles del Dashboard
// Lee directamente del storage (compatible con SDK.mock que escribe pos_sales y pos_products)
const CLP = v => '$' + Number(v||0).toLocaleString('es-CL');

function loadJSON(key, def){
  try { const x = JSON.parse(localStorage.getItem(key)||'null'); return x ?? def; }
  catch { return def; }
}

function dateOnly(d){
  const x = new Date(d);
  return new Date(x.getFullYear(), x.getMonth(), x.getDate());
}

function daysFromNow(dateStr){
  const d = new Date(dateStr); if (isNaN(d)) return Infinity;
  const now = dateOnly(new Date());
  const dx = dateOnly(d);
  const ms = dx - now;
  return Math.round(ms / (1000*60*60*24));
}

function computeKPIs(){
  const sales = loadJSON('pos_sales', []);
  const today = dateOnly(new Date());
  const weekAgo = new Date(today); weekAgo.setDate(weekAgo.getDate()-7);
  const monthAgo = new Date(today); monthAgo.setDate(monthAgo.getDate()-30);

  const sumIn = (from)=> sales
    .filter(s => {
      const ts = s.ts ?? (s.date ? new Date(s.date).getTime() : Date.now());
      return new Date(ts) >= from;
    })
    .reduce((a,s)=> a + Number(s.total ?? s.subtotal ?? 0), 0);

  document.getElementById('kpiToday').textContent = CLP(sumIn(today));
  document.getElementById('kpiWeek').textContent  = CLP(sumIn(weekAgo));
  document.getElementById('kpiMonth').textContent = CLP(sumIn(monthAgo));

  const prods = loadJSON('pos_products', []);
  document.getElementById('kpiInventory').textContent = String(prods.length || 0);
}

function renderLowStock(){
  const settings = loadJSON('pos_settings', { lowStock:5, expiryDays:30 });
  document.getElementById('lowStockBadge').textContent = `≤ ${settings.lowStock}`;
  const prods = loadJSON('pos_products', []);
  const low = prods
    .filter(p => Number(p.stock||0) <= Number(settings.lowStock||5))
    .sort((a,b)=> Number(a.stock||0) - Number(b.stock||0))
    .slice(0, 20);

  const body = document.getElementById('lowBody');
  body.innerHTML = low.length ? '' : '<tr><td colspan="3" class="text-muted">Sin alertas</td></tr>';
  low.forEach(p=>{
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${p.id||'-'}</td>
      <td>${p.nombre||'-'}</td>
      <td class="text-end">${Number(p.stock||0)}</td>
    `;
    body.appendChild(tr);
  });
}

function renderExpiring(){
  const settings = loadJSON('pos_settings', { lowStock:5, expiryDays:30 });
  document.getElementById('expiryBadge').textContent = `≤ ${settings.expiryDays} días`;
  const prods = loadJSON('pos_products', []);
  const soon = prods
    .filter(p => p.vence)
    .map(p => ({...p, dleft: daysFromNow(p.vence)}))
    .filter(p => p.dleft <= Number(settings.expiryDays||30))
    .sort((a,b)=> a.dleft - b.dleft)
    .slice(0, 20);

  const body = document.getElementById('expBody');
  body.innerHTML = soon.length ? '' : '<tr><td colspan="3" class="text-muted">Sin alertas</td></tr>';
  soon.forEach(p=>{
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${p.id||'-'}</td>
      <td>${p.nombre||'-'}</td>
      <td>${p.vence||'-'}</td>
    `;
    body.appendChild(tr);
  });
}

function renderRecentSales(){
  const sales = loadJSON('pos_sales', []);
  const recent = sales
    .slice()
    .sort((a,b)=> (b.ts ?? 0) - (a.ts ?? 0))
    .slice(0, 10);

  const body = document.getElementById('recentBody');
  body.innerHTML = recent.length ? '' : '<tr><td colspan="5" class="text-muted">No hay ventas aún</td></tr>';
  recent.forEach(s=>{
    const when = s.ts ? new Date(s.ts) : (s.date ? new Date(s.date) : new Date());
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${s.id}</td>
      <td>${when.toLocaleString('es-CL')}</td>
      <td>${s.user||'-'}</td>
      <td>${s.method||'-'}</td>
      <td class="text-end">${CLP(s.total ?? s.subtotal ?? 0)}</td>
    `;
    body.appendChild(tr);
  });
}

function refreshAll(){
  computeKPIs();
  renderLowStock();
  renderExpiring();
  renderRecentSales();
}

document.addEventListener('DOMContentLoaded', ()=>{
  refreshAll();
  document.getElementById('btnRefresh')?.addEventListener('click', refreshAll);
});

