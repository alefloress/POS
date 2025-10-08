package pos.ui.views;

import pos.store.InMemoryStore;
import pos.repo.SaleRepo;
import pos.model.Product;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class DashboardPanel extends JPanel {

    private final JLabel lblHoyTotal = new JLabel("Total hoy: $0");
    private final JLabel lblSemanaTotal = new JLabel("Semana: $0");
    private final JLabel lblMesTotal = new JLabel("Mes: $0");

    private final JTable tblLow = new JTable(new LowModel());
    private final JTable tblExp = new JTable(new ExpModel());

    private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
    private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public DashboardPanel() {
        setLayout(new BorderLayout(12,12));
        setBackground(new Color(0xF9FAFB));

        // ===== Título =====
        JLabel title = new JLabel("Dashboard — Resumen");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(BorderFactory.createEmptyBorder(12,12,0,12));
        add(title, BorderLayout.NORTH);

        // ===== Top: cards de totales + botón actualizar =====
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.setOpaque(false);

        top.add(cardMetric(lblHoyTotal, "Total hoy"));
        top.add(cardMetric(lblSemanaTotal, "Semana"));
        top.add(cardMetric(lblMesTotal, "Mes"));

        JButton btnRefrescar = new JButton("Actualizar");
        btnRefrescar.addActionListener(e -> recargar());
        top.add(btnRefrescar);

        add(top, BorderLayout.PAGE_START);

        // ===== Centro: tablas =====
        JPanel center = new JPanel(new GridLayout(1,2,12,12));
        center.setOpaque(false);

        JScrollPane spLow = new JScrollPane(tblLow);
        JScrollPane spExp = new JScrollPane(tblExp);

        center.add(wrap("Stock bajo (≤ 5)", spLow));
        center.add(wrap("Vencen en ≤ 30 días", spExp));
        add(center, BorderLayout.CENTER);

        // Config tablas (ordenadores, anchos, renderers)
        tuneLowTable();
        tuneExpTable();

        recargar();
    }

    private JPanel cardMetric(JLabel valueLabel, String caption) {
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 16f));
        JLabel cap = new JLabel(caption);
        cap.setFont(cap.getFont().deriveFont(Font.PLAIN, 12f));
        cap.setForeground(new Color(0x6B7280)); // gris

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(10,14,10,14));
        p.setBackground(Color.WHITE);
        p.setOpaque(true);
        p.setPreferredSize(new Dimension(180, 54));
        p.add(valueLabel);
        p.add(cap);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(10,14,10,14)
        ));
        return p;
    }

    private JPanel wrap(String title, JComponent c){
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setBorder(BorderFactory.createEmptyBorder(0,0,6,0));
        t.setFont(t.getFont().deriveFont(Font.BOLD, 13f));
        p.add(t, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    /** Llamar cuando vuelves al Dashboard para refrescar datos */
    public void recargar() {
        // Rango HOY
        LocalDate hoy = LocalDate.now();

        // Rango semana (Lunes a hoy)
        LocalDate inicioSemana = hoy.with(DayOfWeek.MONDAY);

        // Rango mes (primer día a hoy)
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        int totalHoy = SaleRepo.get().total(hoy, hoy);
        int totalSemana = SaleRepo.get().total(inicioSemana, hoy);
        int totalMes = SaleRepo.get().total(inicioMes, hoy);

        lblHoyTotal.setText("Total hoy: " + CLP.format(totalHoy));
        lblSemanaTotal.setText("Semana: " + CLP.format(totalSemana));
        lblMesTotal.setText("Mes: " + CLP.format(totalMes));

        // Alertas desde InMemoryStore
        ((LowModel) tblLow.getModel()).set(InMemoryStore.lowStock(5));
        ((ExpModel) tblExp.getModel()).set(InMemoryStore.expiringInDays(30));
    }

    // ===== Config de tablas =====
    private void tuneLowTable() {
        tblLow.setFillsViewportHeight(true);
        tblLow.setRowHeight(22);
        tblLow.setAutoCreateRowSorter(true);
        ((TableRowSorter<?>) tblLow.getRowSorter()).toggleSortOrder(2); // ordenar por stock asc

        // Alinear columnas y color de alerta
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        tblLow.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                          boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.RIGHT);
                int modelRow = table.convertRowIndexToModel(row);
                Product p = ((LowModel) table.getModel()).data.get(modelRow);
                // stock <= 3 rojo; <=5 naranja
                if (!isSelected) {
                    if (p.getStock() <= 3) c.setForeground(new Color(0xB91C1C)); // rojo
                    else c.setForeground(new Color(0xB45309)); // naranja
                }
                return c;
            }
        });

        // Anchos sugeridos
        tblLow.getColumnModel().getColumn(0).setPreferredWidth(70);
        tblLow.getColumnModel().getColumn(1).setPreferredWidth(220);
        tblLow.getColumnModel().getColumn(2).setPreferredWidth(60);
    }

    private void tuneExpTable() {
        tblExp.setFillsViewportHeight(true);
        tblExp.setRowHeight(22);
        tblExp.setAutoCreateRowSorter(true);
        ((TableRowSorter<?>) tblExp.getRowSorter()).toggleSortOrder(2); // ordenar por fecha asc

        // Renderer para fecha + color de proximidad
        tblExp.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                          boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.LEFT);
                int modelRow = table.convertRowIndexToModel(row);
                Product p = ((ExpModel) table.getModel()).data.get(modelRow);
                LocalDate e = p.getExpiry();
                setText(e == null ? "-" : DF.format(e));

                if (!isSelected && e != null) {
                    long dias = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), e);
                    if (dias <= 7) c.setForeground(new Color(0xB91C1C));      // <=7 días rojo
                    else if (dias <= 30) c.setForeground(new Color(0xB45309)); // <=30 días naranja
                    else c.setForeground(new Color(0x111827));                 // normal
                }
                return c;
            }
        });

        // Anchos sugeridos
        tblExp.getColumnModel().getColumn(0).setPreferredWidth(70);
        tblExp.getColumnModel().getColumn(1).setPreferredWidth(220);
        tblExp.getColumnModel().getColumn(2).setPreferredWidth(110);
    }

    // ===== Modelos de tabla =====
    private static class LowModel extends AbstractTableModel {
        private final String[] cols = {"Código","Nombre","Stock"};
        private List<Product> data = java.util.List.of();
        public void set(List<Product> rows){ data = rows; fireTableDataChanged(); }
        @Override public int getRowCount(){ return data.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Object getValueAt(int r, int c){
            Product p = data.get(r);
            return switch (c) {
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> p.getStock();
                default -> "";
            };
        }
    }

    private static class ExpModel extends AbstractTableModel {
        private final String[] cols = {"Código","Nombre","Vence"};
        private List<Product> data = java.util.List.of();
        public void set(List<Product> rows){ data = rows; fireTableDataChanged(); }
        @Override public int getRowCount(){ return data.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Object getValueAt(int r, int c){
            Product p = data.get(r);
            return switch (c) {
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> p.getExpiry(); // Se formatea en el renderer
                default -> "";
            };
        }
    }
}
