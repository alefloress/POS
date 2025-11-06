// js/sdk.js
const API = {
  base: typeof window.__API_BASE__ === 'string' && window.__API_BASE__.trim()
    ? window.__API_BASE__.replace(/\/+$/,'')
    : null,
  token: null,
};
function hdr(json=true){
  return { ...(json?{'Content-Type':'application/json'}:{}), ...(API.token?{Authorization:`Bearer ${API.token}`}:{}) };
}
async function http(method, path, {params, body, json=true} = {}){
  if(!API.base) throw new Error('NO_API');
  const url = new URL(API.base + path);
  if(params) Object.entries(params).forEach(([k,v])=>{
    if(v!==undefined && v!==null && v!=='') url.searchParams.set(k,v);
  });
  const res = await fetch(url.toString(), { method, headers: hdr(json), body: body?(json?JSON.stringify(body):body):undefined });
  const text = await res.text(); const data = text ? JSON.parse(text) : null;
  if(!res.ok) throw new Error(data?.message || data?.detail || `HTTP ${res.status}`);
  return data;
}

// ===== LocalStorage helpers + seeds =====
const LS = {
  userKey:'pos_user', prodKey:'pos_products', supKey:'pos_suppliers', saleKey:'pos_sales',
  set:(k,v)=>localStorage.setItem(k, JSON.stringify(v)),
  get:(k,def)=>{ try{ return JSON.parse(localStorage.getItem(k)||'null') ?? def; } catch{ return def; } },
};
(function seed(){
  if(!LS.get(LS.prodKey,null)){
    LS.set(LS.prodKey,[{id:'P-0001', name:'Producto Demo', stock:10, price:1000, expiresAt:'2025-12-31'}]);
  }
  if(!LS.get(LS.supKey,null)){
    LS.set(LS.supKey,[{id:'PR-0001', name:'Proveedor Demo SPA', contact:'Ana Soto', email:'contacto@demo.cl', phone:'+56 9 1234 5678', address:'Santiago'}]);
  }
})();

export const SDK = {
  setToken(t){ API.token = t; },

  Auth:{
    async login(username, password){
      try{
        const { token, user } = await http('POST','/auth/login',{ body:{ username, password } });
        SDK.setToken(token); const payload = { ...user, token };
        LS.set(LS.userKey, payload); return payload;
      }catch(e){
        if(String(e.message)!=='NO_API') throw e;
        const valid = { admin:{pass:'admin123', role:'ADMIN'}, cajero:{pass:'cajero123', role:'CAJERO'} };
        const rec = valid[username]; if(!rec || rec.pass!==password) throw new Error('Usuario o contraseña incorrectos');
        const payload = { username, role:rec.role, token:'mock-token' }; LS.set(LS.userKey, payload); return payload;
      }
    },
    me(){ return LS.get(LS.userKey,null); },
    logout(){ localStorage.removeItem(LS.userKey); API.token=null; }
  },

  Products:{
    async list(){ try{ return await http('GET','/products'); }catch(e){ if(String(e.message)!=='NO_API') throw e; return LS.get(LS.prodKey,[]); } },
    async upsert(p){
      try{
        const id = p.id;
        return await http(id?'PUT':'POST', id?`/products/${encodeURIComponent(id)}`:'/products', { body:p });
      }catch(e){
        if(String(e.message)!=='NO_API') throw e;
        const arr = LS.get(LS.prodKey,[]); const i = arr.findIndex(x=>x.id===p.id);
        i>=0 ? (arr[i]=p) : arr.push(p); LS.set(LS.prodKey,arr); return p;
      }
    },
    async remove(id){
      try{ return await http('DELETE', `/products/${encodeURIComponent(id)}`); }
      catch(e){ if(String(e.message)!=='NO_API') throw e; LS.set(LS.prodKey, LS.get(LS.prodKey,[]).filter(x=>x.id!==id)); }
    }
  },

  Suppliers:{
    async list(){ try{ return await http('GET','/suppliers'); }catch(e){ if(String(e.message)!=='NO_API') throw e; return LS.get(LS.supKey,[]); } },
    async upsert(s){
      try{
        const id = s.id;
        return await http(id?'PUT':'POST', id?`/suppliers/${encodeURIComponent(id)}`:'/suppliers', { body:s });
      }catch(e){
        if(String(e.message)!=='NO_API') throw e;
        const arr = LS.get(LS.supKey,[]); const i = arr.findIndex(x=>x.id===s.id);
        i>=0 ? (arr[i]=s) : arr.push(s); LS.set(LS.supKey,arr); return s;
      }
    },
    async remove(id){
      try{ return await http('DELETE', `/suppliers/${encodeURIComponent(id)}`); }
      catch(e){ if(String(e.message)!=='NO_API') throw e; LS.set(LS.supKey, LS.get(LS.supKey,[]).filter(x=>x.id!==id)); }
    }
  },

  Sales:{
    async create(payload){
      try{ return await http('POST','/sales',{ body:payload }); }
      catch(e){
        if(String(e.message)!=='NO_API') throw e;
        const sales=LS.get(LS.saleKey,[]); const id=Date.now(), ts=Date.now();
        sales.push({id, ts, user:payload.user, method:payload.method, total:payload.total});
        LS.set(LS.saleKey, sales);
        // bajar stock mock
        const prods = LS.get(LS.prodKey,[]);
        (payload.items||[]).forEach(it=>{
          const i = prods.findIndex(p=>p.id===it.productId);
          if(i>=0) prods[i].stock = Math.max(0,(prods[i].stock||0)-Number(it.qty||0));
        });
        LS.set(LS.prodKey, prods);
        return { id, ts };
      }
    },
    async getOne(id){
      try{ return await http('GET', `/sales/${encodeURIComponent(id)}`); }
      catch(e){
        if(String(e.message)!=='NO_API') throw e;
        const s = LS.get(LS.saleKey,[]).find(x=>String(x.id)===String(id));
        if(!s) throw new Error('Venta no encontrada');
        return { id:s.id, ts:s.ts, items:[], subtotal:0, tax:0, total:s.total };
      }
    },
    async listRange(from,to){
      try{ return await http('GET','/sales',{ params:{from,to} }); }
      catch(e){
        if(String(e.message)!=='NO_API') throw e;
        const sales=LS.get(LS.saleKey,[]); const f=from?new Date(from):null; const t=to?new Date(to):null;
        return sales.filter(s=>{
          const d=new Date(s.ts);
          if(f && d < new Date(f.getFullYear(),f.getMonth(),f.getDate())) return false;
          if(t && d > new Date(t.getFullYear(),t.getMonth(),t.getDate()+1)) return false;
          return true;
        });
      }
    }
  },

  Settings:{
    KEY:'pos_settings',
    load(){ try{ const def={lowStock:5, expiryDays:30}; const raw=localStorage.getItem(this.KEY); return raw?{...def,...JSON.parse(raw)}:def; }catch{ return {lowStock:5, expiryDays:30}; } },
    save(v){ localStorage.setItem(this.KEY, JSON.stringify(v)); }
  }
};
