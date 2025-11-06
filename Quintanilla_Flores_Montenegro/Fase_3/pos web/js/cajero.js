// js/cajero.js
import { requireAuth } from './auth.js';
import { mountSidebar } from './ui.js';

const u = requireAuth(['CAJERO','ADMIN']);
if(u){
  document.addEventListener('DOMContentLoaded', () => {
    mountSidebar('ventas');

    const catalogo = (()=>{ try{ return JSON.parse(localStorage.getItem('pos_products')||'[]'); }catch{ return []; } })();
    const sel = document.getElementById('product');
    catalogo.forEach(p=>{
      const o = document.createElement('option');
      o.value = p.id;
      o.textContent = `${p.id} · ${p.nombre} · $${(p.precio||0).toLocaleString('es-CL')}`;
      sel.appendChild(o);
    });

    const tbody = document.querySelector('tbody');
    const totalEl = document.getElementById('total');
    const methodSel = document.getElementById('payMethod');
    let total = 0;

    function addLine(item, qty){
      const tr = document.createElement('tr');
      const monto = (item.precio||0) * qty;
      total += monto;
      tr.innerHTML = `<td>${item.id}</td><td>${item.nombre}</td><td>${qty}</td><td>$${monto.toLocaleString('es-CL')}</td>`;
      tr.style.cursor='pointer'; tr.title='Click para eliminar línea';
      tr.addEventListener('click', ()=>{
        total -= monto; tr.remove();
        totalEl.textContent = `$${total.toLocaleString('es-CL')}`;
      });
      tbody.appendChild(tr);
      totalEl.textContent = `$${total.toLocaleString('es-CL')}`;
    }

    document.getElementById('addBtn').addEventListener('click', ()=>{
      const id = sel.value;
      const qty = Math.max(1, parseInt(document.getElementById('qty').value||'1',10));
      const item = catalogo.find(x=>x.id===id);
      if(item) addLine(item, qty);
    });

    function saveSale(total, items, method){
      const KEY = 'pos_sales';
      try{
        const list = JSON.parse(localStorage.getItem(KEY)||'[]');
        list.push({ id:'S-'+Date.now(), ts: Date.now(), user: u.u, total, items, method });
        localStorage.setItem(KEY, JSON.stringify(list));
      }catch{}
    }

    document.getElementById('payBtn').addEventListener('click', ()=>{
      if(total <= 0){ alert('Agrega productos primero.'); return; }
      const items = Array.from(tbody.querySelectorAll('tr')).map(tr => {
        const t = tr.querySelectorAll('td');
        return { id: t[0].textContent, nombre: t[1].textContent, qty: Number(t[2].textContent), monto: Number(t[3].textContent.replace(/[^0-9]/g,'')) };
      });
      const method = methodSel.value || 'EFECTIVO';
      saveSale(total, items, method);
      alert(`Venta registrada por ${u.u}. Total: ${total.toLocaleString('es-CL')} (${method})`);
      tbody.innerHTML = ''; total = 0; totalEl.textContent = '$0';
    });

    // Imprimir boleta simple
    document.getElementById('printBtn').addEventListener('click', ()=>{
      const rows = Array.from(tbody.querySelectorAll('tr')).map(tr => {
        const tds = tr.querySelectorAll('td');
        return { id: tds[0]?.textContent||'', nombre: tds[1]?.textContent||'', qty: tds[2]?.textContent||'1', monto: tds[3]?.textContent||'$0' };
      });
      const win = window.open('', 'PRINT', 'height=600,width=420');
      const fecha = new Date().toLocaleString('es-CL');
      const totalTxt = document.getElementById('total').textContent;
      win.document.write(`<!doctype html><html><head><title>Boleta</title>
        <style>
          body{font-family:ui-monospace, SFMono-Regular, Menlo, monospace; padding:10px; font-size:13px}
          h3{margin:0 0 6px 0} table{width:100%; border-collapse:collapse}
          td,th{padding:4px; border-bottom:1px dashed #999; text-align:left} .r{text-align:right}
        </style></head><body>`);
      win.document.write(`<h3>POS Demo · Boleta</h3><div>${fecha}</div><hr>`);
      win.document.write('<table><thead><tr><th>ID</th><th>Producto</th><th class="r">Cant.</th><th class="r">Monto</th></tr></thead><tbody>');
      rows.forEach(r=> win.document.write(`<tr><td>${r.id}</td><td>${r.nombre}</td><td class="r">${r.qty}</td><td class="r">${r.monto}</td></tr>`));
      win.document.write(`</tbody></table><h3 style="text-align:right">Total: ${totalTxt}</h3></body></html>`);
      win.document.close(); win.focus(); win.print();
    });
  });
}
