// ======================================================================
// sysadmin.js
// Maneja:
//   • Login especial del SYSADMIN
//   • Protección de páginas del panel
//   • Cierre de sesión
//
// Este archivo NO se conecta a la API. Solo controla sesión y rol.
// ======================================================================


// ======================================================================
// 1) LOGIN DEL SYSADMIN
// ======================================================================
function doSysAdminLogin(e) {
    e.preventDefault();

    const user = document.getElementById("sys_user").value.trim();
    const pass = document.getElementById("sys_pass").value.trim();

    // Credenciales del SYSADMIN (temporales, NO reales)
    const valido = (user === "sysadmin" && pass === "super123");

    if (!valido) {
        document.getElementById("errorMsg")?.classList.remove("d-none");
        return false;
    }

    // Guardar sesión del SYSADMIN
    localStorage.setItem("pos_user", JSON.stringify({
        user: user,
        rol: "SYSADMIN",
        ts: Date.now()          // timestamp de inicio de sesión
    }));

    // Redirige al panel principal
    window.location.href = "sysadmin-panel.html";
    return false;
}


// ======================================================================
// 2) OBTENER USUARIO LOGEADO
// ======================================================================
function getLoggedUser() {
    try {
        return JSON.parse(localStorage.getItem("pos_user") || "null");
    } catch {
        return null;
    }
}


// ======================================================================
// 3) PROTEGER EL PANEL SYSADMIN
// ======================================================================
// Esta función se ejecuta EN EL onload de sysadmin-panel.html.
//
// Si el usuario NO es SYSADMIN:
//   → lo redirige al login
//
// Si la sesión no existe o está corrupta:
//   → redirige al login
// ======================================================================
function guardSysAdmin() {
    const u = getLoggedUser();

    if (!u || u.rol !== "SYSADMIN") {
        alert("Acceso permitido solo para SYSADMIN.");
        window.location.href = "sysadmin-login.html";
        return;
    }

    // Si quieres agregar expiración de sesión:
    // const maxTiempo = 8 * 60 * 60 * 1000; // 8 horas
    // if (Date.now() - u.ts > maxTiempo) {
    //     alert("Sesión expirada");
    //     logoutSysAdmin();
    // }
}


// ======================================================================
// 4) CERRAR SESIÓN DEL SYSADMIN
// ======================================================================
function logoutSysAdmin() {
    localStorage.removeItem("pos_user");
    window.location.href = "sysadmin-login.html";
}
