// =========================
//  DEMO POS - app.js
// =========================

// --------- AUTH ----------
function doLogin(e){
  e.preventDefault();
  const u = document.getElementById('user').value.trim();
  const p = document.getElementById('pass').value.trim();

  const valido =
    (u === 'admin'  && p === 'admin123') ||
    (u === 'cajero' && p === 'cajero123');

  if (!valido){
    const alertBox = document.getElementById('loginError');
    if (alertBox) alertBox.classList.remove('d-none');
    return false;
  }

  const rol = (u === 'admin') ? 'ADMIN' : 'CAJERO';
  localStorage.setItem('pos_user', JSON.stringify({ u, rol, ts: Date.now() }));

  // Redirección por rol:
  window.location.href = (rol === 'ADMIN') ? 'admin.html' : 'cajero.html';
  return false;
}

function getUser(){
  try { return JSON.parse(localStorage.getItem('pos_user') || 'null'); }
  catch { return null; }
}

function logout(){
  localStorage.removeItem('pos_user');
  // Vuelve al login para que la navegación quede clara en la demo
  window.location.href = 'login.html';
}

// (Opcional) Guardias para proteger rutas
function guardAdmin(){
  const u = getUser();
  if (!u || u.rol !== 'ADMIN') {
    alert('Acceso solo para ADMIN');
    window.location.href = 'login.html';
    throw new Error('Solo ADMIN');
  }
}
function guardCajeroOrAdmin(){
  const u = getUser();
  if (!u || (u.rol !== 'CAJERO' && u.rol !== 'ADMIN')) {
    alert('Acceso permitido a CAJERO o ADMIN');
    window.location.href = 'login.html';
    throw new Error('No autorizado');
  }
}

// --------- DATOS MOCK ----------
const productos = [
  {id:1, sku:'P001', nombre:'Pan marraqueta',  precio:1200},
  {id:2, sku:'P002', nombre:'Leche 1L',        precio:1100},
  {id:3, sku:'P003', nombre:'Harina 1kg',      precio:1500},
  {id:4, sku:'P004', nombre:'Azúcar 1kg',      precio:1400},
  {id:5, sku:'P005', nombre:'Mantequilla 250g',precio:1800}
];

let carrito = []; // {id, nombre, precio, qty}

// --------- PANTALLA CAJERO (ventas) ----------
function initVentas(){
  // Permite CAJERO y ADMIN (admin puede probar el POS)
  let user = getUser();
  if (!user){ window.location.href = 'login.html'; return; }

  const ub = document.getElementById('userBadge');
  if (ub) ub.textContent = user.rol + ' · ' + user.u;

  renderLista(productos);
  renderCarrito();
}

function buscar(){
  const q = (document.getElementById('q')?.value || '').toLowerCase();
  const res = productos.filter(p =>
    p.nombre.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q)
  );
  renderLista(res);
}

function renderLista(list){
  const cont = document.getElementById('listaProductos');
  if (!cont) return;
  cont.innerHTML = '';
  list.forEach(p => {
    const li = document.createElement('div');
    li.className = 'list-group-item d-flex justify-content-between align-items-center';
    li.innerHTML = `
      <div><strong>${p.nombre}</strong><div class="small text-secondary">${p.sku}</div></div>
      <div class="text-end">
        <div class="fw-bold">$${p.precio}</div>
        <button class="btn btn-sm btn-info mt-1">Agregar</button>
      </div>`;
    li.querySelector('button').onclick = () => addItem(p);
    cont.appendChild(li);
  });
}

function addItem(p){
  const ex = carrito.find(i => i.id === p.id);
  if (ex){ ex.qty += 1; }
  else { carrito.push({ id:p.id, nombre:p.nombre, precio:p.precio, qty:1 }); }
  renderCarrito();
}

function delItem(id){
  carrito = carrito.filter(i => i.id !== id);
  renderCarrito();
}
function inc(id){
  const it = carrito.find(i => i.id === id);
  if (it){ it.qty++; renderCarrito(); }
}
function dec(id){
  const it = carrito.find(i => i.id === id);
  if (it && it.qty > 1){ it.qty--; renderCarrito(); }
}

function renderCarrito(){
  const c = document.getElementById('carrito');
  if (!c) return;
  if (carrito.length === 0){
    c.innerHTML = '<div class="text-secondary">Agrega productos…</div>';
  } else {
    c.innerHTML = carrito.map(i => `
      <div class="d-flex justify-content-between align-items-center py-1">
        <div class="me-2">${i.nombre} <span class="text-secondary">x${i.qty}</span></div>
        <div>
          <button class="btn btn-sm btn-outline-light" onclick="dec(${i.id})">-</button>
          <button class="btn btn-sm btn-outline-light" onclick="inc(${i.id})">+</button>
          <button class="btn btn-sm btn-outline-danger" onclick="delItem(${i.id})">x</button>
        </div>
      </div>`).join('');
  }
  calcularTotales();
}

function calcularTotales(){
  const sub = carrito.reduce((a,i)=> a + i.precio*i.qty, 0);
  const iva = Math.round(sub * 0.19);
  const tot = sub + iva;
  setText('subtotal', '$'+sub);
  setText('iva',      '$'+iva);
  setText('total',    '$'+tot);
}

function setText(id, val){
  const el = document.getElementById(id);
  if (el) el.textContent = val;
}

function cobrar(){
  if (carrito.length === 0) return alert('Carrito vacío');

  const sub = carrito.reduce((a,i)=> a + i.precio*i.qty, 0);
  const iva = Math.round(sub * 0.19);
  const tot = sub + iva;

  const venta = {
    id: Date.now(),
    fecha: new Date().toLocaleString(),
    items: carrito,
    sub, iva, tot
  };

  localStorage.setItem('pos_ticket', JSON.stringify(venta));
  carrito = [];
  window.location.href = 'ticket.html';
}

// --------- TICKET ----------
function renderTicket(){
  const v = JSON.parse(localStorage.getItem('pos_ticket') || 'null');
  if (!v){
    document.body.innerHTML = '<div class="ticket">No hay ticket.</div>';
    return;
  }
  setById('t-meta',  `Folio: ${v.id} · ${v.fecha}`);
  setHTML('t-items', v.items.map(i =>
    `<div class="d-flex justify-content-between">
      <span>${i.nombre} x${i.qty}</span><span>$${i.precio*i.qty}</span>
    </div>`).join('')
  );
  setById('t-sub',  '$'+v.sub);
  setById('t-iva',  '$'+v.iva);
  setById('t-total','$'+v.tot);
}

function setById(id, txt){
  const el = document.getElementById(id);
  if (el) el.textContent = txt;
}
function setHTML(id, html){
  const el = document.getElementById(id);
  if (el) el.innerHTML = html;
}

// --------- LANDING: CONTACTO (simulado) ----------
function enviarContacto(e){
  e.preventDefault();
  const ok = document.getElementById('cAlert');
  if (ok){
    ok.classList.remove('d-none');
    setTimeout(()=> ok.classList.add('d-none'), 3000);
  }
  return false;
}

// --------- (Opcional) UI condicional por rol en páginas unificadas ----------
function applyRoleUI(user){
  // Muestra/oculta elementos con data-role="admin"
  document.querySelectorAll('[data-role="admin"]').forEach(el=>{
    el.classList.toggle('d-none', user?.rol !== 'ADMIN');
  });
}
