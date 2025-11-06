// js/reportes.js

import { requireAuth } from './auth.js';
import { mountSidebar } from './ui.js';

const u = requireAuth(['ADMIN']);
if (u) {
  document.addEventListener('DOMContentLoaded', () => {

    // aseguramos sidebar activo en "reportes"
    mountSidebar('reportes');

    const from     = document.getElementById('from');
    const to       = document.getElementById('to');
    const salesTbd = document.getElementById('salesBody');
    const cashTbd  = document.getElementById('cashBody');

    const fmtCLP = v => `$${Number(v || 0).toLocaleString('es-CL')}`;

    // === HELPERS PARA TRAER DATA DEL SDK O LOCAL ===

    async function getSalesRange(fISO, tISO) {
      // 1) Intentar SDK.Sales.listRange
      if (window.SDK?.Sales?.listRange) {
        try {
          const out = await window.SDK.Sales.listRange({ from: fISO, to: tISO });
          return (out || []).map(s => ({
            id: s.id,
            ts: s.ts
              ? new Date(s.ts).getTime()
              : (s.date ? new Date(s.date).getTime() : Date.now()),
            user:   s.user || s.username || '-',
            method: s.method || s.paymentMethod || '-',
            total:  Number(s.total || 0)
          }));
        } catch (err) {
          console.warn('SDK.Sales.listRange error:', err);
        }
      }

      // 2) Fallback localStorage
      try {
        const all = JSON.parse(localStorage.getItem('pos_sales') || '[]');
        const f = fISO ? new Date(fISO) : null;
        const t = tISO ? new Date(tISO) : null;
        return all.filter(s => {
          const d = new Date(s.ts || s.date || Date.now());
          if (f && d < new Date(f.getFullYear(), f.getMonth(), f.getDate())) return false;
          if (t && d > new Date(t.getFullYear(), t.getMonth(), t.getDate() + 1)) return false;
          return true;
        }).map(s => ({
          id: s.id,
          ts: Number(s.ts || new Date(s.date).getTime()),
          user: s.user || '-',
          method: s.method || '-',
          total: Number(s.total || 0)
        }));
      } catch {
        return [];
      }
    }

    async function getCashRange(fISO, tISO) {
      // 1) Intentar SDK.Cash.listRange
      if (window.SDK?.Cash?.listRange) {
        try {
          const out = await window.SDK.Cash.listRange({ from: fISO, to: tISO });
          return (out || []).map(evt => ({
            type: evt.type || '-',                   // 'open' | 'close'
            ts:   evt.ts ? new Date(evt.ts).getTime() : Date.now(),
            user: evt.user || '-',
            amount: Number(evt.amount || 0)
          }));
        } catch (err) {
          console.warn('SDK.Cash.listRange error:', err);
        }
      }

      // 2) Fallback localStorage
      try {
        const all = JSON.parse(localStorage.getItem('pos_cash') || '[]');
        const f = fISO ? new Date(fISO) : null;
        const t = tISO ? new Date(tISO) : null;
        return all.filter(evt => {
          const d = new Date(evt.ts || Date.now());
          if (f && d < new Date(f.getFullYear(), f.getMonth(), f.getDate())) return false;
          if (t && d > new Date(t.getFullYear(), t.getMonth(), t.getDate() + 1)) return false;
          return true;
        }).map(evt => ({
          type: evt.type || '-',
          ts: Number(evt.ts || Date.now()),
          user: evt.user || '-',
          amount: Number(evt.amount || 0)
        }));
      } catch {
        return [];
      }
    }

    // === RENDER PANTALLA ===

    async function render() {
      const fISO = from.value || null;
      const tISO = to.value   || null;

      // Ventas
      const sales = (await getSalesRange(fISO, tISO))
        .sort((a, b) => b.ts - a.ts);

      salesTbd.innerHTML = '';
      sales.forEach(s => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>${s.id}</td>
          <td>${new Date(s.ts).toLocaleString('es-CL')}</td>
          <td>${s.user}</td>
          <td>${s.method}</td>
          <td class="text-end">${fmtCLP(s.total)}</td>
        `;
        salesTbd.appendChild(tr);
      });

      const total = sales.reduce((acc, s) => acc + Number(s.total || 0), 0);
      const count = sales.length;
      document.getElementById('rTotal').textContent = fmtCLP(total);
      document.getElementById('rCount').textContent = String(count);
      document.getElementById('rAvg').textContent   = fmtCLP(count ? total / count : 0);
      document.getElementById('rLast').textContent  =
        sales[0] ? new Date(sales[0].ts).toLocaleString('es-CL') : '-';

      // Caja
      const cashLogs = (await getCashRange(fISO, tISO))
        .sort((a,b) => b.ts - a.ts);

      cashTbd.innerHTML = '';
      if (!cashLogs.length) {
        const tr = document.createElement('tr');
        tr.innerHTML = `<td colspan="4" class="text-muted">Sin movimientos en este rango</td>`;
        cashTbd.appendChild(tr);
      } else {
        cashLogs.forEach(evt => {
          const label = evt.type === 'open'  ? 'APERTURA' :
                        evt.type === 'close' ? 'CIERRE'   : evt.type;
          const tr = document.createElement('tr');
          tr.innerHTML = `
            <td>${label}</td>
            <td>${new Date(evt.ts).toLocaleString('es-CL')}</td>
            <td>${evt.user}</td>
            <td class="text-end">${fmtCLP(evt.amount)}</td>
          `;
          cashTbd.appendChild(tr);
        });
      }
    }

    // === Eventos ===
    document.getElementById('btnApply').addEventListener('click', render);

    document.getElementById('btnExport').addEventListener('click', async () => {
      // export solo ventas por ahora
      const rows = await getSalesRange(from.value || null, to.value || null);
      const csv = [
        ['id','fecha','usuario','metodo','total'].join(','),
        ...rows.map(s => [
          s.id,
          new Date(s.ts).toISOString(),
          s.user,
          s.method || '',
          s.total
        ].join(','))
      ].join('\n');

      const blob = new Blob([csv], {type:'text/csv;charset=utf-8;'});
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'ventas.csv';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    });

    // === Set rango inicial (mes actual) y render ===
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1);
    from.value = start.toISOString().slice(0,10);
    to.value   = now.toISOString().slice(0,10);

    render();
  });
}
