package pos.ui.views;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.Locale;

public class VentasPanel extends JPanel {

    // ====== Tipos internos ======
    private static class Product {
        private String code;
        private String name;
        private String category;
        private int price; // CLP
        private int stock;

        Product(String code, String name, String category, int price, int stock) {
            this.code = code;
            this.name = name;
            this.category = category;
            this.price = price;
            this.stock = stock;
        }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public int getPrice() { return price; }
        public void setPrice(int price) { this.price = price; }
        public int getStock() { return stock; }
        public void setStock(int s) { this.stock = Math.max(0, s); }
    }

    private static class SaleItem {
        private final Product product;
        private int qty;
        SaleItem(Product product, int qty) {
            this.product = product;
            this.qty = Math.max(0, qty);
        }
        public Product getProduct() { return product; }
        public int getQty() { return qty; }
        public void setQty(int q) { this.qty = Math.max(0, q); }
        public int getSubtotal() { return product.getPrice() * qty; }
    }

    private static class ItemsModel extends AbstractTableModel {
        private final String[] cols = {"Código", "Nombre", "Precio", "Cantidad", "Subtotal"};
        private final java.util.List<SaleItem> data = new ArrayList<>();

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 2,3,4 -> Integer.class;
                default -> String.class;
            };
        }
        @Override public boolean isCellEditable(int r, int c) { return c == 3; }

        @Override public Object getValueAt(int r, int c) {
            SaleItem it = data.get(r);
            Product p = it.getProduct();
            return switch (c) {
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> p.getPrice();
                case 3 -> it.getQty();
                case 4 -> it.getSubtotal();
                default -> "";
            };
        }

        @Override public void setValueAt(Object v, int r, int c) {
            if (c == 3) {
                try {
                    int q = Integer.parseInt(String.valueOf(v));
                    if (q < 0) q = 0;
                    data.get(r).setQty(q);
                    fireTableRowsUpdated(r, r);
                } catch (Exception ignored) { }
            }
        }

        public void addOrIncrement(Product p, int qty) {
            for (int i = 0; i < data.size(); i++) {
                SaleItem it = data.get(i);
                if (it.getProduct().getCode().equals(p.getCode())) {
                    it.setQty(it.getQty() + qty);
                    fireTableRowsUpdated(i, i);
                    return;
                }
            }
            data.add(new SaleItem(p, qty));
            int idx = data.size() - 1;
            fireTableRowsInserted(idx, idx);
        }

        public void removeAt(int modelRow) {
            if (modelRow < 0 || modelRow >= data.size()) return;
            data.remove(modelRow);
            fireTableRowsDeleted(modelRow, modelRow);
        }

        public void clear() {
            int n = data.size();
            if (n == 0) return;
            data.clear();
            fireTableRowsDeleted(0, n - 1);
        }

        public java.util.List<SaleItem> getItems() {
            return new ArrayList<>(data);
        }
    }
    // ====== Fin tipos internos ======

    // UI
    private JTextField txtCodigo;
    private JButton btnServicio, btnCobrar, btnAdmin;
    private JTable tbl;
    private final JLabel lblInfo = new JLabel("Listo.");

    // Modelo
    private final ItemsModel itemsModel = new ItemsModel();
    private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es","CL"));

    // Inventario (local al panel; se carga desde InMemoryStore)
    private final Map<String, Product> inventory = new HashMap<>();

    // Folio simple para documentos (demo)
    private static long FOLIO = 10000;

    public VentasPanel() {
        setLayout(new BorderLayout(10,10));

        // ===== Barra superior =====
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        txtCodigo = new JTextField(16);
        txtCodigo.setToolTipText("Escanea o escribe el código y presiona Enter");

        JButton btnAgregar = new JButton("➕ Agregar");
        btnServicio = new JButton("🧾 Servicio/Precio abierto");
        btnAdmin = new JButton("⚙️ Productos (CRUD)");

        barra.add(new JLabel("Código:"));
        barra.add(txtCodigo);
        barra.add(btnAgregar);
        barra.add(btnServicio);
        barra.add(btnAdmin);

        add(barra, BorderLayout.NORTH);

        // ===== Tabla =====
        tbl = new JTable(itemsModel);
        tbl.setRowHeight(26);
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tbl), BorderLayout.CENTER);

        // ===== Pie =====
        JPanel pie = new JPanel(new BorderLayout());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnQuitar = new JButton("🗑 Quitar ítem");
        btnCobrar = new JButton("💵 Cobrar (F9)");
        btnCobrar.setMnemonic(KeyEvent.VK_F9);
        acciones.add(btnQuitar);
        acciones.add(btnCobrar);
        pie.add(lblInfo, BorderLayout.WEST);
        pie.add(acciones, BorderLayout.EAST);
        add(pie, BorderLayout.SOUTH);

        // ===== Acciones =====
        txtCodigo.addActionListener(e -> agregarPorCodigo());
        btnAgregar.addActionListener(e -> agregarPorCodigo());
        btnServicio.addActionListener(e -> agregarServicioManual());
        btnQuitar.addActionListener(e -> quitarSeleccion());
        btnCobrar.addActionListener(e -> cobrar());
        btnAdmin.addActionListener(e -> abrirAdminProductos());

        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "del");
        getActionMap().put("del", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { quitarSeleccion(); }
        });

        // ===== Cargar inventario desde el store global =====
        precargarInventarioDesdeStore();
    }

    // --------- NUEVO: carga inicial desde InMemoryStore ----------
    private void precargarInventarioDesdeStore() {
        java.util.List<pos.model.Product> lista = pos.store.InMemoryStore.allProducts();
        inventory.clear();
        for (pos.model.Product pr : lista) {
            inventory.put(pr.getCode(),
                new Product(pr.getCode(), pr.getName(), pr.getCategory(), pr.getPrice(), pr.getStock()));
        }
    }

    // ===== Registro rápido por código (escáner) =====
    private void agregarPorCodigo() {
        String code = txtCodigo.getText().trim();
        if (code.isEmpty()) return;

        Product p = inventory.get(code);
        if (p == null) {
            // Buscar en store global (por si se agregó desde otro panel)
            java.util.Optional<pos.model.Product> opt = pos.store.InMemoryStore.findByCode(code);
            if (opt.isPresent()) {
                pos.model.Product real = opt.get();
                p = new Product(real.getCode(), real.getName(), real.getCategory(), real.getPrice(), real.getStock());
                inventory.put(p.getCode(), p);
            }
        }

        if (p == null) {
            info("Código no encontrado: " + code);
            JOptionPane.showMessageDialog(this, "Código no encontrado: " + code);
            return;
        }
        if (p.getStock() <= 0) {
            info("Sin stock: " + p.getName());
            JOptionPane.showMessageDialog(this, "Sin stock para: " + p.getName());
            return;
        }
        itemsModel.addOrIncrement(p, 1);
        txtCodigo.setText("");
        txtCodigo.requestFocusInWindow();
        info("Agregado: " + p.getName());
    }

    // ===== Servicio / precio abierto =====
    private void agregarServicioManual() {
        String desc = JOptionPane.showInputDialog(this, "Descripción del servicio:");
        if (desc == null || desc.isBlank()) return;

        String precioStr = JOptionPane.showInputDialog(this, "Precio (CLP):");
        if (precioStr == null || precioStr.isBlank()) return;

        int precio;
        try { precio = Integer.parseInt(precioStr.trim()); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this, "Precio inválido"); return; }

        Product temp = new Product("SERV-" + System.currentTimeMillis(), desc, "SERVICIO", precio, 1);
        itemsModel.addOrIncrement(temp, 1);
        info("Servicio agregado: " + desc);
    }

    private void quitarSeleccion() {
        int row = tbl.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un ítem."); return; }
        int modelRow = tbl.convertRowIndexToModel(row);
        itemsModel.removeAt(modelRow);
        info("Ítem eliminado.");
    }

    // ===== Cobro: boleta/factura + medios de pago =====
    private void cobrar() {
        if (itemsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay ítems en la venta."); return;
        }

        List<SaleItem> items = itemsModel.getItems();
        int total = items.stream().mapToInt(SaleItem::getSubtotal).sum();

        // 1) Tipo de documento
        String[] tipos = {"Boleta", "Factura"};
        String tipoDoc = (String) JOptionPane.showInputDialog(this,
                "Tipo de documento:", "Documento",
                JOptionPane.QUESTION_MESSAGE, null, tipos, tipos[0]);
        if (tipoDoc == null) return;

        String rut = null;
        if ("Factura".equals(tipoDoc)) {
            rut = JOptionPane.showInputDialog(this, "RUT/Razón Social (opcional):");
            if (rut != null && rut.isBlank()) rut = null;
        }

        // 2) Medio(s) de pago
        String[] pagos = {"Efectivo", "Tarjeta", "Transferencia", "Mixto"};
        String medioPago = (String) JOptionPane.showInputDialog(
                this, "Medio de pago:", "Cobro",
                JOptionPane.QUESTION_MESSAGE, null, pagos, pagos[0]
        );
        if (medioPago == null) return;

        int ef=0, tj=0, tr=0;
        if ("Mixto".equals(medioPago)) {
            try {
                String sEf = JOptionPane.showInputDialog(this, "Monto en Efectivo (CLP):", "0");
                String sTj = JOptionPane.showInputDialog(this, "Monto en Tarjeta (CLP):", "0");
                String sTr = JOptionPane.showInputDialog(this, "Monto en Transferencia (CLP):", "0");
                ef = parseIntSafe(sEf); tj = parseIntSafe(sTj); tr = parseIntSafe(sTr);
                if (ef + tj + tr != total) {
                    JOptionPane.showMessageDialog(this, "La suma de montos (" + (ef+tj+tr) + ") debe ser igual al total (" + total + ").");
                    return;
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Montos inválidos."); return; }
        }

        // 3) Desglose Neto/IVA (19%)
        int neto = (int)Math.round(total / 1.19);
        int iva  = total - neto;

        // 4) Descontar stock en inventario global (InMemoryStore) y en el mapa local
        for (SaleItem it : items) {
            String c = it.getProduct().getCode();

            // Store global
            java.util.Optional<pos.model.Product> opt = pos.store.InMemoryStore.findByCode(c);
            if (opt.isPresent()) {
                pos.model.Product pr = opt.get();
                int nuevo = Math.max(0, pr.getStock() - it.getQty());
                pr.setStock(nuevo);
                pos.store.InMemoryStore.update(pr);
            }

            // Local (panel)
            Product local = inventory.get(c);
            if (local != null) {
                local.setStock(Math.max(0, local.getStock() - it.getQty()));
            }
        }

        // 5) Folio simple
        long folio = ++FOLIO;

        // ====== GUARDAR VENTA EN HISTÓRICO (para Reportes) ======
        String customerId = ("Factura".equals(tipoDoc) && rut != null && !rut.isBlank()) ? rut.trim() : null;

        // Convertir ítems internos -> pos.model.SaleItem
        java.util.List<pos.model.SaleItem> itemsRepo = new java.util.ArrayList<>();
        for (SaleItem it : items) {
            Product p = it.getProduct();
            pos.model.Product pm = new pos.model.Product(
                    p.getCode(), p.getName(), p.getCategory(), p.getPrice(), p.getStock()
            );
            itemsRepo.add(new pos.model.SaleItem(pm, it.getQty()));
        }

        // Guardar en repositorio en memoria
        pos.repo.SaleRepo.get().save(
            new pos.model.Sale(
                String.valueOf(folio),
                tipoDoc,
                java.time.LocalDateTime.now(),
                itemsRepo,
                medioPago,
                ef, tj, tr,
                neto, iva, total,
                customerId
            )
        );
        // ================================================

        // 6) Ticket
        imprimirDocumento(items, tipoDoc, folio, rut, medioPago, ef, tj, tr, neto, iva, total);

        // 7) Limpiar carrito
        itemsModel.clear();
        info(tipoDoc + " #" + folio + " emitida. Total: " + CLP.format(total));
        JOptionPane.showMessageDialog(this, tipoDoc + " emitida.\nFolio: " + folio + "\nTotal: " + CLP.format(total));
    }

    private int parseIntSafe(String s) { return (s == null || s.isBlank()) ? 0 : Integer.parseInt(s.trim()); }

    private void imprimirDocumento(List<SaleItem> items, String tipoDoc, long folio, String rut,
                                   String medioPago, int ef, int tj, int tr,
                                   int neto, int iva, int total) {
        StringBuilder sb = new StringBuilder();
        sb.append(tipoDoc).append(" N° ").append(folio).append(" — ").append(LocalDateTime.now()).append("\n");
        if (rut != null) sb.append("Cliente: ").append(rut).append("\n");
        sb.append("--------------------------------\n");
        for (SaleItem it : items) {
            sb.append(it.getProduct().getName())
              .append(" x").append(it.getQty())
              .append("  ").append(CLP.format(it.getSubtotal()))
              .append("\n");
        }
        sb.append("--------------------------------\n");
        sb.append("Neto: ").append(CLP.format(neto)).append("\n");
        sb.append("IVA 19%: ").append(CLP.format(iva)).append("\n");
        sb.append("TOTAL: ").append(CLP.format(total)).append("\n");
        sb.append("Pago: ").append(medioPago);
        if ("Mixto".equals(medioPago)) {
            sb.append("  [Ef: ").append(CLP.format(ef))
              .append(" | Tj: ").append(CLP.format(tj))
              .append(" | Tr: ").append(CLP.format(tr)).append("]");
        }
        sb.append("\n");

        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        try { ta.print(); } catch (Exception ignore) {}
    }

    // ===== Admin de productos (CRUD) =====
    private void abrirAdminProductos() {
        new ProductAdminDialog(SwingUtilities.getWindowAncestor(this), inventory, CLP, this::info).setVisible(true);
    }

    private void info(String msg) { lblInfo.setText(msg); }

    // ====== Diálogo CRUD ======
    private static class ProductAdminDialog extends JDialog {
        private final Map<String, Product> inventory;
        private final ProductTableModel model;
        private final NumberFormat CLP;
        private final java.util.function.Consumer<String> notify;

        ProductAdminDialog(Window owner, Map<String, Product> inventory, NumberFormat CLP, java.util.function.Consumer<String> notify) {
            super(owner, "Productos — Admin", ModalityType.APPLICATION_MODAL);
            this.inventory = inventory;
            this.model = new ProductTableModel(inventory);
            this.CLP = CLP;
            this.notify = notify;
            buildUI();
            setSize(760, 440);
            setLocationRelativeTo(owner);
        }

        private void buildUI() {
            setLayout(new BorderLayout(8,8));
            JTable table = new JTable(model);
            table.setRowHeight(24);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            JButton btnAdd = new JButton("➕ Agregar");
            JButton btnEdit = new JButton("✏️ Editar");
            JButton btnDel = new JButton("🗑 Eliminar");
            JButton btnClose = new JButton("Cerrar");
            acciones.add(btnAdd); acciones.add(btnEdit); acciones.add(btnDel); acciones.add(btnClose);
            add(acciones, BorderLayout.SOUTH);

            btnAdd.addActionListener(e -> editOrCreate(null));
            btnEdit.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un producto."); return; }
                int modelRow = table.convertRowIndexToModel(row);
                Product p = model.getAt(modelRow);
                editOrCreate(p);
            });
            btnDel.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un producto."); return; }
                int modelRow = table.convertRowIndexToModel(row);
                Product p = model.getAt(modelRow);
                int ok = JOptionPane.showConfirmDialog(this, "Eliminar " + p.getName() + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) {
                    inventory.remove(p.getCode());
                    // reflejar en store real
                    pos.store.InMemoryStore.removeByCode(p.getCode());
                    model.reload();
                    notify.accept("Producto eliminado: " + p.getName());
                }
            });
            btnClose.addActionListener(e -> dispose());
        }

        private void editOrCreate(Product original) {
            JTextField fCode = new JTextField(original == null ? "" : original.getCode());
            JTextField fName = new JTextField(original == null ? "" : original.getName());
            JTextField fCat  = new JTextField(original == null ? "" : original.getCategory());
            JTextField fPrice= new JTextField(original == null ? "" : String.valueOf(original.getPrice()));
            JTextField fStock= new JTextField(original == null ? "" : String.valueOf(original.getStock()));

            JPanel form = new JPanel(new GridLayout(0,2,8,8));
            form.add(new JLabel("Código:")); form.add(fCode);
            form.add(new JLabel("Nombre:")); form.add(fName);
            form.add(new JLabel("Categoría:")); form.add(fCat);
            form.add(new JLabel("Precio (CLP):")); form.add(fPrice);
            form.add(new JLabel("Stock:")); form.add(fStock);

            int ok = JOptionPane.showConfirmDialog(this, form, original == null ? "Agregar producto" : "Editar producto", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;

            String code = fCode.getText().trim();
            String name = fName.getText().trim();
            String cat  = fCat.getText().trim();
            int price, stock;
            try {
                price = Integer.parseInt(fPrice.getText().trim());
                stock = Integer.parseInt(fStock.getText().trim());
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Precio/Stock inválidos"); return; }
            if (code.isEmpty() || name.isEmpty()) { JOptionPane.showMessageDialog(this, "Código y Nombre son obligatorios"); return; }

            if (original == null) {
                if (inventory.containsKey(code)) { JOptionPane.showMessageDialog(this, "Código ya existe."); return; }
                inventory.put(code, new Product(code, name, cat, price, stock));
                // reflejar en store real
                pos.store.InMemoryStore.upsert(new pos.model.Product(code, name, cat, price, stock));
                notify.accept("Producto agregado: " + name);
            } else {
                if (!original.getCode().equals(code) && inventory.containsKey(code)) {
                    JOptionPane.showMessageDialog(this, "Código ya existe."); return;
                }
                inventory.remove(original.getCode());
                inventory.put(code, new Product(code, name, cat, price, stock));
                // reflejar en store real
                pos.store.InMemoryStore.upsert(new pos.model.Product(code, name, cat, price, stock));
                notify.accept("Producto actualizado: " + name);
            }
            model.reload();
        }

        private static class ProductTableModel extends AbstractTableModel {
            private final String[] cols = {"Código", "Nombre", "Categoría", "Precio", "Stock"};
            private final Map<String, Product> inventory;
            private java.util.List<Product> data;

            ProductTableModel(Map<String, Product> inventory) {
                this.inventory = inventory;
                reload();
            }
            void reload() { data = new ArrayList<>(inventory.values()); fireTableDataChanged(); }
            @Override public int getRowCount() { return data.size(); }
            @Override public int getColumnCount() { return cols.length; }
            @Override public String getColumnName(int c) { return cols[c]; }
            @Override public Class<?> getColumnClass(int c) {
                return switch (c) { case 3,4 -> Integer.class; default -> String.class; };
            }
            @Override public Object getValueAt(int r, int c) {
                Product p = data.get(r);
                return switch (c) {
                    case 0 -> p.getCode();
                    case 1 -> p.getName();
                    case 2 -> p.getCategory();
                    case 3 -> p.getPrice();
                    case 4 -> p.getStock();
                    default -> "";
                };
            }
            Product getAt(int row) { return data.get(row); }
        }
    }
}

