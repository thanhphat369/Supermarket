package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import mypack.entity.Category;
import mypack.entity.Product;
import mypack.sessionbean.CategoryFacadeLocal;
import mypack.sessionbean.ProductFacadeLocal;

@Named(value = "customerProductMBean")
@ViewScoped
public class CustomerProductMBean implements Serializable {

    @EJB
    private ProductFacadeLocal productFacade;
    
    @EJB
    private CategoryFacadeLocal categoryFacade;
    
    private Integer selectedCategoryId;
    private String searchKeyword;
    
    public CustomerProductMBean() {
    }
    
    // Lấy danh sách tất cả categories
    public List<Category> getAllCategories() {
        try {
            return categoryFacade.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách sản phẩm
    public List<Product> getProducts() {
        try {
            List<Product> all = productFacade.findAll();
            
            if (all == null) {
                return new java.util.ArrayList<>();
            }
            
            // Lọc theo category nếu có
            if (selectedCategoryId != null) {
                all = all.stream()
                        .filter(product -> 
                            product.getCategoryID() != null && 
                            product.getCategoryID().getCategoryID().equals(selectedCategoryId)
                        )
                        .collect(java.util.stream.Collectors.toList());
            }
            
            // Áp dụng tìm kiếm nếu có keyword
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keyword = searchKeyword.trim().toLowerCase();
                all = all.stream()
                        .filter(product -> 
                            (product.getName() != null && product.getName().toLowerCase().contains(keyword)) ||
                            (product.getDescription() != null && product.getDescription().toLowerCase().contains(keyword)) ||
                            (product.getBrandID() != null && product.getBrandID().getBrandName() != null && 
                             product.getBrandID().getBrandName().toLowerCase().contains(keyword)) ||
                            (product.getCategoryID() != null && product.getCategoryID().getCategoryName() != null && 
                             product.getCategoryID().getCategoryName().toLowerCase().contains(keyword))
                        )
                        .collect(java.util.stream.Collectors.toList());
            }
            
            return all;
        } catch (Exception e) {
            System.err.println("CustomerProductMBean.getProducts() - Error: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy sản phẩm theo category
    public List<Product> getProductsByCategory(Integer categoryId) {
        try {
            List<Product> all = productFacade.findAll();
            if (all == null || categoryId == null) {
                return new java.util.ArrayList<>();
            }
            
            return all.stream()
                    .filter(product -> 
                        product.getCategoryID() != null && 
                        product.getCategoryID().getCategoryID().equals(categoryId)
                    )
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy URL hình ảnh sản phẩm
    public String getImageUrl(Product product) {
        if (product == null || product.getImageURL() == null || product.getImageURL().isEmpty()) {
            return null;
        }
        
        // Extract filename if it contains path (old data)
        String imageURL = product.getImageURL();
        String fileName;
        if (imageURL.contains("\\") || imageURL.contains("/")) {
            java.io.File file = new java.io.File(imageURL);
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
    
    // Format giá tiền
    public String formatPrice(int price) {
        return String.format("%,d", price) + "đ";
    }
    
    // Format giá tiền với đơn vị
    public String formatPriceWithUnit(int price, String unit) {
        if (unit == null || unit.isEmpty()) {
            return formatPrice(price);
        }
        return formatPrice(price) + "/" + unit;
    }
    
    // Lấy số lượng tồn kho
    public int getStock(Product product) {
        if (product == null || product.getInventory() == null) {
            return 0;
        }
        return product.getInventory().getStock();
    }
    
    // Kiểm tra sản phẩm còn hàng
    public boolean isInStock(Product product) {
        return getStock(product) > 0;
    }
    
    // Getters and Setters
    public Integer getSelectedCategoryId() {
        return selectedCategoryId;
    }
    
    public void setSelectedCategoryId(Integer selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }
    
    public String getSearchKeyword() {
        return searchKeyword;
    }
    
    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }
    
    // Filter by category
    public String filterByCategory(Integer categoryId) {
        this.selectedCategoryId = categoryId;
        return null; // Stay on same page
    }
    
    // Clear filter
    public String clearFilter() {
        this.selectedCategoryId = null;
        this.searchKeyword = null;
        return null; // Stay on same page
    }
}

