package pos.ui.views;

import pos.model.InventoryMovement;
import pos.model.Product;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// ==== DAOs ====
import pos.dao.InventoryDao;
import pos.dao.MovementDao;

public class InventarioPanel extends JPanel {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // Barra superior
    private final JTextField txtBuscar = new JTextField(18);
    private final JButton btnBuscar    = new JButton("Buscar");
    private final JButton btnEntrada   = new JButton("Entrada (+)");
    private final JButton btnSalida    = new JButton("Salida (−)");
    private final JButton btnAjuste    = new JButton("Ajuste");
    private final JButton btnHistorial = new JButton("Historial");
    private final JButton btnEliminar  = new JButton("Eliminar");

    // Tabla
    private final JTable tabla = new JTable(new ProductosModel());
    private final ProductosModel modelo = (ProductosModel) tabla.getModel();

    // DAOs
    private final InventoryDao dao = new InventoryDao();
    private final MovementDao movementDao = new MovementDao();

    private final String currentUser = "admin"; 

    public InventarioPanel() {
        setLayout(new BorderLayout(8,8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar por nombre o código…");
        top.add(txtBuscar);
        top.add(btnBuscar);
        top.add(btnEntrada);
        top.add(btnSalida);
        top.add(btnAjuste);
        top.add(btnHistorial);
        top.add(btnEliminar);
        add(top, BorderLayout.NORTH);

        tabla.setRowHeight(22);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Carga inicial desde BD
        recargar();

        btnBuscar.addActionListener(e -> filtrar());
        btnEntrada.addActionListener(e -> onEntradaCrearOSumar());
        btnSalida.addActionListener(e -> onSalida());
        btnAjuste.addActionListener(e -> onAjuste());
        btnHistorial.addActionListener(e -> openHistorial());
        btnEliminar.addActionListener(e -> onDelete());
    }

  
    private void recargar() {
        List<Product> datos = dao.listAll();
        modelo.set(datos);
    }

    private void filtrar() {
        String q = txtBuscar.getText().trim().toLowerCase();
        if (q.isEmpty()) { recargar(); return; }
        List<Product> base = dao.listAll();
        List<Product> list = base.stream()
                .filter(p -> (p.getName()!=null && p.getName().toLowerCase().contains(q))
                          || (p.getCode()!=null && p.getCode().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        modelo.set(list);
    }

    private Product getSeleccionado() {
        int row = tabla.getSelectedRow();
        if (row < 0) return null;
        return modelo.getAt(row);
    }

   
    private Product findByCode(String code) {
        if (code == null || code.isBlank()) return null;
        return dao.findByCode(code.trim());
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private void warn(String msg) { JOptionPane.showMessageDialog(this, msg, "Atención", JOptionPane.WARNING_MESSAGE); }
    private void info(String msg) { JOptionPane.showMessageDialog(this, msg, "OK", JOptionPane.INFORMATION_MESSAGE); }

    private JPanel form2(String l1, JComponent c1, String l2, JComponent c2) {
        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel(l1)); p.add(c1);
        p.add(new JLabel(l2)); p.add(c2);
        return p;
    }

   
    private String nextCodeFromDb() {
        return dao.listAll().stream()
                .map(Product::getCode)
                .filter(Objects::nonNull)
                .filter(c -> c.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(1000) + 1 + "";
    }

    
    private void onEntradaCrearOSumar() {
       
        JTextField txtCode  = new JTextField(nextCodeFromDb());
        JTextField txtName  = new JTextField();
        JTextField txtCat   = new JTextField("General");
        JTextField txtPrice = new JTextField("0");
        JTextField txtExp   = new JTextField(""); 
        JSpinner spQty      = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
        JTextField txtReason= new JTextField("Compra a proveedor");

        
        JPanel p1 = form2("Código:", txtCode, "Nombre:", txtName);
        JPanel p2 = form2("Categoría:", txtCat, "Precio:", txtPrice);
        JPanel p3 = form2("Vencimiento (AAAA-MM-DD):", txtExp, "Cantidad (+):", spQty);
        JPanel p4 = form2("Motivo:", txtReason, " ", new JLabel("<html><i>Si el código existe, se usará ese producto.</i></html>"));
        JPanel wrap = new JPanel(new GridLayout(0,1,4,4));
        wrap.add(p1); wrap.add(p2); wrap.add(p3); wrap.add(p4);

        int ok = JOptionPane.showConfirmDialog(this, wrap, "Entrada de stock (crear o usar existente)", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        String code = txtCode.getText().trim();
        if (code.isEmpty()) { warn("El código es obligatorio."); return; }

        Product prod = findByCode(code); 

       
        if (prod == null) {
            String name = txtName.getText().trim();
            if (name.isEmpty()) { warn("El nombre es obligatorio para nuevos productos."); return; }
            String cat  = txtCat.getText().trim();
            int price   = parseIntSafe(txtPrice.getText(), -1);
            if (price < 0) { warn("Precio inválido."); return; }

            LocalDate exp = null;
            String expRaw = txtExp.getText().trim();
            if (!expRaw.isEmpty()) {
                try { exp = LocalDate.parse(expRaw); }
                catch (Exception ex) { warn("Fecha de vencimiento inválida (usa AAAA-MM-DD)."); return; }
            }

            prod = new Product(code, name, cat.isEmpty() ? "General" : cat, price, 0, exp);
            dao.insert(prod); 

           
            if (prod.getId() == 0) {
                Product db = dao.findByCode(prod.getCode());
                if (db != null) prod.setId(db.getId());
            }
        }

        int qty = (int) spQty.getValue();
        String reason = txtReason.getText().isBlank() ? "Entrada" : txtReason.getText().trim();

       
        InventoryMovement m = InventoryMovement.entry(prod, qty, reason, currentUser);
        m.applyToProduct(); 

        
        movementDao.insert(
                prod.getCode(), "ENTRY", qty,
                m.getPreviousStock(), m.getResultingStock(),
                reason, currentUser, LocalDateTime.now()
        );

       
        dao.update(prod);

        info("Entrada aplicada al producto " + prod.getName()
                + ". Stock: " + m.getPreviousStock() + " → " + m.getResultingStock());

        recargar();
    }

    private void onSalida() {
        Product p = getSeleccionado();
        if (p == null) { warn("Selecciona un producto."); return; }

        JSpinner spQty      = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
        JTextField txtReason= new JTextField("Salida manual");
        JPanel form = new JPanel(new GridLayout(0,2,6,6));
        form.add(new JLabel("Cantidad (−):")); form.add(spQty);
        form.add(new JLabel("Motivo:"));       form.add(txtReason);

        int ok = JOptionPane.showConfirmDialog(this, form, "Salida de stock", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        int qty = (int) spQty.getValue();
        if (qty > p.getStock()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "La cantidad supera el stock actual (" + p.getStock() + ").\n¿Continuar? (quedará en 0)",
                    "Advertencia", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
        }
        String reason = txtReason.getText().isBlank() ? "Salida" : txtReason.getText().trim();

        InventoryMovement m = InventoryMovement.exit(p, qty, reason, currentUser);
        m.applyToProduct();

        // Guardar movimiento en BD
        movementDao.insert(
                p.getCode(), "EXIT", qty,
                m.getPreviousStock(), m.getResultingStock(),
                reason, currentUser, LocalDateTime.now()
        );

        // Persistir UPDATE tras salida
        dao.update(p);

        info("Salida aplicada.\nStock: " + m.getPreviousStock() + " → " + m.getResultingStock());
        recargar();
    }

    /** AJUSTE que también permite EDITAR el producto (nombre, categoría, precio, vencimiento). */
    private void onAjuste() {
        Product p = getSeleccionado();
        if (p == null) { warn("Selecciona un producto."); return; }

        // Campos editables de PRODUCTO
        JTextField txtName = new JTextField(p.getName());
        JTextField txtCat  = new JTextField(p.getCategory());
        JSpinner spPrice   = new JSpinner(new SpinnerNumberModel(p.getPrice(), 0, 1_000_000_000, 1));
        JTextField txtExp  = new JTextField(p.getExpiry() == null ? "" : p.getExpiry().toString()); // AAAA-MM-DD

        // Campos del AJUSTE
        JSpinner spNew     = new JSpinner(new SpinnerNumberModel(p.getStock(), 0, 1_000_000, 1));
        JTextField txtReason = new JTextField("Ajuste / Edición");

        // Formulario
        JPanel form = new JPanel(new GridLayout(0,2,6,6));
        form.add(new JLabel("Código:"));          form.add(new JLabel(p.getCode()));      // no editable
        form.add(new JLabel("Nombre:"));          form.add(txtName);
        form.add(new JLabel("Categoría:"));       form.add(txtCat);
        form.add(new JLabel("Precio:"));          form.add(spPrice);
        form.add(new JLabel("Vencimiento (AAAA-MM-DD):")); form.add(txtExp);
        form.add(new JLabel("Nuevo stock:"));     form.add(spNew);
        form.add(new JLabel("Motivo:"));          form.add(txtReason);

        int ok = JOptionPane.showConfirmDialog(this, form, "Ajuste de stock y edición de producto",
                JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        // Validaciones
        String name = txtName.getText().trim();
        if (name.isEmpty()) { warn("El nombre no puede estar vacío."); return; }
        String cat = txtCat.getText().trim();
        int price = (Integer) spPrice.getValue();

        LocalDate exp = null;
        String expRaw = txtExp.getText().trim();
        if (!expRaw.isEmpty()) {
            try { exp = LocalDate.parse(expRaw); }
            catch (Exception ex) { warn("Fecha inválida. Usa AAAA-MM-DD."); return; }
        }

        int newStock = (Integer) spNew.getValue();
        String reason = txtReason.getText().isBlank() ? "Ajuste/Edición" : txtReason.getText().trim();

        // Detectar cambios
        boolean productChanged =
                !name.equals(p.getName()) ||
                !cat.equals(p.getCategory()) ||
                price != p.getPrice() ||
                !Objects.equals(exp, p.getExpiry());

        boolean stockChanged = newStock != p.getStock();

        // Aplicar cambios del producto
        if (productChanged) {
            p.setName(name);
            p.setCategory(cat);
            p.setPrice(price);
            p.setExpiry(exp);
        }

        // Aplicar ajuste de stock (con movimiento) si corresponde
        if (stockChanged) {
            InventoryMovement m = InventoryMovement.adjustment(p, newStock, reason, currentUser);
            m.applyToProduct();

            // Guardar movimiento en BD
            movementDao.insert(
                    p.getCode(), "ADJUST", Math.abs(newStock - m.getPreviousStock()),
                    m.getPreviousStock(), m.getResultingStock(),
                    reason, currentUser, LocalDateTime.now()
            );
        }

        // Persistir y refrescar
        dao.update(p);
        recargar();

        // Feedback
        if (productChanged && stockChanged) {
            info("Producto actualizado y ajuste aplicado. Nuevo stock: " + newStock);
        } else if (productChanged) {
            info("Producto actualizado (sin cambio de stock).");
        } else if (stockChanged) {
            info("Ajuste aplicado. Nuevo stock: " + newStock);
        } else {
            info("No hubo cambios.");
        }
    }

    // ==== eliminar producto ====
    private void onDelete() {
        Product p = getSeleccionado();
        if (p == null) { warn("Selecciona un producto."); return; }

        String extra = p.getStock() > 0
                ? "\n\n⚠️ El producto tiene stock (" + p.getStock() + "). Se registrará un movimiento DELETE a stock 0."
                : "";
        int r = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar el producto \"" + p.getName() + "\" (" + p.getCode() + ")?"
                        + "\nEsta acción no se puede deshacer." + extra,
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION
        );
        if (r != JOptionPane.YES_OPTION) return;

        // Guardar movimiento DELETE (prev_stock -> 0)
        movementDao.insert(
                p.getCode(), "DELETE", Math.max(0, p.getStock()),
                p.getStock(), 0,
                "Eliminación de producto", currentUser, LocalDateTime.now()
        );

        // Eliminar en BD
        if (p.getId() > 0) {
            dao.delete(p.getId());
        } else {
            Product db = dao.findByCode(p.getCode());
            if (db != null) dao.delete(db.getId());
        }

        info("Producto eliminado.");
        recargar();
    }

    // ==== abrir historial de movimientos ====
    private void openHistorial() {
        String preset = "";
        Product sel = getSeleccionado();
        if (sel != null) preset = sel.getCode();

        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Historial de Movimientos", Dialog.ModalityType.MODELESS);
        d.setContentPane(new MovementsPanel(preset)); // pasa el código seleccionado
        d.setSize(900, 500);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    // ================== Tabla ==================

    private static class ProductosModel extends AbstractTableModel {
        private final String[] cols = {"Código", "Nombre", "Categoría", "Precio", "Stock", "Estado", "Vence"};
        private List<Product> data = List.of();

        public void set(List<Product> rows) { data = Objects.requireNonNullElse(rows, List.of()); fireTableDataChanged(); }
        public Product getAt(int row) { return data.get(row); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Product p = data.get(r);
            switch (c) {
                case 0: return p.getCode();
                case 1: return p.getName();
                case 2: return p.getCategory();
                case 3: return "$" + p.getPrice();
                case 4: return p.getStock();
                case 5: return p.getStock() > 0 ? "OK" : "SIN STOCK";
                case 6: {
                    LocalDate exp = p.getExpiry();
                    return exp == null ? "-" : DF.format(exp);
                }
                default: return "";
            }
        }
    }
}
