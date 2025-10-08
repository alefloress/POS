package pos.ui.dialogs;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialogo modal para buscar productos por nombre/codigo y seleccionar uno.
 * - Filtro en vivo
 * - Doble click o Enter: selecciona
 * - ESC o Cancel: cierra sin seleccionar
 *
 * Uso:
 *   ProductSearchDialog dlg = new ProductSearchDialog(parent, productos);
 *   dlg.setVisible(true);
 *   Product seleccionado = dlg.getSelectedProduct();
 */
public class ProductSearchDialog extends JDialog {

    public static class Product {
        private final String id;       // o codigo
        private final String name;
        private final BigDecimal price;

        public Product(String id, String name, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public BigDecimal getPrice() { return price; }
    }

    private final JTextField txtBuscar = new JTextField();
    private final JTable tbl = new JTable();
    private final JButton btnAceptar = new JButton("Elegir (Enter)");
    private final JButton btnCancelar = new JButton("Cancelar (Esc)");

    private final List<Product> allProducts;
    private final List<Product> filtered = new ArrayList<>();
    private Product selected;

    public ProductSearchDialog(Window parent, List<Product> products) {
        super(parent, "Buscar producto (F2)", ModalityType.APPLICATION_MODAL);
        this.allProducts = products != null ? products : new ArrayList<>();
        this.filtered.addAll(this.allProducts);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(720, 480);
        setLocationRelativeTo(parent);

        initUI();
        wireEvents();
    }

    public Product getSelectedProduct() {
        return selected;
    }

    private void initUI() {
        // Top: búsqueda
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JLabel lbl = new JLabel("Buscar por nombre o código:");
        top.add(lbl, BorderLayout.WEST);
        top.add(txtBuscar, BorderLayout.CENTER);

        // Center: tabla
        tbl.setModel(new ProductTableModel(filtered));
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sp = new JScrollPane(tbl);

        // Bottom: acciones
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bottom.add(btnCancelar);
        bottom.add(btnAceptar);

        // Layout
        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void wireEvents() {
        // Filtro en vivo
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refiltrar(); }
            public void removeUpdate(DocumentEvent e) { refiltrar(); }
            public void changedUpdate(DocumentEvent e) { refiltrar(); }
        });

        // Enter = aceptar si hay selección
        getRootPane().setDefaultButton(btnAceptar);

        // Doble click en la tabla = aceptar
        tbl.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tbl.getSelectedRow() >= 0) {
                    aceptarSeleccion();
                }
            }
        });

        // Enter en tabla = aceptar
        tbl.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && tbl.getSelectedRow() >= 0) {
                    e.consume(); // evita salto de fila
                    aceptarSeleccion();
                }
            }
        });

        // Botones
        btnAceptar.addActionListener(e -> aceptarSeleccion());
        btnCancelar.addActionListener(e -> {
            selected = null;
            dispose();
        });

        // ESC para cerrar
        getRootPane().registerKeyboardAction(e -> {
            selected = null;
            dispose();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void aceptarSeleccion() {
        int viewRow = tbl.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = tbl.convertRowIndexToModel(viewRow);
        ProductTableModel m = (ProductTableModel) tbl.getModel();
        selected = m.getAt(modelRow);
        dispose();
    }

    private void refiltrar() {
        String q = txtBuscar.getText().trim().toLowerCase();
        filtered.clear();
        if (q.isEmpty()) {
            filtered.addAll(allProducts);
        } else {
            for (Product p : allProducts) {
                boolean match = (p.getId() != null && p.getId().toLowerCase().contains(q))
                        || (p.getName() != null && p.getName().toLowerCase().contains(q));
                if (match) filtered.add(p);
            }
        }
        ((AbstractTableModel) tbl.getModel()).fireTableDataChanged();
        if (!filtered.isEmpty()) {
            tbl.setRowSelectionInterval(0, 0);
        }
    }

    private static class ProductTableModel extends AbstractTableModel {
        private final String[] cols = {"Código", "Nombre", "Precio"};
        private final List<Product> data;
        ProductTableModel(List<Product> data) { this.data = data; }

        public int getRowCount() { return data.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }
        public boolean isCellEditable(int r, int c) { return false; }

        public Object getValueAt(int r, int c) {
            Product p = data.get(r);
            switch (c) {
                case 0: return p.getId();
                case 1: return p.getName();
                case 2: return formatCurrency(p.getPrice());
                default: return "";
            }
        }

        public Product getAt(int r) { return data.get(r); }

        private static String formatCurrency(BigDecimal v) {
            if (v == null) return "";
            // Formato CLP sin decimales (ajusta si usas otra moneda)
            return "$" + v.setScale(0, BigDecimal.ROUND_HALF_UP).toPlainString();
        }
    }
}
