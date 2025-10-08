/**
 * 
 */
/**
 * 
 */
module POS_Sistema {
    requires java.desktop;  // Swing/AWT
    requires java.sql;      // JDBC (Connection, DriverManager, etc.)

    exports pos.login;
    exports pos.ui;
    exports pos.ui.views;
}
