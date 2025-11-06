
import { logout, getUser } from './auth.js';


export function mountSidebar(active = 'dashboard'){
  const user = getUser();
  const el = document.getElementById('sidebar');
  if (!el) return;

 
  el.classList.add('sidebar', 'sidenav');

  el.innerHTML = `
    <div class="brand">Almacén Sonia</div>
    <button class="navbtn ${active==='dashboard'?'active':''}" data-href="admin.html" type="button">Dashboard</button>
    <button class="navbtn ${active==='ventas'?'active':''}" data-href="ventas.html" type="button">Ventas</button>
    <button class="navbtn ${active==='proveedores'?'active':''}" data-href="proveedores.html" type="button">Proveedores</button>
    <button class="navbtn ${active==='inventario'?'active':''}" data-href="inventario.html" type="button">Inventario</button>
    <button class="navbtn ${active==='reportes'?'active':''}" data-href="reportes.html" type="button">Reportes</button>
    <button class="navbtn ${active==='usuarios'?'active':''}" data-href="usuarios.html" type="button">Usuarios</button>
    <button class="navbtn ${active==='ajustes'?'active':''}" data-href="ajustes.html" type="button">Ajustes</button>
    <hr class="navsep">
    <div class="small" style="margin:6px 8px">Usuario: ${user?.u || '-'} · Rol: ${user?.rol || user?.rol || '-'}</div>
    <button id="btnSidebarLogout" class="navbtn" type="button">Cerrar sesión</button>
  `;

  el.querySelectorAll('.navbtn').forEach(b => {
    const href = b.dataset.href;
    if (href) b.addEventListener('click', () => { location.href = href; });
  });

  const out = document.getElementById('btnSidebarLogout');
  if (out) out.addEventListener('click', logout);
}



