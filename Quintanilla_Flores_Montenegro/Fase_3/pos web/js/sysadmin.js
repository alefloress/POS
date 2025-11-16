// =============================
//     LOGIN SYSADMIN
// =============================
function doSysAdminLogin(e){
    e.preventDefault();

    const u = document.getElementById("sys_user").value.trim();
    const p = document.getElementById("sys_pass").value.trim();

    // Credenciales del SYSADMIN (puedes cambiarlas)
    const valido = (u === "sysadmin" && p === "super123");

    if(!valido){
        const msg = document.getElementById("errorMsg");
        if(msg) msg.classList.remove("d-none");
        return false;
    }

    // Guardamos sesión especial del SYSADMIN
    localStorage.setItem("pos_user", JSON.stringify({
        user: u,
        rol: "SYSADMIN",
        ts: Date.now()
    }));

    // Redirección al panel
    window.location.href = "sysadmin-panel.html";
    return false;
}

// =============================
//   PROTEGER PÁGINAS SYSADMIN
// =============================
function guardSysAdmin(){
    try{
        const u = JSON.parse(localStorage.getItem("pos_user") || "null");
        if(!u || u.rol !== "SYSADMIN"){
            alert("Acceso solo para SYSADMIN");
            window.location.href = "sysadmin-login.html";
            throw new Error("No autorizado");
        }
    } catch {
        window.location.href = "sysadmin-login.html";
    }
}

// =============================
//      CERRAR SESIÓN
// =============================
function logoutSysAdmin(){
    localStorage.removeItem("pos_user");
    window.location.href = "sysadmin-login.html";
}
