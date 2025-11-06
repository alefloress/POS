// js/pos.js
// POS compartido para ventas.html y cajero.html (usa window.SDK)
// Flujo: carrito -> pago.html -> ticket.html
// + soporte lector de código de barras (escáner USB actúa como teclado)

export function initPOS({ selectors, user }) {
  // Utilidades
  const $ = (sel) => document.querySelector(sel);
  const CLP = v => '$' + Number(v || 0).toLocaleString('es-CL');

  // Referencias DOM
  const qInput    = $(selectors.q);
  const listEl    = $(selectors.list);
  const cartEl    = $(selectors.cart);
  const subEl     = $(selectors.sub);
  const ivaEl     = $(selectors.iva);
  const totalEl   = $(selectors.total);
  const chargeBtn = $(selectors.chargeBtn);

  if (!qInput || !listEl || !cartEl || !subEl || !ivaEl || !totalEl || !chargeBtn) {
    console.error('initPOS: faltan selectores/elementos en el HTML', selectors);
    return;
  }

  // Estado
  let catalogo = []; // [{id, nombre, precio, stock,...}]
  let carrito  = []; // [{id, nombre, precio, qty}]

  // =========================
  //   Cargar catálogo
  // =========================
  async function cargarCatalogo() {
    try {
      if (window.SDK?.Products?.list) {
        catalogo = await window.SDK.Products.list();
      } else {
        // Fallback local
        const raw = localStorage.getItem('pos_products') || '[]';
        catalogo = JSON.parse(raw);
      }
      renderLista(catalogo);
    } catch (err) {
      console.error('Error al listar productos:', err);
      listEl.innerHTML = `<div class="text-danger small p-2">No se pudo cargar el catálogo.</div>`;
    }
  }

  // =========================
  //   Búsqueda / lista izquierda
  // =========================
  function renderLista(rows) {
    listEl.innerHTML = '';
    if (!rows.length) {
      listEl.innerHTML = `<div class="text-secondary">Sin resultados…</div>`;
      return;
    }

    rows.forEach(p => {
      const stockNum  = Number(p.stock || 0);
      const precioNum = Number(p.precio || p.price || 0);

      const div = document.createElement('div');
      div.className = 'product-card d-flex justify-content-between align-items-start';

      div.innerHTML = `
        <div>
          <div class="p-name">${p.nombre || p.name || '(sin nombre)'}</div>
          <div class="p-meta">${p.id} · Stock ${stockNum}</div>
        </div>
        <div class="text-end">
          <div class="p-price fw-bold">${CLP(precioNum)}</div>
          <button class="btn btn-info btn-sm p-add-btn">Agregar</button>
        </div>
      `;

      // botón "Agregar"
      div.querySelector('.p-add-btn').onclick = () => addItem(p);

      listEl.appendChild(div);
    });
  }

  function filtrar() {
    const q = (qInput.value || '').toLowerCase();
    const res = catalogo.filter(p =>
      (p.nombre || p.name || '').toLowerCase().includes(q) ||
      String(p.id || '').toLowerCase().includes(q)
    );
    renderLista(res);
  }

  // =========================
  //   LECTOR DE CÓDIGO DE BARRAS
  // =========================
  // Idea:
  // - El lector escribe "001" en qInput y manda Enter.
  // - Si coincide EXACTO con id del producto -> lo agregamos directo al carrito
  // - Si no coincide, usamos modo búsqueda normal.

  function intentarScanDirecto() {
    const code = (qInput.value || '').trim().toLowerCase();
    if (!code) return;

    // buscar match exacto por ID
    const prod = catalogo.find(p =>
      String(p.id || '').toLowerCase() === code
    );

    if (prod) {
      // si encontramos el producto, lo metemos al carrito altiro
      addItem(prod);
      // limpiar input para el próximo escaneo
      qInput.value = '';
      renderLista(catalogo); // volvemos a lista completa
    } else {
      // si no hay match directo, hacemos la búsqueda normal
      filtrar();
    }
  }

  // cuando el cajero presiona Enter (o el lector manda Enter)
  qInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      intentarScanDirecto();
    }
  });

  // también filtrar en vivo cuando escribe manualmente
  qInput.addEventListener('input', filtrar);

  // =========================
  //   Carrito
  // =========================
  function addItem(p) {
    const id = p.id;
    const precioNum = Number(p.precio || p.price || 0);

    const existe = carrito.find(i => i.id === id);
    if (existe) {
      existe.qty += 1;
    } else {
      carrito.push({
        id,
        nombre: p.nombre || p.name || '',
        precio: precioNum,
        qty: 1
      });
    }
    renderCarrito();
  }

  function inc(id) {
    const it = carrito.find(i => i.id === id);
    if (it) {
      it.qty += 1;
      renderCarrito();
    }
  }
  function dec(id) {
    const it = carrito.find(i => i.id === id);
    if (it) {
      it.qty -= 1;
      if (it.qty <= 0) {
        carrito = carrito.filter(x => x.id !== id);
      }
      renderCarrito();
    }
  }
  function delItem(id) {
    carrito = carrito.filter(i => i.id !== id);
    renderCarrito();
  }

  function calcTotales() {
    const subtotal = carrito.reduce((a, i) => a + (i.precio * i.qty), 0);
    const iva      = Math.round(subtotal * 0.19);
    const total    = subtotal + iva;
    return { subtotal, iva, total };
  }

  function renderCarrito() {
    if (carrito.length === 0) {
      cartEl.innerHTML = '<div class="text-secondary">Escanea un código o agrega desde la izquierda…</div>';
    } else {
      cartEl.innerHTML = carrito.map(i => `
        <div class="cart-item">
          <div class="ci-left">
            <div class="ci-name">${i.nombre}</div>
            <div class="ci-sub">
              <span>x${i.qty} · ${CLP(i.precio)}</span>
              <span class="qty-controls">
                <button class="btn btn-outline-light btn-sm" data-act="dec" data-id="${i.id}">-</button>
                <button class="btn btn-outline-light btn-sm" data-act="inc" data-id="${i.id}">+</button>
                <button class="btn btn-outline-danger btn-sm" data-act="del" data-id="${i.id}">x</button>
              </span>
            </div>
          </div>
          <div class="ci-price">${CLP(i.precio * i.qty)}</div>
        </div>
      `).join('');

      // enganchar botones + - x
      cartEl.querySelectorAll('button').forEach(b => {
        const id  = b.getAttribute('data-id');
        const act = b.getAttribute('data-act');
        b.onclick = () => {
          if (act === 'inc') inc(id);
          else if (act === 'dec') dec(id);
          else if (act === 'del') delItem(id);
        };
      });
    }

    const { subtotal, iva, total } = calcTotales();
    subEl.textContent   = CLP(subtotal);
    ivaEl.textContent   = CLP(iva);
    totalEl.textContent = CLP(total);
  }

  // =========================
  //   Cobrar -> pago.html
  // =========================
  function cobrar() {
    if (carrito.length === 0) {
      alert('Carrito vacío');
      return;
    }

    const { subtotal, iva, total } = calcTotales();

    const checkoutPayload = {
      items: carrito.map(i => ({
        id: i.id,
        nombre: i.nombre,
        precio: i.precio,
        qty: i.qty
      })),
      subtotal,
      iva,
      total,
      ts: Date.now(),
      user: user?.u || 'desconocido'
    };

    sessionStorage.setItem('pos_checkout', JSON.stringify(checkoutPayload));

    window.location.href = 'pago.html';
  }

  // botón Cobrar
  chargeBtn.addEventListener('click', cobrar);

  // Init
  cargarCatalogo();
  renderCarrito();
}
