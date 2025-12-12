package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import mypack.entity.Brand;
import mypack.sessionbean.BrandFacadeLocal;

@Named(value = "brandMBean")
@SessionScoped
public class BrandMBean implements Serializable {

    @EJB
    private BrandFacadeLocal brandFacade;
    
    private Brand selected = new Brand();
    private boolean editMode = false;
    private String searchKeyword;
    private int currentPage = 1;
    private int pageSize = 10;
    
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
        editMode = false;
    }
    
    // Chỉnh sửa
    public void prepareEdit(Brand b) {
        selected = b;
        editMode = true;
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
                addErr("⚠️ Please enter brand name!");
                return;
            }
            
            boolean isNew = selected.getBrandID() == null;
            if (isNew) {
                brandFacade.create(selected);
                addInfo("✅ New brand added!");
            } else {
                brandFacade.edit(selected);
                addInfo("✅ Brand information updated!");
            }
            
            prepareCreate();
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Save failed: " + e.getMessage());
        }
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

