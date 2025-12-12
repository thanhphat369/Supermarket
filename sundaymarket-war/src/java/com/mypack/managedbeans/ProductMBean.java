package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import mypack.entity.Brand;
import mypack.entity.Category;
import mypack.entity.Product;
import mypack.sessionbean.BrandFacadeLocal;
import mypack.sessionbean.CategoryFacadeLocal;
import mypack.sessionbean.ProductFacadeLocal;

@Named(value = "productMBean")
@SessionScoped
public class ProductMBean implements Serializable {

    @EJB
    private ProductFacadeLocal productFacade;
    
    @EJB
    private BrandFacadeLocal brandFacade;
    
    @EJB
    private CategoryFacadeLocal categoryFacade;
    
    private Part uploadedFile;
    private Product selected = new Product();
    private boolean editMode = false;
    private Integer selectedBrandId;
    private Integer selectedCategoryId;
    private String searchKeyword;
    private int currentPage = 1;
    private int pageSize = 10;
    
    // Lấy danh sách brand
    public List<Brand> getAllBrands() {
        try {
            return brandFacade.findAll();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách category
    public List<Category> getAllCategories() {
        try {
            return categoryFacade.findAll();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách product
    public List<Product> getItems() {
        try {
            List<Product> all = productFacade.findAll();
            
            if (all == null) {
                return new java.util.ArrayList<>();
            }
            
            // Áp dụng tìm kiếm nếu có keyword
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keyword = searchKeyword.trim().toLowerCase();
                return all.stream()
                        .filter(product -> 
                            (product.getName() != null && product.getName().toLowerCase().contains(keyword)) ||
                            (product.getDescription() != null && product.getDescription().toLowerCase().contains(keyword)) ||
                            (product.getBrandID() != null && product.getBrandID().getBrandName() != null && product.getBrandID().getBrandName().toLowerCase().contains(keyword)) ||
                            (product.getCategoryID() != null && product.getCategoryID().getCategoryName() != null && product.getCategoryID().getCategoryName().toLowerCase().contains(keyword))
                        )
                        .collect(java.util.stream.Collectors.toList());
            }
            
            return all;
        } catch (Exception e) {
            System.err.println("ProductMBean.getItems() - Error: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách product phân trang
    public List<Product> getPagedItems() {
        try {
            List<Product> base = getItems();
            
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
            System.err.println("ProductMBean.getPagedItems() - Error: " + e.getMessage());
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
            List<Product> items = getItems();
            return items != null ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Tạo mới
    public void prepareCreate() {
        selected = new Product();
        selectedBrandId = null;
        selectedCategoryId = null;
        editMode = false;
        uploadedFile = null;
    }
    
    // Chỉnh sửa
    public void prepareEdit(Product p) {
        selected = p;
        selectedBrandId = (p.getBrandID() != null) ? p.getBrandID().getBrandID() : null;
        selectedCategoryId = (p.getCategoryID() != null) ? p.getCategoryID().getCategoryID() : null;
        editMode = true;
        uploadedFile = null;
    }
    
    // Xóa
    public void delete(Product p) {
        try {
            // Check if product is being used
            if (p.getOrderDetailsCollection() != null && !p.getOrderDetailsCollection().isEmpty()) {
                addErr("⚠️ Cannot delete this product because there are related orders!");
                return;
            }
            
            if (p.getShoppingCartCollection() != null && !p.getShoppingCartCollection().isEmpty()) {
                addErr("⚠️ Cannot delete this product because it is in shopping cart!");
                return;
            }
            
            productFacade.remove(p);
            addInfo("✅ Product deleted!");
            
            if (selected != null && selected.getProductID() != null && selected.getProductID().equals(p.getProductID())) {
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
            if (selected.getName() == null || selected.getName().trim().isEmpty()) {
                addErr("⚠️ Please enter product name!");
                return;
            }
            
            if (selected.getUnitPrice() <= 0) {
                addErr("⚠️ Please enter valid product price!");
                return;
            }
            
            // Set Brand
            if (selectedBrandId != null) {
                Brand brand = brandFacade.find(selectedBrandId);
                if (brand != null) {
                    selected.setBrandID(brand);
                } else {
                    addErr("⚠️ Invalid brand!");
                    return;
                }
            } else {
                addErr("⚠️ Please select brand!");
                return;
            }
            
            // Set Category
            if (selectedCategoryId != null) {
                Category category = categoryFacade.find(selectedCategoryId);
                if (category != null) {
                    selected.setCategoryID(category);
                } else {
                    addErr("⚠️ Invalid category!");
                    return;
                }
            } else {
                addErr("⚠️ Please select category!");
                return;
            }
            
            boolean isNew = selected.getProductID() == null;
            
            // Upload image if available
            if (uploadedFile != null && uploadedFile.getSize() > 0) {
                uploadImageFile();
            }
            
            if (isNew) {
                productFacade.create(selected);
                addInfo("✅ New product added!");
            } else {
                productFacade.edit(selected);
                addInfo("✅ Product information updated!");
            }
            
            // Refresh selected product
            if (selected.getProductID() != null) {
                Product refreshed = productFacade.find(selected.getProductID());
                if (refreshed != null) {
                    selected = refreshed;
                    selectedBrandId = (selected.getBrandID() != null) ? selected.getBrandID().getBrandID() : null;
                    selectedCategoryId = (selected.getCategoryID() != null) ? selected.getCategoryID().getCategoryID() : null;
                }
            }
            
            if (isNew) {
                prepareCreate();
            }
            
            uploadedFile = null;
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Save failed: " + e.getMessage());
        }
    }
    
    // Upload image file
    private void uploadImageFile() {
        if (uploadedFile == null) {
            return;
        }
        
        try {
            String uploadDir = getUploadDirectory();
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            if (!dir.canWrite()) {
                addErr("❌ No write permission to images directory!");
                return;
            }
            
            String originalFileName = uploadedFile.getSubmittedFileName();
            if (originalFileName == null || originalFileName.isEmpty()) {
                addErr("❌ Invalid filename!");
                return;
            }
            
            // Sanitize filename
            String sanitizedName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                                                   .replaceAll("_{2,}", "_")
                                                   .replaceAll("^_|_$", "");
            
            String timestamp = String.valueOf(System.currentTimeMillis());
            String productName = selected.getName() != null ? selected.getName().replaceAll("[^a-zA-Z0-9]", "_") : "product";
            String fileName = "product_" + productName + "_" + timestamp + "_" + sanitizedName;
            File file = new File(dir, fileName);
            
            // Upload file
            try (InputStream in = uploadedFile.getInputStream();
                 FileOutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
            
            if (!file.exists() || file.length() == 0) {
                addErr("❌ File was not created successfully!");
                return;
            }
            
            // Save only filename to DB
            selected.setImageURL(fileName);
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Image upload failed: " + e.getMessage());
        }
    }
    
    private String getUploadDirectory() {
        String uploadDir = "D:\\Netbean\\DO_AN_4\\sundaymarket\\sundaymarket-war\\web\\resources\\images";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return uploadDir;
    }
    
    // Get image URL for display
    public String getImageUrlForProduct(Product p) {
        if (p == null || p.getImageURL() == null || p.getImageURL().isEmpty()) {
            return null;
        }
        
        // Extract filename if it contains path (old data)
        String imageURL = p.getImageURL();
        String fileName;
        if (imageURL.contains("\\") || imageURL.contains("/")) {
            File file = new File(imageURL);
            fileName = file.getName();
        } else {
            fileName = imageURL;
        }
        
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String contextPath = facesContext.getExternalContext().getRequestContextPath();
            return contextPath + "/resources/images/" + fileName + "?v=" + (System.currentTimeMillis() % 1000000);
        }
        return "/resources/images/" + fileName;
    }
    
    // Getters and Setters
    public Product getSelected() {
        return selected;
    }
    
    public void setSelected(Product selected) {
        this.selected = selected;
    }
    
    public boolean isEditMode() {
        return editMode;
    }
    
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }
    
    public Integer getSelectedBrandId() {
        return selectedBrandId;
    }
    
    public void setSelectedBrandId(Integer selectedBrandId) {
        this.selectedBrandId = selectedBrandId;
    }
    
    public Integer getSelectedCategoryId() {
        return selectedCategoryId;
    }
    
    public void setSelectedCategoryId(Integer selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }
    
    public Part getUploadedFile() {
        return uploadedFile;
    }
    
    public void setUploadedFile(Part uploadedFile) {
        this.uploadedFile = uploadedFile;
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

