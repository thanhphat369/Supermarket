package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import mypack.entity.Inventory;
import mypack.entity.Product;
import mypack.entity.StockTransactions;
import mypack.entity.Supplier;
import mypack.sessionbean.InventoryFacadeLocal;
import mypack.sessionbean.ProductFacadeLocal;
import mypack.sessionbean.StockTransactionsFacadeLocal;
import mypack.sessionbean.SupplierFacadeLocal;

@Named(value = "inventoryMBean")
@SessionScoped
public class InventoryMBean implements Serializable {

    @EJB
    private InventoryFacadeLocal inventoryFacade;
    
    @EJB
    private ProductFacadeLocal productFacade;
    
    @EJB
    private StockTransactionsFacadeLocal stockTransactionsFacade;
    
    @EJB
    private SupplierFacadeLocal supplierFacade;
    
    private Inventory selected = new Inventory();
    private boolean editMode = false;
    private boolean showForm = false; // Control form visibility
    private Integer selectedProductId;
    private String searchKeyword;
    private int currentPage = 1;
    private int pageSize = 10;
    
    // Pagination for transaction history
    private int transactionCurrentPage = 1;
    private int transactionPageSize = 10;
    private String transactionSearchKeyword;
    
    // Fields for stock transaction (nhập/xuất kho)
    private boolean showTransactionForm = false;
    private Integer transactionProductId;
    private String transactionType; // "Import" or "Export"
    private Integer transactionQuantity;
    private Integer transactionUnitCost; // Giá nhập (chỉ cho import)
    private Integer transactionSupplierId; // Optional
    private String transactionNote;
    
    // Lấy danh sách product
    public List<Product> getAllProducts() {
        try {
            return productFacade.findAll();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách suppliers
    public List<Supplier> getAllSuppliers() {
        try {
            return supplierFacade.findAll();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
    
    // ========== TRANSACTION HISTORY METHODS ==========
    
    // Lấy danh sách lịch sử giao dịch (StockTransactions)
    public List<StockTransactions> getTransactionHistory() {
        try {
            List<StockTransactions> all = stockTransactionsFacade.findAll();
            
            if (all == null) {
                return new java.util.ArrayList<>();
            }
            
            // Sắp xếp theo thời gian mới nhất trước
            all.sort((a, b) -> {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
            
            // Áp dụng tìm kiếm nếu có keyword
            if (transactionSearchKeyword != null && !transactionSearchKeyword.trim().isEmpty()) {
                String keyword = transactionSearchKeyword.trim().toLowerCase();
                return all.stream()
                        .filter(t -> 
                            (t.getProductID() != null && t.getProductID().getName() != null && 
                             t.getProductID().getName().toLowerCase().contains(keyword)) ||
                            (t.getType() != null && t.getType().toLowerCase().contains(keyword)) ||
                            (t.getNote() != null && t.getNote().toLowerCase().contains(keyword)) ||
                            (t.getSupplierID() != null && t.getSupplierID().getSupplierName() != null &&
                             t.getSupplierID().getSupplierName().toLowerCase().contains(keyword))
                        )
                        .collect(java.util.stream.Collectors.toList());
            }
            
            return all;
        } catch (Exception e) {
            System.err.println("InventoryMBean.getTransactionHistory() - Error: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách transaction phân trang
    public List<StockTransactions> getPagedTransactionHistory() {
        try {
            List<StockTransactions> base = getTransactionHistory();
            
            if (base == null || base.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            
            int start = (transactionCurrentPage - 1) * transactionPageSize;
            int end = Math.min(start + transactionPageSize, base.size());
            
            if (start >= base.size()) {
                transactionCurrentPage = 1;
                start = 0;
                end = Math.min(transactionPageSize, base.size());
            }
            
            if (start < 0 || start >= end || end > base.size()) {
                return new java.util.ArrayList<>();
            }
            
            return base.subList(start, end);
        } catch (Exception e) {
            System.err.println("InventoryMBean.getPagedTransactionHistory() - Error: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Tổng số transaction
    public int getTotalTransactions() {
        try {
            List<StockTransactions> items = getTransactionHistory();
            return items != null ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Tổng số trang transaction
    public int getTotalTransactionPages() {
        int total = getTotalTransactions();
        if (total == 0) {
            return 1;
        }
        return (int) Math.ceil((double) total / transactionPageSize);
    }
    
    // Tìm kiếm transaction
    public void performTransactionSearch() {
        transactionCurrentPage = 1;
    }
    
    public void clearTransactionSearch() {
        transactionSearchKeyword = null;
        transactionCurrentPage = 1;
    }
    
    // Navigation cho transaction
    public void transactionFirstPage() {
        transactionCurrentPage = 1;
    }
    
    public void transactionPreviousPage() {
        if (transactionCurrentPage > 1) {
            transactionCurrentPage--;
        }
    }
    
    public void transactionNextPage() {
        if (transactionCurrentPage < getTotalTransactionPages()) {
            transactionCurrentPage++;
        }
    }
    
    public void transactionLastPage() {
        transactionCurrentPage = getTotalTransactionPages();
    }
    
    // Format transaction type
    public String getTransactionTypeText(String type) {
        if (type == null) return "";
        if ("Import".equalsIgnoreCase(type)) {
            return "📥 Nhập kho";
        } else if ("Export".equalsIgnoreCase(type)) {
            return "📤 Xuất kho";
        }
        return type;
    }
    
    public String getTransactionTypeColor(String type) {
        if (type == null) return "#666";
        if ("Import".equalsIgnoreCase(type)) {
            return "#28a745"; // Green
        } else if ("Export".equalsIgnoreCase(type)) {
            return "#dc3545"; // Red
        }
        return "#666";
    }
    
    // Format currency
    public String formatCurrency(Integer amount) {
        if (amount == null) return "-";
        return String.format("%,d", amount) + " VNĐ";
    }
    
    // Xóa transaction (có thể dùng để xóa transaction test như ID 1006, 1008)
    public void deleteTransaction(StockTransactions transaction) {
        try {
            if (transaction == null) {
                addErr("⚠️ Không tìm thấy giao dịch để xóa!");
                return;
            }
            
            // Lưu thông tin để cập nhật lại inventory
            Integer productId = transaction.getProductID() != null ? transaction.getProductID().getProductID() : null;
            String type = transaction.getType();
            Integer quantity = transaction.getQuantity();
            Integer transactionId = transaction.getTransactionID();
            
            // Tìm lại transaction từ database để đảm bảo có đầy đủ thông tin
            StockTransactions transToDelete = stockTransactionsFacade.find(transactionId);
            if (transToDelete == null) {
                addErr("⚠️ Giao dịch không tồn tại!");
                return;
            }
            
            // Lưu lại thông tin trước khi xóa
            if (transToDelete.getProductID() != null) {
                productId = transToDelete.getProductID().getProductID();
            }
            type = transToDelete.getType();
            quantity = transToDelete.getQuantity();
            
            // Xóa transaction
            stockTransactionsFacade.remove(transToDelete);
            
            // Cập nhật lại inventory (hoàn tác transaction)
            if (productId != null && type != null && quantity != null) {
                Inventory inventory = inventoryFacade.find(productId);
                if (inventory != null) {
                    if ("Import".equalsIgnoreCase(type)) {
                        // Nếu là nhập, trừ lại số lượng
                        inventory.setStock(Math.max(0, inventory.getStock() - quantity));
                    } else if ("Export".equalsIgnoreCase(type)) {
                        // Nếu là xuất, cộng lại số lượng
                        inventory.setStock(inventory.getStock() + quantity);
                    }
                    inventory.setLastUpdate(new Date());
                    inventoryFacade.edit(inventory);
                }
            }
            
            addInfo("✅ Đã xóa giao dịch ID: " + transactionId);
            
            // Nếu xóa transaction có ID lớn (>= 1000), gợi ý reset sequence
            if (transactionId != null && transactionId >= 1000) {
                addInfo("💡 Gợi ý: Để reset ID sequence về 8, vui lòng xóa tất cả transaction có ID >= 1000, sau đó chạy SQL: DBCC CHECKIDENT ('Stock_Transactions', RESEED, 7)");
            }
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Lỗi khi xóa giao dịch: " + e.getMessage());
        }
    }
    
    // Xóa transaction theo ID (dùng khi truyền qua AJAX)
    public void deleteTransactionById(Integer transactionId) {
        try {
            if (transactionId == null) {
                addErr("⚠️ Không tìm thấy ID giao dịch!");
                return;
            }
            
            // Tìm transaction từ database
            StockTransactions transaction = stockTransactionsFacade.find(transactionId);
            if (transaction == null) {
                addErr("⚠️ Giao dịch không tồn tại!");
                return;
            }
            
            // Gọi method deleteTransaction với object
            deleteTransaction(transaction);
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Lỗi khi xóa giao dịch: " + e.getMessage());
        }
    }
    
    // Xóa tất cả transaction có ID >= 1000 (để reset về ID nhỏ)
    public void deleteHighIdTransactions() {
        try {
            List<StockTransactions> allTransactions = stockTransactionsFacade.findAll();
            int deletedCount = 0;
            
            for (StockTransactions trans : allTransactions) {
                if (trans.getTransactionID() != null && trans.getTransactionID() >= 1000) {
                    // Lưu thông tin để cập nhật lại inventory
                    Integer productId = trans.getProductID() != null ? trans.getProductID().getProductID() : null;
                    String type = trans.getType();
                    Integer quantity = trans.getQuantity();
                    
                    // Xóa transaction
                    stockTransactionsFacade.remove(trans);
                    deletedCount++;
                    
                    // Cập nhật lại inventory
                    if (productId != null && type != null && quantity != null) {
                        Inventory inventory = inventoryFacade.find(productId);
                        if (inventory != null) {
                            if ("Import".equalsIgnoreCase(type)) {
                                inventory.setStock(Math.max(0, inventory.getStock() - quantity));
                            } else if ("Export".equalsIgnoreCase(type)) {
                                inventory.setStock(inventory.getStock() + quantity);
                            }
                            inventory.setLastUpdate(new Date());
                            inventoryFacade.edit(inventory);
                        }
                    }
                }
            }
            
            if (deletedCount > 0) {
                addInfo("✅ Đã xóa " + deletedCount + " giao dịch có ID >= 1000");
                addInfo("💡 Bây giờ chạy SQL trong database: DBCC CHECKIDENT ('Stock_Transactions', RESEED, 7) để reset ID sequence về 8");
            } else {
                addInfo("ℹ️ Không có giao dịch nào có ID >= 1000");
            }
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Lỗi khi xóa giao dịch: " + e.getMessage());
        }
    }
    
    // Lấy danh sách inventory
    public List<Inventory> getItems() {
        try {
            List<Inventory> all = inventoryFacade.findAll();
            
            if (all == null) {
                return new java.util.ArrayList<>();
            }
            
            // Áp dụng tìm kiếm nếu có keyword
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keyword = searchKeyword.trim().toLowerCase();
                return all.stream()
                        .filter(inv -> {
                            // Tìm theo tên sản phẩm
                            boolean matchProductName = inv.getProduct() != null && 
                                    inv.getProduct().getName() != null && 
                                    inv.getProduct().getName().toLowerCase().contains(keyword);
                            
                            // Tìm theo ID sản phẩm
                            boolean matchProductID = inv.getProductID() != null && 
                                    String.valueOf(inv.getProductID()).contains(keyword);
                            
                            // Tìm theo số lượng tồn
                            boolean matchStock = String.valueOf(inv.getStock()).contains(keyword);
                            
                            // Tìm theo tồn tối thiểu
                            boolean matchMinStock = String.valueOf(inv.getMinStock()).contains(keyword);
                            
                            // Tìm theo trạng thái (In Stock, Low Stock, Out of Stock)
                            String status = getStockStatus(inv).toLowerCase();
                            boolean matchStatus = status.contains(keyword) || 
                                    (keyword.contains("stock") && status.contains("stock")) ||
                                    (keyword.contains("tồn") && status.contains("stock")) ||
                                    (keyword.contains("hết") && status.contains("out")) ||
                                    (keyword.contains("thấp") && status.contains("low")) ||
                                    (keyword.contains("đủ") && status.contains("in"));
                            
                            return matchProductName || matchProductID || matchStock || matchMinStock || matchStatus;
                        })
                        .collect(java.util.stream.Collectors.toList());
            }
            
            return all;
        } catch (Exception e) {
            System.err.println("InventoryMBean.getItems() - Error: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách inventory phân trang
    public List<Inventory> getPagedItems() {
        try {
            List<Inventory> base = getItems();
            
            if (base == null || base.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, base.size());
            
            if (start >= base.size()) {
                currentPage = 1;
                start = 0;
                end = Math.min(pageSize, base.size());
            }
            
            if (start < 0 || start >= end || end > base.size()) {
                return new java.util.ArrayList<>();
            }
            
            return base.subList(start, end);
        } catch (Exception e) {
            System.err.println("InventoryMBean.getPagedItems() - Error: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách inventory cần nhập hàng (stock < minStock)
    public List<Inventory> getLowStockItems() {
        try {
            return getItems().stream()
                    .filter(inv -> inv.getStock() < inv.getMinStock())
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
    
    // Tìm kiếm
    public void performSearch() {
        currentPage = 1;
    }
    
    public void clearSearch() {
        searchKeyword = null;
        currentPage = 1;
    }
    
    // Tổng số trang
    public int getTotalPages() {
        int total = getTotalItems();
        if (total == 0) {
            return 1;
        }
        return (int) Math.ceil((double) total / pageSize);
    }
    
    // Tổng số items
    public int getTotalItems() {
        try {
            List<Inventory> items = getItems();
            return items != null ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Tạo mới - Kết hợp với nhập/xuất kho
    public void prepareCreate() {
        selected = new Inventory();
        selectedProductId = null;
        editMode = false;
        // Reset transaction fields
        transactionType = null;
        transactionQuantity = null;
        transactionUnitCost = null;
        transactionSupplierId = null;
        transactionNote = null;
        showForm = true;
        showTransactionForm = false; // Use unified form
    }
    
    // Load inventory khi chọn sản phẩm (tự động load số lượng tồn kho)
    public void loadInventoryByProduct() {
        if (selectedProductId == null) {
            selected = new Inventory();
            return;
        }

        Inventory inv = inventoryFacade.findByProductId(selectedProductId);

        if (inv != null) {
            // ✅ Đã có tồn kho → load lên để hiển thị
            selected = inv;
            editMode = true;
        } else {
            // ✅ Chưa có tồn kho → tạo object mới nhưng gán product
            selected = new Inventory();
            selected.setProductID(selectedProductId);
            Product product = productFacade.find(selectedProductId);
            if (product != null) {
                selected.setProduct(product);
            }
            selected.setStock(0);
            selected.setMinStock(0);
            editMode = false;
        }
    }
    
    // Chỉnh sửa - Đảm bảo form giống nhập mới
    public void prepareEdit(Inventory inv) {
        selected = inv;
        selectedProductId = inv.getProductID(); // productID is Integer, not Product
        editMode = true;
        showForm = true; // Show form when editing
        // Reset transaction fields khi edit (để form edit giống form nhập mới)
        transactionType = null;
        transactionQuantity = null;
        transactionUnitCost = null;
        transactionSupplierId = null;
        transactionNote = null;
    }
    
    // Cancel form (close form)
    public void cancelForm() {
        showForm = false;
        selected = new Inventory();
        selectedProductId = null;
        editMode = false;
    }
    
    // ========== STOCK TRANSACTION METHODS (Nhập/Xuất kho) ==========
    
    // Mở form nhập/xuất kho - Kết hợp với create
    public void prepareTransaction() {
        selected = new Inventory();
        selectedProductId = null;
        editMode = false;
        transactionType = null;
        transactionQuantity = null;
        transactionUnitCost = null;
        transactionSupplierId = null;
        transactionNote = null;
        showForm = true; // Use unified form
        showTransactionForm = false;
    }
    
    // Đóng form nhập/xuất kho
    public void cancelTransaction() {
        showTransactionForm = false;
        transactionProductId = null;
        transactionType = null;
        transactionQuantity = null;
        transactionUnitCost = null;
        transactionSupplierId = null;
        transactionNote = null;
    }
    
    // Tạo giao dịch nhập/xuất kho
    public void createStockTransaction() {
        try {
            // Validate
            if (transactionProductId == null) {
                addErr("⚠️ Vui lòng chọn sản phẩm!");
                return;
            }
            
            if (transactionType == null || transactionType.trim().isEmpty()) {
                addErr("⚠️ Vui lòng chọn loại giao dịch (Nhập/Xuất)!");
                return;
            }
            
            if (transactionQuantity == null || transactionQuantity <= 0) {
                addErr("⚠️ Số lượng phải lớn hơn 0!");
                return;
            }
            
            // Validate giá nhập cho import
            if ("Import".equalsIgnoreCase(transactionType) && (transactionUnitCost == null || transactionUnitCost <= 0)) {
                addErr("⚠️ Vui lòng nhập giá nhập cho giao dịch nhập kho!");
                return;
            }
            
            // Tìm product
            Product product = productFacade.find(transactionProductId);
            if (product == null) {
                addErr("⚠️ Sản phẩm không tồn tại!");
                return;
            }
            
            // Tìm hoặc tạo inventory cho sản phẩm
            Inventory inventory = inventoryFacade.find(transactionProductId);
            if (inventory == null) {
                // Tạo inventory mới nếu chưa có
                inventory = new Inventory();
                inventory.setProductID(transactionProductId);
                inventory.setProduct(product);
                inventory.setStock(0);
                inventory.setMinStock(0);
                inventory.setLastUpdate(new Date());
                inventoryFacade.create(inventory);
            }
            
            // Kiểm tra số lượng xuất
            if ("Export".equalsIgnoreCase(transactionType)) {
                if (inventory.getStock() < transactionQuantity) {
                    addErr("⚠️ Số lượng tồn kho không đủ! (Tồn hiện tại: " + inventory.getStock() + ")");
                    return;
                }
            }
            
            // Tạo StockTransaction
            StockTransactions transaction = new StockTransactions();
            transaction.setType(transactionType);
            transaction.setQuantity(transactionQuantity);
            transaction.setUnitCost("Import".equalsIgnoreCase(transactionType) ? transactionUnitCost : null);
            transaction.setProductID(product);
            transaction.setCreatedAt(new Date());
            transaction.setNote(transactionNote);
            
            // Set supplier nếu có
            if (transactionSupplierId != null) {
                Supplier supplier = supplierFacade.find(transactionSupplierId);
                if (supplier != null) {
                    transaction.setSupplierID(supplier);
                }
            }
            
            // Lưu transaction
            stockTransactionsFacade.create(transaction);
            
            // Cập nhật tồn kho
            if ("Import".equalsIgnoreCase(transactionType)) {
                inventory.setStock(inventory.getStock() + transactionQuantity);
            } else if ("Export".equalsIgnoreCase(transactionType)) {
                inventory.setStock(inventory.getStock() - transactionQuantity);
            }
            inventory.setLastUpdate(new Date());
            inventoryFacade.edit(inventory);
            
            // Thông báo thành công
            String typeText = "Import".equalsIgnoreCase(transactionType) ? "Nhập" : "Xuất";
            addInfo("✅ " + typeText + " kho thành công! Số lượng: " + transactionQuantity + 
                    (transactionUnitCost != null ? " | Giá nhập: " + transactionUnitCost + " VNĐ" : ""));
            
            // Đóng form
            cancelTransaction();
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Lỗi khi tạo giao dịch: " + e.getMessage());
        }
    }
    
    // Kiểm tra xem có phải import không (để hiển thị trường giá nhập)
    public boolean isImportType() {
        return "Import".equalsIgnoreCase(transactionType);
    }
    
    // Getter/Setter for showForm
    public boolean isShowForm() {
        return showForm;
    }
    
    public void setShowForm(boolean showForm) {
        this.showForm = showForm;
    }
    
    // Delete
    public void delete(Inventory inv) {
        try {
            inventoryFacade.remove(inv);
            addInfo("✅ Inventory deleted!");
            
            if (selected != null && selected.getProductID() != null && selected.getProductID().equals(inv.getProductID())) {
                prepareCreate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Delete failed: " + e.getMessage());
        }
    }
    
    // Save - Kết hợp với transaction (Form thống nhất)
    public void save() {
        try {
            // Validate required fields
            if (selectedProductId == null) {
                addErr("⚠️ Vui lòng chọn sản phẩm!");
                return;
            }
            
            if (selected.getMinStock() < 0) {
                addErr("⚠️ Số lượng tồn tối thiểu không hợp lệ!");
                return;
            }
            
            // Set Product
            Product product = productFacade.find(selectedProductId);
            if (product == null) {
                addErr("⚠️ Sản phẩm không tồn tại!");
                return;
            }
            
            // Check if inventory exists for this product
            Inventory existing = inventoryFacade.find(selectedProductId);
            boolean isNew = existing == null;
            
            // Nếu có transaction (nhập/xuất), xử lý transaction trước
            if (transactionType != null && !transactionType.trim().isEmpty() && transactionQuantity != null && transactionQuantity > 0) {
                // Validate transaction
                if ("Import".equalsIgnoreCase(transactionType) && (transactionUnitCost == null || transactionUnitCost <= 0)) {
                    addErr("⚠️ Vui lòng nhập giá nhập cho giao dịch nhập kho!");
                    return;
                }
                
                // Tạo inventory nếu chưa có
                if (isNew) {
                    existing = new Inventory();
                    existing.setProductID(selectedProductId);
                    existing.setProduct(product);
                    existing.setStock(0);
                    existing.setMinStock(selected.getMinStock());
                    existing.setLastUpdate(new Date());
                    inventoryFacade.create(existing);
                }
                
                // Kiểm tra số lượng xuất
                if ("Export".equalsIgnoreCase(transactionType)) {
                    if (existing.getStock() < transactionQuantity) {
                        addErr("⚠️ Số lượng tồn kho không đủ! (Tồn hiện tại: " + existing.getStock() + ")");
                        return;
                    }
                }
                
                // Tạo StockTransaction
                StockTransactions transaction = new StockTransactions();
                transaction.setType(transactionType);
                transaction.setQuantity(transactionQuantity);
                transaction.setUnitCost("Import".equalsIgnoreCase(transactionType) ? transactionUnitCost : null);
                transaction.setProductID(product);
                transaction.setCreatedAt(new Date());
                transaction.setNote(transactionNote);
                
                // Set supplier nếu có
                if (transactionSupplierId != null) {
                    Supplier supplier = supplierFacade.find(transactionSupplierId);
                    if (supplier != null) {
                        transaction.setSupplierID(supplier);
                    }
                }
                
                // Lưu transaction
                stockTransactionsFacade.create(transaction);
                
                // Cập nhật tồn kho
                if ("Import".equalsIgnoreCase(transactionType)) {
                    existing.setStock(existing.getStock() + transactionQuantity);
                } else if ("Export".equalsIgnoreCase(transactionType)) {
                    existing.setStock(existing.getStock() - transactionQuantity);
                }
                existing.setMinStock(selected.getMinStock());
                existing.setLastUpdate(new Date());
                inventoryFacade.edit(existing);
                
                String typeText = "Import".equalsIgnoreCase(transactionType) ? "Nhập" : "Xuất";
                addInfo("✅ " + typeText + " kho thành công! Số lượng: " + transactionQuantity + 
                        (transactionUnitCost != null ? " | Giá nhập: " + transactionUnitCost + " VNĐ" : "") +
                        " | Tồn kho hiện tại: " + existing.getStock());
            } else {
                // Chỉ cập nhật thông tin inventory (không có transaction)
                if (isNew) {
                    selected.setProductID(selectedProductId);
                    selected.setProduct(product);
                    selected.setStock(0); // Mặc định 0 nếu tạo mới
                    selected.setLastUpdate(new Date());
                    inventoryFacade.create(selected);
                    addInfo("✅ Tạo tồn kho mới thành công!");
                } else {
                    existing.setMinStock(selected.getMinStock());
                    existing.setLastUpdate(new Date());
                    inventoryFacade.edit(existing);
                    addInfo("✅ Cập nhật thông tin tồn kho thành công!");
                }
            }
            
            // Close form after successful save
            showForm = false;
            // Reset transaction fields
            transactionType = null;
            transactionQuantity = null;
            transactionUnitCost = null;
            transactionSupplierId = null;
            transactionNote = null;
            prepareCreate();
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Lỗi: " + e.getMessage());
        }
    }
    
    // Format date
    public String formatDate(java.util.Date date) {
        if (date == null) {
            return "-";
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
    
    // Check stock status
    public String getStockStatus(Inventory inv) {
        if (inv == null) {
            return "";
        }
        if (inv.getStock() < inv.getMinStock()) {
            return "⚠️ Out of Stock";
        } else if (inv.getStock() <= inv.getMinStock() * 1.5) {
            return "⚠️ Low Stock";
        } else {
            return "✅ In Stock";
        }
    }
    
    public String getStockStatusColor(Inventory inv) {
        if (inv == null) {
            return "#666";
        }
        if (inv.getStock() < inv.getMinStock()) {
            return "#dc3545";
        } else if (inv.getStock() <= inv.getMinStock() * 1.5) {
            return "#ffc107";
        } else {
            return "#28a745";
        }
    }
    
    // Getters and Setters
    public Inventory getSelected() {
        return selected;
    }
    
    public void setSelected(Inventory selected) {
        this.selected = selected;
    }
    
    public boolean isEditMode() {
        return editMode;
    }
    
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }
    
    public Integer getSelectedProductId() {
        return selectedProductId;
    }
    
    public void setSelectedProductId(Integer selectedProductId) {
        this.selectedProductId = selectedProductId;
    }
    
    public String getSearchKeyword() {
        return searchKeyword;
    }
    
    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    // Getters and Setters for Transaction
    public boolean isShowTransactionForm() {
        return showTransactionForm;
    }
    
    public void setShowTransactionForm(boolean showTransactionForm) {
        this.showTransactionForm = showTransactionForm;
    }
    
    public Integer getTransactionProductId() {
        return transactionProductId;
    }
    
    public void setTransactionProductId(Integer transactionProductId) {
        this.transactionProductId = transactionProductId;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    
    public Integer getTransactionQuantity() {
        return transactionQuantity;
    }
    
    public void setTransactionQuantity(Integer transactionQuantity) {
        this.transactionQuantity = transactionQuantity;
    }
    
    public Integer getTransactionUnitCost() {
        return transactionUnitCost;
    }
    
    public void setTransactionUnitCost(Integer transactionUnitCost) {
        this.transactionUnitCost = transactionUnitCost;
    }
    
    public Integer getTransactionSupplierId() {
        return transactionSupplierId;
    }
    
    public void setTransactionSupplierId(Integer transactionSupplierId) {
        this.transactionSupplierId = transactionSupplierId;
    }
    
    public String getTransactionNote() {
        return transactionNote;
    }
    
    public void setTransactionNote(String transactionNote) {
        this.transactionNote = transactionNote;
    }
    
    // Getters and Setters for Transaction History
    public int getTransactionCurrentPage() {
        return transactionCurrentPage;
    }
    
    public void setTransactionCurrentPage(int transactionCurrentPage) {
        this.transactionCurrentPage = transactionCurrentPage;
    }
    
    public int getTransactionPageSize() {
        return transactionPageSize;
    }
    
    public void setTransactionPageSize(int transactionPageSize) {
        this.transactionPageSize = transactionPageSize;
    }
    
    public String getTransactionSearchKeyword() {
        return transactionSearchKeyword;
    }
    
    public void setTransactionSearchKeyword(String transactionSearchKeyword) {
        this.transactionSearchKeyword = transactionSearchKeyword;
    }
    
    // Navigation
    public void firstPage() {
        currentPage = 1;
    }
    
    public void previousPage() {
        if (currentPage > 1) {
            currentPage--;
        }
    }
    
    public void nextPage() {
        if (currentPage < getTotalPages()) {
            currentPage++;
        }
    }
    
    public void lastPage() {
        currentPage = getTotalPages();
    }
    
    // Helper methods
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }
    
    private void addErr(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
}

