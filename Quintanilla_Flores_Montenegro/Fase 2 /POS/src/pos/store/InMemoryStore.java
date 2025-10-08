package pos.store;

import pos.model.Product;
import pos.model.Client;
import pos.model.User;   // <-- NUEVO

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fuente de datos EN MEMORIA para el front-end.
 * No hay base de datos real: solo datos de ejemplo y operaciones CRUD básicas.
 */
public class InMemoryStore {

    // ===================== PRODUCTOS =====================
    private static final List<Product> PRODUCTS = new ArrayList<>();

    static {
        // Semillas de productos
        PRODUCTS.add(new Product("1001", "Arroz 5kg", "Alimentos", 2490, 50, null));
        PRODUCTS.add(new Product("1002", "Azúcar 1kg", "Alimentos", 420, 100, null));
        PRODUCTS.add(new Product("1003", "Aceite 1L", "Alimentos", 850, 30, null));
        PRODUCTS.add(new Product("1004", "Leche 1L", "Lácteos", 380, 80, LocalDate.now().plusMonths(2)));
        PRODUCTS.add(new Product("1005", "Fideos 500g", "Alimentos", 290, 60, null));
    }

    /** Devuelve lista de solo lectura con todos los productos. */
    public static List<Product> allProducts() {
        return Collections.unmodifiableList(PRODUCTS);
    }

    /** Búsqueda por código o nombre (contiene, case-insensitive). */
    public static List<Product> search(String query) {
        if (query == null || query.isBlank()) return allProducts();
        final String q = query.toLowerCase(Locale.ROOT);
        return PRODUCTS.stream()
                .filter(p -> p.getCode().toLowerCase(Locale.ROOT).contains(q)
                          || p.getName().toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());
    }

    /** Busca por código exacto (case-insensitive). */
    public static Optional<Product> findByCode(String code) {
        if (code == null) return Optional.empty();
        return PRODUCTS.stream()
                .filter(p -> p.getCode().equalsIgnoreCase(code))
                .findFirst();
    }

    /** Inserta (si ya existe el código, lo reemplaza). */
    public static void upsert(Product product) {
        removeByCode(product.getCode());
        PRODUCTS.add(product);
    }

    /** Elimina por código. */
    public static boolean removeByCode(String code) {
        return PRODUCTS.removeIf(p -> p.getCode().equalsIgnoreCase(code));
    }

    /** Actualiza un producto existente (por código). Devuelve true si lo encontró. */
    public static boolean update(Product product) {
        Optional<Product> opt = findByCode(product.getCode());
        if (opt.isEmpty()) return false;
        Product p = opt.get();
        p.setName(product.getName());
        p.setCategory(product.getCategory());
        p.setPrice(product.getPrice());
        p.setStock(product.getStock());
        p.setExpiry(product.getExpiry());
        return true;
    }

    /** Genera un código nuevo simple (siguiente correlativo de 4 dígitos). */
    public static String nextCode() {
        int max = PRODUCTS.stream()
                .map(Product::getCode)
                .filter(c -> c.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max().orElse(1000);
        return String.valueOf(max + 1);
    }

    /** Productos con stock bajo. */
    public static List<Product> lowStock(int threshold) {
        return PRODUCTS.stream()
                .filter(p -> p.getStock() <= threshold)
                .collect(Collectors.toList());
    }

    /** Productos próximos a vencer (incluye ya vencidos hasta el límite). */
    public static List<Product> expiringInDays(int days) {
        LocalDate limit = LocalDate.now().plusDays(days);
        return PRODUCTS.stream()
                .filter(p -> p.getExpiry() != null && !p.getExpiry().isAfter(limit))
                .collect(Collectors.toList());
    }

    // ========= ADAPTADORES PRODUCTOS (compatibilidad UI) =========
    public static List<Product> getAllProducts() { return allProducts(); }
    public static void addProduct(Product product) { upsert(product); }
    public static void updateProduct(Product product) { if (!update(product)) upsert(product); }
    public static boolean removeProduct(String code) { return removeByCode(code); }


    // ===================== CLIENTES =====================
    private static final List<Client> CLIENTS = new ArrayList<>();

    static {
        // Semillas de clientes
        CLIENTS.add(new Client("C0001","Juan Pérez","+56 9 1234 5678","juan@example.com","Santiago",    LocalDate.now().minusMonths(2)));
        CLIENTS.add(new Client("C0002","María López","+56 9 9876 5432","maria@example.com","Providencia",LocalDate.now().minusWeeks(3)));
        CLIENTS.add(new Client("C0003","Carlos Díaz","+56 2 2222 2222","carlos@example.com","Ñuñoa",     LocalDate.now().minusDays(10)));
    }

    /** Lista inmutable de clientes. */
    public static List<Client> allClients() {
        return Collections.unmodifiableList(CLIENTS);
    }

    /** Adaptador usado por la UI. */
    public static List<Client> getAllClients() { return allClients(); }

    /** Buscar por id, nombre, teléfono o email (contains, case-insensitive). */
    public static List<Client> searchClients(String q) {
        if (q == null || q.isBlank()) return allClients();
        String s = q.toLowerCase(Locale.ROOT);
        return CLIENTS.stream().filter(c ->
                c.getId().toLowerCase(Locale.ROOT).contains(s) ||
                c.getName().toLowerCase(Locale.ROOT).contains(s) ||
                (c.getPhone()!=null && c.getPhone().toLowerCase(Locale.ROOT).contains(s)) ||
                (c.getEmail()!=null && c.getEmail().toLowerCase(Locale.ROOT).contains(s))
        ).collect(Collectors.toList());
    }

    /** Buscar cliente por ID exacto. */
    public static Optional<Client> findClientById(String id) {
        if (id == null) return Optional.empty();
        return CLIENTS.stream().filter(c -> c.getId().equalsIgnoreCase(id)).findFirst();
    }

    /** Insertar/actualizar (por id). */
    public static void upsertClient(Client c) {
        removeClient(c.getId());
        CLIENTS.add(c);
    }

    /** Actualizar campos (si no existe, false). */
    public static boolean updateClient(Client c) {
        Optional<Client> op = findClientById(c.getId());
        if (op.isEmpty()) return false;
        Client x = op.get();
        x.setName(c.getName());
        x.setPhone(c.getPhone());
        x.setEmail(c.getEmail());
        x.setAddress(c.getAddress());
        x.setCreatedAt(c.getCreatedAt());
        return true;
    }

    /** Adaptadores para la UI. */
    public static void addClient(Client c) { upsertClient(c); }
    public static void saveClient(Client c) { if (!updateClient(c)) upsertClient(c); }

    /** Eliminar por id. */
    public static boolean removeClient(String id) {
        return CLIENTS.removeIf(cl -> cl.getId().equalsIgnoreCase(id));
    }

    /** Siguiente correlativo C0001, C0002, ... */
    public static String nextClientId() {
        int max = CLIENTS.stream()
                .map(Client::getId)
                .filter(s -> s.matches("C\\d+"))
                .map(s -> s.substring(1))
                .mapToInt(Integer::parseInt)
                .max().orElse(0);
        return String.format("C%04d", max + 1);
    }


    // ===================== USUARIOS (NUEVO) =====================
    private static final List<User> USERS = new ArrayList<>();

    static {
        // Semillas de usuarios
        USERS.add(new User("U0001","admin","ADMIN", true, LocalDate.now().minusMonths(3), "admin123"));
        USERS.add(new User("U0002","cajero","CAJERO", true, LocalDate.now().minusWeeks(2), "cajero123"));
    }

    public static List<User> allUsers() {
        return Collections.unmodifiableList(USERS);
    }
    public static List<User> getAllUsers() { return allUsers(); }

    public static List<User> searchUsers(String q) {
        if (q == null || q.isBlank()) return allUsers();
        String s = q.toLowerCase(Locale.ROOT);
        return USERS.stream().filter(u ->
                u.getId().toLowerCase(Locale.ROOT).contains(s) ||
                u.getUsername().toLowerCase(Locale.ROOT).contains(s) ||
                u.getRole().toLowerCase(Locale.ROOT).contains(s)
        ).collect(Collectors.toList());
    }

    public static Optional<User> findUserById(String id) {
        if (id == null) return Optional.empty();
        return USERS.stream().filter(u -> u.getId().equalsIgnoreCase(id)).findFirst();
    }

    public static void upsertUser(User u) {
        removeUser(u.getId());
        USERS.add(u);
    }
    public static void addUser(User u) { upsertUser(u); }

    public static boolean updateUser(User u) {
        Optional<User> op = findUserById(u.getId());
        if (op.isEmpty()) return false;
        User x = op.get();
        x.setUsername(u.getUsername());
        x.setRole(u.getRole());
        x.setActive(u.isActive());
        x.setCreatedAt(u.getCreatedAt());
        if (u.getPassword()!=null && !u.getPassword().isBlank()) x.setPassword(u.getPassword());
        return true;
    }
    public static void saveUser(User u) { if (!updateUser(u)) upsertUser(u); }

    public static boolean removeUser(String id) {
        return USERS.removeIf(x -> x.getId().equalsIgnoreCase(id));
    }

    public static String nextUserId() {
        int max = USERS.stream()
                .map(User::getId)
                .filter(s -> s.matches("U\\d+"))
                .map(s -> s.substring(1))
                .mapToInt(Integer::parseInt)
                .max().orElse(0);
        return String.format("U%04d", max + 1);
    }

    public static boolean resetUserPassword(String id, String newPass) {
        Optional<User> op = findUserById(id);
        if (op.isEmpty()) return false;
        op.get().setPassword(newPass);
        return true;
    }
}


