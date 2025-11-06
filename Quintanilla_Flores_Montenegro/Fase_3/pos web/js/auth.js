// js/auth.js

const DEMO_VALID = {
  admin:  { pass: 'admin123',  rol: 'ADMIN'  },
  cajero: { pass: 'cajero123', rol: 'CAJERO' },
};

// ─── Helpers sesión ───────────────────────────────────────────────
export function getUser(){
  try{ return JSON.parse(localStorage.getItem('pos_user')||'null'); }
  catch(_){ return null; }
}

export function logout(){
  localStorage.removeItem('pos_user');
  // Siempre volver al login
  location.replace('login.html');
}

/**
 * Protege rutas. Si pasas roles, valida pertenencia.
 * - Si no está logueado -> login.html
 * - Si está logueado pero NO tiene el rol -> redirige a su home (admin o cajero)
 */
export function requireAuth(roles = []){
  const u = getUser();
  if(!u){
    location.replace('login.html');
    return null;
  }
  if(roles.length && !roles.includes(u.rol)){
    if(u.rol === 'ADMIN') location.replace('admin.html');
    else location.replace('cajero.html');
    return null;
  }
  return u;
}

// ─── Login ────────────────────────────────────────────────────────
export async function doLogin(e){
  e?.preventDefault?.();
  const user = document.getElementById('user')?.value?.trim() || '';
  const pass = document.getElementById('pass')?.value?.trim() || '';
  const errorEl = document.getElementById('loginError');

  // 1) Intento real via SDK si existe
  if (window.SDK?.Auth?.login) {
    try{
      const res = await window.SDK.Auth.login({ username:user, password:pass });
      // Se espera { username, role, token }
      if(!res || !res.username){
        throw new Error('Respuesta inválida del SDK');
      }
      const payload = {
        u: res.username,
        rol: res.role || 'ADMIN',
        token: res.token || null,
        ts: Date.now()
      };
      localStorage.setItem('pos_user', JSON.stringify(payload));
      location.href = (payload.rol === 'ADMIN') ? 'admin.html' : 'cajero.html';
      return false;
    }catch(err){
      console.warn('SDK.Auth.login falló, usando DEMO fallback:', err?.message || err);
      // cae a DEMO
    }
  }

  // 2) Fallback demo local (sin backend)
  const rec = DEMO_VALID[user];
  const ok = !!rec && pass === rec.pass;
  if(!ok){
    if(errorEl){
      errorEl.classList.remove('d-none');
      errorEl.textContent = 'Usuario o contraseña incorrectos.';
    }
    return false;
  }

  const payload = { u: user, rol: rec.rol, token: null, ts: Date.now() };
  localStorage.setItem('pos_user', JSON.stringify(payload));
  location.href = (payload.rol === 'ADMIN') ? 'admin.html' : 'cajero.html';
  return false;
}
