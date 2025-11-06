// js/idleTimeout.js
// Cierra sesión automáticamente si nadie toca el sistema por cierto tiempo.

(function(){
  // tiempo máximo sin hacer nada (en milisegundos)
  const MAX_IDLE_MS = 5 * 60 * 1000; // 5 minutos

  let lastActive = Date.now();

  // cada vez que el usuario mueve el mouse, hace click o escribe, lo consideramos "activo"
  function resetTimer(){
    lastActive = Date.now();
  }

  ['mousemove','keydown','click','touchstart'].forEach(evt => {
    window.addEventListener(evt, resetTimer, { passive: true });
  });

  async function checkIdle(){
    const now = Date.now();
    const diff = now - lastActive;

    if (diff >= MAX_IDLE_MS){
      // borramos sesión del usuario
      localStorage.removeItem('pos_user');

      // opcional: también borramos el checkout temporal por seguridad
      sessionStorage.removeItem('pos_checkout');

      // redirigimos al login
      window.location.href = 'login.html';
      return;
    }

    // si todavía no se pasó el tiempo, volvemos a revisar más tarde
    setTimeout(checkIdle, 10000); // revisa cada 10s
  }

  // arrancar la vigilancia
  setTimeout(checkIdle, 10000);
})();
