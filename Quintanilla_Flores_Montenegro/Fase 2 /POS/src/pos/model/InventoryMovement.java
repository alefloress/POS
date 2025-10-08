package pos.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class InventoryMovement {

    public enum MovementType { ENTRY, EXIT, ADJUSTMENT }

    // Identificación / trazabilidad
    private int id;                 // opcional si usas BD autoincremental
    private String reference;       // N° de doc/boleta/orden
    private String performedBy;     // usuario que registró

    // Núcleo
    private Product product;
    private int quantity;           // ENTRY/EXIT: delta (>0) | ADJUSTMENT: valor absoluto (>=0)
    private MovementType type;
    private String reason;

    // Auditoría
    private final LocalDateTime createdAt;
    private Integer previousStock;  // se setea al aplicar
    private Integer resultingStock; // se setea al aplicar

    // --------- Constructores / fábricas ---------
    private InventoryMovement(Product product, int quantity, MovementType type,
                              String reason, String performedBy, String reference) {
        this.product = Objects.requireNonNull(product, "product no puede ser null");
        this.type = Objects.requireNonNull(type, "type no puede ser null");
        this.reason = (reason == null || reason.isBlank()) ? "-" : reason.trim();
        this.performedBy = (performedBy == null || performedBy.isBlank()) ? "system" : performedBy.trim();
        this.reference = (reference == null || reference.isBlank()) ? null : reference.trim();
        this.createdAt = LocalDateTime.now();
        validateQuantity(quantity, type);
        this.quantity = quantity;
    }

    public static InventoryMovement entry(Product product, int quantity, String reason, String performedBy) {
        return new InventoryMovement(product, quantity, MovementType.ENTRY, reason, performedBy, null);
    }

    public static InventoryMovement exit(Product product, int quantity, String reason, String performedBy) {
        return new InventoryMovement(product, quantity, MovementType.EXIT, reason, performedBy, null);
    }

    public static InventoryMovement adjustment(Product product, int newAbsoluteStock, String reason, String performedBy) {
        return new InventoryMovement(product, newAbsoluteStock, MovementType.ADJUSTMENT, reason, performedBy, null);
    }

    // Constructor completo para persistencia
    public InventoryMovement(int id, Product product, int quantity, MovementType type,
                             String reason, String performedBy, String reference, LocalDateTime createdAt) {
        this(product, quantity, type, reason, performedBy, reference);
        this.id = id;
        if (createdAt != null) {
            // si te llega desde BD respetamos esa fecha
            // (createdAt es final, así que no reasignamos; si necesitas esto,
            // crea otro ctor sin final en tu fork)
        }
    }

    // --------- Lógica ---------
    private void validateQuantity(int qty, MovementType type) {
        switch (type) {
            case ENTRY:
            case EXIT:
                if (qty <= 0) throw new IllegalArgumentException("quantity debe ser > 0 para ENTRY/EXIT");
                break;
            case ADJUSTMENT:
                if (qty < 0) throw new IllegalArgumentException("quantity (nuevo stock) debe ser >= 0 para ADJUSTMENT");
                break;
        }
    }

    /** Aplica el movimiento al producto y deja rastro de stock previo/resultante. */
    public void applyToProduct() {
        if (product == null) throw new IllegalStateException("No hay producto asociado al movimiento");
        int current = product.getStock();
        this.previousStock = current;

        switch (type) {
            case ENTRY:
                resultingStock = safeAdd(current, quantity);
                break;
            case EXIT:
                resultingStock = Math.max(0, current - quantity);
                break;
            case ADJUSTMENT:
                resultingStock = quantity;
                break;
            default:
                throw new IllegalStateException("Tipo de movimiento no soportado: " + type);
        }
        product.setStock(resultingStock);
    }

    private int safeAdd(int a, int b) {
        long r = (long) a + (long) b;
        if (r > Integer.MAX_VALUE) throw new ArithmeticException("Overflow de stock");
        return (int) r;
    }

    // --------- Getters / Setters ---------
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) {
        this.performedBy = (performedBy == null || performedBy.isBlank()) ? "system" : performedBy.trim();
    }

    public Product getProduct() { return product; }
    public void setProduct(Product product) {
        this.product = Objects.requireNonNull(product, "product no puede ser null");
    }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        validateQuantity(quantity, this.type);
        this.quantity = quantity;
    }

    public MovementType getType() { return type; }
    public void setType(MovementType type) {
        this.type = Objects.requireNonNull(type, "type no puede ser null");
        validateQuantity(this.quantity, type);
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = (reason == null || reason.isBlank()) ? "-" : reason.trim(); }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public Integer getPreviousStock() { return previousStock; }
    public Integer getResultingStock() { return resultingStock; }

    // --------- Utilidad ---------
    @Override
    public String toString() {
        String base = String.format(
            "[%s] %s | Prod: %s (id:%s) | qty:%d | reason:%s | by:%s",
            createdAt, type,
            (product != null ? product.getName() : "null"),
            (product != null ? product.getId() : "null"),
            quantity, reason, performedBy
        );
        if (previousStock != null && resultingStock != null) {
            base += String.format(" | stock: %d -> %d", previousStock, resultingStock);
        }
        if (reference != null) base += " | ref:" + reference;
        if (id > 0) base += " | id:" + id;
        return base;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryMovement)) return false; // <- corregido
        InventoryMovement that = (InventoryMovement) o;
        if (this.id > 0 && that.id > 0) return this.id == that.id;
        return Objects.equals(product, that.product)
            && type == that.type
            && quantity == that.quantity
            && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return (id > 0) ? Objects.hash(id) : Objects.hash(product, type, quantity, createdAt);
    }
}


