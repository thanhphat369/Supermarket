package com.mypack.managedbeans;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mypack.entity.Category;
import mypack.entity.Product;
import mypack.sessionbean.CategoryFacadeLocal;
import mypack.sessionbean.ProductFacadeLocal;

@Named("customerProductMBean")
@ViewScoped
public class CustomerProductMBean implements Serializable {
    
    @EJB
    private CategoryFacadeLocal categoryFacade;
    
    @EJB
    private ProductFacadeLocal productFacade;

    private Integer selectedCategoryId; // Selected Level 3 (for filtering products)
    private Integer selectedLevel1Id;   // Expanded Level 1
    private Integer selectedLevel2Id;   // Selected Level 2 (shows Level 3 in top bar)
    
    // Cache
    private List<Category> level1Cache;
    private Map<Integer, List<Category>> level2Cache = new HashMap<>();
    private Map<Integer, List<Category>> level3Cache = new HashMap<>();
    private List<Product> productsCache;
    private Integer lastProductQueryCategoryId = null;

    @PostConstruct
    public void init() {
        System.out.println("=== CustomerProductMBean.init() START ===");
        
        try {
            if (categoryFacade == null) {
                System.err.println("ERROR: categoryFacade is NULL!");
                level1Cache = new ArrayList<>();
                return;
            }
            
            level1Cache = categoryFacade.findLevel1();
            System.out.println("✓ Loaded " + (level1Cache != null ? level1Cache.size() : 0) + " Level 1 categories");
            
            if (level1Cache != null && !level1Cache.isEmpty()) {
                System.out.println("Level 1 categories:");
                level1Cache.forEach(c -> 
                    System.out.println("  - " + c.getCategoryName() + " (ID: " + c.getCategoryID() + ")")
                );
            }
            
        } catch (Exception e) {
            System.err.println("ERROR in init(): " + e.getMessage());
            e.printStackTrace();
            level1Cache = new ArrayList<>();
        }
        
        System.out.println("=== CustomerProductMBean.init() COMPLETE ===");
    }

    // =================== CATEGORY GETTERS WITH CACHE ===================
    
    public List<Category> getLevel1Categories() {
        if (level1Cache == null) {
            level1Cache = categoryFacade.findLevel1();
        }
        return level1Cache != null ? level1Cache : new ArrayList<>();
    }

    public List<Category> getLevel2Categories(Category lv1) {
        if (lv1 == null) return new ArrayList<>();
        
        Integer lv1Id = lv1.getCategoryID();
        if (!level2Cache.containsKey(lv1Id)) {
            List<Category> lv2List = categoryFacade.findLevel2(lv1);
            level2Cache.put(lv1Id, lv2List);
            System.out.println("Loaded " + lv2List.size() + " Level 2 categories for: " + lv1.getCategoryName());
        }
        return level2Cache.get(lv1Id);
    }

    public List<Category> getLevel3Categories(Category lv2) {
        if (lv2 == null) return new ArrayList<>();
        
        Integer lv2Id = lv2.getCategoryID();
        if (!level3Cache.containsKey(lv2Id)) {
            List<Category> lv3List = categoryFacade.findLevel3(lv2);
            level3Cache.put(lv2Id, lv3List);
            System.out.println("Loaded " + lv3List.size() + " Level 3 categories for: " + lv2.getCategoryName());
        }
        return level3Cache.get(lv2Id);
    }

    /**
     * Get Level 3 categories to show in TOP BAR
     * Only show when Level 2 is selected
     */
    public List<Category> getCurrentLevel3Categories() {
        if (selectedLevel2Id == null) {
            return new ArrayList<>();
        }
        Category lv2 = categoryFacade.find(selectedLevel2Id);
        if (lv2 == null) {
            return new ArrayList<>();
        }
        return getLevel3Categories(lv2);
    }
    
    // =================== SELECTION ACTIONS ===================
    
    /**
     * Click Level 1 -> Toggle expand/collapse Level 2 in sidebar
     */
    public String selectLevel1(Integer categoryId) {
        System.out.println("selectLevel1: " + categoryId);
        
        if (this.selectedLevel1Id != null && this.selectedLevel1Id.equals(categoryId)) {
            // Collapse if clicking same Level 1
            this.selectedLevel1Id = null;
            this.selectedLevel2Id = null;
            this.selectedCategoryId = null;
        } else {
            // Expand Level 1
            this.selectedLevel1Id = categoryId;
            this.selectedLevel2Id = null; // Reset Level 2
            this.selectedCategoryId = null; // Reset filter
        }
        return null;
    }
    
    /**
     * Click Level 2 -> Show Level 3 in top bar (don't filter yet)
     */
    public String selectLevel2(Integer categoryId) {
        System.out.println("selectLevel2: " + categoryId);
        
        this.selectedLevel2Id = categoryId;
        this.selectedCategoryId = null; // Reset Level 3 filter
        
        // Auto expand Level 1 if not already
        Category lv2 = categoryFacade.find(categoryId);
        if (lv2 != null && lv2.getParentCategoryID() != null) {
            this.selectedLevel1Id = lv2.getParentCategoryID().getCategoryID();
        }
        
        return null;
    }
    
    /**
     * Click Level 3 (in top bar) -> Filter products
     */
    public String filterByCategory(Integer categoryId) {
        System.out.println("filterByCategory: " + categoryId);
        this.selectedCategoryId = categoryId;
        return null;
    }
    
    /**
     * Clear ALL filters (sidebar "Tất cả" button)
     */
    public String clearFilter() {
        System.out.println("clearFilter - Clear ALL");
        this.selectedCategoryId = null;
        this.selectedLevel1Id = null;
        this.selectedLevel2Id = null;
        this.productsCache = null;
        this.lastProductQueryCategoryId = null;
        return null;
    }
    
    /**
     * Clear only Level 3 filter (top bar "Tất cả" button)
     * Keep Level 1 & Level 2 selected
     */
    public String clearCategoryFilter() {
        System.out.println("clearCategoryFilter - Keep Level 2 selected");
        this.selectedCategoryId = null;
        return null;
    }
    
    // =================== PRODUCT METHODS ===================
    
    /**
     * Get products based on selected category
     * With smart caching
     */
    public List<Product> getProducts() {
        boolean needRefresh = false;
        
        if (selectedCategoryId == null) {
            if (selectedLevel2Id != null) {
                // Show all products of Level 2
                if (lastProductQueryCategoryId == null || !lastProductQueryCategoryId.equals(selectedLevel2Id)) {
                    needRefresh = true;
                }
            } else {
                // Show all products
                if (lastProductQueryCategoryId != null) {
                    needRefresh = true;
                }
            }
        } else {
            // Level 3 is selected
            if (!selectedCategoryId.equals(lastProductQueryCategoryId)) {
                needRefresh = true;
            }
        }
        
        if (needRefresh || productsCache == null) {
            try {
                if (selectedCategoryId != null) {
                    // Filter by Level 3
                    productsCache = productFacade.findByCategory(selectedCategoryId);
                    lastProductQueryCategoryId = selectedCategoryId;
                    System.out.println("Loaded " + productsCache.size() + " products for Level 3 ID: " + selectedCategoryId);
                    
                } else if (selectedLevel2Id != null) {
                    // Show all products of Level 2 (all Level 3 children)
                    productsCache = productFacade.findByCategory(selectedLevel2Id);
                    lastProductQueryCategoryId = selectedLevel2Id;
                    System.out.println("Loaded " + productsCache.size() + " products for Level 2 ID: " + selectedLevel2Id);
                    
                } else {
                    // Show all products
                    productsCache = productFacade.findAllActive();
                    lastProductQueryCategoryId = null;
                    System.out.println("Loaded " + productsCache.size() + " total active products");
                }
            } catch (Exception e) {
                System.err.println("Error loading products: " + e.getMessage());
                e.printStackTrace();
                productsCache = new ArrayList<>();
            }
        }
        
        return productsCache != null ? productsCache : new ArrayList<>();
    }

    // =================== HELPER METHODS ===================
    
    public Integer getStock(Product product) {
        if (product == null) return 0;
        return product.getStock() != null ? product.getStock() : 0;
    }
    
    public boolean isInStock(Product product) {
        return getStock(product) > 0;
    }
    
    public String getImageUrl(Product product) {
        if (product == null || product.getImageURL() == null || product.getImageURL().isEmpty()) {
            return null;
        }
        
        String imageURL = product.getImageURL();
        String fileName;
        
        // Extract filename from path
        if (imageURL.contains("\\") || imageURL.contains("/")) {
            java.io.File file = new java.io.File(imageURL);
            fileName = file.getName();
        } else {
            fileName = imageURL;
        }
        
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        // Build full URL
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String contextPath = facesContext.getExternalContext().getRequestContextPath();
            return contextPath + "/resources/images/" + fileName;
        }
        return "/resources/images/" + fileName;
    }
    
 
    
    /**
     * Check if Level 1 is currently expanded
     */
    public boolean isLevel1Expanded(Category lv1) {
        return selectedLevel1Id != null && selectedLevel1Id.equals(lv1.getCategoryID());
    }
    
    /**
     * Check if Level 2 is currently selected
     */
    public boolean isLevel2Selected(Category lv2) {
        return selectedLevel2Id != null && selectedLevel2Id.equals(lv2.getCategoryID());
    }

    // =================== GETTERS/SETTERS ===================
    
    public Integer getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(Integer selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }
    
    public Integer getSelectedLevel1Id() {
        return selectedLevel1Id;
    }

    public void setSelectedLevel1Id(Integer selectedLevel1Id) {
        this.selectedLevel1Id = selectedLevel1Id;
    }

    public Integer getSelectedLevel2Id() {
        return selectedLevel2Id;
    }

    public void setSelectedLevel2Id(Integer selectedLevel2Id) {
        this.selectedLevel2Id = selectedLevel2Id;
    }
}