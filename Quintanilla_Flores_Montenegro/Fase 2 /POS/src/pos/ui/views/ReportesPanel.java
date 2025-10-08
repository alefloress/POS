package pos.ui.views;

import pos.repo.SaleRepo;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font; 
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReportesPanel extends JPanel {

    private final JSpinner dpDesde;
    private final JSpinner dpHasta;
    private final JLabel lblTotal;
    private final JTable tblPagos;
    private final JTable tblTop;
    private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es","CL"));

    public ReportesPanel() {
        setLayout(new BorderLayout(10,10));
        setBackground(new Color(0xF9FAFB));

        // ----- Encabezado -----
        JLabel titulo = new JLabel("Reportes");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        titulo.setBorder(BorderFactory.createEmptyBorder(12,12,0,12));
        add(titulo, BorderLayout.NORTH);

        // ----- Filtros -----
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        dpDesde = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        dpHasta = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        dpDesde.setEditor(new JSpinner.DateEditor(dpDesde, "yyyy-MM-dd"));
        dpHasta.setEditor(new JSpinner.DateEditor(dpHasta, "yyyy-MM-dd"));
        JButton btnAplicar = new JButton("Aplicar");
        filtros.add(new JLabel("Desde:")); filtros.add(dpDesde);
        filtros.add(new JLabel("Hasta:")); filtros.add(dpHasta);
        filtros.add(btnAplicar);
        add(filtros, BorderLayout.PAGE_START);

        // ----- Cuerpo -----
        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        center.setOpaque(false);

        // Tabla: Totales por medio de pago
        tblPagos = new JTable(new PagosModel(CLP));
        center.add(wrap("Medios de pago", new JScrollPane(tblPagos)));

        // Tabla: Top productos por cantidad
        tblTop = new JTable(new TopModel());
        center.add(wrap("Productos más vendidos", new JScrollPane(tblTop)));

        add(center, BorderLayout.CENTER);

        // ----- Total grande -----
        lblTotal = new JLabel("Total: $0");
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 16f));
        lblTotal.setBorder(BorderFactory.createEmptyBorder(0,12,12,12));
        add(lblTotal, BorderLayout.SOUTH);

        // Acciones
        btnAplicar.addActionListener(e -> reload());

        // Carga inicial (hoy)
        reload();
    }

    private JPanel wrap(String title, JComponent c){
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setBorder(BorderFactory.createEmptyBorder(0,0,6,0));
        p.add(t, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    /** Método público para que MainFrame refresque los datos al abrir la vista */
    public void reload(){
        LocalDate desde = toLocalDate((Date) dpDesde.getValue());
        LocalDate hasta = toLocalDate((Date) dpHasta.getValue());

        // Total periodo
        int total = SaleRepo.get().total(desde, hasta);
        lblTotal.setText("Total: " + CLP.format(total));

        // Medios de pago
        java.util.Map<String,Integer> porPago = SaleRepo.get().totalsByPayment(desde, hasta);
        ((PagosModel) tblPagos.getModel()).setData(porPago);

        // Top N productos
        java.util.List<java.util.Map.Entry<String,Integer>> top =
                SaleRepo.get().topProducts(desde, hasta, 10);
        ((TopModel) tblTop.getModel()).setData(top);
    }

    private static LocalDate toLocalDate(Date d){
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    // ===== Modelos de tabla =====

    private static class PagosModel extends AbstractTableModel {
        private final String[] cols = {"Medio", "Total"};
        private final java.util.List<Object[]> data = new java.util.ArrayList<>();
        private final NumberFormat CLP;
        PagosModel(NumberFormat CLP){ this.CLP = CLP; }

        public void setData(java.util.Map<String,Integer> map){
            data.clear();
            for (java.util.Map.Entry<String,Integer> e : map.entrySet()) {
                data.add(new Object[]{ e.getKey(), e.getValue() });
            }
            fireTableDataChanged();
        }

        @Override public int getRowCount(){ return data.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Object getValueAt(int r, int c){
            Object[] row = data.get(r);
            return (c==0)? row[0] : CLP.format((Integer)row[1]);
        }
    }

    private static class TopModel extends AbstractTableModel {
        private final String[] cols = {"Código", "Cantidad"};
        private final java.util.List<java.util.Map.Entry<String,Integer>> data = new java.util.ArrayList<>();

        /** 👉 ESTE MÉTODO ES EL QUE ESTABAS LLAMANDO DESDE reload() */
        public void setData(java.util.List<java.util.Map.Entry<String,Integer>> list){
            data.clear();
            data.addAll(list);
            fireTableDataChanged();
        }

        @Override public int getRowCount(){ return data.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Object getValueAt(int r, int c){
            java.util.Map.Entry<String,Integer> e = data.get(r);
            return (c==0)? e.getKey() : e.getValue();
        }
    }
}