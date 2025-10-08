package pos.util;

import java.text.NumberFormat;
import java.util.Locale;

public class Money {

    // Formato CLP (pesos chilenos, sin decimales)
    private static final NumberFormat FORMAT;

    static {
        FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
        FORMAT.setMaximumFractionDigits(0); // no mostrar decimales
        FORMAT.setMinimumFractionDigits(0);
    }

    public static String format(double amount) {
        return FORMAT.format(amount);
    }

    public static double parse(String text) {
        try {
            return FORMAT.parse(text).doubleValue();
        } catch (Exception e) {
            return 0.0;
        }
    }
}