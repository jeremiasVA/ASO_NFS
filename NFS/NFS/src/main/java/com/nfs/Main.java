// src/main/java/com/ejemplo/nfs/Main.java
package com.ejemplo.nfs;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                // Opcional: FlatLaf (más moderno)
                // UIManager.setLookAndFeel(new FlatDarkLaf());
            } catch (Exception e) {
                e.printStackTrace();
            }

            new NfsConfigurator().setVisible(true);
        });
    }
}