// ======================================================
// CONFIGURACIÓN DEL SISTEMA (Modo LOCAL / Modo WEB API)
// Esta capa es esencial para conectar el POS a un backend.
// Aquí se guarda la URL del backend y el modo de operación.
// ======================================================

// ------------------------------------------------------
// OBTENER CONFIGURACIÓN ACTUAL DESDE localStorage
// ------------------------------------------------------
function getConfig() {
    return JSON.parse(localStorage.getItem("pos_config") || `{
        "modo": "local",                // local = sin backend, web = usar API
        "url_api": "https://backend-api.com/api"
    }`);
}

// ------------------------------------------------------
// GUARDAR CONFIGURACIÓN EN localStorage
// ------------------------------------------------------
function saveConfig(cfg) {
    localStorage.setItem("pos_config", JSON.stringify(cfg));
}

// ------------------------------------------------------
// GUARDAR LOS PARÁMETROS DESDE EL PANEL SYSADMIN
// ------------------------------------------------------
function guardarParametros() {
    const modo = document.getElementById("cfg_modo").value;
    const url  = document.getElementById("cfg_api").value.trim();

    if (!url) return alert("Debes ingresar una URL de API");

    const cfg = { modo, url_api: url };
    saveConfig(cfg);

    alert("Parámetros guardados");
}

// ------------------------------------------------------
// FUNCIÓN CENTRAL PARA USAR API DEL BACKEND
// Tu compañero usará esto: apiFetch("/usuarios")
// ------------------------------------------------------
async function apiFetch(endpoint, options = {}) {
    const cfg = getConfig();

    // Si está en modo local → no llama al backend
    if (cfg.modo === "local") {
        console.warn("apiFetch llamado en modo LOCAL → no se usa backend");
        return null;
    }

    const url = cfg.url_api + endpoint;

    try {
        const res = await fetch(url, {
            headers: { "Content-Type": "application/json" },
            ...options
        });

        if (!res.ok) throw new Error("HTTP " + res.status);

        return await res.json();

    } catch (err) {
        console.error("Error en apiFetch:", err);
        alert("Error conectando al backend:\n" + err.message);
        return null;
    }
}
