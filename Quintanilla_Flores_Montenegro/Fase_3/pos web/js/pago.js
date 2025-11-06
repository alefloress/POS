// js/pago.js

// Utilidad CLP
const CLP = v => '$' + Number(v || 0).toLocaleString('es-CL');

// Leo payload temporal del checkout
let checkout;
try {
  checkout = JSON.parse(sessionStorage.getItem('pos_checkout') || 'null');
} catch {
  checkout = null;
}

// refs DOM
const listaResumen = document.getElementById('listaResumen');
const p_subtotal   = document.getElementById('p_subtotal');
const p_iva        = document.getElementById('p_iva');
const p_total      = document.getElementById('p_total');

const metodoSel    = document.getElementById('metodo');
const entregadoInp = document.getElementById('entregado');
const vueltoInp    = document.getElementById('vuelto');

const btnConfirmar = document.getElementById('btnConfirmar');
const btnVolver    = document.getElementById('btnVolver');

// Para volver al módulo correcto según el rol guardado
function getUser() {
  try {
    return JSON.parse(localStorage.getItem('pos_user') || 'null');
  } catch {
    return null;
  }
}

// Render inicial; si no hay checkout, mostramos error
function renderResumen() {
  if (!checkout) {
    listaResumen.innerHTML = `<div class="text-danger">No hay datos de compra.</div>`;
    p_subtotal.textContent = '$0';
    p_iva.textContent      = '$0';
    p_total.textContent    = '$0';
    return;
  }

  // detalle items
  listaResumen.innerHTML = checkout.items.map(it => `
    <div class="d-flex justify-content-between border-bottom border-secondary py-1">
      <div class="me-2">
        <div class="text-white fw-semibold">${it.nombre}</div>
        <div class="text-secondary small">x${it.qty} · ${CLP(it.precio)}</div>
      </div>
      <div class="text-end text-white small fw-bold">${CLP(it.precio * it.qty)}</div>
    </div>
  `).join('');

  p_subtotal.textContent = CLP(checkout.subtotal);
  p_iva.textContent      = CLP(checkout.iva);
  p_total.textContent    = CLP(checkout.total);
}

// Calcula y muestra vuelto
function recalcVuelto() {
  const metodo = metodoSel.value;
  const totalPagar = Number(checkout?.total || 0);

  // si no es efectivo, el vuelto es 0 y el campo se bloquea visualmente
  if (metodo !== 'Efectivo') {
    entregadoInp.value = totalPagar.toString();
    vueltoInp.value = '0';
    entregadoInp.disabled = true;
    return;
  }

  // efectivo:
  entregadoInp.disabled = false;

  const entregadoNum = Number(entregadoInp.value || 0);
  const cambio = Math.max(entregadoNum - totalPagar, 0);
  vueltoInp.value = cambio.toString();
}

// Confirmar el pago = crear la venta "real"
async function confirmarPago() {
  if (!checkout) {
    alert('No hay datos de compra.');
    return;
  }

  const metodo = metodoSel.value;
  const totalPagar = Number(checkout.total || 0);
  const entregadoNum = (metodo === 'Efectivo')
    ? Number(entregadoInp.value || 0)
    : totalPagar;

  if (metodo === 'Efectivo' && entregadoNum < totalPagar) {
    alert('El monto entregado es menor al total.');
    return;
  }

  const cambio = Math.max(entregadoNum - totalPagar, 0);

  // Construimos la venta que va a SDK
  const ventaPayload = {
    user: checkout.user || 'desconocido',
    method: metodo,
    cashGiven: entregadoNum,
    change: cambio,
    items: checkout.items // [{id,nombre,precio,qty}...]
  };

  try {
    // guardamos en SDK
    const venta = await window.SDK.Sales.create(ventaPayload);
    // venta trae { id, ts, total, ... }

    // guardo el último id para ticket
    sessionStorage.setItem('pos_last_sale', venta.id);

    // limpiamos el checkout temporal
    sessionStorage.removeItem('pos_checkout');

    // vamos al ticket
    window.location.href = 'ticket.html';
  } catch (err) {
    console.error('Error al confirmar pago:', err);
    alert('No se pudo registrar la venta.');
  }
}

// set up botón volver dinámico (cajero vs admin)
function wireVolver() {
  const u = getUser();
  const href = (u?.rol === 'CAJERO') ? 'cajero.html' : 'ventas.html';
  btnVolver.setAttribute('href', href);
}

// EVENTOS
metodoSel?.addEventListener('change', recalcVuelto);
entregadoInp?.addEventListener('input', recalcVuelto);
btnConfirmar?.addEventListener('click', confirmarPago);

// INIT
wireVolver();
renderResumen();
recalcVuelto();

