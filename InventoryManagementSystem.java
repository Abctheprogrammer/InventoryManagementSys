import java.util.*;
import java.util.stream.Collectors;

public class InventoryManagementSystem {
    
    // === MODELS ===
    static class Product implements Comparable<Product> {
        String sku, name, category;
        double price;
        int quantity;
        Date lastUpdated;
        
        Product(String sku, String name, String category, double price, int quantity) {
            this.sku = sku;
            this.name = name;
            this.category = category;
            this.price = price;
            this.quantity = quantity;
            this.lastUpdated = new Date();
        }
        
        @Override
        public int compareTo(Product other) {
            return this.sku.compareTo(other.sku);
        }
        
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Product p && sku.equals(p.sku);
        }
        
        @Override
        public int hashCode() {
            return sku.hashCode();
        }
        
        double getInventoryValue() { return price * quantity; }
        void setQuantity(int qty) { quantity = qty; lastUpdated = new Date(); }
        void setPrice(double price) { this.price = price; lastUpdated = new Date(); }
        
        @Override
        public String toString() {
            return String.format("%s | %s | %s | ₹%.2f | %d | ₹%.2f", 
                sku, name, category, price, quantity, getInventoryValue());
        }
    }
    
    static class Transaction {
        String type, sku, details;
        Date timestamp;
        
        Transaction(String type, String sku, String details) {
            this.type = type;
            this.sku = sku;
            this.details = details;
            this.timestamp = new Date();
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s: %s", 
                new Date(timestamp.getTime()).toString().substring(11, 19), type, details);
        }
    }
    
    // === COLLECTIONS ===
    HashSet<Product> productSet = new HashSet<>();
    TreeSet<Product> sortedProducts = new TreeSet<>();
    LinkedList<Transaction> transactionHistory = new LinkedList<>();
    Stack<Product> undoStack = new Stack<>();
    Queue<Product> lowStockQueue = new LinkedList<>();
    
    int totalProducts = 0;
    double totalValue = 0.0;
    
    // === MAIN OPERATIONS ===
    void addProduct(Product p) {
        if (productSet.add(p)) {
            sortedProducts.add(p);
            totalProducts++;
            totalValue += p.getInventoryValue();
            
            transactionHistory.addFirst(new Transaction("ADD", p.sku, 
                p.name + " (Qty:" + p.quantity + ")"));
            
            if (p.quantity < 10) lowStockQueue.add(p);
            System.out.println("✅ Added: " + p.sku);
        } else {
            System.out.println("❌ SKU exists: " + p.sku);
        }
    }
    
    void updateQuantity(String sku, int newQty) {
        for (Product p : productSet) {
            if (p.sku.equals(sku)) {
                undoStack.push(new Product(p.sku, p.name, p.category, p.price, p.quantity));
                
                totalValue -= p.price * p.quantity;
                p.setQuantity(newQty);
                totalValue += p.price * newQty;
                
                transactionHistory.addFirst(new Transaction("UPDATE", sku, 
                    "Qty: " + (newQty - p.quantity + p.quantity) + " → " + newQty));
                
                lowStockQueue.remove(p);
                if (newQty < 10) lowStockQueue.add(p);
                
                System.out.println("✅ Updated: " + sku + " → " + newQty);
                return;
            }
        }
        System.out.println("❌ Not found: " + sku);
    }
    
    void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("❌ Nothing to undo!");
            return;
        }
        Product prev = undoStack.pop();
        updateQuantity(prev.sku, prev.quantity);
        System.out.println("✅ Undone: " + prev.sku);
    }
    
    // === DISPLAY ===
    void display(String sortBy) {
        List<Product> list = new ArrayList<>(productSet);
        
        switch (sortBy.toLowerCase()) {
            case "price" -> list.sort(Comparator.comparingDouble(p -> p.price));
            case "value" -> list.sort(Comparator.comparingDouble(p -> -p.getInventoryValue()));
            case "name" -> list.sort(Comparator.comparing(p -> p.name, String.CASE_INSENSITIVE_ORDER));
            case "qty" -> list.sort(Comparator.comparingInt(p -> p.quantity));
            default -> Collections.sort(list);
        }
        
        System.out.println("\n" + "=".repeat(85));
        System.out.printf("%-10s %-20s %-12s %-8s %-6s %-12s\n", 
            "SKU", "NAME", "CATEGORY", "PRICE", "QTY", "VALUE");
        System.out.println("-".repeat(85));
        
        for (Product p : list) {
            System.out.printf("%-10s %-20s %-12s ₹%-7.0f %-5d ₹%-10.0f\n",
                p.sku, p.name, p.category, p.price, p.quantity, p.getInventoryValue());
        }
        System.out.println("=".repeat(85));
    }
    
    void search(String query) {
        List<Product> results = productSet.stream()
            .filter(p -> p.sku.contains(query) || p.name.toLowerCase().contains(query.toLowerCase()))
            .toList();
        
        if (results.isEmpty()) {
            System.out.println("❌ No matches for '" + query + "'");
            return;
        }
        display("name"); // Show sorted by name
    }
    
    void lowStock() {
        if (lowStockQueue.isEmpty()) {
            System.out.println("✅ All stock levels good!");
            return;
        }
        System.out.println("\n⚠️ LOW STOCK (" + lowStockQueue.size() + "):");
        int i = 1;
        for (Product p : lowStockQueue) {
            System.out.printf("%d. %s - %s (Qty: %d) ⚠️\n", i++, p.sku, p.name, p.quantity);
        }
    }
    
    void history(int count) {
        System.out.println("\n📜 LAST " + count + " TRANSACTIONS:");
        System.out.println("-".repeat(50));
        for (int i = 0; i < Math.min(count, transactionHistory.size()); i++) {
            System.out.println(transactionHistory.get(i));
        }
    }
    
    void stats() {
        Map<String, Double> catValues = new HashMap<>();
        for (Product p : productSet) {
            catValues.merge(p.category, p.getInventoryValue(), Double::sum);
        }
        
        System.out.println("\n📊 STATS:");
        System.out.println("=".repeat(40));
        System.out.printf("Products: %d | Total Value: ₹%.0f\n", totalProducts, totalValue);
        System.out.println("\nBy Category:");
        catValues.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(e -> System.out.printf("  %s: ₹%.0f (%.1f%%)\n", 
                e.getKey(), e.getValue(), (e.getValue()/totalValue)*100));
    }
    
    // === UTILS ===
    static Scanner sc = new Scanner(System.in);
    
    static String getStr(String prompt) {
        System.out.print(prompt); return sc.nextLine().trim();
    }
    
    static int getInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(getStr(prompt));
            } catch (NumberFormatException e) {
                System.out.println("❌ Enter valid number!");
            }
        }
    }
    
    static double getDouble(String prompt) {
        while (true) {
            try {
                return Double.parseDouble(getStr(prompt));
            } catch (NumberFormatException e) {
                System.out.println("❌ Enter valid number!");
            }
        }
    }
    
    static void loadSamples(InventoryManagementSystem ims) {
        ims.addProduct(new Product("LAP001", "Gaming Laptop", "Electronics", 85000, 12));
        ims.addProduct(new Product("MOU001", "Wireless Mouse", "Electronics", 1500, 8));
        ims.addProduct(new Product("PEN001", "Gel Pen", "Stationery", 20, 250));
        ims.addProduct(new Product("NBK001", "Notebook", "Stationery", 50, 100));
        ims.addProduct(new Product("KBD001", "Keyboard", "Electronics", 3500, 5));
        System.out.println("✅ Loaded 5 sample products!");
    }
    
    // === MAIN MENU ===
    public static void main(String[] args) {
        InventoryManagementSystem ims = new InventoryManagementSystem();
        loadSamples(ims);
        
        while (true) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("🏪 INVENTORY SYSTEM");
            System.out.println("1. Add Product  2. Update Qty  3. View (Sort)");
            System.out.println("4. Search      5. Low Stock    6. History");
            System.out.println("7. Stats       8. Undo         0. Exit");
            System.out.print("Choice: ");
            
            int choice = getInt("");
            
            switch (choice) {
                case 1 -> {
                    System.out.println("\n➕ ADD PRODUCT");
                    Product p = new Product(
                        getStr("SKU: "), getStr("Name: "), getStr("Category: "),
                        getDouble("Price: "), getInt("Quantity: ")
                    );
                    ims.addProduct(p);
                }
                case 2 -> ims.updateQuantity(getStr("SKU: "), getInt("New Qty: "));
                case 3 -> ims.display(getStr("Sort (sku/price/value/name/qty): "));
                case 4 -> ims.search(getStr("Search: "));
                case 5 -> ims.lowStock();
                case 6 -> ims.history(getInt("Count: "));
                case 7 -> ims.stats();
                case 8 -> ims.undo();
                case 0 -> {
                    System.out.println("👋 Goodbye!");
                    return;
                }
                default -> System.out.println("❌ Invalid!");
            }
        }
    }
}