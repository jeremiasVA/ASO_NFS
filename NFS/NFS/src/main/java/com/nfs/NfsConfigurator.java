// src/main/java/com/ejemplo/nfs/NfsConfigurator.java
package com.ejemplo.nfs;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class NfsConfigurator extends JFrame {
    private final List<ExportEntry> entries = new ArrayList<>();
    private DefaultTableModel tableModel;
    private JTable table;

    private final String[] OPTIONS = {
            "rw", "ro", "sync", "async", "no_root_squash", "root_squash",
            "all_squash", "no_subtree_check", "subtree_check", "insecure",
            "secure", "anonuid=1000", "anongid=1000"
    };

    public NfsConfigurator() {
        setTitle("NFS Configurator - openSUSE 15.6");
        setSize(1000, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(30, 30, 30));

        initUI();
        loadExports();
    }

    private void initUI() {
        // === Panel superior: Iniciar / Reiniciar servicio ===
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBackground(new Color(45, 45, 45));

        JButton btnStart = createStyledButton("Iniciar Servicio", new Color(76, 175, 80)); // Verde
        JButton btnRestart = createStyledButton("Reiniciar Servicio", new Color(255, 152, 0)); // Naranja

        btnStart.addActionListener(e -> runSystemCommand("systemctl start nfs-server"));
        btnRestart.addActionListener(e -> runSystemCommand("systemctl restart nfs-server"));

        topPanel.add(btnStart);
        topPanel.add(btnRestart);

        // === Título y sintaxis ===
        JLabel title = new JLabel("NFS Configurator - openSUSE 15.6");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JLabel syntax = new JLabel("<carpeta> <host>(<opciones>)");
        syntax.setFont(new Font("Consolas", Font.ITALIC, 13));
        syntax.setForeground(new Color(180, 180, 180));

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(new Color(45, 45, 45));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        headerPanel.add(title);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(syntax);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(new Color(45, 45, 45));
        northPanel.add(topPanel, BorderLayout.WEST);
        northPanel.add(headerPanel, BorderLayout.CENTER);

        add(northPanel, BorderLayout.NORTH);

        // === Tabla ===
        String[] columns = {"Carpeta", "Host / Red", "Opciones"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setBackground(new Color(60, 60, 60));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionBackground(new Color(33, 150, 243));
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(70, 70, 70));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(new Color(50, 50, 50));
        add(scrollPane, BorderLayout.CENTER);

        // === Botones inferiores ===
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(new Color(40, 40, 40));

        JButton addBtn = createStyledButton("Añadir", new Color(33, 150, 243));     // Azul
        JButton editBtn = createStyledButton("Editar", new Color(156, 39, 176));   // Morado
        JButton deleteBtn = createStyledButton("Eliminar", new Color(244, 67, 54)); // Rojo
        JButton applyBtn = createStyledButton("Aplicar Cambios", new Color(46, 125, 50)); // Verde oscuro
        JButton refreshBtn = createStyledButton("Recargar", new Color(108, 117, 125)); // Gris

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(applyBtn);
        buttonPanel.add(refreshBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        // === Acciones ===
        addBtn.addActionListener(e -> showEditDialog(null));
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) showEditDialog(entries.get(row));
        });
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                entries.remove(row);
                tableModel.removeRow(row);
            }
        });
        applyBtn.addActionListener(e -> applyChanges());
        refreshBtn.addActionListener(e -> loadExports());
    }

    // === Botón con estilo moderno ===
    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Efecto hover
        Color darker = bg.darker();
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(darker);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg);
            }
        });

        return btn;
    }

    // === Diálogo de edición (mejorado) ===
    private void showEditDialog(ExportEntry entry) {
        JDialog dialog = new JDialog(this, entry == null ? "Añadir Export" : "Editar Export", true);
        dialog.setSize(560, 500);
        dialog.getContentPane().setBackground(new Color(45, 45, 45));
        dialog.setLayout(new BorderLayout(10, 10));

        // Formulario
        JPanel form = new JPanel(new GridLayout(2, 2, 10, 10));
        form.setBackground(new Color(45, 45, 45));
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        JTextField dirField = new JTextField(entry != null ? entry.getDirectory() : "/export/datos");
        JTextField hostField = new JTextField(entry != null ? entry.getHost() : "192.168.1.0/24");

        dirField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        hostField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        form.add(createLabel("Carpeta:"));
        form.add(dirField);
        form.add(createLabel("Host / Red:"));
        form.add(hostField);

        // Opciones
        JPanel optionsPanel = new JPanel(new GridLayout(0, 3, 8, 8));
        optionsPanel.setBackground(new Color(45, 45, 45));
        optionsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)), "Opciones NFS",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.LIGHT_GRAY));

        JCheckBox[] checkBoxes = new JCheckBox[OPTIONS.length];
        for (int i = 0; i < OPTIONS.length; i++) {
            checkBoxes[i] = new JCheckBox(OPTIONS[i]);
            checkBoxes[i].setBackground(new Color(45, 45, 45));
            checkBoxes[i].setForeground(Color.WHITE);
            checkBoxes[i].setFont(new Font("Segoe UI", Font.PLAIN, 12));
            if (entry != null && entry.hasOption(OPTIONS[i].split("=")[0])) {
                checkBoxes[i].setSelected(true);
            }
            optionsPanel.add(checkBoxes[i]);
        }

        JScrollPane optionsScroll = new JScrollPane(optionsPanel);
        optionsScroll.setBorder(null);

        dialog.add(form, BorderLayout.NORTH);
        dialog.add(optionsScroll, BorderLayout.CENTER);

        // Botones
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.setBackground(new Color(40, 40, 40));
        JButton save = createStyledButton("Guardar", new Color(46, 125, 50));
        JButton cancel = createStyledButton("Cancelar", new Color(108, 117, 125));
        buttons.add(save);
        buttons.add(cancel);

        save.addActionListener(e -> {
            ExportEntry newEntry = new ExportEntry(dirField.getText().trim(), hostField.getText().trim());
            for (JCheckBox cb : checkBoxes) {
                if (cb.isSelected()) {
                    String opt = cb.getText();
                    newEntry.addOption(opt.contains("=") ? opt.split("=")[0] : opt);
                }
            }
            if (entry == null) {
                entries.add(newEntry);
                tableModel.addRow(new Object[]{newEntry.getDirectory(), newEntry.getHost(),
                        String.join(", ", newEntry.getOptions())});
            } else {
                int row = table.getSelectedRow();
                entries.set(row, newEntry);
                tableModel.setValueAt(newEntry.getDirectory(), row, 0);
                tableModel.setValueAt(newEntry.getHost(), row, 1);
                tableModel.setValueAt(String.join(", ", newEntry.getOptions()), row, 2);
            }
            dialog.dispose();
        });
        cancel.addActionListener(e -> dialog.dispose());

        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return label;
    }

    // === Ejecutar comando del sistema ===
    private void runSystemCommand(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", cmd});
            int code = p.waitFor();
            String msg = code == 0 ? "Éxito: " + cmd : "Falló (código: " + code + ")";
            JOptionPane.showMessageDialog(this, msg, "Sistema", code == 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // === Resto de métodos (loadExports, applyChanges) ===
    private void loadExports() {
        entries.clear();
        tableModel.setRowCount(0);
        try {
            Path path = Paths.get("/etc/exports");
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] parts = line.split("\\s+");
                    if (parts.length < 2) continue;

                    String dir = parts[0];
                    String hostPart = parts[1];
                    String host = hostPart.contains("(") ? hostPart.substring(0, hostPart.indexOf('(')) : hostPart;
                    ExportEntry entry = new ExportEntry(dir, host);

                    if (hostPart.contains("(")) {
                        String opts = hostPart.substring(hostPart.indexOf('(') + 1, hostPart.indexOf(')'));
                        for (String opt : opts.split(",")) {
                            String trimmed = opt.trim();
                            if (!trimmed.isEmpty()) {
                                entry.addOption(trimmed.split("=")[0]);
                            }
                        }
                    }
                    entries.add(entry);
                    tableModel.addRow(new Object[]{dir, host, String.join(", ", entry.getOptions())});
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error leyendo /etc/exports: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyChanges() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# Configuración NFS generada por NFS Configurator\n");
            sb.append("# Copia este contenido a /etc/exports y ejecuta: sudo exportfs -ra\n\n");
            for (ExportEntry e : entries) {
                sb.append(e.toExportsLine());
            }

            Path output = Paths.get(System.getProperty("user.home"), "nfs-exports.conf");
            Files.write(output, sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            String msg = """
                ¡Configuración generada!
                
                Archivo: %s
                
                Para aplicar:
                1. sudo cp "%s" /etc/exports
                2. sudo exportfs -ra
                """.formatted(output.toAbsolutePath(), output.toAbsolutePath());

            JOptionPane.showMessageDialog(this, msg, "Listo", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // === main ===
    public void setVisible(boolean b) {
        super.setVisible(b);
    }
}