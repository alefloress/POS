package pos.ui.views;

import pos.dao.MovementDao;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MovementsPanel extends JPanel {

    private final JTextField txtCode = new JTextField(12);
    private final JComboBox<String> cboType =
            new JComboBox<>(new String[]{"", "ENTRY", "EXIT", "ADJUST", "DELETE"});
    private final JButton btnFilter  = new JButton("Filtrar");
    private final JButton btnReset   = new JButton("Limpiar");
    private final JButton btnRefresh = new JButton("Actualizar");
    private final JButton btnExport  = new JButton("Exportar CSV");

    private final JTable table;
    private final DefaultTableModel model;

    private final MovementDao dao = new MovementDao();

    public MovementsPanel() {
        this("");
    }

    /** Permite abrir el historial ya filtrado por un código. */
    public MovementsPanel(String presetCode) {
        setLayout(new BorderLayout(8,8));

        // ====== Barra de filtros ======
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filters.add(new JLabel("Código:"));
        filters.add(txtCode);
        filters.add(new JLabel("Tipo:"));
        filters.add(cboType);
        filters.add(btnFilter);
        filters.add(btnReset);
        filters.add(btnRefresh);
        filters.add(btnExport);
        add(filters, BorderLayout.NORTH);

        // ====== Tabla ======
        model = new DefaultTableModel(new Object[]{
                "Fecha", "Tipo", "Código", "Producto", "Cantidad",
                "Stock (antes→después)", "Usuario", "Motivo"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        add(new JScrollPane(table), BorderLayout.CENTER);

        if (presetCode != null && !presetCode.isBlank()) {
            txtCode.setText(presetCode);
        }

        // ====== Listeners ======
        btnFilter.addActionListener(e -> loadData());
        btnReset.addActionListener(e -> { txtCode.setText(""); cboType.setSelectedIndex(0); loadData(); });
        btnRefresh.addActionListener(e -> loadData());
        btnExport.addActionListener(e -> exportCsv());

        // Carga inicial
        loadData();
    }

    private void loadData() {
        model.setRowCount(0);

        String code = txtCode.getText().trim();
        String type = Objects.toString(cboType.getSelectedItem(), "").trim();

        // Si hay código, traemos por código; si no, últimos 500 en general
        List<String[]> rows = (!code.isEmpty()) ? dao.listByCode(code, 500)
                                                : dao.listRecent(500);

        // Estructuras devueltas:
        // listByCode: [type, qty, reason, prev_stock, new_stock, user, created_at, product_name]
        // listRecent: [code, product_name, type, qty, reason, prev_stock, new_stock, user, created_at]

        // Filtro por tipo si corresponde
        if (!type.isEmpty()) {
            rows = rows.stream()
                    .filter(r -> {
                        if (r.length == 8) return type.equalsIgnoreCase(r[0]); // listByCode
                        else               return type.equalsIgnoreCase(r[2]); // listRecent
                    })
                    .collect(Collectors.toList());
        }

        for (String[] r : rows) {
            if (r.length == 8) {
                // listByCode
                String t = r[0], qty = r[1], reason = r[2], prev = r[3], now = r[4], user = r[5], when = r[6], name = r[7];
                model.addRow(new Object[]{ when, t, code, name, qty, prev + " → " + now, user, reason });
            } else {
                // listRecent
                String c = r[0], name = r[1], t = r[2], qty = r[3], reason = r[4], prev = r[5], now = r[6], user = r[7], when = r[8];
                model.addRow(new Object[]{ when, t, c, name, qty, prev + " → " + now, user, reason });
            }
        }
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("movimientos_" + LocalDateTime.now().toString().replace(":", "-") + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try (FileWriter w = new FileWriter(fc.getSelectedFile(), false)) {
            // Encabezados
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) w.write(",");
                w.write(model.getColumnName(c));
            }
            w.write("\n");
            // Filas
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) w.write(",");
                    Object val = model.getValueAt(r, c);
                    String s = (val == null) ? "" : val.toString().replace("\"", "\"\"");
                    w.write("\"" + s + "\"");
                }
                w.write("\n");
            }
            JOptionPane.showMessageDialog(this, "CSV exportado correctamente.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
