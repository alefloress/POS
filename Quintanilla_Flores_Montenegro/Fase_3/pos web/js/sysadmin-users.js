// ======================================================================
//  SYSADMIN — CRUD de Usuarios (Modo LOCAL + Modo WEB API real)
// ======================================================================
//  Este módulo controla completamente:
//    - Listar usuarios
//    - Crear usuarios
//    - Eliminar usuarios
//
//  Funciona HOY en modo LOCAL (localStorage)
//  y está listo para mañana usar API REAL de tu compañero.
//
//  Usa la configuración de:
//        sysadmin-config.js  → getConfig()
//        api.js              → apiFetch()
//
// ======================================================================


// =============================================================
//  DETECTAR MODO (local / web) y API URL desde sysadmin-config
// =============================================================
function getModo() {
    const cfg = getConfig();           // viene de sysadmin-config.js
    return cfg.modo || "local";
}

function getApiUrl() {
    const cfg = getConfig();
    return cfg.url_api || "";
}


// ======================================================================
//  --- CRUD LOCAL (TEMPORAL) ---
// ======================================================================
//  Se usa cuando modo === "local"
function ls_getUsuarios() {
    return JSON.parse(localStorage.getItem("sys_usuarios") || "[]");
}

function ls_saveUsuarios(list) {
    localStorage.setItem("sys_usuarios", JSON.stringify(list));
}



// ======================================================================
//  --- CRUD API REAL (LISTO PARA BACKEND) ---
// ======================================================================
//  Tu compañero solo debe usar estos endpoints:
//
//     GET    /usuarios
//     POST   /usuarios
//     DELETE /usuarios/:id
//
// ======================================================================

// ---- LISTAR USUARIOS desde API ----
async function apiListarUsuarios() {
    try {
        return await apiFetch("/usuarios", { method: "GET" });
    } catch (err) {
        console.error("API Listar Usuarios:", err);
        alert("⚠ Error conectando al backend (listar usuarios).");
        return [];
    }
}


// ---- CREAR USUARIO en API ----
async function apiCrearUsuario(user, pass, rol) {
    try {
        return await apiFetch("/usuarios", {
            method: "POST",
            body: JSON.stringify({ user, pass, rol })
        });
    } catch (err) {
        console.error("API Crear Usuario:", err);
        alert("⚠ Error conectando al backend (crear usuario).");
        return false;
    }
}


// ---- ELIMINAR USUARIO en API ----
async function apiEliminarUsuario(user) {
    try {
        return await apiFetch("/usuarios/" + encodeURIComponent(user), {
            method: "DELETE"
        });
    } catch (err) {
        console.error("API Eliminar Usuario:", err);
        alert("⚠ Error conectando al backend (eliminar usuario).");
        return false;
    }
}



// ======================================================================
//  RENDERIZAR TABLA DE USUARIOS
// ======================================================================
async function renderUsuarios() {
    const tbody = document.getElementById("tablaUsuarios");
    if (!tbody) return;

    let list = [];

    // ------------------------------------
    // MODO WEB REAL
    // ------------------------------------
    if (getModo() === "web") {
        list = await apiListarUsuarios();
    }
    // ------------------------------------
    // MODO LOCAL (demo)
    // ------------------------------------
    else {
        list = ls_getUsuarios();
    }

    // ------------------------------------
    // TABLA VACÍA
    // ------------------------------------
    if (!list || list.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="4" class="text-center text-secondary">
                    No hay usuarios
                </td>
            </tr>`;
        return;
    }

    // ------------------------------------
    // RENDERIZAR
    // ------------------------------------
    tbody.innerHTML = list.map(u => `
        <tr>
            <td>${u.user}</td>
            <td>${u.rol}</td>
            <td><span class="badge bg-success">Activo</span></td>
            <td>
                <button class="btn btn-sm btn-outline-danger"
                        onclick="deleteUsuario('${u.user}')">
                    Eliminar
                </button>
            </td>
        </tr>
    `).join("");
}



// ======================================================================
//  CREAR USUARIO
// ======================================================================
async function crearUsuario() {
    const user = document.getElementById("newUser").value.trim();
    const pass = document.getElementById("newPass").value.trim();
    const rol  = document.getElementById("newRol").value;

    if (!user || !pass) return alert("Completa usuario y contraseña");

    // ------------------------------
    // MODO WEB REAL → llamar backend
    // ------------------------------
    if (getModo() === "web") {
        const ok = await apiCrearUsuario(user, pass, rol);
        if (ok) {
            renderUsuarios();
            alert("Usuario creado correctamente (API)");
        }
        return;
    }

    // ------------------------------
    // MODO LOCAL → localStorage
    // ------------------------------
    const list = ls_getUsuarios();

    if (list.find(u => u.user === user)) {
        return alert("Ese usuario ya existe");
    }

    list.push({ user, pass, rol });
    ls_saveUsuarios(list);

    document.getElementById("newUser").value = "";
    document.getElementById("newPass").value = "";

    renderUsuarios();
    alert("Usuario creado con éxito (Local)");
}



// ======================================================================
//  ELIMINAR USUARIO
// ======================================================================
async function deleteUsuario(user) {
    if (!confirm("¿Eliminar usuario " + user + "?")) return;

    // MODO WEB REAL
    if (getModo() === "web") {
        const ok = await apiEliminarUsuario(user);
        if (ok) renderUsuarios();
        return;
    }

    // MODO LOCAL
    const list = ls_getUsuarios().filter(u => u.user !== user);
    ls_saveUsuarios(list);
    renderUsuarios();
}


