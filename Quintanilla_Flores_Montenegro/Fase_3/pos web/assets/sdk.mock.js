(function(){
  const KEYS = {
    user: 'pos_user',
    products: 'pos_products',
    suppliers: 'pos_suppliers',
    sales: 'pos_sales',
    cash: 'pos_cash'
  };

  const sleep = (ms)=> new Promise(r=>setTimeout(r, ms));
  const read  = (k, def)=> {
    try{
      return JSON.parse(localStorage.getItem(k) || 'null') ?? def;
    } catch {
      return def;
    }
  };
  const write = (k, v)=> localStorage.setItem(k, JSON.stringify(v));
  const toNum = (v)=> Number.isFinite(Number(v)) ? Number(v) : 0;

  // =========================
  // Seeds iniciales mínimas
  // =========================
  if(!read(KEYS.products)) write(KEYS.products, [
    {
      id:'P-0001',
      nombre:'Producto Demo',
      stock:10,
      precio:1000,
      vence:'2025-12-31',
      barcode:'780000000001' // ← ejemplo código de barras
    },
    {
      id:'P-0002',
      nombre:'Galletas',
      stock:25,
      precio:800,
      barcode:'780000000002'
    }
  ]);

  if(!read(KEYS.suppliers)) write(KEYS.suppliers, [
    {
      id:'PR-0001',
      nombre:'Proveedor Demo SPA',
      contacto:'Ana Soto',
      mail:'contacto@demo.cl',
      tel:'+56 9 1234 5678',
      addr:'Santiago'
    }
  ]);

  if(!read(KEYS.sales)) write(KEYS.sales, []);
  if(!read(KEYS.cash)) write(KEYS.cash, []);

  const SDK = {
    // =====================================================
    // Auth
    // =====================================================
    Auth: {
      async login({username, password}){
        await sleep(150);

        const users = {
          admin:  { pass:'admin123',  rol:'ADMIN'  },
          cajero: { pass:'cajero123', rol:'CAJERO' }
        };

        const rec = users[username];
        if(!rec || rec.pass !== password){
          throw new Error('Credenciales inválidas');
        }

        const out = {
          username,
          role: rec.rol,
          token: null,
          ts: Date.now()
        };

        // Guardamos sesión espejo en localStorage
        write(KEYS.user, {
          u: username,
          rol: rec.rol,
          ts: out.ts
        });

        return out;
      },

      async me(){
        await sleep(80);
        return read(KEYS.user, null);
      },

      async logout(){
        await sleep(50);
        localStorage.removeItem(KEYS.user);
        return true;
      }
    },

    // =====================================================
    // Products
    // =====================================================
    Products: {
      async list(){
        await sleep(80);
        const list = read(KEYS.products, []);
        return list.map(p=>({
          id: String(p.id),
          nombre: p.nombre ?? p.name ?? '',
          stock: toNum(p.stock),
          precio: toNum(p.precio ?? p.price),
          vence: p.vence ?? p.expiryDate ?? null,
          barcode: p.barcode ?? p.code ?? '' // ← NUEVO campo
        }));
      },

      async upsert(prod){
        await sleep(120);

        const list = read(KEYS.products, []);
        const dto = {
          id: String(prod.id),
          nombre: prod.nombre ?? prod.name ?? '',
          stock: toNum(prod.stock),
          precio: toNum(prod.precio ?? prod.price),
          vence: prod.vence ?? prod.expiryDate ?? null,
          barcode: prod.barcode ?? prod.code ?? '' // ← NUEVO campo
        };

        const i = list.findIndex(x => String(x.id) === dto.id);
        if(i >= 0){
          list[i] = dto;
        }else{
          list.push(dto);
        }

        write(KEYS.products, list);
        return dto;
      },

      // compat inventario.js
      async create(prod){
        return this.upsert(prod);
      },

      async update(id, prod){
        return this.upsert({ ...prod, id });
      },

      async remove(id){
        await sleep(100);
        const list = read(KEYS.products, [])
          .filter(p => String(p.id) !== String(id));
        write(KEYS.products, list);
        return true;
      },

      // buscar por barcode (para el lector)
      async findByBarcode(code){
        await sleep(40);
        const list = read(KEYS.products, []);
        const found = list.find(p =>
          String(p.barcode || '').trim() === String(code).trim()
        );
        if(!found) return null;
        return {
          id: String(found.id),
          nombre: found.nombre ?? found.name ?? '',
          stock: toNum(found.stock),
          precio: toNum(found.precio ?? found.price),
          vence: found.vence ?? found.expiryDate ?? null,
          barcode: found.barcode ?? ''
        };
      }
    },

    // =====================================================
    // Suppliers
    // =====================================================
    Suppliers: {
      async list(){
        await sleep(80);
        const list = read(KEYS.suppliers, []);
        return list.map(s=>({
          id: String(s.id),
          nombre: s.nombre ?? s.name ?? '',
          contacto: s.contacto ?? s.contact ?? '',
          mail: s.mail ?? s.email ?? '',
          tel: s.tel ?? s.phone ?? '',
          addr: s.addr ?? s.address ?? ''
        }));
      },

      async upsert(row){
        await sleep(120);

        const list = read(KEYS.suppliers, []);
        const dto = {
          id: String(row.id),
          nombre: row.nombre ?? row.name ?? '',
          contacto: row.contacto ?? row.contact ?? '',
          mail: row.mail ?? row.email ?? '',
          tel: row.tel ?? row.phone ?? '',
          addr: row.addr ?? row.address ?? ''
        };

        const i = list.findIndex(x => String(x.id) === dto.id);
        if(i >= 0){
          list[i] = dto;
        }else{
          list.push(dto);
        }

        write(KEYS.suppliers, list);
        return dto;
      },

      async create(row){
        return this.upsert(row);
      },

      async update(id, row){
        return this.upsert({ ...row, id });
      },

      async remove(id){
        await sleep(100);
        const list = read(KEYS.suppliers, [])
          .filter(s => String(s.id) !== String(id));
        write(KEYS.suppliers, list);
        return true;
      }
    },

    // =====================================================
    // Cash  (apertura / cierre de caja)
    // =====================================================
    Cash: {
      async current(){
        await sleep(40);

        const logs = read(KEYS.cash, []);
        let openEvt = null;
        for (const evt of logs){
          if (evt.type === 'open'){
            openEvt = {
              id: evt.id,
              user: evt.user,
              openTs: evt.ts,
              amount: evt.amount
            };
          } else if (evt.type === 'close'){
            openEvt = null;
          }
        }
        return openEvt;
      },

      async open({user, openingAmount=0} = {}){
        await sleep(120);

        const already = await this.current();
        if (already){
          throw new Error('Ya hay una caja abierta.');
        }

        const logs = read(KEYS.cash, []);
        const evt = {
          id: 'C'+Date.now(),
          type: 'open',
          user: user || (read(KEYS.user,null)?.u || 'desconocido'),
          amount: toNum(openingAmount),
          ts: Date.now()
        };

        logs.push(evt);
        write(KEYS.cash, logs);
        return evt;
      },

      async close({user, closingAmount=0} = {}){
        await sleep(120);

        const already = await this.current();
        if (!already){
          throw new Error('No hay caja abierta.');
        }

        const logs = read(KEYS.cash, []);
        const evt = {
          id: 'C'+Date.now(),
          type: 'close',
          user: user || (read(KEYS.user,null)?.u || 'desconocido'),
          amount: toNum(closingAmount),
          ts: Date.now()
        };

        logs.push(evt);
        write(KEYS.cash, logs);
        return evt;
      },

      async listRange({from, to} = {}){
        await sleep(100);

        const f = from ? new Date(from) : null;
        const t = to ? new Date(to)   : null;
        const logs = read(KEYS.cash, []);

        return logs
          .filter(x=>{
            const d = new Date(x.ts);
            if(f && d < new Date(f.getFullYear(),f.getMonth(),f.getDate())) return false;
            if(t && d > new Date(t.getFullYear(),t.getMonth(),t.getDate()+1)) return false;
            return true;
          })
          .sort((a,b)=> b.ts - a.ts);
      }
    },

    // =====================================================
    // Sales  (ventas / boleta / ticket)
    // =====================================================
    Sales: {
      async create(payload){
        // payload:
        //   user, method, cashGiven, change,
        //   items: [{id,nombre,precio,qty}]
        await sleep(150);

        const cur = await SDK.Cash.current();
        if (!cur) {
          throw new Error('No hay caja abierta. Abra caja antes de cobrar.');
        }

        const items = (payload.items||[]).map(i=>({
          id: String(i.id),
          nombre: i.nombre ?? i.name ?? '',
          precio: toNum(i.precio ?? i.price),
          qty: toNum(i.qty || 1)
        }));

        const sub = items.reduce((a,i)=> a + i.precio*i.qty, 0);
        const iva = Math.round(sub * 0.19);
        const total = sub + iva;

        const sale = {
          id: 'S'+Date.now(),
          ts: Date.now(),
          user:   payload.user,
          method: payload.method || 'Efectivo',
          cashGiven: toNum(payload.cashGiven),
          change:    toNum(payload.change),
          items,
          sub,
          subtotal: sub,
          iva,
          total
        };

        const list = read(KEYS.sales, []);
        list.push(sale);
        write(KEYS.sales, list);

        return sale;
      },

      async getOne(id){
        await sleep(80);
        const s = read(KEYS.sales, []).find(x => String(x.id) === String(id));
        if(!s) throw new Error('Venta no encontrada');
        return s;
      },

      async listRange({from, to} = {}){
        await sleep(120);

        const f = from ? new Date(from) : null;
        const t = to ? new Date(to)   : null;
        const list = read(KEYS.sales, []);

        return list
          .filter(s=>{
            const d = new Date(s.ts);
            if(f && d < new Date(f.getFullYear(),f.getMonth(),f.getDate())) return false;
            if(t && d > new Date(t.getFullYear(),t.getMonth(),t.getDate()+1)) return false;
            return true;
          })
          .sort((a,b)=> b.ts - a.ts);
      }
    }
  };

  // Exponer globalmente
  window.SDK = SDK;
  window.SDK_VERSION = 'mock-1.4-barcode';
})();

