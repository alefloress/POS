package pos.ui.views;

import pos.model.Product;
import pos.model.Sale;
import pos.model.SaleItem;
import pos.repo.SaleRepo;

// SQLite DAOs
import pos.dao.InventoryDao;
import pos.dao.MovementDao;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.Locale;

public class CajeroPanel extends JPanel {

    // --- Búsqueda inventario ---
    private final JTextField txtBuscarCodigo = new JTextField(14);
    private final JTextField txtBuscarNombre = new JTextField(16);
    private final JTable tblInv = new JTable(new InvModel());

    // --- Carrito / pago ---
    private final ItemsModel itemsModel = new ItemsModel();
    private final JTable tblCarrito = new JTable(itemsModel);
    private final JLabel lblTotal = new JLabel("$0");
    private final JTextField txtPagaCon = new JTextField(10);
    private final JLabel lblVuelto = new JLabel("$0");
    private final JComboBox<String> cbPago =
            new JComboBox<>(new String[]{"Efectivo","Tarjeta","Transferencia","Mixto"});

    private final NumberFormat CLP =
            NumberFormat.getCurrencyInstance(new Locale("es","CL"));
    private static long FOLIO = 10000;

    // Usuario actual (ajusta si tienes login real)
    private final String currentUser = "cajero";

    // DAOs BD
    private final InventoryDao inventoryDao = new InventoryDao();
    private final MovementDao  movementDao  = new MovementDao();

    public CajeroPanel() {
        setLayout(new BorderLayout(10,10));
        setBackground(new Color(0xF3F4F6));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        split.setLeftComponent(buildLeft());
        split.setRightComponent(buildRight());
        add(split, BorderLayout.CENTER);

        // Carga inventario desde BD
        ((InvModel)tblInv.getModel()).reload();

        // Doble click inventario -> agrega al carrito
        tblInv.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount()==2) agregarProductoSeleccionado();
            }
        });

        cbPago.addActionListener(e -> togglePagoFields());
        togglePagoFields();
    }

    private JComponent buildLeft() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        p.setBorder(BorderFactory.createEmptyBorder(8,8,8,4));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        JButton btnBuscar = new JButton("Buscar");
        JButton btnTodos  = new JButton("Todos");
        top.add(new JLabel("Código:"));  top.add(txtBuscarCodigo);
        top.add(new JLabel("Nombre:"));  top.add(txtBuscarNombre);
        top.add(btnBuscar); top.add(btnTodos);
        p.add(top, BorderLayout.NORTH);

        tblInv.setRowHeight(24);
        p.add(new JScrollPane(tblInv), BorderLayout.CENTER);

        btnBuscar.addActionListener(e -> {
            String c = txtBuscarCodigo.getText().trim();
            String n = txtBuscarNombre.getText().trim();
            ((InvModel)tblInv.getModel()).search(c, n);
        });
        btnTodos.addActionListener(e -> {
            txtBuscarCodigo.setText(""); txtBuscarNombre.setText("");
            ((InvModel)tblInv.getModel()).reload();
        });
        return p;
    }

    private JComponent buildRight() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        p.setBorder(BorderFactory.createEmptyBorder(8,4,8,8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        JButton btnNuevo = new JButton("Nuevo");
        JButton btnServicio = new JButton("Servicio/Precio abierto");
        top.add(btnNuevo); top.add(btnServicio);
        p.add(top, BorderLayout.NORTH);

        tblCarrito.setRowHeight(26);
        tblCarrito.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p.add(new JScrollPane(tblCarrito), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8,8));

        JPanel tot = new JPanel(new GridLayout(0,2,8,4));
        tot.setBorder(BorderFactory.createTitledBorder("Totales"));
        tot.add(new JLabel("TOTAL:")); tot.add(lblTotal);

        JPanel pago = new JPanel(new GridLayout(0,2,8,4));
        pago.setBorder(BorderFactory.createTitledBorder("Pago"));
        pago.add(new JLabel("Medio:"));      pago.add(cbPago);
        pago.add(new JLabel("Paga con:"));   pago.add(txtPagaCon);
        pago.add(new JLabel("Vuelto:"));     pago.add(lblVuelto);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        JButton btnQuitar = new JButton("Quitar ítem");
        JButton btnCobrar = new JButton("Cobrar");
        JButton btnImprimir = new JButton("Imprimir");
        acciones.add(btnQuitar); acciones.add(btnCobrar); acciones.add(btnImprimir);

        bottom.add(tot, BorderLayout.NORTH);
        bottom.add(pago, BorderLayout.CENTER);
        bottom.add(acciones, BorderLayout.SOUTH);
        p.add(bottom, BorderLayout.SOUTH);

        btnNuevo.addActionListener(e -> { itemsModel.clear(); recalcTotales(); });
        btnServicio.addActionListener(e -> agregarServicioManual());
        btnQuitar.addActionListener(e -> quitarSeleccion());
        btnCobrar.addActionListener(e -> cobrar());
        btnImprimir.addActionListener(e -> imprimirTicketSolo());

        return p;
    }

    private void togglePagoFields(){
        String tipo = (String) cbPago.getSelectedItem();
        boolean efectivo = "Efectivo".equals(tipo);
        txtPagaCon.setEnabled(efectivo);
        lblVuelto.setEnabled(efectivo);
        recalcTotales();
    }

    private void agregarProductoSeleccionado() {
        int r = tblInv.getSelectedRow();
        if (r<0) return;
        Product p = ((InvModel)tblInv.getModel()).getAt(tblInv.convertRowIndexToModel(r));
        if (p.getStock()<=0){ JOptionPane.showMessageDialog(this,"Sin stock."); return; }
        itemsModel.addOrInc(p,1);
        recalcTotales();
    }

    private void agregarServicioManual() {
        String desc = JOptionPane.showInputDialog(this,"Descripción del servicio:");
        if (desc==null || desc.isBlank()) return;
        String precioStr = JOptionPane.showInputDialog(this,"Precio (CLP):","0");
        if (precioStr==null) return;
        int precio;
        try { precio = Math.max(0, Integer.parseInt(precioStr.trim())); }
        catch (Exception ex){ JOptionPane.showMessageDialog(this,"Precio inválido"); return; }
        Product temp = new Product("SERV-"+System.currentTimeMillis(), desc, "SERVICIO", precio, 1, null);
        itemsModel.addOrInc(temp,1);
        recalcTotales();
    }

    private void quitarSeleccion(){
        int r = tblCarrito.getSelectedRow();
        if (r<0) return;
        itemsModel.removeAt(tblCarrito.convertRowIndexToModel(r));
        recalcTotales();
    }

    private void recalcTotales(){
        int total = itemsModel.total();
        lblTotal.setText(CLP.format(total));
        if (txtPagaCon.isEnabled()) {
            try {
                int paga = Integer.parseInt(txtPagaCon.getText().trim());
                int vuelto = Math.max(0, paga - total);
                lblVuelto.setText(CLP.format(vuelto));
            } catch (Exception e) {
                lblVuelto.setText("$0");
            }
        } else {
            lblVuelto.setText("$0");
        }
    }

    private void imprimirTicketSolo() {
        if (itemsModel.getRowCount()==0){ JOptionPane.showMessageDialog(this,"Carrito vacío."); return; }
        long folio = FOLIO + 1;
        imprimir(itemsModel.getItems(), "Boleta", folio, null, (String)cbPago.getSelectedItem(), itemsModel.total(), LocalDateTime.now());
    }

    private void cobrar() {
        if (itemsModel.getRowCount()==0) { JOptionPane.showMessageDialog(this,"Carrito vacío."); return; }

        String tipoDoc = "Boleta";
        String medio = (String) cbPago.getSelectedItem();

        int total = itemsModel.total();
        int ef=0,tj=0,tr=0;

        if ("Efectivo".equals(medio)) {
            try {
                int paga = Integer.parseInt(txtPagaCon.getText().trim());
                if (paga < total) { JOptionPane.showMessageDialog(this,"Monto insuficiente."); return; }
                ef = total;
            } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Paga con inválido."); return; }
        } else if ("Tarjeta".equals(medio)) {
            tj = total;
        } else if ("Transferencia".equals(medio)) {
            tr = total;
        } else {
            try {
                String sEf = JOptionPane.showInputDialog(this,"Monto Efectivo:","0");
                String sTj = JOptionPane.showInputDialog(this,"Monto Tarjeta:","0");
                String sTr = JOptionPane.showInputDialog(this,"Monto Transferencia:","0");
                ef = parseInt(sEf); tj = parseInt(sTj); tr = parseInt(sTr);
                if (ef+tj+tr != total) { JOptionPane.showMessageDialog(this,"La suma debe ser igual al total."); return; }
            } catch (Exception e) { return; }
        }

        // Generar folio antes (para usarlo en motivo)
        long folio = ++FOLIO;

        // === Descontar stock en BD y registrar movimientos EXIT ===
        for (Item it : itemsModel.getItems()) {
            String code = it.product.getCode();
            int qty = it.qty;

            Product dbp = inventoryDao.findByCode(code);
            if (dbp == null) {
                System.err.println("Producto no encontrado en BD: " + code);
                continue;
            }

            int prev  = dbp.getStock();
            int nuevo = prev - qty;
            if (nuevo < 0) nuevo = 0;

            dbp.setStock(nuevo);
            inventoryDao.update(dbp);

            movementDao.insert(
                dbp.getCode(),
                "EXIT",
                qty,
                prev,
                nuevo,
                "Venta POS #" + folio,
                currentUser,
                LocalDateTime.now()
            );
        }

        // Refrescar inventario mostrado
        ((InvModel)tblInv.getModel()).reload();

        // Guardar venta (lógico original)
        List<SaleItem> repoItems = new ArrayList<>();
        for (Item it : itemsModel.getItems()) {
            Product p = it.product;
            Product copy = new Product(p.getCode(), p.getName(), p.getCategory(), p.getPrice(), p.getStock(), p.getExpiry());
            repoItems.add(new SaleItem(copy, it.qty));
        }
        int neto = (int)Math.round(total / 1.19);
        int iva  = total - neto;
        SaleRepo.get().save(new Sale(
                String.valueOf(folio),
                tipoDoc,
                LocalDateTime.now(),
                repoItems,
                medio,
                ef,tj,tr,
                neto, iva, total,
                null
        ));

        // Ticket e interfaz
        imprimir(itemsModel.getItems(), tipoDoc, folio, null, medio, total, LocalDateTime.now());
        itemsModel.clear(); recalcTotales();
        JOptionPane.showMessageDialog(this, tipoDoc+" emitida.\nFolio: "+folio+"\nTotal: "+CLP.format(total));
    }

    private void imprimir(List<Item> items, String tipo, long folio, String rut, String medio, int total, LocalDateTime ts){
        StringBuilder sb = new StringBuilder();
        sb.append(tipo).append(" N° ").append(folio).append(" — ").append(ts).append("\n");
        if (rut != null) sb.append("Cliente: ").append(rut).append("\n");
        sb.append("--------------------------------\n");
        for (Item it : items) {
            sb.append(it.product.getName()).append(" x").append(it.qty)
              .append("  ").append(CLP.format(it.product.getPrice() * it.qty)).append("\n");
        }
        int neto = (int)Math.round(total / 1.19);
        int iva  = total - neto;
        sb.append("--------------------------------\n");
        sb.append("Neto: ").append(CLP.format(neto)).append("\n");
        sb.append("IVA 19%: ").append(CLP.format(iva)).append("\n");
        sb.append("TOTAL: ").append(CLP.format(total)).append("\n");
        sb.append("Pago: ").append(medio).append("\n");

        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        try { ta.print(); } catch (Exception ignore) {}
    }

    private int parseInt(String s){ return (s==null||s.isBlank())?0:Integer.parseInt(s.trim()); }

    // ===== Modelos =====
    private class InvModel extends AbstractTableModel {
        private final String[] cols = {"Código","Nombre","Categoría","Precio","Stock"};
        private java.util.List<Product> data = new ArrayList<>();

        void reload(){
            data = new ArrayList<>(inventoryDao.listAll());
            fireTableDataChanged();
        }
        void search(String code, String name){
            List<Product> base = inventoryDao.listAll();
            data = new ArrayList<>();
            for (Product p : base) {
                boolean ok = true;
                if (!code.isBlank())  ok &= p.getCode() != null && p.getCode().toLowerCase().contains(code.toLowerCase());
                if (!name.isBlank())  ok &= p.getName() != null && p.getName().toLowerCase().contains(name.toLowerCase());
                if (ok) data.add(p);
            }
            fireTableDataChanged();
        }
        Product getAt(int r){ return data.get(r); }
        @Override public int getRowCount(){ return data.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Class<?> getColumnClass(int c){ return (c==3||c==4)? Integer.class : String.class; }
        @Override public Object getValueAt(int r, int c){
            Product p = data.get(r);
            return switch (c){
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> p.getCategory();
                case 3 -> p.getPrice();
                case 4 -> p.getStock();
                default -> "";
            };
        }
    }

    private static class Item { final Product product; int qty; Item(Product p,int q){ product=p; qty=Math.max(1,q);} }
    private static class ItemsModel extends AbstractTableModel {
        private final String[] cols = {"Código","Nombre","Precio","Cantidad","Subtotal"};
        private final java.util.List<Item> data = new ArrayList<>();
        @Override public int getRowCount(){ return data.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Class<?> getColumnClass(int c){ return (c==2||c==3||c==4)? Integer.class : String.class; }
        @Override public boolean isCellEditable(int r,int c){ return c==3; }
        @Override public Object getValueAt(int r,int c){
            Item it = data.get(r); Product p = it.product;
            return switch (c){
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> p.getPrice();
                case 3 -> it.qty;
                case 4 -> p.getPrice()*it.qty;
                default -> "";
            };
        }
        @Override public void setValueAt(Object v,int r,int c){
            if (c==3){
                try{ int q = Math.max(1, Integer.parseInt(String.valueOf(v).trim())); data.get(r).qty=q; fireTableRowsUpdated(r,r);}
                catch(Exception ignore){ }
            }
        }
        void addOrInc(Product p, int q){
            for (int i=0;i<data.size();i++){
                if (data.get(i).product.getCode().equals(p.getCode())){
                    data.get(i).qty += q; fireTableRowsUpdated(i,i); return;
                }
            }
            data.add(new Item(p,q)); fireTableRowsInserted(data.size()-1,data.size()-1);
        }
        void removeAt(int modelRow){
            if (modelRow<0||modelRow>=data.size()) return;
            data.remove(modelRow); fireTableRowsDeleted(modelRow,modelRow);
        }
        void clear(){ int n=data.size(); if (n==0) return; data.clear(); fireTableRowsDeleted(0,n-1); }
        int total(){ return data.stream().mapToInt(it-> it.product.getPrice()*it.qty).sum(); }
        List<Item> getItems(){ return new ArrayList<>(data); }
    }
}
