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
    private boolean showForm = false; // Control form visibility
    private String searchKeyword;
    private int currentPage = 1;
    private int pageSize = 10;
    private Integer selectedParentCategoryId; // ID của parent category được chọn
    
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
    
    // Kiểm tra xem có category nào không
    public boolean hasCategories() {
        try {
            List<Category> all = categoryFacade.findAll();
            return all != null && !all.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    // Lấy danh sách tất cả categories (để chọn parent, loại trừ category hiện tại khi edit)
    public List<Category> getAllCategories() {
        try {
            List<Category> all = categoryFacade.findAll();
            if (all == null) {
                return new java.util.ArrayList<>();
            }
            // Loại trừ category hiện tại khi edit (không cho chọn chính nó làm parent)
            if (editMode && selected != null && selected.getCategoryID() != null) {
                return all.stream()
                        .filter(cat -> !cat.getCategoryID().equals(selected.getCategoryID()))
                        .collect(java.util.stream.Collectors.toList());
            }
            return all;
        } catch (Exception e) {
            System.err.println("CategoryMBean.getAllCategories() - Error: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Format tên category để hiển thị trong dropdown (phân biệt thư mục cha/con)
    public String getCategoryDisplayName(Category cat) {
        if (cat == null || cat.getCategoryName() == null) {
            return "";
        }
        if (cat.getParentCategoryID() == null) {
            // Thư mục cha
            return "📁 " + cat.getCategoryName() + " (Thư mục cha)";
        } else {
            // Thư mục con
            return "📂 " + cat.getCategoryName() + " (Thư mục con)";
        }
    }
    
    // Tạo mới
    public void prepareCreate() {
        selected = new Category();
        selectedParentCategoryId = null;
        editMode = false;
        showForm = true; // Show form when adding new
    }
    
    // Chỉnh sửa
    public void prepareEdit(Category c) {
        // Load lại từ database để đảm bảo có đầy đủ thông tin
        if (c != null && c.getCategoryID() != null) {
            selected = categoryFacade.find(c.getCategoryID());
            if (selected == null) {
                selected = c; // Fallback nếu không tìm thấy
            }
        } else {
            selected = c;
        }
        
        // Set parent category ID nếu có
        if (selected != null && selected.getParentCategoryID() != null) {
            selectedParentCategoryId = selected.getParentCategoryID().getCategoryID();
        } else {
            selectedParentCategoryId = null;
        }
        editMode = true;
        showForm = true; // Show form when editing
    }
    
    // Cancel form (close form)
    public void cancelForm() {
        showForm = false;
        selected = new Category();
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
    public void delete(Category c) {
        try {
            // Load lại từ database để đảm bảo có đầy đủ thông tin
            Category categoryToDelete = categoryFacade.find(c.getCategoryID());
            if (categoryToDelete == null) {
                categoryToDelete = c;
            }
            
            // Check if category is being used by products
            if (categoryToDelete.getProductCollection() != null && !categoryToDelete.getProductCollection().isEmpty()) {
                addErr("⚠️ Cannot delete this category because it has related products!");
                return;
            }
            
            // Check if category has child categories
            if (categoryToDelete.getCategoryCollection() != null && !categoryToDelete.getCategoryCollection().isEmpty()) {
                addErr("⚠️ Không thể xóa danh mục này vì có danh mục con! Vui lòng xóa các danh mục con trước.");
                return;
            }
            
            categoryFacade.remove(categoryToDelete);
            addInfo("✅ Category deleted successfully!");
            
            if (selected != null && selected.getCategoryID() != null && selected.getCategoryID().equals(c.getCategoryID())) {
                prepareCreate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Xóa thất bại: " + e.getMessage());
        }
    }
    
    // Save
    public void save() {
        try {
            // Validate required fields
            if (selected.getCategoryName() == null || selected.getCategoryName().trim().isEmpty()) {
                addErr("⚠️ Vui lòng nhập tên danh mục!");
                return;
            }
            
            // Xử lý parent category
            if (selectedParentCategoryId != null && selectedParentCategoryId > 0) {
                // Có chọn parent category
                Category parent = categoryFacade.find(selectedParentCategoryId);
                if (parent != null) {
                    selected.setParentCategoryID(parent);
                } else {
                    addErr("⚠️ Danh mục cha không tồn tại!");
                    return;
                }
            } else {
                // Không chọn parent = tạo thư mục cha (root category)
                selected.setParentCategoryID(null);
            }
            
            boolean isNew = selected.getCategoryID() == null;
            if (isNew) {
                categoryFacade.create(selected);
                addInfo("✅ Thêm danh mục mới thành công!");
            } else {
                categoryFacade.edit(selected);
                addInfo("✅ Category updated successfully!");
            }
            
            // Close form after successful save
            showForm = false;
            prepareCreate();
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Lưu thất bại: " + e.getMessage());
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
    
    public Integer getSelectedParentCategoryId() {
        return selectedParentCategoryId;
    }
    
    public void setSelectedParentCategoryId(Integer selectedParentCategoryId) {
        this.selectedParentCategoryId = selectedParentCategoryId;
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
    
    // Format ID - loại bỏ "10" ở đầu nếu ID >= 1000
    public String formatID(Integer id) {
        if (id == null) return "-";
        // Nếu ID >= 1000 và bắt đầu bằng "10", loại bỏ "10" ở đầu
        if (id >= 1000 && id.toString().startsWith("10")) {
            String idStr = id.toString();
            if (idStr.length() > 2) {
                return idStr.substring(2); // Bỏ 2 ký tự đầu "10"
            }
        }
        return id.toString();
    }
    
    // Helper methods
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }
    
    private void addErr(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
}

