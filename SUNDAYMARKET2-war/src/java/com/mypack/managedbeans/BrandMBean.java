package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import mypack.entity.Brand;
import mypack.entity.Product;
import mypack.entity.Supplier;
import mypack.sessionbean.BrandFacadeLocal;
import mypack.sessionbean.SupplierFacadeLocal;

@Named(value = "brandMBean")
@SessionScoped
public class BrandMBean implements Serializable {

    @EJB
    private BrandFacadeLocal brandFacade;
    
    @EJB
    private SupplierFacadeLocal supplierFacade;
    
    private Brand selected = new Brand();
    private boolean editMode = false;
    private boolean showForm = false; // Control form visibility
    private Integer selectedSupplierId;
    private String searchKeyword;
    private int currentPage = 1;
    private int pageSize = 10;
    
    // For detail view
    private Brand selectedBrandForDetail;
    private boolean showDetailModal = false;
    
    // Lấy danh sách supplier
    public List<Supplier> getAllSuppliers() {
        try {
            return supplierFacade.findAll();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách brand
    public List<Brand> getItems() {
        try {
            List<Brand> all = brandFacade.findAll();
            
            if (all == null) {
                return new java.util.ArrayList<>();
            }
            
            // Áp dụng tìm kiếm nếu có keyword
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keyword = searchKeyword.trim().toLowerCase();
                return all.stream()
                        .filter(brand -> 
                            (brand.getBrandName() != null && brand.getBrandName().toLowerCase().contains(keyword))
                        )
                        .collect(java.util.stream.Collectors.toList());
            }
            
            return all;
        } catch (Exception e) {
            System.err.println("BrandMBean.getItems() - Error: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách brand phân trang
    public List<Brand> getPagedItems() {
        try {
            List<Brand> base = getItems();
            
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
            System.err.println("BrandMBean.getPagedItems() - Error: " + e.getMessage());
            e.printStackTrace();
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
            List<Brand> items = getItems();
            return items != null ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Tạo mới
    public void prepareCreate() {
        selected = new Brand();
        selectedSupplierId = null;
        editMode = false;
        showForm = true; // Show form when adding new
    }
    
    // Chỉnh sửa
    public void prepareEdit(Brand b) {
        selected = b;
        selectedSupplierId = (selected.getSupplierID() != null) ? selected.getSupplierID().getSupplierID() : null;
        editMode = true;
        showForm = true; // Show form when editing
    }
    
    // Cancel form (close form)
    public void cancelForm() {
        showForm = false;
        selected = new Brand();
        selectedSupplierId = null;
        editMode = false;
    }
    
    // Getter/Setter for showForm
    public boolean isShowForm() {
        return showForm;
    }
    
    public void setShowForm(boolean showForm) {
        this.showForm = showForm;
    }
    
    // Delete
    public void delete(Brand b) {
        try {
            // Check if brand is being used
            if (b.getProductCollection() != null && !b.getProductCollection().isEmpty()) {
                addErr("⚠️ Cannot delete this brand because there are related products!");
                return;
            }
            
            brandFacade.remove(b);
            addInfo("✅ Brand deleted!");
            
            if (selected != null && selected.getBrandID() != null && selected.getBrandID().equals(b.getBrandID())) {
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
            if (selected.getBrandName() == null || selected.getBrandName().trim().isEmpty()) {
                addErr("⚠️ Vui lòng nhập tên thương hiệu!");
                return;
            }
            
            // Set Supplier
            if (selectedSupplierId != null) {
                Supplier supplier = supplierFacade.find(selectedSupplierId);
                if (supplier != null) {
                    selected.setSupplierID(supplier);
                } else {
                    addErr("⚠️ Nhà cung cấp không hợp lệ!");
                    return;
                }
            } else {
                // Có thể để null nếu không bắt buộc
                selected.setSupplierID(null);
            }
            
            boolean isNew = selected.getBrandID() == null;
            if (isNew) {
                brandFacade.create(selected);
                addInfo("✅ New brand added successfully!");
            } else {
                brandFacade.edit(selected);
                addInfo("✅ Brand updated successfully!");
            }
            
            // Close form after successful save
            showForm = false;
            prepareCreate();
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Lưu thất bại: " + e.getMessage());
        }
    }
    
    // Lấy danh sách products của brand
    public List<Product> getProductsForBrand(Brand brand) {
        if (brand == null || brand.getProductCollection() == null) {
            return new java.util.ArrayList<>();
        }
        return new java.util.ArrayList<>(brand.getProductCollection());
    }
    
    // Đếm số lượng products
    public int getProductCount(Brand brand) {
        if (brand == null || brand.getProductCollection() == null) {
            return 0;
        }
        return brand.getProductCollection().size();
    }
    
    // Format currency
    public String formatCurrency(int amount) {
        return String.format("%,d", amount) + " VNĐ";
    }
    
    // Xem chi tiết products của brand
    public void viewBrandDetails(Brand brand) {
        selectedBrandForDetail = brand;
        showDetailModal = true;
    }
    
    public void closeDetailModal() {
        showDetailModal = false;
        selectedBrandForDetail = null;
    }
    
    public Brand getSelectedBrandForDetail() {
        return selectedBrandForDetail;
    }
    
    public boolean isShowDetailModal() {
        return showDetailModal;
    }
    
    // Getters and Setters
    public Brand getSelected() {
        return selected;
    }
    
    public void setSelected(Brand selected) {
        this.selected = selected;
    }
    
    public boolean isEditMode() {
        return editMode;
    }
    
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
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
    
    public Integer getSelectedSupplierId() {
        return selectedSupplierId;
    }
    
    public void setSelectedSupplierId(Integer selectedSupplierId) {
        this.selectedSupplierId = selectedSupplierId;
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

