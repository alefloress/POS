import { requireAuth } from './auth.js';
import { mountSidebar } from './ui.js';

// Solo ADMIN
const u = requireAuth(['ADMIN']);
if (!u) {
  // requireAuth ya redirige
  throw new Error('No autenticado');
}

document.addEventListener('DOMContentLoaded', () => {
  mountSidebar('inventario');

  // ─────────────────────────────────────────────
  //  DATA LAYER: sólo API real (window.SDK.Products)
  // ─────────────────────────────────────────────
  if (!window.SDK || !window.SDK.Products) {
    console.error('SDK.Products no está definido. ¿Cargaste js/sdk.js en el HTML?');
  }
  const Products = window.SDK.Products;

  // ─────────────────────────────────────────────
  //  DOM refs
  // ─────────────────────────────────────────────
  const tbody         = document.querySelector('tbody');
  const search        = document.getElementById('search');

  const modalBackdrop = document.getElementById('modalBackdrop');
  const modalTitle    = document.getElementById('modalTitle');
  const modalClose    = document.getElementById('modalClose');
  const btnNew        = document.getElementById('btnNew');
  const btnSave       = document.getElementById('btnSave');
  const formError     = document.getElementById('formError');

  const f_id     = document.getElementById('f_id');      // Código (code)
  const f_nombre = document.getElementById('f_nombre');
  const f_stock  = document.getElementById('f_stock');
  const f_precio = document.getElementById('f_precio');
  const f_vence  = document.getElementById('f_vence');

  let mode    = 'new';   // 'new' | 'edit'
  let editing = null;    // producto completo que se está editando (incluye id numérico)
  let cache   = [];      // productos actuales

  // ─────────────────────────────────────────────
  //  Carga inicial
  // ─────────────────────────────────────────────
  async function load() {
    try {
      cache = await Products.list();   // viene de la BD
    } catch (err) {
      console.error('Error al cargar productos desde la API', err);
      cache = [];
    }
    renderTable();
  }

  // ─────────────────────────────────────────────
  //  Render tabla
  // ─────────────────────────────────────────────
  function renderTable() {
    const q = (search.value || '').toLowerCase();

    const rows = (cache || [])
      .map(p => ({
        id:        p.id,                        // id numérico BD
        code:      p.code || '',               // código visible
        nombre:    p.nombre || p.name || '',
        stock:     Number(p.stock ?? 0),
        precio:    Number(p.precio ?? p.sale_price ?? 0),
        vence:     p.vence ?? p.expiry_date ?? ''
      }))
      .filter(p =>
        p.code.toLowerCase().includes(q) ||
        p.nombre.toLowerCase().includes(q)
      )
      .sort((a, b) => a.code.localeCompare(b.code, 'es'));

    tbody.innerHTML = '';

    if (!rows.length) {
      tbody.innerHTML = `
        <tr><td colspan="5" class="text-center text-secondary">
          No hay productos para esta tienda
        </td></tr>`;
      return;
    }

    rows.forEach(p => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${p.code}</td>
        <td>${p.nombre}</td>
        <td class="text-end">${p.stock}</td>
        <td class="text-end">$${p.precio.toLocaleString('es-CL')}</td>
        <td>${p.vence || ''}</td>
        <td class="actions">
          <button data-id="${p.id}" class="btn btn-sm btn-outline-dark btn-edit">Editar</button>
          <button data-id="${p.id}" class="btn btn-sm btn-danger btn-del">Eliminar</button>
        </td>
      `;
      tbody.appendChild(tr);
    });

    attachRowHandlers();
  }

  // ─────────────────────────────────────────────
  //  Handlers de fila (editar / eliminar)
  // ─────────────────────────────────────────────
  function attachRowHandlers() {
    tbody.querySelectorAll('.btn-edit').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = Number(btn.dataset.id);
        const p  = cache.find(x => Number(x.id) === id);
        if (p) openModal('edit', p);
      });
    });

    tbody.querySelectorAll('.btn-del').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = Number(btn.dataset.id);
        const p  = cache.find(x => Number(x.id) === id);
        if (!p) return;

        if (!confirm(`¿Eliminar producto ${p.code || id}?`)) return;

        try {
          await Products.remove(id);
          await load();
        } catch (err) {
          console.error('Error al eliminar producto', err);
          alert('No se pudo eliminar el producto.');
        }
      });
    });
  }

  // ─────────────────────────────────────────────
  //  Modal
  // ─────────────────────────────────────────────
  function openModal(m, p = null) {
    mode    = m;
    editing = p;
    formError.classList.add('d-none');
    formError.textContent = '';

    modalTitle.textContent = (m === 'new')
      ? 'Nuevo producto'
      : `Editar producto: ${p.code || p.id}`;

    if (m === 'new') {
      f_id.disabled  = false;
      f_id.value     = '';
      f_nombre.value = '';
      f_stock.value  = '0';
      f_precio.value = '0';
      f_vence.value  = '';
    } else {
      f_id.disabled  = false;              // también permitimos cambiar código
      f_id.value     = p.code || '';
      f_nombre.value = p.nombre || p.name || '';
      f_stock.value  = String(p.stock ?? 0);
      f_precio.value = String(p.precio ?? p.sale_price ?? 0);
      f_vence.value  = p.vence ?? p.expiry_date ?? '';
    }

    modalBackdrop.classList.remove('pos-modal-hidden');
    f_nombre.focus();
  }

  function closeModal() {
    modalBackdrop.classList.add('pos-modal-hidden');
  }

  // Validación básica
  function validate() {
    const code   = f_id.value.trim();
    const nombre = f_nombre.value.trim();
    const stock  = Number(f_stock.value);
    const precio = Number(f_precio.value);

    if (!code || !nombre) return 'Código y nombre son obligatorios.';
    if (!Number.isFinite(stock)  || stock  < 0) return 'Stock debe ser un número ≥ 0.';
    if (!Number.isFinite(precio) || precio < 0) return 'Precio debe ser un número ≥ 0.';

    // Unicidad por código
    const codeLower = code.toLowerCase();
    if (mode === 'new' &&
        cache.some(x => (x.code || '').toLowerCase() === codeLower)) {
      return 'Ya existe un producto con ese código.';
    }

    if (f_vence.value) {
      const d = new Date(f_vence.value);
      if (isNaN(d)) return 'Fecha de vencimiento no válida.';
    }
    return '';
  }

  // ─────────────────────────────────────────────
  //  Botones globales
  // ─────────────────────────────────────────────
  btnNew.addEventListener('click', () => openModal('new'));
  modalClose.addEventListener('click', closeModal);

  // Cerrar modal clickeando el fondo oscuro
  modalBackdrop.addEventListener('click', (e) => {
    if (e.target === modalBackdrop) closeModal();
  });

  btnSave.addEventListener('click', async () => {
    const err = validate();
    if (err) {
      formError.textContent = err;
      formError.classList.remove('d-none');
      return;
    }

    const dto = {
      code:   f_id.value.trim(),
      nombre: f_nombre.value.trim(),
      stock:  Number(f_stock.value),
      precio: Number(f_precio.value),
      vence:  f_vence.value || null
    };

    try {
      if (mode === 'new') {
        await Products.create(dto);
      } else if (editing) {
        await Products.update(editing.id, dto);
      }
      closeModal();
      await load();
    } catch (err) {
      console.error('Error al guardar producto', err);
      formError.textContent = 'No se pudo guardar el producto.';
      formError.classList.remove('d-none');
    }
  });

  // Exportar CSV (desde cache)
  document.getElementById('btnExport').addEventListener('click', () => {
    const rows = cache || [];
    const csv = [
      ['code','nombre','stock','precio','vence'].join(','),
      ...rows.map(p => [
        p.code || '',
        JSON.stringify(p.nombre || p.name || ''),
        Number(p.stock ?? 0),
        Number(p.precio ?? p.sale_price ?? 0),
        p.vence ?? p.expiry_date ?? ''
      ].join(','))
    ].join('\n');

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = 'productos.csv';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  });

  // Filtro en vivo
  search.addEventListener('input', renderTable);

  // GO
  load();
});
