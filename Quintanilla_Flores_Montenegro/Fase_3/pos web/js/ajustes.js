// js/ajustes.js
import { requireAuth } from './auth.js';
import { mountSidebar } from './ui.js';

const Settings = {
  KEY: 'pos_settings',
  load(){ try{
    const def = { lowStock: 5, expiryDays: 30 };
    const raw = localStorage.getItem(this.KEY);
    return raw ? { ...def, ...JSON.parse(raw) } : def;
  }catch{ return { lowStock:5, expiryDays:30 }; } },
  save(v){ localStorage.setItem(this.KEY, JSON.stringify(v)); }
};

const u = requireAuth(['ADMIN']);
if(u){
  document.addEventListener('DOMContentLoaded', () => {
    mountSidebar('ajustes');
    const low = document.getElementById('lowStock');
    const exp = document.getElementById('expiryDays');
    const cfg = Settings.load();
    low.value = cfg.lowStock;
    exp.value = cfg.expiryDays;
    document.getElementById('btnSave').addEventListener('click', ()=>{
      Settings.save({ lowStock: Number(low.value||5), expiryDays: Number(exp.value||30) });
      alert('Guardado. El Dashboard usará estos parámetros.');
    });
  });
}
