// js/throttle.js
//
// Simulación simple de control de concurrencia / cola de espera.
// En un sistema real esto vive en el servidor. Aquí lo hacemos
// local para demo (con localStorage).

(function(){
  const KEY = 'pos_active_sessions';

  // límite máximo permitido antes de mandar a sala de espera
  const MAX_ACTIVE = 2; // <-- ajusta este número para la demo (ej: 2 cajas activas máx)

  // lee lista de sesiones activas
  function readList(){
    try {
      const raw = localStorage.getItem(KEY) || '[]';
      return JSON.parse(raw);
    } catch {
      return [];
    }
  }

  // escribe lista
  function writeList(arr){
    localStorage.setItem(KEY, JSON.stringify(arr));
  }

  // genera un id de sesión único
  function genId(){
    return 'sess-' + Date.now() + '-' + Math.floor(Math.random()*999999);
  }

  // registra una nueva sesión si hay cupo
  function tryEnter(){
    const list = readList().filter(s => s && s.id && s.alive);
    // limpiamos muertos con timeout viejo (>10min sin tocar)
    const now = Date.now();
    const cleaned = list.filter(s => (now - s.ts) < (10*60*1000));
    writeList(cleaned);

    if (cleaned.length >= MAX_ACTIVE){
      // no hay cupo, calcular posición "en cola"
      const position = cleaned.length + 1;
      return { allowed:false, sessionId:null, active:cleaned.length, max:MAX_ACTIVE, position };
    }

    // sí hay cupo
    const sessionId = genId();
    cleaned.push({
      id: sessionId,
      ts: now,
      alive: true
    });
    writeList(cleaned);

    return { allowed:true, sessionId, active:cleaned.length, max:MAX_ACTIVE, position:null };
  }

  // "toco vida" de una sesión activa (para que no caduque)
  function heartbeat(sessionId){
    const list = readList();
    const now = Date.now();
    let changed = false;
    for (const s of list){
      if (s.id === sessionId){
        s.ts = now;
        s.alive = true;
        changed = true;
      }
    }
    if (changed){
      writeList(list);
    }
  }

  // liberar la sesión (cuando el cajero sale / logout)
  function leave(sessionId){
    const list = readList().filter(s => s && s.id !== sessionId);
    writeList(list);
  }

  // status general para pintar en wait.html
  function getStatus(){
    const list = readList().filter(Boolean);
    return {
      active: list.length,
      max: MAX_ACTIVE,
      position: null // esto lo seteamos cuando bloqueamos
    };
  }

  // exponer global
  window.OverloadGuard = {
    tryEnter,
    heartbeat,
    leave,
    getStatus,
  };
})();
