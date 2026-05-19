package resto;

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
        String orderSql = "INSERT INTO orders (order_type, total, notes, pwd_name, pwd_id, payment_method, cash_tendered, change_amount, order_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
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

    public static double getTotalRevenueToday() {
        String sql = "SELECT COALESCE(SUM(total), 0) AS revenue " +
                "FROM orders WHERE DATE(order_time) = CURDATE()";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble("revenue");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getTotalOrdersToday() {
        String sql = "SELECT COUNT(*) AS total FROM orders " +
                "WHERE DATE(order_time) = CURDATE()";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static List<String> getRecentOrders() {
        List<String> orders = new ArrayList<>();
        String sql = "SELECT id, order_type, total, order_time FROM orders ORDER BY order_time DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String text = "Order #" + String.format("%04d", rs.getInt("id")) +
                        " | " + String.format("%-8s", rs.getString("order_type")) +
                        " | ₱" + String.format("%.2f", rs.getDouble("total")) +
                        " | " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(rs.getTimestamp("order_time"));
                orders.add(text);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
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
    private String selectedCategory = "";

    private static final String MAIN_FONT = "Segoe UI";

    private static Map<Integer, BufferedImage> originalImageCache = new HashMap<>();

    public RestaurantKiosk() {
        setTitle("Kaon & Kain Filipino Cuisine Kiosk");
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
        private Image bgImage;

        public WelcomePanel() {
            setLayout(new BorderLayout());

            try {
                bgImage = javax.imageio.ImageIO.read(new java.io.File("menu_images/welcome_bg.png"));
            } catch (Exception e) {
                System.out.println("Welcome background image not found!");
            }

            JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
            topRow.setOpaque(false);
            JButton admin = new JButton("⚙");
            admin.setForeground(Color.WHITE);
            try {
                admin.setFont(new Font("Segoe UI Symbol", Font.BOLD, 28));
            } catch (Exception e) {
                admin.setFont(new Font("SansSerif", Font.BOLD, 28));
            }
            admin.setContentAreaFilled(false);
            admin.setBorderPainted(false);
            admin.setFocusPainted(false);
            admin.setCursor(new Cursor(Cursor.HAND_CURSOR));
            admin.setPreferredSize(new Dimension(50, 50));
            topRow.add(admin);
            add(topRow, BorderLayout.NORTH);

            JPanel centerPanel = new JPanel(new GridBagLayout());
            centerPanel.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            
            gbc.insets = new Insets(380, 10, 10, 10); 

            JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
            buttonContainer.setOpaque(false);

            JButton dineIn = createExactMenuButton(
                "Dine-In", 
                "Order for dining in", 
                "menu_images/dine_in_icon.jpg", 
                Color.decode("#2e5a44"),
                Color.decode("#fdfbf7")
            );

            JButton takeOut = createExactMenuButton(
                "Take-Out", 
                "Order for take-out", 
                "menu_images/take_out_icon.jpg", 
                Color.decode("#b89765"),
                Color.decode("#fdfbf7")
            );

            buttonContainer.add(dineIn);
            buttonContainer.add(takeOut);
            centerPanel.add(buttonContainer, gbc);
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

        private JButton createExactMenuButton(String title, String subtitle, String iconPath, Color themeColor, Color bgColor) {
            JButton btn = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };

            btn.setLayout(new GridBagLayout());
            btn.setBackground(bgColor);
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setBorder(BorderFactory.createLineBorder(themeColor, 2));
            btn.setPreferredSize(new Dimension(250, 90)); 
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            GridBagConstraints c = new GridBagConstraints();
            
            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 2; 
            c.anchor = GridBagConstraints.CENTER;
            c.insets = new Insets(0, 15, 0, 15);

            JLabel iconLabel = new JLabel();
            try {
                ImageIcon origIcon = new ImageIcon(iconPath);
                Image scaledImg = origIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(scaledImg));
            } catch (Exception e) {
                iconLabel.setText("☕"); 
                iconLabel.setFont(new Font("Times New Roman", Font.PLAIN, 28));
                iconLabel.setForeground(themeColor);
            }
            btn.add(iconLabel, c);

            c.gridx = 1;
            c.gridy = 0;
            c.gridheight = 1;
            c.anchor = GridBagConstraints.SOUTHWEST;
            c.insets = new Insets(0, 0, 2, 15);

            JLabel mainLabel = new JLabel(title);
            mainLabel.setFont(new Font("Times New Roman", Font.BOLD, 24));
            mainLabel.setForeground(themeColor); 
            btn.add(mainLabel, c);

            c.gridy = 1;
            c.anchor = GridBagConstraints.NORTHWEST; 
            c.insets = new Insets(2, 0, 0, 15);

            JLabel subLabel = new JLabel(subtitle);
            subLabel.setFont(new Font("Times New Roman", Font.PLAIN, 12));
            subLabel.setForeground(Color.decode("#555555")); 
            btn.add(subLabel, c);

            return btn;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bgImage != null) {
                g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        private boolean authenticateAdmin() {
            String pass = JOptionPane.showInputDialog(this, "Enter Admin PIN:");
            return "1234".equals(pass);
        }
    }

    class CategoryPanel extends JPanel {
        private JButton viewCartButton;
        private JPanel menuTilePanel;
        private String selectedCategory = "";

        public CategoryPanel() {
            setLayout(new BorderLayout());
            setBackground(Color.decode("#fdfbf7"));

            JPanel leftSidebar = new JPanel(new BorderLayout());
            leftSidebar.setBackground(Color.decode("#f5efe4")); 
            leftSidebar.setPreferredSize(new Dimension(220, 0));
            leftSidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.decode("#e6dfd3")));

            JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
            logoPanel.setOpaque(false);
            JLabel logoLabel = new JLabel();
            try {
                ImageIcon origLogo = new ImageIcon("menu_images/logo.jpg");
                Image scaledLogo = origLogo.getImage().getScaledInstance(140, 110, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaledLogo));
            } catch (Exception e) {
                logoLabel.setText("Kaon & Kain");
                logoLabel.setFont(new Font("Times New Roman", Font.BOLD, 20));
                logoLabel.setForeground(Color.decode("#4a3b32"));
            }
            logoPanel.add(logoLabel);
            leftSidebar.add(logoPanel, BorderLayout.NORTH);

            JPanel whiteCardWrapper = new JPanel(new BorderLayout());
            whiteCardWrapper.setOpaque(false);
            whiteCardWrapper.setBorder(new EmptyBorder(0, 12, 20, 12)); 

            JPanel outerShadowPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    Color shadowColor = Color.decode("#d49b4d");
                    g2.setColor(new Color(shadowColor.getRed(), shadowColor.getGreen(), shadowColor.getBlue(), 40));
                    for (int i = 0; i < 5; i++) {
                        g2.fillRoundRect(2 + i, 2 + i, getWidth() - 4 - i, getHeight() - 4 - i, 16, 16);
                    }
                    
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 16, 16);
                    
                    g2.setColor(Color.decode("#e6dfd3"));
                    g2.drawRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 16, 16);
                    g2.dispose();
                }
            };
            outerShadowPanel.setOpaque(false);
            outerShadowPanel.setBorder(new EmptyBorder(15, 12, 18, 14));

            JPanel sidebarButtonsPanel = new JPanel();
            sidebarButtonsPanel.setOpaque(false);
            sidebarButtonsPanel.setLayout(new BoxLayout(sidebarButtonsPanel, BoxLayout.Y_AXIS));

            JLabel titleLabel = new JLabel("CATEGORIES");
            titleLabel.setFont(new Font("Times New Roman", Font.BOLD, 15));
            titleLabel.setForeground(Color.decode("#354d2b")); 
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            titleLabel.setBorder(new EmptyBorder(0, 2, 6, 2));
            
            JSeparator catLine = new JSeparator();
            catLine.setForeground(Color.decode("#e6dfd3"));
            catLine.setMaximumSize(new Dimension(165, 2));
            catLine.setAlignmentX(Component.LEFT_ALIGNMENT);

            outerShadowPanel.add(titleLabel, BorderLayout.NORTH);
            
            JPanel listWrapper = new JPanel();
            listWrapper.setOpaque(false);
            listWrapper.setLayout(new BoxLayout(listWrapper, BoxLayout.Y_AXIS));
            listWrapper.setBorder(new EmptyBorder(6, 0, 0, 0));
            listWrapper.add(catLine);
            listWrapper.add(Box.createVerticalStrut(10));
            listWrapper.add(sidebarButtonsPanel);

            outerShadowPanel.add(listWrapper, BorderLayout.CENTER);
            whiteCardWrapper.add(outerShadowPanel, BorderLayout.CENTER);
            leftSidebar.add(whiteCardWrapper, BorderLayout.CENTER);
            add(leftSidebar, BorderLayout.WEST);

            JPanel mainContent = new JPanel(new BorderLayout(0, 0));
            mainContent.setOpaque(false);
            mainContent.setBorder(new EmptyBorder(25, 30, 15, 30));

            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setOpaque(false);
            JLabel header = new JLabel("Select Category & Add Item");
            header.setFont(new Font("Times New Roman", Font.BOLD, 36)); 
            header.setForeground(Color.decode("#1a1a1a"));
            headerPanel.add(header, BorderLayout.WEST);

            JSeparator titleLine = new JSeparator();
            titleLine.setForeground(Color.decode("#b89765"));
            titleLine.setPreferredSize(new Dimension(0, 2));
            titleLine.setBorder(new EmptyBorder(8, 0, 0, 0));
            headerPanel.add(titleLine, BorderLayout.SOUTH);

            JPanel gridWrapperPanel = new JPanel(new BorderLayout());
            gridWrapperPanel.setOpaque(false);
            gridWrapperPanel.setBorder(new EmptyBorder(25, 0, 0, 0)); 

            JPanel gridContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            gridContainer.setBackground(Color.decode("#fdfbf7"));

            menuTilePanel = new JPanel(new GridLayout(0, 3, 15, 15));
            menuTilePanel.setBackground(Color.decode("#fdfbf7"));
            gridContainer.add(menuTilePanel);
            gridWrapperPanel.add(gridContainer, BorderLayout.CENTER);

            JScrollPane tileScroll = new JScrollPane(gridWrapperPanel); 
            tileScroll.getViewport().setBackground(Color.decode("#fdfbf7"));
            tileScroll.setBorder(BorderFactory.createEmptyBorder()); 
            tileScroll.getVerticalScrollBar().setUnitIncrement(22);
            tileScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            mainContent.add(headerPanel, BorderLayout.NORTH);
            mainContent.add(tileScroll, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 12));
            bottomPanel.setOpaque(false);

            viewCartButton = new JButton("View Cart (" + cart.size() + " item/s)") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            viewCartButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
            viewCartButton.setBackground(Color.decode("#2e4726")); 
            viewCartButton.setForeground(Color.WHITE);
            viewCartButton.setFocusPainted(false);
            viewCartButton.setContentAreaFilled(false);
            viewCartButton.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));
            viewCartButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            viewCartButton.addActionListener(e -> cardLayout.show(mainPanel, "cart"));

            JButton backBtn = new JButton("Back to Welcome") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(Color.decode("#d1b47c"));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            backBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
            backBtn.setBackground(Color.decode("#fdfbf7"));
            backBtn.setForeground(Color.BLACK);
            backBtn.setFocusPainted(false);
            backBtn.setContentAreaFilled(false);
            backBtn.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));
            backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            backBtn.addActionListener(e -> cardLayout.show(mainPanel, "welcome"));

            bottomPanel.add(viewCartButton);
            bottomPanel.add(backBtn);
            mainContent.add(bottomPanel, BorderLayout.SOUTH);

            add(mainContent, BorderLayout.CENTER);
            
            // Load categories
            List<String> categories = MenuDAO.getCategories();
            for (String cat : categories) {
                JButton catBtn = new JButton(cat);
                catBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
                catBtn.setFocusPainted(false);
                catBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                catBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
                catBtn.setMaximumSize(new Dimension(170, 36));
                catBtn.setHorizontalAlignment(SwingConstants.LEFT);
                catBtn.setContentAreaFilled(false);
                catBtn.setOpaque(false);
                catBtn.setForeground(Color.decode("#5a423d"));
                catBtn.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
                
                catBtn.addActionListener(e -> {
                    selectedCategory = cat;
                    loadMenuItemsForCategory(cat);
                    // Highlight selected
                    for (Component comp : sidebarButtonsPanel.getComponents()) {
                        if (comp instanceof JButton) {
                            JButton btn = (JButton) comp;
                            if (btn.getText().equals(cat)) {
                                btn.setOpaque(true);
                                btn.setBackground(Color.decode("#6b7a3a"));
                                btn.setForeground(Color.WHITE);
                            } else {
                                btn.setOpaque(false);
                                btn.setForeground(Color.decode("#5a423d"));
                            }
                        }
                    }
                });
                
                sidebarButtonsPanel.add(catBtn);
                sidebarButtonsPanel.add(Box.createVerticalStrut(5));
            }
            
            if (!categories.isEmpty()) {
                selectedCategory = categories.get(0);
                loadMenuItemsForCategory(selectedCategory);
                // Highlight first category
                if (sidebarButtonsPanel.getComponent(0) instanceof JButton) {
                    JButton firstBtn = (JButton) sidebarButtonsPanel.getComponent(0);
                    firstBtn.setOpaque(true);
                    firstBtn.setBackground(Color.decode("#6b7a3a"));
                    firstBtn.setForeground(Color.WHITE);
                }
            }
        }

        public void loadMenuItemsForCategory(String category) {
            menuTilePanel.removeAll();
            List<MenuItem> items = MenuDAO.getItemsByCategory(category);

            for (MenuItem item : items) {
                JPanel itemCard = createMenuItemCard(item);
                menuTilePanel.add(itemCard);
            }

            int itemCount = items.size();
            int rows = (int) Math.ceil((double) itemCount / 3);
            int totalHeight = (rows * 260) + ((rows - 1) * 15); 
            menuTilePanel.setPreferredSize(new Dimension(660, totalHeight));

            menuTilePanel.revalidate();
            menuTilePanel.repaint();
        }

        private JPanel createMenuItemCard(MenuItem item) {
            JPanel shadowWrapper = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    g2.setColor(new Color(0, 0, 0, 12));
                    g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 14, 14);
                    
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 14, 14);
                    
                    g2.setColor(Color.decode("#e6dfd3"));
                    g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 14, 14);
                    g2.dispose();
                }
            };
            shadowWrapper.setOpaque(false);
            shadowWrapper.setPreferredSize(new Dimension(195, 245));
            shadowWrapper.setBorder(new EmptyBorder(0, 0, 4, 4));

            JPanel mainCardContent = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                    super.paintComponent(g2);
                    g2.dispose();
                }
            };
            mainCardContent.setOpaque(false);

            JLabel imgLabel = new JLabel();
            imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imgLabel.setPreferredSize(new Dimension(195, 105)); 
            
            BufferedImage originalImg = loadOriginalImage(item);
            if (originalImg != null) {
                int targetW = 195;
                int targetH = 105;
                BufferedImage croppedImg = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = croppedImg.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                
                double srcWidth = originalImg.getWidth();
                double srcHeight = originalImg.getHeight();
                double scale = Math.max(targetW / srcWidth, targetH / srcHeight);
                
                int finalW = (int) (srcWidth * scale);
                int finalH = (int) (srcHeight * scale);
                int x = (targetW - finalW) / 2;
                int y = (targetH - finalH) / 2;
                
                g2.drawImage(originalImg, x, y, finalW, finalH, null);
                g2.dispose();
                imgLabel.setIcon(new ImageIcon(croppedImg));
            } else {
                imgLabel.setText("[ No Image ]");
                imgLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                imgLabel.setBackground(Color.decode("#f0ece1"));
                imgLabel.setOpaque(true);
            }

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            mainCardContent.add(imgLabel, gbc);

            JPanel infoPanel = new JPanel(new GridBagLayout());
            infoPanel.setOpaque(false);
            infoPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
            GridBagConstraints c = new GridBagConstraints();

            JLabel nameLabel = new JLabel(item.name);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            nameLabel.setForeground(Color.decode("#1a1a1a"));
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 2;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 2, 0);
            infoPanel.add(nameLabel, c);

            JLabel priceLabel = new JLabel(String.format("₱%.2f", item.price));
            priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            priceLabel.setForeground(Color.decode("#3b542c")); 
            c.gridy = 1;
            c.insets = new Insets(0, 0, 8, 0);
            infoPanel.add(priceLabel, c);

            JPanel actionRow = new JPanel(new BorderLayout(6, 0)); 
            actionRow.setOpaque(false);

            JButton addBtn = new JButton("Add to Cart") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            addBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
            addBtn.setBackground(Color.decode("#ca934a")); 
            addBtn.setForeground(Color.WHITE);
            addBtn.setFocusPainted(false);
            addBtn.setContentAreaFilled(false);
            addBtn.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JSpinner qtyBox = new JSpinner(new SpinnerNumberModel(0, 0, 20, 1));
            qtyBox.setPreferredSize(new Dimension(42, 25));
            qtyBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            JComponent editor = qtyBox.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                ((JSpinner.DefaultEditor) editor).getTextField().setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
                ((JSpinner.DefaultEditor) editor).getTextField().setBackground(Color.WHITE);
                ((JSpinner.DefaultEditor) editor).getTextField().setEditable(false);
            }
            qtyBox.setBorder(BorderFactory.createLineBorder(Color.decode("#cccccc"), 1));

            actionRow.add(addBtn, BorderLayout.CENTER);
            actionRow.add(qtyBox, BorderLayout.EAST);

            c.gridy = 2;
            c.gridx = 0;
            c.gridwidth = 2;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 0, 0);
            infoPanel.add(actionRow, c);

            gbc.gridy = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1.0;
            mainCardContent.add(infoPanel, gbc);

            shadowWrapper.add(mainCardContent, BorderLayout.CENTER);

            addBtn.addActionListener(e -> {
                int qty = (Integer) qtyBox.getValue();
                if (qty <= 0) {
                    JOptionPane.showMessageDialog(this, "Please increase the quantity before adding to cart!", "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                addToCart(item, qty);
                JOptionPane.showMessageDialog(this, qty + " x " + item.name + " added to cart!");
                qtyBox.setValue(0);
                updateCartButton();
            });

            return shadowWrapper;
        }

        public void updateCartButton() {
            viewCartButton.setText("View Cart (" + cart.size() + " item/s)");
        }
    }

    class CartPanel extends JPanel {
        private JPanel itemsContainer;
        private JLabel subtotalLabel, pwdDiscountLabel, afterPwdLabel, voucherLabel, taxLabel, totalLabel;
        private JButton applyPwdBtn, clearPwdBtn;
        private JTextField voucherField;
        private JLabel orderTitleLabel;
        private JTextField noteField;

        public CartPanel() {
            setLayout(new BorderLayout(0, 0));
            setBackground(Color.decode("#fdfbf7"));

            JPanel leftLayoutMain = new JPanel(new BorderLayout());
            leftLayoutMain.setOpaque(false);

            JPanel titleContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
            titleContainer.setOpaque(false);
            orderTitleLabel = new JLabel("Your Order (" + orderType + ")");
            orderTitleLabel.setFont(new Font("Times New Roman", Font.BOLD, 38));
            orderTitleLabel.setForeground(Color.decode("#1a1a1a"));
            titleContainer.add(orderTitleLabel);
            leftLayoutMain.add(titleContainer, BorderLayout.NORTH);

            itemsContainer = new JPanel();
            itemsContainer.setBackground(Color.decode("#fdfbf7"));
            itemsContainer.setLayout(new BoxLayout(itemsContainer, BoxLayout.Y_AXIS));

            JScrollPane cartScroll = new JScrollPane(itemsContainer);
            cartScroll.setBorder(BorderFactory.createEmptyBorder());
            cartScroll.getVerticalScrollBar().setUnitIncrement(16);
            cartScroll.getViewport().setBackground(Color.decode("#fdfbf7"));
            leftLayoutMain.add(cartScroll, BorderLayout.CENTER);

            JPanel bottomNotePanel = new JPanel(new GridBagLayout());
            bottomNotePanel.setBackground(Color.decode("#d9a762"));
            bottomNotePanel.setBorder(new EmptyBorder(15, 25, 15, 25));
            GridBagConstraints bgcNote = new GridBagConstraints();

            JButton specialInstBtn = new JButton("Add Special Instructions") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            specialInstBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            specialInstBtn.setBackground(new Color(179, 130, 66, 180));
            specialInstBtn.setForeground(Color.WHITE);
            specialInstBtn.setContentAreaFilled(false);
            specialInstBtn.setFocusPainted(false);
            specialInstBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
            ));
            specialInstBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JPanel noteInputWrapper = new JPanel(new BorderLayout(0, 2));
            noteInputWrapper.setOpaque(false);
            JLabel noteLabel = new JLabel("Note:");
            noteLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            noteLabel.setForeground(Color.decode("#2e1f10"));
            
            noteField = new JTextField();
            noteField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            noteField.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            noteInputWrapper.add(noteLabel, BorderLayout.NORTH);
            noteInputWrapper.add(noteField, BorderLayout.CENTER);

            bgcNote.gridx = 0;
            bgcNote.fill = GridBagConstraints.NONE;
            bgcNote.insets = new Insets(8, 0, 0, 20);
            bottomNotePanel.add(specialInstBtn, bgcNote);

            bgcNote.gridx = 1;
            bgcNote.weightx = 1.0;
            bgcNote.fill = GridBagConstraints.HORIZONTAL;
            bgcNote.insets = new Insets(0, 0, 0, 0);
            bottomNotePanel.add(noteInputWrapper, bgcNote);
            leftLayoutMain.add(bottomNotePanel, BorderLayout.SOUTH);

            add(leftLayoutMain, BorderLayout.CENTER);

            JPanel rightSidebar = new JPanel();
            rightSidebar.setBackground(Color.decode("#fdfbf7"));
            rightSidebar.setPreferredSize(new Dimension(330, 0));
            rightSidebar.setLayout(new BoxLayout(rightSidebar, BoxLayout.Y_AXIS));
            rightSidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.decode("#e6dfd3")));

            JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
            logoPanel.setOpaque(false);
            JLabel logoLabel = new JLabel();
            try {
                ImageIcon origLogo = new ImageIcon("menu_images/logo.png");
                Image scaledLogo = origLogo.getImage().getScaledInstance(140, 110, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaledLogo));
            } catch (Exception e) {
                logoLabel.setText("Kaon & Kain");
                logoLabel.setFont(new Font("Times New Roman", Font.BOLD, 22));
                logoLabel.setForeground(Color.decode("#4a3b32"));
            }
            logoPanel.add(logoLabel);
            rightSidebar.add(logoPanel);

            JPanel summaryCardOuter = new JPanel(new BorderLayout());
            summaryCardOuter.setOpaque(false);
            summaryCardOuter.setBorder(new EmptyBorder(0, 15, 10, 15));

            JPanel summaryCard = new JPanel();
            summaryCard.setBackground(Color.decode("#fdfbf7"));
            summaryCard.setLayout(new GridBagLayout());
            
            TitledBorder sBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.decode("#c5bba8"), 1), 
                "ORDER SUMMARY"
            );
            sBorder.setTitleFont(new Font("Times New Roman", Font.BOLD, 15));
            sBorder.setTitleColor(Color.decode("#563b2e"));
            summaryCard.setBorder(BorderFactory.createCompoundBorder(sBorder, new EmptyBorder(15, 15, 15, 15)));

            subtotalLabel = new JLabel("₱0.00");
            pwdDiscountLabel = new JLabel("₱0.00");
            afterPwdLabel = new JLabel("₱0.00");
            voucherLabel = new JLabel("₱0.00");
            taxLabel = new JLabel("₱0.00");
            totalLabel = new JLabel("₱0.00");

            applyPwdBtn = new JButton("Apply PWD/Senior Discount") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            applyPwdBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            applyPwdBtn.setBackground(Color.decode("#ca934a"));
            applyPwdBtn.setForeground(Color.WHITE);
            applyPwdBtn.setContentAreaFilled(false);
            applyPwdBtn.setFocusPainted(false);
            applyPwdBtn.setBorder(new EmptyBorder(5, 10, 5, 10));
            applyPwdBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            applyPwdBtn.addActionListener(e -> applyPwdDiscount());

            clearPwdBtn = new JButton("Clear PWD Discount") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            clearPwdBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            clearPwdBtn.setBackground(Color.decode("#ca934a"));
            clearPwdBtn.setForeground(Color.WHITE);
            clearPwdBtn.setContentAreaFilled(false);
            clearPwdBtn.setFocusPainted(false);
            clearPwdBtn.setBorder(new EmptyBorder(5, 10, 5, 10));
            clearPwdBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            clearPwdBtn.addActionListener(e -> {
                pwdApplied = false;
                pwdName = "";
                pwdId = "";
                refreshDisplay();
                JOptionPane.showMessageDialog(this, "PWD discount removed!");
            });

            int rowIdx = 0;
            addSummaryRow(summaryCard, "Subtotal:", subtotalLabel, rowIdx++, false);
            addSummaryRow(summaryCard, "PWD/Senior Discount:", pwdDiscountLabel, rowIdx++, false);
            
            GridBagConstraints gbcBtn = new GridBagConstraints();
            gbcBtn.gridx = 0; gbcBtn.gridy = rowIdx++; gbcBtn.gridwidth = 2;
            gbcBtn.anchor = GridBagConstraints.WEST; gbcBtn.insets = new Insets(4, 0, 4, 0);
            summaryCard.add(applyPwdBtn, gbcBtn);
            
            gbcBtn.gridy = rowIdx++;
            summaryCard.add(clearPwdBtn, gbcBtn);

            addSummaryRow(summaryCard, "After PWD Discount:", afterPwdLabel, rowIdx++, false);
            addSummaryRow(summaryCard, "Voucher Discount:", voucherLabel, rowIdx++, false);
            addSummaryRow(summaryCard, "Tax (12%):", taxLabel, rowIdx++, false);

            GridBagConstraints gbcSep = new GridBagConstraints();
            gbcSep.gridx = 0; gbcSep.gridy = rowIdx++; gbcSep.gridwidth = 2; gbcSep.fill = GridBagConstraints.HORIZONTAL;
            gbcSep.insets = new Insets(10, 0, 10, 0);
            summaryCard.add(new JSeparator(), gbcSep);

            JLabel totalTitle = new JLabel("TOTAL:");
            totalTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
            totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
            GridBagConstraints gbcT1 = new GridBagConstraints();
            gbcT1.gridx = 0; gbcT1.gridy = rowIdx; gbcT1.anchor = GridBagConstraints.WEST;
            summaryCard.add(totalTitle, gbcT1);
            GridBagConstraints gbcT2 = new GridBagConstraints();
            gbcT2.gridx = 1; gbcT2.gridy = rowIdx++; gbcT2.anchor = GridBagConstraints.EAST;
            summaryCard.add(totalLabel, gbcT2);

            JLabel promoTitle = new JLabel("Promo Code:");
            promoTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
            promoTitle.setForeground(Color.decode("#4a3b32"));
            GridBagConstraints gbcP1 = new GridBagConstraints();
            gbcP1.gridx = 0; gbcP1.gridy = rowIdx++; gbcP1.gridwidth = 2; gbcP1.anchor = GridBagConstraints.WEST;
            gbcP1.insets = new Insets(12, 0, 4, 0);
            summaryCard.add(promoTitle, gbcP1);

            JPanel promoInputRow = new JPanel(new BorderLayout(8, 0));
            promoInputRow.setOpaque(false);
            voucherField = new JTextField();
            voucherField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            voucherField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#cccccc"), 1),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)
            ));
            
            JButton applyVoucherBtn = new JButton("Apply Voucher") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            applyVoucherBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            applyVoucherBtn.setBackground(Color.decode("#ca934a"));
            applyVoucherBtn.setForeground(Color.WHITE);
            applyVoucherBtn.setContentAreaFilled(false);
            applyVoucherBtn.setFocusPainted(false);
            applyVoucherBtn.setBorder(new EmptyBorder(5, 12, 5, 12));
            applyVoucherBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
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
            promoInputRow.add(voucherField, BorderLayout.CENTER);
            promoInputRow.add(applyVoucherBtn, BorderLayout.EAST);

            GridBagConstraints gbcP2 = new GridBagConstraints();
            gbcP2.gridx = 0; gbcP2.gridy = rowIdx++; gbcP2.gridwidth = 2; gbcP2.fill = GridBagConstraints.HORIZONTAL;
            summaryCard.add(promoInputRow, gbcP2);

            summaryCardOuter.add(summaryCard, BorderLayout.CENTER);
            rightSidebar.add(summaryCardOuter);

            JPanel actionButtonsContainer = new JPanel();
            actionButtonsContainer.setOpaque(false);
            actionButtonsContainer.setLayout(new BoxLayout(actionButtonsContainer, BoxLayout.Y_AXIS));
            actionButtonsContainer.setBorder(new EmptyBorder(5, 15, 15, 15));

            JButton proceedBtn = new JButton("Proceed to Payment") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            proceedBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
            proceedBtn.setBackground(Color.decode("#5e361b"));
            proceedBtn.setForeground(Color.WHITE);
            proceedBtn.setContentAreaFilled(false);
            proceedBtn.setFocusPainted(false);
            proceedBtn.setBorder(new EmptyBorder(12, 0, 12, 0));
            proceedBtn.setMaximumSize(new Dimension(310, 45));
            proceedBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            proceedBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            proceedBtn.addActionListener(e -> processPayment());

            JPanel row2 = new JPanel(new GridLayout(1, 2, 10, 0));
            row2.setOpaque(false);
            row2.setMaximumSize(new Dimension(310, 36));

            JButton removeBtn = new JButton("Remove Selected") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            removeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            removeBtn.setBackground(Color.decode("#ca934a"));
            removeBtn.setForeground(Color.WHITE);
            removeBtn.setContentAreaFilled(false);
            removeBtn.setFocusPainted(false);
            removeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JButton clearBtn = new JButton("Clear Cart") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            clearBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            clearBtn.setBackground(Color.decode("#ca934a"));
            clearBtn.setForeground(Color.WHITE);
            clearBtn.setContentAreaFilled(false);
            clearBtn.setFocusPainted(false);
            clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            row2.add(removeBtn);
            row2.add(clearBtn);

            JButton backBtn = new JButton("Back to Categories") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            backBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            backBtn.setBackground(Color.decode("#ca934a"));
            backBtn.setForeground(Color.WHITE);
            backBtn.setContentAreaFilled(false);
            backBtn.setFocusPainted(false);
            backBtn.setBorder(new EmptyBorder(10, 0, 10, 0));
            backBtn.setMaximumSize(new Dimension(310, 40));
            backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            actionButtonsContainer.add(proceedBtn);
            actionButtonsContainer.add(Box.createVerticalStrut(12));
            actionButtonsContainer.add(row2);
            actionButtonsContainer.add(Box.createVerticalStrut(12));
            actionButtonsContainer.add(backBtn);

            rightSidebar.add(actionButtonsContainer);
            add(rightSidebar, BorderLayout.EAST);

            removeBtn.addActionListener(e -> {
                JOptionPane.showMessageDialog(this, "To change item quantity or remove, please adjust using the row controllers (+ / -) directly!");
            });

            clearBtn.addActionListener(e -> {
                cart.clear();
                resetDiscounts();
                refreshDisplay();
                refreshCategoryButton();
            });

            backBtn.addActionListener(e -> cardLayout.show(mainPanel, "categories"));
            refreshDisplay();
        }

        public void updateOrderType(String type) {
            if (orderTitleLabel != null) {
                orderTitleLabel.setText("Your Order (" + type + ")");
            }
        }

        private void addSummaryRow(JPanel panel, String titleText, JLabel valueLabel, int row, boolean isBold) {
            JLabel titleLabel = new JLabel(titleText);
            titleLabel.setFont(new Font("Segoe UI", isBold ? Font.BOLD : Font.PLAIN, 14));
            titleLabel.setForeground(Color.decode("#1a1a1a"));
            valueLabel.setFont(new Font("Segoe UI", isBold ? Font.BOLD : Font.PLAIN, 14));
            valueLabel.setForeground(Color.decode("#1a1a1a"));

            GridBagConstraints gbcL = new GridBagConstraints();
            gbcL.gridx = 0; gbcL.gridy = row; gbcL.weightx = 1.0;
            gbcL.anchor = GridBagConstraints.WEST; gbcL.insets = new Insets(4, 0, 4, 0);
            panel.add(titleLabel, gbcL);

            GridBagConstraints gbcR = new GridBagConstraints();
            gbcR.gridx = 1; gbcR.gridy = row; gbcR.weightx = 0.0;
            gbcR.anchor = GridBagConstraints.EAST; gbcR.insets = new Insets(4, 0, 4, 0);
            panel.add(valueLabel, gbcR);
        }

        public void refreshDisplay() {
            if (orderTitleLabel != null) {
                orderTitleLabel.setText("Your Order (" + orderType + ")");
            }

            itemsContainer.removeAll();

            for (CartItem ci : cart) {
                JPanel rowCard = createExactCartRow(ci);
                itemsContainer.add(rowCard);
                itemsContainer.add(Box.createVerticalStrut(1)); 
            }

            double subtotal = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
            double pwdDiscount = 0.0;
            if (pwdApplied && !cart.isEmpty()) {
                pwdDiscount = subtotal * 0.20; // Fixed: Apply to subtotal, not just max item
            }
            double afterPwd = subtotal - pwdDiscount;
            double voucherDiscount = (appliedVoucher != null) ? appliedVoucher.discount : 0;
            double afterDiscount = afterPwd - voucherDiscount;
            double tax = afterDiscount * TAX_RATE; // Fixed: Tax on discounted amount
            double total = afterDiscount + tax;

            subtotalLabel.setText("₱" + String.format("%.2f", subtotal));
            pwdDiscountLabel.setText("₱" + String.format("%.2f", pwdDiscount));
            afterPwdLabel.setText("₱" + String.format("%.2f", afterPwd));
            voucherLabel.setText("₱" + String.format("%.2f", voucherDiscount));
            taxLabel.setText("₱" + String.format("%.2f", tax));
            totalLabel.setText("₱" + String.format("%.2f", total));

            itemsContainer.revalidate();
            itemsContainer.repaint();
        }

        private JPanel createExactCartRow(CartItem ci) {
            JPanel row = new JPanel(new BorderLayout());
            row.setBackground(Color.decode("#fdfbf7"));
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.decode("#ebdcb9")),
                new EmptyBorder(15, 25, 15, 35)
            ));
            row.setMaximumSize(new Dimension(2000, 110));

            JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
            leftGroup.setOpaque(false);

            JLabel imgLabel = new JLabel();
            imgLabel.setPreferredSize(new Dimension(105, 75));
            BufferedImage bImg = loadOriginalImage(ci.item);
            if (bImg != null) {
                Image sc = bImg.getScaledInstance(105, 75, Image.SCALE_SMOOTH);
                imgLabel.setIcon(new ImageIcon(sc));
            } else {
                imgLabel.setBackground(Color.decode("#ebdcb9"));
                imgLabel.setOpaque(true);
            }
            leftGroup.add(imgLabel);

            JPanel textDetails = new JPanel();
            textDetails.setOpaque(false);
            textDetails.setLayout(new BoxLayout(textDetails, BoxLayout.Y_AXIS));

            JLabel title = new JLabel(ci.item.name);
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            title.setForeground(Color.BLACK);

            JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
            controlRow.setOpaque(false);

            JButton minusBtn = new JButton("-") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            minusBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            minusBtn.setBackground(Color.decode("#ca934a"));
            minusBtn.setForeground(Color.WHITE);
            minusBtn.setContentAreaFilled(false);
            minusBtn.setBorder(BorderFactory.createEmptyBorder());
            minusBtn.setPreferredSize(new Dimension(24, 24));
            minusBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JLabel qtyLabel = new JLabel(String.valueOf(ci.quantity), SwingConstants.CENTER);
            qtyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            qtyLabel.setPreferredSize(new Dimension(32, 24));

            JButton plusBtn = new JButton("+") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            plusBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            plusBtn.setBackground(Color.decode("#ca934a"));
            plusBtn.setForeground(Color.WHITE);
            plusBtn.setContentAreaFilled(false);
            plusBtn.setBorder(BorderFactory.createEmptyBorder());
            plusBtn.setPreferredSize(new Dimension(24, 24));
            plusBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            controlRow.add(minusBtn);
            controlRow.add(qtyLabel);
            controlRow.add(plusBtn);

            textDetails.add(title);
            textDetails.add(Box.createVerticalStrut(4));
            textDetails.add(controlRow);
            leftGroup.add(textDetails);
            row.add(leftGroup, BorderLayout.WEST);

            JPanel rightGroup = new JPanel();
            rightGroup.setOpaque(false);
            rightGroup.setLayout(new BoxLayout(rightGroup, BoxLayout.Y_AXIS));

            JLabel priceLabel = new JLabel("₱" + String.format("%.2f", ci.item.price));
            priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            priceLabel.setForeground(Color.decode("#2e4726"));
            priceLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

            JLabel subtotalRowLabel = new JLabel("x" + ci.quantity);
            subtotalRowLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
            subtotalRowLabel.setForeground(Color.BLACK);
            subtotalRowLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

            rightGroup.add(priceLabel);
            rightGroup.add(Box.createVerticalStrut(6));
            rightGroup.add(subtotalRowLabel);
            row.add(rightGroup, BorderLayout.EAST);

            minusBtn.addActionListener(e -> {
                if (ci.quantity > 1) {
                    ci.quantity--;
                } else {
                    cart.remove(ci);
                }
                refreshDisplay();
                refreshCategoryButton();
            });

            plusBtn.addActionListener(e -> {
                if (ci.quantity < 20) {
                    ci.quantity++;
                    refreshDisplay();
                    refreshCategoryButton();
                } else {
                    JOptionPane.showMessageDialog(this, "Maximum of 20 items per row.");
                }
            });

            return row;
        }

        private void resetDiscounts() {
            pwdApplied = false;
            appliedVoucher = null;
        }

        private void applyPwdDiscount() {
            if (pwdApplied) {
                JOptionPane.showMessageDialog(this, "PWD discount already applied.");
                return;
            }
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Cart is empty! Add items before applying discount.");
                return;
            }
            String name = JOptionPane.showInputDialog(this, "Enter PWD/Senior Name:");
            if (name == null || name.trim().isEmpty()) return;
            String id = JOptionPane.showInputDialog(this, "Enter PWD/Senior ID Number:");
            if (id == null || id.trim().isEmpty()) return;
            pwdApplied = true;
            pwdName = name;
            pwdId = id;
            refreshDisplay();
            JOptionPane.showMessageDialog(this, "20% PWD discount applied for " + name);
        }

        private void processPayment() {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Cart is empty!");
                return;
            }
            double subtotal = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
            double pwdDiscount = 0.0;
            if (pwdApplied && !cart.isEmpty()) {
                pwdDiscount = subtotal * 0.20;
            }
            double afterPwd = subtotal - pwdDiscount;
            double voucherDiscount = (appliedVoucher != null) ? appliedVoucher.discount : 0;
            double afterVoucher = afterPwd - voucherDiscount;
            double tax = afterVoucher * TAX_RATE;
            double total = afterVoucher + tax;

            String[] options = {"Cash", "Card"};
            int choice = JOptionPane.showOptionDialog(this,
                    "Total Amount: ₱" + String.format("%.2f", total) + "\nSelect Payment Method",
                    "Payment", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

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

            if (success) {
                StringBuilder receipt = new StringBuilder();
                receipt.append("                    RECEIPT\n");
                receipt.append("-----------------------------------------------\n");
                receipt.append("Order Type: ").append(orderType).append("\n");
                receipt.append("Date: ").append(new java.util.Date()).append("\n");
                if (!noteField.getText().trim().isEmpty()) {
                    receipt.append("Note: ").append(noteField.getText().trim()).append("\n");
                }
                if (pwdApplied) {
                    receipt.append("PWD/Senior: ").append(pwdName).append(" (").append(pwdId).append(")\n");
                }
                receipt.append("\nItems:\n");
                for (CartItem ci : cart) {
                    receipt.append(String.format("  %-20s x%d  ₱%.2f\n", ci.item.name, ci.quantity, ci.getSubtotal()));
                }
                receipt.append("\n");
                receipt.append(String.format("%-22s  %s\n", "Subtotal:", String.format("₱%.2f", subtotal)));
                if (pwdApplied) {
                    receipt.append(String.format("%-22s  %s\n", "PWD/Senior 20% off:", String.format("-₱%.2f", pwdDiscount)));
                    receipt.append(String.format("%-22s  %s\n", "After PWD:", String.format("₱%.2f", afterPwd)));
                }
                if (appliedVoucher != null) {
                    receipt.append(String.format("%-22s  %s\n", "Voucher (" + appliedVoucher.code + "):", String.format("-₱%.2f", voucherDiscount)));
                }
                receipt.append(String.format("%-22s  %s\n", "Tax (12%):", String.format("₱%.2f", tax)));
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
                scrollPane.setPreferredSize(new Dimension(350, 450));
                JOptionPane.showMessageDialog(this, scrollPane, "Receipt", JOptionPane.INFORMATION_MESSAGE);

                OrderDAO.saveOrder(orderType, total, cart, noteField.getText().trim(), pwdName, pwdId, paymentMethod, cashTendered, change);
                cart.clear();
                noteField.setText("");
                resetDiscounts();
                refreshDisplay();
                refreshCategoryButton();
                cardLayout.show(mainPanel, "welcome");
            }
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
            tabs.addTab("View Dashboard", createDashboardPanel());
            tabs.addTab("Manage Menu", createMenuPanel());
            tabs.addTab("Manage Vouchers", createVoucherPanel());
            add(tabs);
        }

        private JPanel createDashboardPanel() {
            JPanel panel = new JPanel(new GridLayout(1, 2, 15, 15));
            panel.setBackground(Color.LIGHT_GRAY);
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));

            JPanel leftSide = new JPanel();
            leftSide.setLayout(new GridLayout(10, 1, 0, 10));
            leftSide.setBackground(Color.LIGHT_GRAY);

            addAdminStatRow(leftSide,
                    "Total Revenue Today",
                    "₱" + String.format("%.2f", OrderDAO.getTotalRevenueToday())
            );
            addAdminStatRow(leftSide,
                    "Total Orders Today",
                    String.valueOf(OrderDAO.getTotalOrdersToday())
            );
            addAdminStatRow(leftSide,
                    "Total Menu Categories",
                    String.valueOf(MenuDAO.getCategories().size())
            );
            addAdminStatRow(leftSide,
                    "Total Menu Items",
                    String.valueOf(MenuDAO.getAllItems().size())
            );

            panel.add(leftSide);

            JPanel rightSide = new JPanel(new BorderLayout());
            rightSide.setBackground(Color.WHITE);
            rightSide.setBorder(new TitledBorder("Recent Orders"));

            DefaultListModel<String> recentModel = new DefaultListModel<>();
            List<String> recentOrders = OrderDAO.getRecentOrders();
            for (String order : recentOrders) {
                recentModel.addElement(order);
            }

            JList<String> recentList = new JList<>(recentModel);
            recentList.setSelectionModel(new DefaultListSelectionModel() {
                @Override
                public void setSelectionInterval(int index0, int index1) {}
            });
            recentList.setFont(new Font("Courier New", Font.PLAIN, 12));
            recentList.setBorder(new EmptyBorder(2, 2, 2, 2));
            recentList.setVisibleRowCount(5);

            JScrollPane scrollPane = new JScrollPane(recentList);
            scrollPane.setBorder(null);
            scrollPane.setPreferredSize(new Dimension(400, 200));
            rightSide.add(scrollPane, BorderLayout.CENTER);

            panel.add(rightSide);
            return panel;
        }

        private void addAdminStatRow(JPanel panel, String label, String value) {
            JPanel row = new JPanel(new BorderLayout());
            row.setBackground(Color.WHITE);
            row.setBorder(new EmptyBorder(7, 12, 7, 12));
            row.setPreferredSize(new Dimension(1000, 45));

            JLabel lbl = new JLabel(label);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 15));

            JLabel val = new JLabel(value);
            val.setFont(new Font("SansSerif", Font.BOLD, 16));

            row.add(lbl, BorderLayout.WEST);
            row.add(val, BorderLayout.EAST);
            panel.add(row);
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
                    categoryPanel.loadMenuItemsForCategory(selectedCategory);
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
                        categoryPanel.loadMenuItemsForCategory(selectedCategory);
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
                        categoryPanel.loadMenuItemsForCategory(selectedCategory);
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
                    categoryPanel.loadMenuItemsForCategory(selectedCategory);
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
            categoryPanel.loadMenuItemsForCategory(selectedCategory);
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
