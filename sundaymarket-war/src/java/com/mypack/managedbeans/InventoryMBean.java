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
import mypack.sessionbean.InventoryFacadeLocal;
import mypack.sessionbean.ProductFacadeLocal;

@Named(value = "inventoryMBean")
@SessionScoped
public class InventoryMBean implements Serializable {

    @EJB
    private InventoryFacadeLocal inventoryFacade;
    
    @EJB
    private ProductFacadeLocal productFacade;
    
    private Inventory selected = new Inventory();
    private boolean editMode = false;
    private Integer selectedProductId;
    private String searchKeyword;
    private int currentPage = 1;
    private int pageSize = 10;
    
    // Lấy danh sách product
    public List<Product> getAllProducts() {
        try {
            return productFacade.findAll();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
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
                        .filter(inv -> 
                            (inv.getProduct() != null && inv.getProduct().getName() != null && inv.getProduct().getName().toLowerCase().contains(keyword)) ||
                            (inv.getProductID() != null && String.valueOf(inv.getProductID()).contains(keyword))
                        )
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
    
    // Tạo mới
    public void prepareCreate() {
        selected = new Inventory();
        selectedProductId = null;
        editMode = false;
    }
    
    // Chỉnh sửa
    public void prepareEdit(Inventory inv) {
        selected = inv;
        selectedProductId = inv.getProductID();
        editMode = true;
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
    
    // Save
    public void save() {
        try {
            // Validate required fields
            if (selectedProductId == null) {
                addErr("⚠️ Please select product!");
                return;
            }
            
            if (selected.getStock() < 0) {
                addErr("⚠️ Invalid stock quantity!");
                return;
            }
            
            if (selected.getMinStock() < 0) {
                addErr("⚠️ Invalid minimum stock quantity!");
                return;
            }
            
            // Set Product
            Product product = productFacade.find(selectedProductId);
            if (product == null) {
                addErr("⚠️ Product does not exist!");
                return;
            }
            
            selected.setProductID(selectedProductId);
            selected.setProduct(product);
            selected.setLastUpdate(new Date());
            
            boolean isNew = selected.getProductID() == null || inventoryFacade.find(selected.getProductID()) == null;
            
            if (isNew) {
                inventoryFacade.create(selected);
                addInfo("✅ New inventory added!");
            } else {
                inventoryFacade.edit(selected);
                addInfo("✅ Inventory information updated!");
            }
            
            prepareCreate();
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Save failed: " + e.getMessage());
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

