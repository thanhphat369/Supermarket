package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import mypack.entity.Product;
import mypack.entity.StockTransactions;
import mypack.entity.Supplier;
import mypack.sessionbean.ProductFacadeLocal;
import mypack.sessionbean.StockTransactionsFacadeLocal;
import mypack.sessionbean.SupplierFacadeLocal;

@Named(value = "stockTransactionsMBean")
@SessionScoped
public class StockTransactionsMBean implements Serializable {

    @EJB
    private StockTransactionsFacadeLocal stockTransactionsFacade;
    
    @EJB
    private ProductFacadeLocal productFacade;
    
    @EJB
    private SupplierFacadeLocal supplierFacade;

    private StockTransactions selected = new StockTransactions();
    private boolean showForm = false;
    private Integer selectedProductId;
    private Integer selectedSupplierId;
    private Product selectedProduct; // Sản phẩm đã chọn để hiển thị thông tin
    
    // Search for stock
    private String stockSearchKeyword;
    
    // Search for history
    private String historySearchKeyword;
    private String historyFilterType;
    
    // Pagination for stock
    private int stockCurrentPage = 1;
    private int stockPageSize = 10;
    
    // Pagination for history
    private int historyCurrentPage = 1;
    private int historyPageSize = 10;

    // ==================== DANH SÁCH SẢN PHẨM VÀ TỒN KHO ====================
    
    // Lấy tất cả sản phẩm có tồn kho
    public List<Product> getAllProductsWithStock() {
        try {
            System.out.println("=== getAllProductsWithStock() called ===");
            System.out.println("productFacade is null? " + (productFacade == null));
            
            List<Product> all = productFacade.findAll();
            System.out.println("Products found: " + (all != null ? all.size() : "null"));
            
            if (all == null) return new java.util.ArrayList<>();
            
            // Filter by search keyword
            if (stockSearchKeyword != null && !stockSearchKeyword.trim().isEmpty()) {
                String keyword = stockSearchKeyword.trim().toLowerCase();
                all = all.stream()
                        .filter(p -> 
                            (p.getName() != null && p.getName().toLowerCase().contains(keyword)) ||
                            (p.getBrandID() != null && p.getBrandID().getBrandName() != null && 
                             p.getBrandID().getBrandName().toLowerCase().contains(keyword)) ||
                            (p.getCategoryID() != null && p.getCategoryID().getCategoryName() != null && 
                             p.getCategoryID().getCategoryName().toLowerCase().contains(keyword))
                        )
                        .collect(Collectors.toList());
            }
            
            return all;
        } catch (Exception e) {
            System.err.println("ERROR in getAllProductsWithStock(): " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách sản phẩm phân trang
    public List<Product> getPagedProductsWithStock() {
        try {
            List<Product> base = getAllProductsWithStock();
            if (base == null || base.isEmpty()) return new java.util.ArrayList<>();
            
            int start = (stockCurrentPage - 1) * stockPageSize;
            int end = Math.min(start + stockPageSize, base.size());
            
            if (start >= base.size()) {
                stockCurrentPage = 1;
                start = 0;
                end = Math.min(stockPageSize, base.size());
            }
            
            return base.subList(start, end);
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
    
    // Tổng số lượng tồn kho của tất cả sản phẩm
    public int getTotalStockQuantity() {
        try {
            List<Product> all = productFacade.findAll();
            if (all == null) return 0;
            return all.stream()
                    .mapToInt(p -> p.getQuantity() != null ? p.getQuantity() : 0)
                    .sum();
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Tổng số sản phẩm
    public int getTotalProducts() {
        try {
            List<Product> items = getAllProductsWithStock();
            return items != null ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Số sản phẩm hết hàng (quantity = 0)
    public int getOutOfStockCount() {
        try {
            List<Product> all = productFacade.findAll();
            if (all == null) return 0;
            return (int) all.stream()
                    .filter(p -> p.getQuantity() == null || p.getQuantity() <= 0)
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Số sản phẩm sắp hết (quantity <= minStock)
    public int getLowStockCount() {
        try {
            List<Product> all = productFacade.findAll();
            if (all == null) return 0;
            return (int) all.stream()
                    .filter(p -> {
                        int qty = p.getQuantity() != null ? p.getQuantity() : 0;
                        int minStock = p.getMinStock() != null ? p.getMinStock() : 10;
                        return qty > 0 && qty <= minStock;
                    })
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Pagination cho stock
    public int getStockTotalPages() {
        int total = getTotalProducts();
        if (total == 0) return 1;
        return (int) Math.ceil((double) total / stockPageSize);
    }
    
    public void stockFirstPage() { stockCurrentPage = 1; }
    public void stockPreviousPage() { if (stockCurrentPage > 1) stockCurrentPage--; }
    public void stockNextPage() { if (stockCurrentPage < getStockTotalPages()) stockCurrentPage++; }
    public void stockLastPage() { stockCurrentPage = getStockTotalPages(); }
    
    public void searchStock() { stockCurrentPage = 1; }
    public void clearStockSearch() { stockSearchKeyword = null; stockCurrentPage = 1; }

    // ==================== LỊCH SỬ GIAO DỊCH (CHỈ XEM) ====================
    
    // Lấy danh sách giao dịch
    public List<StockTransactions> getTransactionHistory() {
        try {
            List<StockTransactions> all = stockTransactionsFacade.findAll();
            if (all == null) return new java.util.ArrayList<>();

            // Sort by createdAt descending (newest first)
            all = all.stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .collect(Collectors.toList());

            // Filter by type
            if (historyFilterType != null && !historyFilterType.trim().isEmpty()) {
                all = all.stream()
                        .filter(t -> historyFilterType.equalsIgnoreCase(t.getType()))
                        .collect(Collectors.toList());
            }

            // Filter by search keyword
            if (historySearchKeyword != null && !historySearchKeyword.trim().isEmpty()) {
                String keyword = historySearchKeyword.trim().toLowerCase();
                all = all.stream()
                        .filter(t -> 
                            (t.getProductID() != null && t.getProductID().getName() != null && 
                             t.getProductID().getName().toLowerCase().contains(keyword)) ||
                            (t.getSupplierID() != null && t.getSupplierID().getSupplierName() != null && 
                             t.getSupplierID().getSupplierName().toLowerCase().contains(keyword)) ||
                            (t.getNote() != null && t.getNote().toLowerCase().contains(keyword))
                        )
                        .collect(Collectors.toList());
            }

            return all;
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    // Lấy danh sách phân trang
    public List<StockTransactions> getPagedTransactionHistory() {
        try {
            List<StockTransactions> base = getTransactionHistory();
            if (base == null || base.isEmpty()) return new java.util.ArrayList<>();

            int start = (historyCurrentPage - 1) * historyPageSize;
            int end = Math.min(start + historyPageSize, base.size());

            if (start >= base.size()) {
                historyCurrentPage = 1;
                start = 0;
                end = Math.min(historyPageSize, base.size());
            }

            return base.subList(start, end);
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
    
    public int getTotalTransactions() {
        try {
            List<StockTransactions> items = getTransactionHistory();
            return items != null ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // Pagination cho history
    public int getHistoryTotalPages() {
        int total = getTotalTransactions();
        if (total == 0) return 1;
        return (int) Math.ceil((double) total / historyPageSize);
    }

    public void historyFirstPage() { historyCurrentPage = 1; }
    public void historyPreviousPage() { if (historyCurrentPage > 1) historyCurrentPage--; }
    public void historyNextPage() { if (historyCurrentPage < getHistoryTotalPages()) historyCurrentPage++; }
    public void historyLastPage() { historyCurrentPage = getHistoryTotalPages(); }
    
    public void searchHistory() { historyCurrentPage = 1; }
    public void clearHistorySearch() { 
        historySearchKeyword = null; 
        historyFilterType = null;
        historyCurrentPage = 1; 
    }

    // ==================== THÊM GIAO DỊCH MỚI ====================
    
    // Lấy tất cả sản phẩm
    public List<Product> getAllProducts() {
        try {
            return productFacade.findAll();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    // Lấy tất cả nhà cung cấp
    public List<Supplier> getAllSuppliers() {
        try {
            return supplierFacade.findAll();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    // Chuẩn bị tạo mới
    public void prepareCreate() {
        selected = new StockTransactions();
        selected.setType("Import");
        selectedProductId = null;
        selectedSupplierId = null;
        selectedProduct = null;
        showForm = true;
    }

    // Hủy form
    public void cancelForm() {
        showForm = false;
        selected = new StockTransactions();
        selectedProductId = null;
        selectedSupplierId = null;
        selectedProduct = null;
    }
    
    // Load thông tin sản phẩm + giá nhập + supplier từ lần nhập gần nhất
    public void loadSelectedProduct() {
        if (selectedProductId == null) {
            selectedProduct = null;
            selected.setUnitCost(null);
            selectedSupplierId = null;
            return;
        }

        // Load product
        selectedProduct = productFacade.find(selectedProductId);

        // Load giao dịch IMPORT gần nhất để lấy giá nhập và nhà cung cấp
        StockTransactions lastImport = stockTransactionsFacade.findLastImportByProduct(selectedProductId);

        if (lastImport != null) {
            selected.setUnitCost(lastImport.getUnitCost());
            if (lastImport.getSupplierID() != null) {
                selectedSupplierId = lastImport.getSupplierID().getSupplierID();
            } else {
                selectedSupplierId = null;
            }
        } else {
            selected.setUnitCost(null);
            selectedSupplierId = null;
        }
    }

    // Lưu giao dịch mới
    public void save() {
        try {
            // Validate
            if (selectedProductId == null) {
                addErr("⚠️ Vui lòng chọn sản phẩm!");
                return;
            }

            if (selected.getType() == null || selected.getType().trim().isEmpty()) {
                addErr("⚠️ Vui lòng chọn loại giao dịch!");
                return;
            }

            if (selected.getQuantity() <= 0) {
                addErr("⚠️ Số lượng phải lớn hơn 0!");
                return;
            }

            // Set Product
            Product product = productFacade.find(selectedProductId);
            if (product == null) {
                addErr("⚠️ Sản phẩm không hợp lệ!");
                return;
            }

            // Check stock for Export
            if ("Export".equalsIgnoreCase(selected.getType())) {
                int currentStock = product.getQuantity() != null ? product.getQuantity() : 0;
                if (selected.getQuantity() > currentStock) {
                    addErr("⚠️ Số lượng xuất (" + selected.getQuantity() + ") vượt quá tồn kho (" + currentStock + ")!");
                    return;
                }
            }

            selected.setProductID(product);

            // Set Supplier (optional for Export)
            if (selectedSupplierId != null) {
                Supplier supplier = supplierFacade.find(selectedSupplierId);
                selected.setSupplierID(supplier);
            } else {
                selected.setSupplierID(null);
            }

            // Set created time
            selected.setCreatedAt(new Date());

            // Save transaction
            stockTransactionsFacade.create(selected);
            
            // Update product quantity
            int currentQty = product.getQuantity() != null ? product.getQuantity() : 0;
            if ("Import".equalsIgnoreCase(selected.getType())) {
                product.setQuantity(currentQty + selected.getQuantity());
            } else if ("Export".equalsIgnoreCase(selected.getType())) {
                product.setQuantity(currentQty - selected.getQuantity());
            }
            
            // Cập nhật minStock từ selectedProduct (nếu user đã sửa)
            if (selectedProduct != null && selectedProduct.getMinStock() != null) {
                product.setMinStock(selectedProduct.getMinStock());
            }
            
            productFacade.edit(product);
            
            addInfo("✅ Đã thêm giao dịch " + (selected.getType().equals("Import") ? "nhập kho" : "xuất kho") + " thành công!");

            // Reset form
            showForm = false;
            selected = new StockTransactions();
            selectedProductId = null;
            selectedSupplierId = null;
            selectedProduct = null;
            
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Lỗi: " + e.getMessage());
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    // Lấy số lượng tồn kho hiện tại của sản phẩm
    public Integer getCurrentStock(Product product) {
        if (product == null) return 0;
        return product.getQuantity() != null ? product.getQuantity() : 0;
    }

    // Lấy số lượng tồn kho theo Product ID
    public Integer getStockByProductId(Integer productId) {
        if (productId == null) return 0;
        Product product = productFacade.find(productId);
        return getCurrentStock(product);
    }
    
    // Trạng thái tồn kho (dựa vào minStock)
    public String getStockStatus(Product product) {
        if (product == null) return "N/A";
        int qty = product.getQuantity() != null ? product.getQuantity() : 0;
        int minStock = product.getMinStock() != null ? product.getMinStock() : 10;
        
        if (qty <= 0) return "Hết hàng";
        if (qty <= minStock) return "Sắp hết";
        return "Còn hàng";
    }
    
    public String getStockStatusColor(Product product) {
        if (product == null) return "#666";
        int qty = product.getQuantity() != null ? product.getQuantity() : 0;
        int minStock = product.getMinStock() != null ? product.getMinStock() : 10;
        
        if (qty <= 0) return "#dc3545";
        if (qty <= minStock) return "#ffc107";
        return "#28a745";
    }
    
    // Cập nhật số lượng tối thiểu cho sản phẩm (dùng cho nút save trong bảng)
    public void saveMinStock(Product product) {
        try {
            if (product != null) {
                // Nếu minStock null hoặc < 0, set mặc định là 10
                if (product.getMinStock() == null || product.getMinStock() < 0) {
                    product.setMinStock(10);
                }
                productFacade.edit(product);
                addInfo("✅ Đã cập nhật tồn tối thiểu cho " + product.getName() + " = " + product.getMinStock());
            }
        } catch (Exception e) {
            addErr("❌ Lỗi cập nhật: " + e.getMessage());
        }
    }
    
    // Lấy minStock với giá trị mặc định nếu null
    public Integer getDisplayMinStock(Product product) {
        if (product == null) return 10;
        return product.getMinStock() != null ? product.getMinStock() : 10;
    }
    
    // Set mặc định minStock = 10 cho tất cả sản phẩm chưa có
    public void initAllMinStock() {
        try {
            List<Product> all = productFacade.findAll();
            int count = 0;
            for (Product p : all) {
                if (p.getMinStock() == null || p.getMinStock() <= 0) {
                    p.setMinStock(10);
                    productFacade.edit(p);
                    count++;
                }
            }
            if (count > 0) {
                addInfo("✅ Đã cập nhật tồn tối thiểu mặc định (10) cho " + count + " sản phẩm");
            } else {
                addInfo("ℹ️ Tất cả sản phẩm đã có tồn tối thiểu");
            }
        } catch (Exception e) {
            addErr("❌ Lỗi: " + e.getMessage());
        }
    }

    // Format helpers
    public String formatDate(Date date) {
        if (date == null) return "-";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }

    public String formatCurrency(Integer amount) {
        if (amount == null) return "-";
        return String.format("%,d", amount) + " VNĐ";
    }

    public String getTypeText(String type) {
        if (type == null) return "-";
        if ("Import".equalsIgnoreCase(type)) return "📥 Nhập kho";
        if ("Export".equalsIgnoreCase(type)) return "📤 Xuất kho";
        return type;
    }

    public String getTypeColor(String type) {
        if (type == null) return "#666";
        if ("Import".equalsIgnoreCase(type)) return "#28a745";
        if ("Export".equalsIgnoreCase(type)) return "#dc3545";
        return "#666";
    }

    public boolean isImportType() {
        return selected != null && "Import".equalsIgnoreCase(selected.getType());
    }

    // ==================== GETTERS AND SETTERS ====================
    
    public StockTransactions getSelected() { return selected; }
    public void setSelected(StockTransactions selected) { this.selected = selected; }

    public boolean isShowForm() { return showForm; }
    public void setShowForm(boolean showForm) { this.showForm = showForm; }

    public Integer getSelectedProductId() { return selectedProductId; }
    public void setSelectedProductId(Integer selectedProductId) { this.selectedProductId = selectedProductId; }

    public Integer getSelectedSupplierId() { return selectedSupplierId; }
    public void setSelectedSupplierId(Integer selectedSupplierId) { this.selectedSupplierId = selectedSupplierId; }

    public Product getSelectedProduct() { return selectedProduct; }
    public void setSelectedProduct(Product selectedProduct) { this.selectedProduct = selectedProduct; }

    public String getStockSearchKeyword() { return stockSearchKeyword; }
    public void setStockSearchKeyword(String stockSearchKeyword) { this.stockSearchKeyword = stockSearchKeyword; }

    public String getHistorySearchKeyword() { return historySearchKeyword; }
    public void setHistorySearchKeyword(String historySearchKeyword) { this.historySearchKeyword = historySearchKeyword; }

    public String getHistoryFilterType() { return historyFilterType; }
    public void setHistoryFilterType(String historyFilterType) { this.historyFilterType = historyFilterType; }

    public int getStockCurrentPage() { return stockCurrentPage; }
    public void setStockCurrentPage(int stockCurrentPage) { this.stockCurrentPage = stockCurrentPage; }

    public int getStockPageSize() { return stockPageSize; }
    public void setStockPageSize(int stockPageSize) { this.stockPageSize = stockPageSize; }

    public int getHistoryCurrentPage() { return historyCurrentPage; }
    public void setHistoryCurrentPage(int historyCurrentPage) { this.historyCurrentPage = historyCurrentPage; }

    public int getHistoryPageSize() { return historyPageSize; }
    public void setHistoryPageSize(int historyPageSize) { this.historyPageSize = historyPageSize; }

    // Helper methods
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErr(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
}
