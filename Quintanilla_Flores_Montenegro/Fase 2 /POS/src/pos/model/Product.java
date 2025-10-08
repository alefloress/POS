package pos.model;

import java.time.LocalDate;
import java.util.Objects;

public class Product {
    private int id;                 // 🔹 ID interno opcional (para BD o control interno)
    private String code;
    private String name;
    private String category;
    private int price;
    private int stock;
    private LocalDate expiry;

    // ======= Constructores =======

    public Product(int id, String code, String name, String category, int price, int stock, LocalDate expiry) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = Math.max(0, stock);
        this.expiry = expiry;
    }

    // Sobrecargas para compatibilidad con versiones anteriores
    public Product(String code, String name, String category, int price, int stock, LocalDate expiry) {
        this(0, code, name, category, price, stock, expiry);
    }

    public Product(String code, String name, String category, int price, int stock) {
        this(0, code, name, category, price, stock, null);
    }

    public Product(String code, String name, int price, int stock) {
        this(0, code, name, "", price, stock, null);
    }

    // ======= Getters y Setters =======

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPrice() { return price; }
    public void setPrice(int price) {
        if (price < 0) throw new IllegalArgumentException("El precio no puede ser negativo");
        this.price = price;
    }

    public int getStock() { return stock; }
    public void setStock(int stock) {
        if (stock < 0) throw new IllegalArgumentException("El stock no puede ser negativo");
        this.stock = stock;
    }

    public LocalDate getExpiry() { return expiry; }
    public void setExpiry(LocalDate expiry) { this.expiry = expiry; }

    // ======= Utilidad =======

    @Override
    public String toString() {
        return String.format(
            "Product{id=%d, code='%s', name='%s', category='%s', price=%d, stock=%d, expiry=%s}",
            id, code, name, category, price, stock,
            (expiry != null ? expiry.toString() : "-")
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product p = (Product) o;
        // si hay id, compara por id; si no, por code
        if (this.id > 0 && p.id > 0) return this.id == p.id;
        return Objects.equals(code, p.code);
    }

    @Override
    public int hashCode() {
        return (id > 0) ? Objects.hash(id) : Objects.hash(code);
    }
}
