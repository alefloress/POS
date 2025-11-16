// =========================================
//     CRUD DEMO DE USUARIOS (LOCALSTORAGE)
// =========================================

// ---- Obtener lista de usuarios ----
function getUsuarios() {
    return JSON.parse(localStorage.getItem("sys_usuarios") || "[]");
}

// ---- Guardar lista ----
function saveUsuarios(list) {
    localStorage.setItem("sys_usuarios", JSON.stringify(list));
}

// ---- Renderizar tabla ----
function renderUsuarios() {
    const tbody = document.getElementById("tablaUsuarios");
    const list = getUsuarios();

    tbody.innerHTML = list.length === 0
        ? `<tr><td colspan="4" class="text-center text-secondary">No hay usuarios</td></tr>`
        : list.map(u => `
            <tr>
                <td>${u.user}</td>
                <td>${u.rol}</td>
                <td><span class="badge bg-success">Activo</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteUsuario('${u.user}')">Eliminar</button>
                </td>
            </tr>
        `).join("");
}

// ---- Crear usuario ----
function crearUsuario() {
    const user = document.getElementById("newUser").value.trim();
    const pass = document.getElementById("newPass").value.trim();
    const rol  = document.getElementById("newRol").value;

    if (!user || !pass) return alert("Completa usuario y contraseña");

    const list = getUsuarios();

    if (list.find(u => u.user === user)) {
        return alert("Ese usuario ya existe");
    }

    list.push({ user, pass, rol });
    saveUsuarios(list);

    document.getElementById("newUser").value = "";
    document.getElementById("newPass").value = "";

    renderUsuarios();
    alert("Usuario creado con éxito");
}

// ---- Eliminar ----
function deleteUsuario(user) {
    if (!confirm("¿Eliminar usuario " + user + "?")) return;

    const list = getUsuarios().filter(u => u.user !== user);
    saveUsuarios(list);
    renderUsuarios();
}
