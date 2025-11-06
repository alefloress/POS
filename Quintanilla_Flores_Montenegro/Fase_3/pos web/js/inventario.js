import { requireAuth } from './auth.js';
import { mountSidebar } from './ui.js';

const u = requireAuth(['ADMIN']);
if (u) {
  document.addEventListener('DOMContentLoaded', () => {
    mountSidebar('inventario');

    // ─── Data layer (SDK + fallback localStorage) ────────────────
    const KEY  = 'pos_products';
    const seed = [
      { id:'P-0001', nombre:'Producto Demo', stock:10, precio:1000, vence:'2025-12-31' }
    ];

    const Local = {
      _list: [],
      load() {
        try {
          this._list = JSON.parse(localStorage.getItem(KEY) || 'null') || seed;
          this.save();
        } catch {
          this._list = seed;
          this.save();
        }
      },
      save() {
        localStorage.setItem(KEY, JSON.stringify(this._list));
      },
      all() {
        return [...this._list];
      },
      upsert(p) {
        const i = this._list.findIndex(x => x.id === p.id);
        if (i >= 0) this._list[i] = p;
        else this._list.push(p);
        this.save();
      },
      remove(id) {
        this._list = this._list.filter(x => x.id !== id);
        this.save();
      }
    };
    Local.load();

    const Products = {
      async list() {
        if (window.SDK?.Products?.list) {
          try { return await window.SDK.Products.list(); }
          catch (e) { console.warn('[SDK.Products.list] falló, uso local', e); }
        }
        return Local.all();
      },
      async create(p) {
        if (window.SDK?.Products?.create) {
          try { return await window.SDK.Products.create(p); }
          catch (e) { console.warn('[SDK.Products.create] falló, uso local', e); }
        }
        Local.upsert(p);
        return p;
      },
      async update(id, p) {
        if (window.SDK?.Products?.update) {
          try { return await window.SDK.Products.update(id, p); }
          catch (e) { console.warn('[SDK.Products.update] falló, uso local', e); }
        }
        Local.upsert({ ...p, id });
        return p;
      },
      async remove(id) {
        if (window.SDK?.Products?.remove) {
          try { return await window.SDK.Products.remove(id); }
          catch (e) { console.warn('[SDK.Products.remove] falló, uso local', e); }
        }
        Local.remove(id);
        return true;
      }
    };

    // ─── DOM refs ────────────────────────────────────────────────
    const tbody        = document.querySelector('tbody');
    const search       = document.getElementById('search');

    const modalBackdrop= document.getElementById('modalBackdrop');
    const modalTitle   = document.getElementById('modalTitle');
    const modalClose   = document.getElementById('modalClose');
    const btnNew       = document.getElementById('btnNew');
    const btnSave      = document.getElementById('btnSave');
    const formError    = document.getElementById('formError');

    const f_id     = document.getElementById('f_id');
    const f_nombre = document.getElementById('f_nombre');
    const f_stock  = document.getElementById('f_stock');
    const f_precio = document.getElementById('f_precio');
    const f_vence  = document.getElementById('f_vence');

    let mode = 'new';    // 'new' | 'edit'
    let editing = null;  // producto que estoy editando
    let cache = [];      // lista actual en memoria

    // ─── Carga inicial ───────────────────────────────────────────
    async function load() {
      cache = await Products.list();
      renderTable();
    }

    // ─── Render tabla ────────────────────────────────────────────
    function renderTable() {
      const q = (search.value || '').toLowerCase();
      const rows = (cache || [])
        .map(p => ({
          id:     p.id,
          nombre: p.nombre || p.name || '',
          stock:  Number(p.stock || 0),
          precio: Number(p.precio || p.price || 0),
          vence:  p.vence || p.expiryDate || ''
        }))
        .sort((a,b) => a.nombre.localeCompare(b.nombre, 'es'))
        .filter(p =>
          (p.id || '').toLowerCase().includes(q) ||
          (p.nombre || '').toLowerCase().includes(q)
        );

      tbody.innerHTML = '';
      rows.forEach(p => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>${p.id}</td>
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

    // ─── Listeners por fila (editar / eliminar) ──────────────────
    function attachRowHandlers() {
      tbody.querySelectorAll('.btn-edit').forEach(btn => {
        btn.addEventListener('click', () => {
          const p = cache.find(x => x.id === btn.dataset.id);
          openModal('edit', p);
        });
      });

      tbody.querySelectorAll('.btn-del').forEach(btn => {
        btn.addEventListener('click', async () => {
          const id = btn.dataset.id;
          if (confirm(`¿Eliminar producto ${id}?`)) {
            await Products.remove(id);
            await load();
          }
        });
      });
    }

    // ─── Modal logic ─────────────────────────────────────────────
    function openModal(m, p = null) {
      mode = m;
      editing = p;
      formError.classList.add('d-none');
      formError.textContent = '';

      modalTitle.textContent = (m === 'new')
        ? 'Nuevo producto'
        : `Editar producto: ${p.id}`;

      if (m === 'new') {
        f_id.disabled      = false;
        f_id.value         = '';
        f_nombre.value     = '';
        f_stock.value      = '0';
        f_precio.value     = '0';
        f_vence.value      = '';
      } else {
        f_id.disabled      = true;
        f_id.value         = p.id;
        f_nombre.value     = p.nombre || p.name || '';
        f_stock.value      = String(p.stock || 0);
        f_precio.value     = String(p.precio || p.price || 0);
        f_vence.value      = p.vence || p.expiryDate || '';
      }

      // mostrar modal
      modalBackdrop.classList.remove('pos-modal-hidden');
      f_nombre.focus();
    }

    function closeModal() {
      modalBackdrop.classList.add('pos-modal-hidden');
    }

    // validación del form
    function validate() {
      const id     = f_id.value.trim();
      const nombre = f_nombre.value.trim();
      const stock  = Number(f_stock.value);
      const precio = Number(f_precio.value);

      if (!id || !nombre) return 'Código y nombre son obligatorios.';
      if (stock < 0 || !Number.isFinite(stock)) return 'Stock debe ser un número ≥ 0.';
      if (precio < 0 || !Number.isFinite(precio)) return 'Precio debe ser un número ≥ 0.';
      if (mode === 'new' && cache.some(x => x.id === id)) return 'Ya existe un producto con ese código.';

      if (f_vence.value) {
        const d = new Date(f_vence.value);
        if (isNaN(d)) return 'Fecha de vencimiento no válida.';
      }
      return '';
    }

    // ─── Botones globales ────────────────────────────────────────
    btnNew.addEventListener('click', () => openModal('new'));

    modalClose.addEventListener('click', closeModal);

    // cerrar modal si clickeas el fondo oscuro
    modalBackdrop.addEventListener('click', (e) => {
      if (e.target === modalBackdrop) {
        closeModal();
      }
    });

    btnSave.addEventListener('click', async () => {
      const err = validate();
      if (err) {
        formError.textContent = err;
        formError.classList.remove('d-none');
        return;
      }

      const dto = {
        id:     f_id.value.trim(),
        nombre: f_nombre.value.trim(),
        stock:  Number(f_stock.value),
        precio: Number(f_precio.value),
        vence:  f_vence.value || null
      };

      if (mode === 'new') {
        await Products.create(dto);
      } else {
        await Products.update(editing.id, dto);
      }

      closeModal();
      await load();
    });

    // export CSV
    document.getElementById('btnExport').addEventListener('click', () => {
      const rows = cache || [];
      const csv = [
        ['id','nombre','stock','precio','vence'].join(','),
        ...rows.map(p => [
          p.id,
          JSON.stringify(p.nombre || p.name || ''),
          Number(p.stock || 0),
          Number(p.precio || p.price || 0),
          p.vence || p.expiryDate || ''
        ].join(','))
      ].join('\n');

      const blob = new Blob([csv], { type:'text/csv;charset=utf-8;' });
      const url  = URL.createObjectURL(blob);
      const a    = document.createElement('a');
      a.href     = url;
      a.download = 'productos.csv';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    });

    // filtro en vivo
    search.addEventListener('input', renderTable);

    // GO
    load();
  });
}

