import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogViewer extends JFrame {

    private static final Map<String, CategoryStyle> CATEGORIES = new HashMap<>();

    static {
        // Format: "PREFIX", new CategoryStyle("Description for Dropdown", Color)
        CATEGORIES.put("DET", new CategoryStyle("Detection", new Color(173, 216, 230))); // Light Blue
        CATEGORIES.put("NAV", new CategoryStyle("Navigation",  new Color(144, 238, 144))); // Light Green
        CATEGORIES.put("MOV", new CategoryStyle("Movement", new Color(255, 255, 224))); // Light Yellow
        CATEGORIES.put("ERR", new CategoryStyle("Error",       new Color(255, 182, 193))); // Light Pink
    }

    // Helper record to store description and color

    record CategoryStyle(String name, Color color) {}

    private DefaultTableModel model;
    private JTable table;
    private JComboBox<String> macFilterBox;
    private JComboBox<String> catFilterBox;
    private JToggleButton pauseButton;
    private JToggleButton autoScrollButton;
    private TableRowSorter<DefaultTableModel> sorter;

    private final String LOG_FILE = "log.txt";
    private long lastKnownPosition = 0;
    private boolean isPaused = false;

    // Date formatter must match the format written by the server
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public LogViewer() {
        setTitle("Log Viewer Pro");
        setSize(1000, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Table & Model Setup
        String[] columns = {"Timestamp", "MAC Address", "Message"};
        model = new DefaultTableModel(columns, 0);

        // Custom JTable for Row Color Coding
        table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                // If row is selected by user, keep the default selection color
                if (isRowSelected(row)) {
                    return c;
                }

                // Get the message (column 2) for this row
                // We must convert index because sorting changes the view index vs model index
                int modelRow = convertRowIndexToModel(row);
                String msg = (String) model.getValueAt(modelRow, 2);

                // Check for prefix
                c.setBackground(Color.WHITE); // Default background
                for (Map.Entry<String, CategoryStyle> entry : CATEGORIES.entrySet()) {
                    String prefix = entry.getKey();
                    if (msg.startsWith(prefix)) {
                        c.setBackground(entry.getValue().color());
                        break;
                    }
                }
                return c;
            }
        };

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Custom Comparator for accurate Date Sorting
        try {
            sorter.setComparator(0, (o1, o2) -> {
                try {
                    return LocalDateTime.parse((String) o1, formatter)
                            .compareTo(LocalDateTime.parse((String) o2, formatter));
                } catch (Exception e) { return 0; }
            });
        } catch (Exception e) {
            System.err.println("Could not set date comparator: " + e.getMessage());
        }

        // Control Panel Setup
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // MAC Filter Dropdown
        macFilterBox = new JComboBox<>(new String[]{"All MACs"});

        // Category Filter Dropdown (Populated from the Map)
        catFilterBox = new JComboBox<>();
        catFilterBox.addItem("All Categories");
        CATEGORIES.forEach((prefix, style) -> {
            catFilterBox.addItem(prefix + " - " + style.name());
        });

        pauseButton = new JToggleButton("Pause");
        autoScrollButton = new JToggleButton("Auto-scroll", true);

        topPanel.add(new JLabel("MAC:"));
        topPanel.add(macFilterBox);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(new JLabel("Type:"));
        topPanel.add(catFilterBox);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(pauseButton);
        topPanel.add(autoScrollButton);

        // Listeners
        macFilterBox.addActionListener(e -> updateFilters());
        catFilterBox.addActionListener(e -> updateFilters());
        pauseButton.addActionListener(e -> isPaused = pauseButton.isSelected());

        // Layout
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        startLogWatcher();
    }

    /**
     * Updates the table filter based on both MAC and Category selections.
     */

    private void updateFilters() {
        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        // MAC Filter
        String selectedMac = (String) macFilterBox.getSelectedItem();
        if (selectedMac != null && !"All MACs".equals(selectedMac)) {
            // Regex filter on column 1 (MAC Address)
            filters.add(RowFilter.regexFilter(selectedMac, 1));
        }

        // Category Filter
        String selectedCatCombo = (String) catFilterBox.getSelectedItem();
        if (selectedCatCombo != null && !"All Categories".equals(selectedCatCombo)) {
            // Extract the prefix (e.g., "DET" from "DET - Detection")
            String prefix = selectedCatCombo.split(" - ")[0];

            // Custom filter checking if column 2 (Message) starts with prefix
            filters.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<?, ?> entry) {
                    String msg = (String) entry.getStringValue(2);
                    return msg.startsWith(prefix);
                }
            });
        }

        // Apply combined filters
        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    private void startLogWatcher() {
        Timer timer = new Timer(1000, e -> {
            if (!isPaused) readNewLogs();
        });
        timer.start();
    }

    private void readNewLogs() {
        try (RandomAccessFile reader = new RandomAccessFile(LOG_FILE, "r")) {
            long fileLength = reader.length();

            // If file was truncated, reset position
            if (fileLength < lastKnownPosition) {
                lastKnownPosition = 0;
            }

            reader.seek(lastKnownPosition);

            String line;
            boolean newRows = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Expecting format: "DATE, MAC, MESSAGE"
                String[] parts = line.split(", ", 3);
                if (parts.length == 3) {
                    model.addRow(parts);
                    updateMacDropdown(parts[1]);
                    newRows = true;
                }
            }
            lastKnownPosition = reader.getFilePointer();

            if (newRows && autoScrollButton.isSelected()) {
                scrollToBottom();
            }
        } catch (Exception e) {
            // File might be busy or not created yet
        }
    }

    private void updateMacDropdown(String mac) {
        DefaultComboBoxModel<String> comboModel = (DefaultComboBoxModel<String>) macFilterBox.getModel();
        if (comboModel.getIndexOf(mac) == -1) {
            comboModel.addElement(mac);
        }
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            int rowCount = table.getRowCount();
            if (rowCount > 0) {
                Rectangle rect = table.getCellRect(rowCount - 1, 0, true);
                table.scrollRectToVisible(rect);
            }
        });
    }
}