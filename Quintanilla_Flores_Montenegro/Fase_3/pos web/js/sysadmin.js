// =============================
//     LOGIN SYSADMIN (API)
// =============================
async function doSysAdminLogin(e){
    e.preventDefault();

    const uInput = document.getElementById("sys_user").value.trim();
    const pInput = document.getElementById("sys_pass").value.trim();

    const msg = document.getElementById("errorMsg");
    if (msg) msg.classList.add("d-none");

    if (!uInput || !pInput) {
        if (msg) msg.classList.remove("d-none");
        return false;
    }

    try {
        // 1) Login contra la API (usa el SDK que ya llama a /auth/login + /auth/whoami)
        const session = await SDK.Auth.login({ username: uInput, password: pInput });

        // 2) Normalizar username y rol desde lo que devuelva el SDK
        const username = String(
            session.username || session.u || uInput
        ).toLowerCase();

        const role = String(
            session.role || session.rol || ""
        ).toUpperCase();

        // 3) Este panel es SOLO para sysadmin: permitimos si
        //    - username es "sysadmin"   O
        //    - el rol es "SYSADMIN"
        if (username !== "sysadmin" && role !== "SYSADMIN") {
            alert("Este panel es solo para el usuario SYSADMIN.");
            return false;
        }

        // 4) Si todo OK, redirigimos al panel
        window.location.href = "sysadmin-panel.html";
        return false;
    } catch (err) {
        console.error("Error en login SYSADMIN:", err);
        if (msg) msg.classList.remove("d-none");
        return false;
    }
}

// =============================
//   PROTEGER PÁGINAS SYSADMIN
// =============================
function guardSysAdmin(){
    try{
        const raw = localStorage.getItem("pos_user") || "null";
        const u = JSON.parse(raw);

        const username = String(
            (u && (u.u || u.username)) || ""
        ).toLowerCase();

        const role = String(
            (u && (u.rol || u.role)) || ""
        ).toUpperCase();

        // Misma regla: sólo sysadmin por username o rol
        if (username !== "sysadmin" && role !== "SYSADMIN") {
            alert("Acceso solo para SYSADMIN");
            window.location.href = "sysadmin-login.html";
            throw new Error("No autorizado");
        }
    } catch (e) {
        console.error("Error en guardSysAdmin:", e);
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


// =============================
//      CERRAR SESIÓN
// =============================
async function logoutSysAdmin(){
    try {
        await SDK.Auth.logout();
    } catch {
        localStorage.removeItem("pos_user");
    }
    window.location.href = "sysadmin-login.html";
}
