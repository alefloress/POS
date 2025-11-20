// js/pago.js
// Pantalla de pago: usa el checkout de sessionStorage y SDK.Sales

import { requireAuth } from './auth.js';

const u = requireAuth(['ADMIN', 'CASHIER']);
if (!u) {
  throw new Error('Sin sesión');
}

const CLP = (v) => '$' + Number(v || 0).toLocaleString('es-CL');

// Leer checkout guardado por pos.js
const raw = sessionStorage.getItem('pos_checkout');
if (!raw) {
  alert('No hay una venta en curso.');
  window.location.href = 'ventas.html';
}

const checkout = JSON.parse(raw || '{}');
if (!checkout.items || !checkout.items.length) {
  alert('El carrito está vacío.');
  window.location.href = 'ventas.html';
}

// Referencias DOM
const listaResumen  = document.getElementById('listaResumen');
const lblSub        = document.getElementById('p_subtotal');
const lblIva        = document.getElementById('p_iva');
const lblTotal      = document.getElementById('p_total');
const selMetodo     = document.getElementById('metodo');
const inpEntregado  = document.getElementById('entregado');
const inpVuelto     = document.getElementById('vuelto');
const btnConfirm    = document.getElementById('btnConfirmar');
const btnVolver     = document.getElementById('btnVolver');

// Pintar resumen
function renderResumen() {
  listaResumen.innerHTML = checkout.items
    .map(i => `
      <div class="d-flex justify-content-between">
        <span>${i.nombre} x${i.qty}</span>
        <span>${CLP(i.precio * i.qty)}</span>
      </div>
    `)
    .join('');

  lblSub.textContent   = CLP(checkout.subtotal);
  lblIva.textContent   = CLP(checkout.iva);
  lblTotal.textContent = CLP(checkout.total);
}

// Calcular vuelto
function recalcularVuelto() {
  const entregado = Number(inpEntregado.value || 0);
  const total     = Number(checkout.total || 0);
  const vuelto    = Math.max(0, entregado - total);
  inpVuelto.value = vuelto;
}

inpEntregado.addEventListener('input', recalcularVuelto);

// Volver
btnVolver.addEventListener('click', (e) => {
  e.preventDefault();
  window.history.back();
});

// Confirmar pago
btnConfirm.addEventListener('click', async () => {
  const total     = Number(checkout.total || 0);
  const entregado = Number(inpEntregado.value || 0);

  if (entregado < total && selMetodo.value === 'Efectivo') {
    alert('El monto entregado es menor al total.');
    return;
  }

  if (!window.SDK || !window.SDK.Sales || !window.SDK.Sales.create) {
    alert('SDK de ventas no disponible.');
    return;
  }

  const method = selMetodo.value || 'Efectivo';
  const change = Math.max(0, entregado - total);

  try {
    // 1) Verificar caja activa
    const cur = await window.SDK.Cash.current();
    if (!cur || cur.status !== 'OPEN') {
      alert('No hay caja abierta.');
      return;
    }

    // 2) Crear venta
    const sale = await window.SDK.Sales.create({
      sessionId: cur.id,
      method,
      items: checkout.items,
      // si en el futuro quieres, aquí puedes pasar customerName / customerTaxId
    });

    // 3) Guardar info para ticket.html
    checkout.saleId    = sale?.id ?? null;
    checkout.method    = method;
    checkout.cashGiven = entregado;
    checkout.change    = change;
    checkout.user      = u.u;

    sessionStorage.setItem('pos_checkout', JSON.stringify(checkout));
    window.location.href = 'ticket.html';
  } catch (err) {
    console.error('Error al registrar la venta:', err);
    alert(err?.message || 'No se pudo registrar la venta. Revisa la consola.');
  }
});

// Init
renderResumen();
recalcularVuelto();
