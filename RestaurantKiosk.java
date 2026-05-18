import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.*;
import java.util.List;

class MenuItem {
    int id;
    String name, category;
    double price;

    public MenuItem(int id, String name, String category, double price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
    }

    @Override
    public String toString() {
        return name + " - ₱" + String.format("%.2f", price);
    }
}

class Voucher {
    String code;
    double discount;

    public Voucher(String code, double discount) {
        this.code = code;
        this.discount = discount;
    }
}

class CartItem {
    MenuItem item;
    int quantity;

    public CartItem(MenuItem item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    double getSubtotal() {
        return item.price * quantity;
    }
}

class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/restaurant_kiosk";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}

class MenuDAO {
    public static List<MenuItem> getAllItems() {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT * FROM menu_items ORDER BY category, name";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(new MenuItem(rs.getInt("id"), rs.getString("name"),
                        rs.getString("category"), rs.getDouble("price")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static List<String> getCategories() {
        List<String> cats = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM menu_items ORDER BY category";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) cats.add(rs.getString("category"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cats;
    }

    public static List<MenuItem> getItemsByCategory(String category) {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT * FROM menu_items WHERE category = ? ORDER BY name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                items.add(new MenuItem(rs.getInt("id"), rs.getString("name"),
                        rs.getString("category"), rs.getDouble("price")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static void addItem(String name, String category, double price) {
        String sql = "INSERT INTO menu_items (name, category, price) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, category);
            pstmt.setDouble(3, price);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateItem(int id, String name, String category, double price) {
        String sql = "UPDATE menu_items SET name=?, category=?, price=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, category);
            pstmt.setDouble(3, price);
            pstmt.setInt(4, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteItem(int id) {
        String sql = "DELETE FROM menu_items WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

class VoucherDAO {
    public static Voucher getVoucher(String code) {
        String sql = "SELECT * FROM vouchers WHERE code=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Voucher(rs.getString("code"), rs.getDouble("discount_amount"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Voucher> getAllVouchers() {
        List<Voucher> list = new ArrayList<>();
        String sql = "SELECT * FROM vouchers";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Voucher(rs.getString("code"), rs.getDouble("discount_amount")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void addVoucher(String code, double discount) {
        String sql = "INSERT INTO vouchers (code, discount_amount) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code.toUpperCase());
            pstmt.setDouble(2, discount);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteVoucher(String code) {
        String sql = "DELETE FROM vouchers WHERE code=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

class OrderDAO {
    public static int saveOrder(String orderType, double total, List<CartItem> cartItems,
                                String notes, String pwdName, String pwdId,
                                String paymentMethod, double cashTendered, double change) {
        String orderSql = "INSERT INTO orders (order_type, total, notes, pwd_name, pwd_id, payment_method, cash_tendered, change_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, orderType);
            pstmt.setDouble(2, total);
            pstmt.setString(3, notes);
            pstmt.setString(4, pwdName);
            pstmt.setString(5, pwdId);
            pstmt.setString(6, paymentMethod);
            pstmt.setDouble(7, cashTendered);
            pstmt.setDouble(8, change);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int orderId = rs.getInt(1);
                String itemSql = "INSERT INTO order_items (order_id, menu_item_id, quantity, price) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt2 = conn.prepareStatement(itemSql)) {
                    for (CartItem ci : cartItems) {
                        pstmt2.setInt(1, orderId);
                        pstmt2.setInt(2, ci.item.id);
                        pstmt2.setInt(3, ci.quantity);
                        pstmt2.setDouble(4, ci.item.price);
                        pstmt2.executeUpdate();
                    }
                }
                return orderId;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}

public class RestaurantKiosk extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private String orderType = "Dine-In";
    private List<CartItem> cart = new ArrayList<>();
    private Voucher appliedVoucher = null;
    private boolean pwdApplied = false;
    private String pwdName = "";
    private String pwdId = "";
    private String orderNote = "";
    private static final double TAX_RATE = 0.12;
    private CategoryPanel categoryPanel;
    private CartPanel cartPanel;

    private static final String MAIN_FONT = "Segoe UI";

    private static Map<Integer, BufferedImage> originalImageCache = new HashMap<>();

    public RestaurantKiosk() {
        setTitle("Restaurant Kiosk - Self Ordering System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 600));
        getContentPane().setBackground(Color.LIGHT_GRAY);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(Color.LIGHT_GRAY);

        mainPanel.add(new WelcomePanel(), "welcome");
        categoryPanel = new CategoryPanel();
        mainPanel.add(categoryPanel, "categories");
        cartPanel = new CartPanel();
        mainPanel.add(cartPanel, "cart");

        add(mainPanel);
        cardLayout.show(mainPanel, "welcome");
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(MAIN_FONT, Font.BOLD, 18));
        btn.setBackground(bg);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(12, 25, 12, 25));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public void refreshCategoryButton() {
        if (categoryPanel != null) {
            categoryPanel.updateCartButton();
        }
        if (cartPanel != null) {
            cartPanel.refreshDisplay();
        }
    }

    private static BufferedImage loadOriginalImage(MenuItem item) {
        if (originalImageCache.containsKey(item.id)) {
            return originalImageCache.get(item.id);
        }

        BufferedImage img = null;
        String basePath = "menu_images/";
        String[] extensions = {".png", ".jpg", ".jpeg", ".gif"};

        for (String ext : extensions) {
            java.io.File file = new java.io.File(basePath + item.id + ext);
            if (file.exists()) {
                try {
                    img = javax.imageio.ImageIO.read(file);
                    if (img != null) break;
                } catch (Exception ignored) {}
            }
        }

        if (img == null) {
            String nameFile = item.name.toLowerCase().replace(" ", "_");
            for (String ext : extensions) {
                java.io.File file = new java.io.File(basePath + nameFile + ext);
                if (file.exists()) {
                    try {
                        img = javax.imageio.ImageIO.read(file);
                        if (img != null) break;
                    } catch (Exception ignored) {}
                }
            }
        }

        originalImageCache.put(item.id, img);
        return img;
    }

    private static ImageIcon getScaledImageForMenuItem(MenuItem item, int width, int height) {
        BufferedImage original = loadOriginalImage(item);
        if (original == null) return null;
        Image scaled = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    class WelcomePanel extends JPanel {
        public WelcomePanel() {
            setLayout(new BorderLayout());
            setBackground(Color.LIGHT_GRAY);
            setBorder(new EmptyBorder(10, 10, 10, 10));

            JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            topRow.setBackground(Color.LIGHT_GRAY);
            JButton admin = new JButton("⚙");
            try {
                admin.setFont(new Font("Segoe UI Symbol", Font.BOLD, 28));
            } catch (Exception e) {
                admin.setFont(new Font("SansSerif", Font.BOLD, 28));
            }
            admin.setBackground(Color.LIGHT_GRAY);
            admin.setFocusPainted(false);
            admin.setCursor(new Cursor(Cursor.HAND_CURSOR));
            admin.setToolTipText("Admin Panel");
            Dimension btnSize = new Dimension(50, 50);
            admin.setPreferredSize(btnSize);
            topRow.add(admin);
            add(topRow, BorderLayout.NORTH);

            JPanel centerPanel = new JPanel(new GridBagLayout());
            centerPanel.setBackground(Color.LIGHT_GRAY);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(10, 10, 10, 10);

            JLabel line1 = new JLabel("Welcome to Kaon & Kain", SwingConstants.CENTER);
            line1.setFont(new Font(MAIN_FONT, Font.BOLD, 28));
            line1.setForeground(new Color(44, 43, 43));
            centerPanel.add(line1, gbc);

            JLabel line2 = new JLabel("a Filipino Cuisine", SwingConstants.CENTER);
            line2.setFont(new Font(MAIN_FONT, Font.BOLD, 28));
            line2.setForeground(new Color(44, 43, 43));
            centerPanel.add(line2, gbc);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
            buttonPanel.setBackground(Color.LIGHT_GRAY);
            JButton dineIn = createStyledButton("Dine-In", Color.LIGHT_GRAY);
            JButton takeOut = createStyledButton("Take-Out", Color.LIGHT_GRAY);
            Dimension btnSizeLarge = new Dimension(200, 60);
            dineIn.setPreferredSize(btnSizeLarge);
            takeOut.setPreferredSize(btnSizeLarge);
            buttonPanel.add(dineIn);
            buttonPanel.add(takeOut);
            centerPanel.add(buttonPanel, gbc);
            add(centerPanel, BorderLayout.CENTER);

            dineIn.addActionListener(e -> {
                orderType = "Dine-In";
                loadCategoryScreen();
            });
            takeOut.addActionListener(e -> {
                orderType = "Take-Out";
                loadCategoryScreen();
            });
            admin.addActionListener(e -> {
                if (authenticateAdmin()) {
                    new AdminFrame().setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid Admin Password!");
                }
            });
        }

        private boolean authenticateAdmin() {
            String pass = JOptionPane.showInputDialog(this, "Enter Admin PIN:");
            return "1234".equals(pass);
        }
    }

    class CategoryPanel extends JPanel {
        private JButton viewCartButton;
        private JList<String> categoryList;
        private DefaultListModel<String> categoryListModel;
        private JPanel menuTilePanel;
        private MenuItem selectedMenuItem;
        private JSpinner qtySpinner;
        private JButton addToCartBtn;

        public CategoryPanel() {
            setLayout(new BorderLayout(10, 10));
            setBackground(Color.LIGHT_GRAY);
            setBorder(new EmptyBorder(15, 15, 15, 15));

            JLabel header = new JLabel("Select Category & Add Items", SwingConstants.CENTER);
            header.setFont(new Font(MAIN_FONT, Font.BOLD, 24));
            add(header, BorderLayout.NORTH);

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setDividerLocation(220);
            splitPane.setResizeWeight(0.25);
            splitPane.setBorder(null);

            JPanel leftPanel = new JPanel(new BorderLayout());
            leftPanel.setBackground(Color.LIGHT_GRAY);
            leftPanel.setBorder(new TitledBorder("Categories"));
            categoryListModel = new DefaultListModel<>();
            categoryList = new JList<>(categoryListModel);
            categoryList.setFont(new Font(MAIN_FONT, Font.PLAIN, 16));
            categoryList.setBackground(Color.WHITE);
            categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            loadCategories();

            JScrollPane catScroll = new JScrollPane(categoryList);
            catScroll.setBorder(new EmptyBorder(8, 8, 8, 8));
            leftPanel.add(catScroll, BorderLayout.CENTER);
            splitPane.setLeftComponent(leftPanel);

            categoryList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    String selectedCat = categoryList.getSelectedValue();
                    if (selectedCat != null) {
                        loadMenuItemsForCategory(selectedCat);
                    }
                }
            });

            JPanel rightPanel = new JPanel(new BorderLayout());
            rightPanel.setBackground(Color.LIGHT_GRAY);
            rightPanel.setBorder(new TitledBorder("Menu Items"));

            menuTilePanel = new JPanel(new GridBagLayout());
            menuTilePanel.setBackground(Color.WHITE);
            menuTilePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            JScrollPane tileScroll = new JScrollPane(menuTilePanel);
            tileScroll.getViewport().setBackground(Color.WHITE);
            tileScroll.setBorder(null);
            tileScroll.getVerticalScrollBar().setUnitIncrement(16);

            JPanel addControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            addControls.setBackground(Color.LIGHT_GRAY);
            JLabel qtyLabel = new JLabel("Quantity:");
            qtyLabel.setFont(new Font(MAIN_FONT, Font.BOLD, 14));
            addControls.add(qtyLabel);
            qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
            qtySpinner.setPreferredSize(new Dimension(60, 30));
            addControls.add(qtySpinner);
            addToCartBtn = new JButton("Add to Cart");
            addToCartBtn.setFont(new Font(MAIN_FONT, Font.BOLD, 14));
            addToCartBtn.setBackground(Color.LIGHT_GRAY);
            addToCartBtn.addActionListener(e -> addSelectedToCart());
            addControls.add(addToCartBtn);

            rightPanel.add(tileScroll, BorderLayout.CENTER);
            rightPanel.add(addControls, BorderLayout.SOUTH);
            splitPane.setRightComponent(rightPanel);
            add(splitPane, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 10));
            bottomPanel.setBackground(Color.LIGHT_GRAY);
            viewCartButton = new JButton("View Cart (" + cart.size() + " items)");
            viewCartButton.setFont(new Font(MAIN_FONT, Font.BOLD, 16));
            viewCartButton.setBackground(Color.LIGHT_GRAY);
            viewCartButton.addActionListener(e -> cardLayout.show(mainPanel, "cart"));
            JButton backBtn = new JButton("Back to Welcome");
            backBtn.setFont(new Font(MAIN_FONT, Font.BOLD, 16));
            backBtn.setBackground(Color.LIGHT_GRAY);
            backBtn.addActionListener(e -> cardLayout.show(mainPanel, "welcome"));
            bottomPanel.add(viewCartButton);
            bottomPanel.add(backBtn);
            add(bottomPanel, BorderLayout.SOUTH);
        }

        private void loadCategories() {
            categoryListModel.clear();
            List<String> categories = MenuDAO.getCategories();
            for (String cat : categories) {
                categoryListModel.addElement(cat);
            }
            if (categoryListModel.size() > 0) {
                categoryList.setSelectedIndex(0);
            }
        }

        private void loadMenuItemsForCategory(String category) {
            menuTilePanel.removeAll();
            selectedMenuItem = null;
            List<MenuItem> items = MenuDAO.getItemsByCategory(category);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;

            int cols = 3;
            for (int i = 0; i < items.size(); i++) {
                JButton itemButton = createMenuItemButton(items.get(i));
                gbc.gridx = i % cols;
                gbc.gridy = i / cols;
                menuTilePanel.add(itemButton, gbc);
            }

            menuTilePanel.revalidate();
            menuTilePanel.repaint();
        }

        private JButton createMenuItemButton(MenuItem item) {
            JButton btn = new JButton();
            btn.setLayout(new BorderLayout());
            btn.setBackground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            Dimension btnSize = new Dimension(140, 140);
            btn.setPreferredSize(btnSize);
            btn.setMinimumSize(btnSize);
            btn.setMaximumSize(btnSize);

            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            ImageIcon icon = getScaledImageForMenuItem(item, 80, 80);
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            btn.add(iconLabel, BorderLayout.CENTER);

            JPanel textPanel = new JPanel(new GridLayout(2, 1));
            textPanel.setBackground(Color.WHITE);
            JLabel nameLabel = new JLabel(item.name, SwingConstants.CENTER);
            nameLabel.setFont(new Font(MAIN_FONT, Font.BOLD, 12));
            JLabel priceLabel = new JLabel(String.format("₱%.2f", item.price), SwingConstants.CENTER);
            priceLabel.setFont(new Font(MAIN_FONT, Font.PLAIN, 11));
            textPanel.add(nameLabel);
            textPanel.add(priceLabel);
            btn.add(textPanel, BorderLayout.SOUTH);

            btn.addActionListener(e -> {
                selectedMenuItem = item;
                resetMenuItemSelection();
                btn.setBackground(new Color(220, 240, 255));
                btn.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
            });

            return btn;
        }

        private void resetMenuItemSelection() {
            for (Component comp : menuTilePanel.getComponents()) {
                if (comp instanceof JButton) {
                    JButton btn = (JButton) comp;
                    btn.setBackground(Color.WHITE);
                    btn.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                }
            }
        }

        private void addSelectedToCart() {
            if (selectedMenuItem == null) {
                JOptionPane.showMessageDialog(this, "Please select a menu item first.");
                return;
            }
            int qty = (Integer) qtySpinner.getValue();
            addToCart(selectedMenuItem, qty);
            JOptionPane.showMessageDialog(this, qty + " x " + selectedMenuItem.name + " added to cart!");
            refreshCategoryButton();
        }

        public void updateCartButton() {
            viewCartButton.setText("View Cart (" + cart.size() + " items)");
        }
    }

    class CartPanel extends JPanel {
        private DefaultListModel<String> cartModel;
        private JList<String> cartList;
        private JLabel subtotalLabel, pwdDiscountLabel, afterPwdLabel, voucherLabel, taxLabel, totalLabel;
        private JButton applyPwdBtn;
        private JButton addNoteBtn;
        private JLabel noteLabel;

        public CartPanel() {
            setLayout(new BorderLayout(10, 10));
            setBackground(Color.LIGHT_GRAY);
            setBorder(new EmptyBorder(15, 15, 15, 15));

            JLabel title = new JLabel("Your Order (" + orderType + ")", SwingConstants.CENTER);
            title.setFont(new Font(MAIN_FONT, Font.BOLD, 22));
            add(title, BorderLayout.NORTH);

            cartModel = new DefaultListModel<>();
            cartList = new JList<>(cartModel);
            cartList.setFont(new Font(MAIN_FONT, Font.PLAIN, 14));
            cartList.setBackground(Color.WHITE);
            add(new JScrollPane(cartList), BorderLayout.CENTER);

            JPanel detailsPanel = new JPanel();
            detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
            detailsPanel.setBackground(Color.LIGHT_GRAY);
            TitledBorder orderBorder = new TitledBorder("Order Summary");
            orderBorder.setTitleFont(new Font(MAIN_FONT, Font.BOLD, 14));
            orderBorder.setTitleJustification(TitledBorder.LEFT);
            detailsPanel.setBorder(orderBorder);
            detailsPanel.setPreferredSize(new Dimension(300, 0));

            subtotalLabel = new JLabel("Subtotal: ₱0.00");
            pwdDiscountLabel = new JLabel("PWD/Senior Discount (20% on highest item): ₱0.00");
            afterPwdLabel = new JLabel("After PWD Discount: ₱0.00");
            voucherLabel = new JLabel("Voucher Discount: ₱0.00");
            taxLabel = new JLabel("Tax (12%): ₱0.00");
            totalLabel = new JLabel("Total: ₱0.00");

            Font labelFont = new Font(MAIN_FONT, Font.BOLD, 12);
            subtotalLabel.setFont(labelFont);
            pwdDiscountLabel.setFont(labelFont);
            afterPwdLabel.setFont(labelFont);
            voucherLabel.setFont(labelFont);
            taxLabel.setFont(labelFont);
            totalLabel.setFont(new Font(MAIN_FONT, Font.BOLD, 18));

            subtotalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            pwdDiscountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            afterPwdLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            voucherLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            taxLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            applyPwdBtn = new JButton("Apply PWD / Senior Discount");
            applyPwdBtn.setFont(new Font(MAIN_FONT, Font.BOLD, 12));
            applyPwdBtn.setBackground(Color.LIGHT_GRAY);
            applyPwdBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            applyPwdBtn.addActionListener(e -> applyPwdDiscount());

            addNoteBtn = new JButton("Add Special Instructions");
            addNoteBtn.setFont(new Font(MAIN_FONT, Font.BOLD, 12));
            addNoteBtn.setBackground(Color.LIGHT_GRAY);
            addNoteBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            addNoteBtn.addActionListener(e -> {
                String note = JOptionPane.showInputDialog(this, "Enter special instructions (e.g., no onions, extra sauce):", orderNote);
                if (note != null) {
                    orderNote = note;
                    noteLabel.setText("Note: " + (orderNote.isEmpty() ? "None" : orderNote));
                }
            });

            noteLabel = new JLabel("Note: None");
            noteLabel.setFont(new Font(MAIN_FONT, Font.ITALIC, 12));
            noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel voucherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            voucherPanel.setBackground(Color.LIGHT_GRAY);
            voucherPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel promoCodeLabel = new JLabel("Promo Code:");
            promoCodeLabel.setFont(new Font(MAIN_FONT, Font.BOLD, 12));
            JTextField voucherField = new JTextField(10);
            JButton applyVoucherBtn = new JButton("Apply Voucher");
            applyVoucherBtn.setFont(new Font(MAIN_FONT, Font.BOLD, 12));
            applyVoucherBtn.setBackground(Color.LIGHT_GRAY);
            applyVoucherBtn.addActionListener(e -> {
                String code = voucherField.getText().trim();
                Voucher v = VoucherDAO.getVoucher(code);
                if (v != null) {
                    appliedVoucher = v;
                    refreshDisplay();
                    JOptionPane.showMessageDialog(this, "Voucher Applied: " + v.code + " - ₱" + v.discount + " off");
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid Voucher Code");
                }
                voucherField.setText("");
            });
            voucherPanel.add(promoCodeLabel);
            voucherPanel.add(voucherField);
            voucherPanel.add(applyVoucherBtn);

            detailsPanel.add(subtotalLabel);
            detailsPanel.add(Box.createVerticalStrut(5));
            detailsPanel.add(pwdDiscountLabel);
            detailsPanel.add(Box.createVerticalStrut(5));
            detailsPanel.add(afterPwdLabel);
            detailsPanel.add(Box.createVerticalStrut(5));
            detailsPanel.add(voucherLabel);
            detailsPanel.add(Box.createVerticalStrut(5));
            detailsPanel.add(taxLabel);
            detailsPanel.add(Box.createVerticalStrut(10));
            detailsPanel.add(totalLabel);
            detailsPanel.add(Box.createVerticalStrut(10));
            detailsPanel.add(applyPwdBtn);
            detailsPanel.add(Box.createVerticalStrut(5));
            detailsPanel.add(addNoteBtn);
            detailsPanel.add(Box.createVerticalStrut(5));
            detailsPanel.add(noteLabel);
            detailsPanel.add(Box.createVerticalStrut(5));
            detailsPanel.add(voucherPanel);

            JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 10, 10));
            buttonPanel.setBackground(Color.LIGHT_GRAY);
            JButton editBtn = new JButton("Edit Selected");
            JButton removeBtn = new JButton("Remove Selected");
            JButton clearBtn = new JButton("Clear Cart");
            JButton checkoutBtn = new JButton("Proceed to Payment");
            JButton backBtn = new JButton("Back to Categories");

            Font buttonFont = new Font(MAIN_FONT, Font.BOLD, 12);
            editBtn.setFont(buttonFont);
            removeBtn.setFont(buttonFont);
            clearBtn.setFont(buttonFont);
            checkoutBtn.setFont(buttonFont);
            backBtn.setFont(buttonFont);
            editBtn.setBackground(Color.LIGHT_GRAY);
            removeBtn.setBackground(Color.LIGHT_GRAY);
            clearBtn.setBackground(Color.LIGHT_GRAY);
            checkoutBtn.setBackground(Color.LIGHT_GRAY);
            backBtn.setBackground(Color.LIGHT_GRAY);

            editBtn.addActionListener(e -> {
                int idx = cartList.getSelectedIndex();
                if (idx >= 0 && idx < cart.size()) {
                    CartItem ci = cart.get(idx);
                    String newQtyStr = JOptionPane.showInputDialog(CartPanel.this,
                            "Edit quantity for " + ci.item.name + " (current: " + ci.quantity + "):",
                            ci.quantity);
                    if (newQtyStr != null) {
                        try {
                            int newQty = Integer.parseInt(newQtyStr);
                            if (newQty > 0 && newQty <= 20) {
                                ci.quantity = newQty;
                                refreshDisplay();
                                refreshCategoryButton();
                            } else if (newQty <= 0) {
                                cart.remove(idx);
                                refreshDisplay();
                                refreshCategoryButton();
                            } else {
                                JOptionPane.showMessageDialog(CartPanel.this, "Quantity must be between 1 and 20.");
                            }
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(CartPanel.this, "Invalid number.");
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(CartPanel.this, "Please select an item to edit.");
                }
            });

            removeBtn.addActionListener(e -> {
                int idx = cartList.getSelectedIndex();
                if (idx >= 0 && idx < cart.size()) {
                    cart.remove(idx);
                    resetDiscounts();
                    refreshDisplay();
                    refreshCategoryButton();
                }
            });

            clearBtn.addActionListener(e -> {
                cart.clear();
                resetDiscounts();
                refreshDisplay();
                refreshCategoryButton();
            });

            checkoutBtn.addActionListener(e -> processPayment());
            backBtn.addActionListener(e -> cardLayout.show(mainPanel, "categories"));

            buttonPanel.add(editBtn);
            buttonPanel.add(removeBtn);
            buttonPanel.add(clearBtn);
            buttonPanel.add(checkoutBtn);
            buttonPanel.add(backBtn);

            add(detailsPanel, BorderLayout.EAST);
            add(buttonPanel, BorderLayout.SOUTH);
            refreshDisplay();
        }

        public void refreshDisplay() {
            cartModel.clear();
            for (CartItem ci : cart) {
                cartModel.addElement(ci.item.name + " x" + ci.quantity + " = ₱" + String.format("%.2f", ci.getSubtotal()));
            }
            double subtotal = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
            double pwdDiscount = 0.0;
            if (pwdApplied && !cart.isEmpty()) {
                double maxSubtotal = cart.stream().mapToDouble(CartItem::getSubtotal).max().orElse(0.0);
                pwdDiscount = maxSubtotal * 0.20;
            }
            double afterPwd = subtotal - pwdDiscount;
            double voucherDiscount = (appliedVoucher != null) ? appliedVoucher.discount : 0;
            double afterVoucher = afterPwd - voucherDiscount;
            double tax = (!pwdApplied) ? afterPwd * TAX_RATE : 0;
            double total = afterVoucher + tax;

            subtotalLabel.setText("Subtotal: ₱" + String.format("%.2f", subtotal));
            pwdDiscountLabel.setText("PWD/Senior Discount: ₱" + String.format("%.2f", pwdDiscount));
            afterPwdLabel.setText("After PWD Discount: ₱" + String.format("%.2f", afterPwd));
            voucherLabel.setText("Voucher Discount: ₱" + String.format("%.2f", voucherDiscount));
            if (pwdApplied) {
                taxLabel.setText("Tax (PWD Exempt): ₱0.00");
            } else {
                taxLabel.setText("Tax (12%): ₱" + String.format("%.2f", tax));
            }
            totalLabel.setText("Total: ₱" + String.format("%.2f", total));
        }

        private void resetDiscounts() {
            pwdApplied = false;
            pwdName = "";
            pwdId = "";
            appliedVoucher = null;
        }

        private void applyPwdDiscount() {
            if (pwdApplied) {
                JOptionPane.showMessageDialog(this, "PWD discount already applied.");
                return;
            }
            String name = JOptionPane.showInputDialog(this, "Enter PWD/Senior Name:");
            if (name == null || name.trim().isEmpty()) return;
            String id = JOptionPane.showInputDialog(this, "Enter 16-digit PWD/Senior ID Number:");
            if (id == null || id.trim().isEmpty()) return;
            if (!id.matches("\\d{16}")) {
                JOptionPane.showMessageDialog(this, "Invalid ID. Must be exactly 16 digits.");
                return;
            }
            pwdApplied = true;
            pwdName = name.trim();
            pwdId = id.trim();
            refreshDisplay();
            JOptionPane.showMessageDialog(this, "20% PWD discount applied for " + name + " (Tax exempt)");
        }

        private void processPayment() {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Cart is empty!");
                return;
            }

            double subtotal = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
            double pwdDiscount = 0.0;
            if (pwdApplied && !cart.isEmpty()) {
                double maxSubtotal = cart.stream().mapToDouble(CartItem::getSubtotal).max().orElse(0.0);
                pwdDiscount = maxSubtotal * 0.20;
            }
            double afterPwd = subtotal - pwdDiscount;
            double voucherDiscount = (appliedVoucher != null) ? appliedVoucher.discount : 0;
            double afterVoucher = afterPwd - voucherDiscount;
            double tax = (!pwdApplied) ? afterPwd * TAX_RATE : 0;
            double total = afterVoucher + tax;

            String[] options = {"Cash", "Card"};
            int choice = JOptionPane.showOptionDialog(this,
                    "Total Amount: ₱" + String.format("%.2f", total) + "\nSelect Payment Method",
                    "Payment", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

            if (choice == -1) return;

            boolean success = false;
            String paymentMethod = "";
            double cashTendered = 0.0;
            double change = 0.0;

            if (choice == 0) {
                String input = JOptionPane.showInputDialog(this, "Enter Cash Amount:");
                if (input == null) return;
                try {
                    cashTendered = Double.parseDouble(input);
                    if (cashTendered >= total) {
                        change = cashTendered - total;
                        JOptionPane.showMessageDialog(this, "Payment Successful!\nChange: ₱" + String.format("%.2f", change));
                        paymentMethod = "Cash";
                        success = true;
                    } else {
                        JOptionPane.showMessageDialog(this, "Insufficient cash!");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid amount");
                }
            } else if (choice == 1) { 
                JOptionPane.showMessageDialog(this, "Card Payment Processed Successfully!");
                paymentMethod = "Card";
                success = true;
            }

            if (!success) return;

            int orderId = OrderDAO.saveOrder(orderType, total, cart, orderNote, pwdName, pwdId,
                    paymentMethod, cashTendered, change);
            if (orderId == -1) {
                JOptionPane.showMessageDialog(this, "Error saving order. Please try again.");
                return;
            }


            StringBuilder receipt = new StringBuilder();
            receipt.append("                    RECEIPT\n");
            receipt.append("-----------------------------------------------\n");
            receipt.append("Order #: ").append(orderId).append("\n");
            receipt.append("Order Type: ").append(orderType).append("\n");
            receipt.append("Date: ").append(new java.util.Date()).append("\n");
            if (!orderNote.isEmpty()) {
                receipt.append("Note: ").append(orderNote).append("\n");
            }
            receipt.append("\nItems:\n");
            for (CartItem ci : cart) {
                receipt.append(String.format("  %-20s x%d  ₱%.2f\n", ci.item.name, ci.quantity, ci.getSubtotal()));
            }
            receipt.append("\n");
            receipt.append(String.format("%-22s  %s\n", "Subtotal:", String.format("₱%.2f", subtotal)));
            if (pwdApplied) {
                receipt.append(String.format("%-22s  %s\n", "PWD/Senior 20% off:", String.format("-₱%.2f", pwdDiscount)));
                receipt.append(String.format("%-22s  %s\n", "PWD Name:", pwdName));
                receipt.append(String.format("%-22s  %s\n", "PWD ID:", pwdId));
                receipt.append("*** PWD ID must be presented for validation ***\n");
                receipt.append(String.format("%-22s  %s\n", "After PWD:", String.format("₱%.2f", afterPwd)));
                receipt.append(String.format("%-22s  %s\n", "Tax (exempt):", "₱0.00"));
            } else {
                receipt.append(String.format("%-22s  %s\n", "Tax (12%):", String.format("₱%.2f", tax)));
            }
            if (appliedVoucher != null) {
                receipt.append(String.format("%-22s  %s\n", "Voucher (" + appliedVoucher.code + "):", String.format("-₱%.2f", voucherDiscount)));
            }
            receipt.append(String.format("%-22s  %s\n", "TOTAL:", String.format("₱%.2f", total)));
            receipt.append("\nPayment Method: ").append(paymentMethod);
            if (paymentMethod.equals("Cash")) {
                receipt.append(String.format("\nCash Tendered: ₱%.2f", cashTendered));
                receipt.append(String.format("\nChange: ₱%.2f", change));
            }
            receipt.append("\n-----------------------------------------------\n");
            receipt.append("          Thank you! Please come again.");

            JTextArea textArea = new JTextArea(receipt.toString());
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 500));
            JOptionPane.showMessageDialog(this, scrollPane, "Receipt", JOptionPane.INFORMATION_MESSAGE);

            cart.clear();
            resetDiscounts();
            orderNote = "";
            refreshDisplay();
            refreshCategoryButton();
            cardLayout.show(mainPanel, "welcome");
        }
    }

    class AdminFrame extends JFrame {
        private JTable menuTable;
        private DefaultTableModel menuTableModel;
        private JTable voucherTable;
        private DefaultTableModel voucherTableModel;

        public AdminFrame() {
            setTitle("Admin Control Panel");
            setSize(900, 600);
            setLocationRelativeTo(RestaurantKiosk.this);
            getContentPane().setBackground(Color.LIGHT_GRAY);
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Manage Menu", createMenuPanel());
            tabs.addTab("Manage Vouchers", createVoucherPanel());
            add(tabs);
        }

        private JPanel createMenuPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBackground(Color.LIGHT_GRAY);
            String[] cols = {"ID", "Name", "Category", "Price (₱)"};
            menuTableModel = new DefaultTableModel(cols, 0) {
                @Override
                public boolean isCellEditable(int row, int col) { return false; }
            };
            menuTable = new JTable(menuTableModel);
            menuTable.setFont(new Font(MAIN_FONT, Font.PLAIN, 14));
            loadMenuTable();

            JPanel btnPanel = new JPanel(new FlowLayout());
            btnPanel.setBackground(Color.LIGHT_GRAY);
            JButton addBtn = new JButton("Add Item");
            JButton editBtn = new JButton("Edit Item");
            JButton deleteBtn = new JButton("Delete Item");
            JButton refreshBtn = new JButton("Refresh");

            Font btnFont = new Font(MAIN_FONT, Font.BOLD, 12);
            addBtn.setFont(btnFont);
            editBtn.setFont(btnFont);
            deleteBtn.setFont(btnFont);
            refreshBtn.setFont(btnFont);
            addBtn.setBackground(Color.LIGHT_GRAY);
            editBtn.setBackground(Color.LIGHT_GRAY);
            deleteBtn.setBackground(Color.LIGHT_GRAY);
            refreshBtn.setBackground(Color.LIGHT_GRAY);

            addBtn.addActionListener(e -> showAddMenuDialog());
            editBtn.addActionListener(e -> showEditMenuDialog());
            deleteBtn.addActionListener(e -> deleteMenuItem());
            refreshBtn.addActionListener(e -> {
                loadMenuTable();
                if (categoryPanel != null) {
                    categoryPanel.loadCategories();
                    String selectedCat = categoryPanel.categoryList.getSelectedValue();
                    if (selectedCat != null) categoryPanel.loadMenuItemsForCategory(selectedCat);
                }
                originalImageCache.clear();
            });

            btnPanel.add(addBtn);
            btnPanel.add(editBtn);
            btnPanel.add(deleteBtn);
            btnPanel.add(refreshBtn);

            panel.add(new JScrollPane(menuTable), BorderLayout.CENTER);
            panel.add(btnPanel, BorderLayout.SOUTH);
            return panel;
        }

        private void loadMenuTable() {
            menuTableModel.setRowCount(0);
            List<MenuItem> items = MenuDAO.getAllItems();
            for (MenuItem mi : items) {
                menuTableModel.addRow(new Object[]{mi.id, mi.name, mi.category, mi.price});
            }
        }

        private void showAddMenuDialog() {
            JTextField nameField = new JTextField();
            JComboBox<String> catCombo = new JComboBox<>(MenuDAO.getCategories().toArray(new String[0]));
            catCombo.setEditable(true);
            JTextField priceField = new JTextField();
            Object[] msg = {"Name:", nameField, "Category (or type new):", catCombo, "Price:", priceField};
            int opt = JOptionPane.showConfirmDialog(this, msg, "Add Menu Item", JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.OK_OPTION) {
                try {
                    double price = Double.parseDouble(priceField.getText());
                    String category = catCombo.getEditor().getItem().toString().trim();
                    if (category.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Category cannot be empty.");
                        return;
                    }
                    MenuDAO.addItem(nameField.getText(), category, price);
                    loadMenuTable();
                    if (categoryPanel != null) {
                        categoryPanel.loadCategories();
                        String selectedCat = categoryPanel.categoryList.getSelectedValue();
                        if (selectedCat != null) categoryPanel.loadMenuItemsForCategory(selectedCat);
                    }
                    originalImageCache.clear();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid price");
                }
            }
        }

        private void showEditMenuDialog() {
            int row = menuTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select an item"); return; }
            int id = (int) menuTableModel.getValueAt(row, 0);
            String oldName = (String) menuTableModel.getValueAt(row, 1);
            String oldCat = (String) menuTableModel.getValueAt(row, 2);
            double oldPrice = (double) menuTableModel.getValueAt(row, 3);

            JTextField nameField = new JTextField(oldName);
            JComboBox<String> catCombo = new JComboBox<>(MenuDAO.getCategories().toArray(new String[0]));
            catCombo.setEditable(true);
            catCombo.setSelectedItem(oldCat);
            JTextField priceField = new JTextField(String.valueOf(oldPrice));
            Object[] msg = {"Name:", nameField, "Category (or type new):", catCombo, "Price:", priceField};
            int opt = JOptionPane.showConfirmDialog(this, msg, "Edit Item", JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.OK_OPTION) {
                try {
                    double price = Double.parseDouble(priceField.getText());
                    String category = catCombo.getEditor().getItem().toString().trim();
                    if (category.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Category cannot be empty.");
                        return;
                    }
                    MenuDAO.updateItem(id, nameField.getText(), category, price);
                    loadMenuTable();
                    if (categoryPanel != null) {
                        categoryPanel.loadCategories();
                        String selectedCat = categoryPanel.categoryList.getSelectedValue();
                        if (selectedCat != null) categoryPanel.loadMenuItemsForCategory(selectedCat);
                    }
                    originalImageCache.clear();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid price");
                }
            }
        }

        private void deleteMenuItem() {
            int row = menuTable.getSelectedRow();
            if (row == -1) return;
            int id = (int) menuTableModel.getValueAt(row, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Delete this item?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                MenuDAO.deleteItem(id);
                loadMenuTable();
                if (categoryPanel != null) {
                    categoryPanel.loadCategories();
                    String selectedCat = categoryPanel.categoryList.getSelectedValue();
                    if (selectedCat != null) categoryPanel.loadMenuItemsForCategory(selectedCat);
                }
                originalImageCache.remove(id);
            }
        }

        private JPanel createVoucherPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(Color.LIGHT_GRAY);
            String[] cols = {"Voucher Code", "Discount (₱)"};
            voucherTableModel = new DefaultTableModel(cols, 0);
            voucherTable = new JTable(voucherTableModel);
            voucherTable.setFont(new Font(MAIN_FONT, Font.PLAIN, 14));
            loadVoucherTable();

            JPanel btnPanel = new JPanel();
            btnPanel.setBackground(Color.LIGHT_GRAY);
            JButton addVoucher = new JButton("Add Voucher");
            JButton delVoucher = new JButton("Delete Voucher");
            Font btnFont = new Font(MAIN_FONT, Font.BOLD, 12);
            addVoucher.setFont(btnFont);
            delVoucher.setFont(btnFont);
            addVoucher.setBackground(Color.LIGHT_GRAY);
            delVoucher.setBackground(Color.LIGHT_GRAY);
            addVoucher.addActionListener(e -> {
                boolean exit = false;
                while (!exit) {
                    JTextField codeTextField = new JTextField(10);
                    JTextField discounTextField = new JTextField(10);
                    JPanel panelz = new JPanel();
                    panelz.add(new JLabel("Code:"));
                    panelz.add(codeTextField);
                    panelz.add(Box.createHorizontalStrut(10));
                    panelz.add(new JLabel("Discount:"));
                    panelz.add(discounTextField);
                    int result = JOptionPane.showConfirmDialog(this, panelz, "Voucher Addition", JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        String code = codeTextField.getText();
                        String discount = discounTextField.getText();
                        if (code != null && !code.isEmpty() && discount != null && !discount.isEmpty()) {
                            try {
                                double disc = Double.parseDouble(discount);
                                VoucherDAO.addVoucher(code, disc);
                                loadVoucherTable();
                                exit = true;
                            } catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(this, "Invalid number");
                            }
                        } else {
                            JOptionPane.showMessageDialog(this, "Please input a code / discount");
                        }
                    } else {
                        exit = true;
                    }
                }
            });
            delVoucher.addActionListener(e -> {
                int row = voucherTable.getSelectedRow();
                if (row >= 0) {
                    String code = (String) voucherTableModel.getValueAt(row, 0);
                    VoucherDAO.deleteVoucher(code);
                    loadVoucherTable();
                }
            });
            btnPanel.add(addVoucher);
            btnPanel.add(delVoucher);
            panel.add(new JScrollPane(voucherTable), BorderLayout.CENTER);
            panel.add(btnPanel, BorderLayout.SOUTH);
            return panel;
        }

        private void loadVoucherTable() {
            voucherTableModel.setRowCount(0);
            List<Voucher> vouchers = VoucherDAO.getAllVouchers();
            for (Voucher v : vouchers) {
                voucherTableModel.addRow(new Object[]{v.code, v.discount});
            }
        }
    }

    private void loadCategoryScreen() {
        cardLayout.show(mainPanel, "categories");
        if (categoryPanel != null) {
            categoryPanel.loadCategories();
        }
    }

    private void addToCart(MenuItem item, int quantity) {
        for (CartItem ci : cart) {
            if (ci.item.id == item.id) {
                ci.quantity += quantity;
                refreshCategoryButton();
                return;
            }
        }
        cart.add(new CartItem(item, quantity));
        refreshCategoryButton();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) { }
            new RestaurantKiosk().setVisible(true);
        });
    }
}
