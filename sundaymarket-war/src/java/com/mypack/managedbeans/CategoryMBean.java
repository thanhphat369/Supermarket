package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import mypack.entity.Category;
import mypack.sessionbean.CategoryFacadeLocal;

@Named(value = "categoryMBean")
@SessionScoped
public class CategoryMBean implements Serializable {

    @EJB
    private CategoryFacadeLocal categoryFacade;
    
    private Category selected = new Category();
    private boolean editMode = false;
    private String searchKeyword;
    private int currentPage = 1;
    private int pageSize = 10;
    
    // Lấy danh sách category
    public List<Category> getItems() {
        try {
            List<Category> all = categoryFacade.findAll();
            
            if (all == null) {
                return new java.util.ArrayList<>();
            }
            
            // Áp dụng tìm kiếm nếu có keyword
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keyword = searchKeyword.trim().toLowerCase();
                return all.stream()
                        .filter(category -> 
                            (category.getCategoryName() != null && category.getCategoryName().toLowerCase().contains(keyword))
                        )
                        .collect(java.util.stream.Collectors.toList());
            }
            
            return all;
        } catch (Exception e) {
            System.err.println("CategoryMBean.getItems() - Error: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách category phân trang
    public List<Category> getPagedItems() {
        try {
            List<Category> base = getItems();
            
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
            System.err.println("CategoryMBean.getPagedItems() - Error: " + e.getMessage());
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
            List<Category> items = getItems();
            return items != null ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Tạo mới
    public void prepareCreate() {
        selected = new Category();
        editMode = false;
    }
    
    // Chỉnh sửa
    public void prepareEdit(Category c) {
        selected = c;
        editMode = true;
    }
    
    // Delete
    public void delete(Category c) {
        try {
            // Check if category is being used
            if (c.getProductCollection() != null && !c.getProductCollection().isEmpty()) {
                addErr("⚠️ Cannot delete this category because there are related products!");
                return;
            }
            
            categoryFacade.remove(c);
            addInfo("✅ Category deleted!");
            
            if (selected != null && selected.getCategoryID() != null && selected.getCategoryID().equals(c.getCategoryID())) {
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
            if (selected.getCategoryName() == null || selected.getCategoryName().trim().isEmpty()) {
                addErr("⚠️ Please enter category name!");
                return;
            }
            
            boolean isNew = selected.getCategoryID() == null;
            if (isNew) {
                categoryFacade.create(selected);
                addInfo("✅ New category added!");
            } else {
                categoryFacade.edit(selected);
                addInfo("✅ Category information updated!");
            }
            
            prepareCreate();
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Save failed: " + e.getMessage());
        }
    }
    
    // Getters and Setters
    public Category getSelected() {
        return selected;
    }
    
    public void setSelected(Category selected) {
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

