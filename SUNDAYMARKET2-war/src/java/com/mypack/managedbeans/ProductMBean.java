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
import java.util.Collection;
import java.util.List;
import mypack.entity.Brand;
import mypack.entity.Category;
import mypack.entity.Inventory;
import mypack.entity.Product;
import mypack.entity.StockTransactions;
import mypack.sessionbean.BrandFacadeLocal;
import mypack.sessionbean.CategoryFacadeLocal;
import mypack.sessionbean.InventoryFacadeLocal;
import mypack.sessionbean.ProductFacadeLocal;
import mypack.sessionbean.StockTransactionsFacadeLocal;

@Named(value = "productMBean")
@SessionScoped
public class ProductMBean implements Serializable {

    @EJB
    private ProductFacadeLocal productFacade;
    
    @EJB
    private BrandFacadeLocal brandFacade;
    
    @EJB
    private CategoryFacadeLocal categoryFacade;
    
    @EJB
    private InventoryFacadeLocal inventoryFacade;
    
    @EJB
    private StockTransactionsFacadeLocal stockTransactionsFacade;
    
    private Part uploadedFile; // File input (for backward compatibility)
    private String uploadedImageFilesJson; // JSON array of uploaded file names from servlet
    private Product selected = new Product();
    private boolean editMode = false;
    private boolean showForm = false; // Control form visibility
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
        showForm = true; // Show form when adding new
        uploadedFile = null;
    }
    
    // Chỉnh sửa
    public void prepareEdit(Product p) {
        // Refresh product from database to get latest data including images
        if (p != null && p.getProductID() != null) {
            Product refreshed = productFacade.find(p.getProductID());
            if (refreshed != null) {
                selected = refreshed;
            } else {
                selected = p;
            }
        } else {
            selected = p;
        }
        
        selectedBrandId = (selected.getBrandID() != null) ? selected.getBrandID().getBrandID() : null;
        selectedCategoryId = (selected.getCategoryID() != null) ? selected.getCategoryID().getCategoryID() : null;
        editMode = true;
        showForm = true; // Show form when editing
        uploadedFile = null;
        
        // Log for debugging
        System.out.println("ProductMBean.prepareEdit() - Product ID: " + (selected.getProductID() != null ? selected.getProductID() : "null"));
        System.out.println("ProductMBean.prepareEdit() - ImageURL: " + (selected.getImageURL() != null ? selected.getImageURL() : "null"));
        List<String> imageUrls = getImageUrlsForProduct(selected);
        System.out.println("ProductMBean.prepareEdit() - Parsed image URLs count: " + imageUrls.size());
    }
    
    // Cancel form (close form)
    public void cancelForm() {
        showForm = false;
        selected = new Product();
        selectedBrandId = null;
        selectedCategoryId = null;
        editMode = false;
        uploadedFile = null;
        uploadedImageFilesJson = null;
    }
    
    // Getter/Setter for uploadedImageFilesJson
    public String getUploadedImageFilesJson() {
        return uploadedImageFilesJson;
    }
    
    public void setUploadedImageFilesJson(String uploadedImageFilesJson) {
        this.uploadedImageFilesJson = uploadedImageFilesJson;
    }
    
    // Getter/Setter for showForm
    public boolean isShowForm() {
        return showForm;
    }
    
    public void setShowForm(boolean showForm) {
        this.showForm = showForm;
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
            
            // Check if images were uploaded via servlet (stored in uploadedImageFilesJson or hidden field)
            // Try to get from request parameter first (from hidden field)
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                String hiddenFieldValue = facesContext.getExternalContext().getRequestParameterMap().get("uploadedImageFilesHidden");
                if (hiddenFieldValue != null && !hiddenFieldValue.trim().isEmpty()) {
                    uploadedImageFilesJson = hiddenFieldValue;
                }
            }
            
            if (uploadedImageFilesJson != null && !uploadedImageFilesJson.trim().isEmpty()) {
                // Parse JSON array from servlet upload
                List<String> fileNames = parseJsonArray(uploadedImageFilesJson);
                if (!fileNames.isEmpty()) {
                    // Validate: 2-5 images
                    if (fileNames.size() < 2) {
                        addErr("⚠️ Please select at least 2 images!");
                        uploadedImageFilesJson = null;
                        return;
                    }
                    if (fileNames.size() > 5) {
                        addErr("⚠️ Maximum 5 images allowed!");
                        uploadedImageFilesJson = null;
                        return;
                    }
                    // Files already uploaded by servlet, just save JSON to DB
                    selected.setImageURL(uploadedImageFilesJson);
                    System.out.println("ProductMBean.save() - ✅ Using uploaded files from servlet: " + uploadedImageFilesJson);
                }
            } 
            // Fallback: Try to get from request (for backward compatibility)
            else {
                List<Part> parts = getUploadedPartsFromRequest();
                if (parts != null && !parts.isEmpty()) {
                    uploadMultipleImageFiles(parts);
                } else if (uploadedFile != null && uploadedFile.getSize() > 0) {
                    uploadImageFile();
                }
                // Note: If no images uploaded, keep existing images in DB
            }
            
            // Clear uploaded files JSON after use
            uploadedImageFilesJson = null;
            
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
            
            // Close form after successful save
            showForm = false;
            
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
            
            // Save as JSON array with single file (for consistency)
            List<String> fileNames = new java.util.ArrayList<>();
            fileNames.add(fileName);
            String jsonArray = createJsonArray(fileNames);
            selected.setImageURL(jsonArray);
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Image upload failed: " + e.getMessage());
        }
    }
    
    // Get all uploaded parts from request (for multiple file upload)
    private List<Part> getUploadedPartsFromRequest() {
        List<Part> parts = new java.util.ArrayList<>();
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                jakarta.servlet.http.HttpServletRequest request = 
                    (jakarta.servlet.http.HttpServletRequest) facesContext.getExternalContext().getRequest();
                
                // Get all parts from multipart request
                Collection<Part> allParts = request.getParts();
                if (allParts != null) {
                    System.out.println("ProductMBean.getUploadedPartsFromRequest() - Total parts in request: " + allParts.size());
                    
                    for (Part part : allParts) {
                        String fieldName = part.getName();
                        String submittedFileName = part.getSubmittedFileName();
                        String contentType = part.getContentType();
                        long size = part.getSize();
                        
                        System.out.println("ProductMBean.getUploadedPartsFromRequest() - Part: field=" + fieldName + 
                                          ", file=" + submittedFileName + 
                                          ", type=" + contentType + 
                                          ", size=" + size);
                        
                        // Filter only image files (not form fields)
                        // Form fields have null or empty submittedFileName
                        if (part != null && 
                            size > 0 && 
                            submittedFileName != null && 
                            !submittedFileName.isEmpty()) {
                            
                            // Check if it's an image file
                            if (contentType != null && contentType.startsWith("image/")) {
                                // Accept all image files, not just from specific field name
                                // (because HTML input might have different field names)
                                parts.add(part);
                                System.out.println("ProductMBean.getUploadedPartsFromRequest() - ✅ Added image: " + submittedFileName + ", field: " + fieldName + ", size: " + size);
                            } else {
                                System.out.println("ProductMBean.getUploadedPartsFromRequest() - ⚠️ Skipped (not image): " + submittedFileName);
                            }
                        } else {
                            System.out.println("ProductMBean.getUploadedPartsFromRequest() - ⚠️ Skipped (form field or empty): field=" + fieldName);
                        }
                    }
                }
                
                System.out.println("ProductMBean.getUploadedPartsFromRequest() - ✅ Total image files found: " + parts.size());
            }
        } catch (Exception e) {
            System.err.println("ProductMBean.getUploadedPartsFromRequest() - Error: " + e.getMessage());
            e.printStackTrace();
        }
        return parts;
    }
    
    // Upload multiple image files and save as JSON array
    private void uploadMultipleImageFiles(List<Part> parts) {
        if (parts == null || parts.isEmpty()) {
            addErr("⚠️ Please select image files!");
            return;
        }
        
        // Limit to 5 images
        if (parts.size() > 5) {
            addErr("⚠️ Maximum 5 images allowed!");
            return;
        }
        
        // Validate minimum 2 images
        if (parts.size() < 2) {
            addErr("⚠️ Please select at least 2 images!");
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
            
            String timestamp = String.valueOf(System.currentTimeMillis());
            String productName = selected.getName() != null ? selected.getName().replaceAll("[^a-zA-Z0-9]", "_") : "product";
            List<String> uploadedFileNames = new java.util.ArrayList<>();
            
            // Upload each file
            for (Part filePart : parts) {
                if (filePart == null || filePart.getSize() == 0) {
                    continue;
                }
                
                String originalFileName = filePart.getSubmittedFileName();
                if (originalFileName == null || originalFileName.isEmpty()) {
                    continue;
                }
                
                // Sanitize filename
                String sanitizedName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                                                       .replaceAll("_{2,}", "_")
                                                       .replaceAll("^_|_$", "");
                
                String fileName = "product_" + productName + "_" + timestamp + "_" + System.nanoTime() + "_" + sanitizedName;
                File file = new File(dir, fileName);
                
                // Upload file
                try (InputStream in = filePart.getInputStream();
                     FileOutputStream out = new FileOutputStream(file)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                }
                
                if (file.exists() && file.length() > 0) {
                    uploadedFileNames.add(fileName);
                }
            }
            
            if (uploadedFileNames.isEmpty()) {
                addErr("❌ No files were uploaded successfully!");
                return;
            }
            
            // Create JSON array from file names
            String jsonArray = createJsonArray(uploadedFileNames);
            selected.setImageURL(jsonArray);
            
            System.out.println("ProductMBean.uploadMultipleImageFiles() - ✅ Uploaded " + uploadedFileNames.size() + " images, JSON: " + jsonArray);
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Image upload failed: " + e.getMessage());
        }
    }
    
    // Create JSON array string from list of file names
    private String createJsonArray(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return "[]";
        }
        
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < fileNames.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            // Escape quotes in filename and wrap in quotes
            String escaped = fileNames.get(i).replace("\"", "\\\"");
            json.append("\"").append(escaped).append("\"");
        }
        json.append("]");
        return json.toString();
    }
    
    // Parse JSON array string to list of file names
    // Supports multiple formats:
    // 1. JSON array: ["file1.jpg","file2.jpg"]
    // 2. Comma-separated: file1.jpg,file2.jpg
    // 3. Path-separated: /path/file1.jpg,/path/file2.jpg
    // 4. Single file: file1.jpg
    private List<String> parseJsonArray(String jsonString) {
        List<String> fileNames = new java.util.ArrayList<>();
        if (jsonString == null || jsonString.trim().isEmpty() || jsonString.trim().equals("[]")) {
            return fileNames;
        }
        
        try {
            String trimmed = jsonString.trim();
            
            // Format 1: JSON array ["file1.jpg","file2.jpg"]
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                String content = trimmed.substring(1, trimmed.length() - 1).trim();
                if (!content.isEmpty()) {
                    // Split by comma, but handle quoted strings
                    String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    for (String part : parts) {
                        part = part.trim();
                        if (part.startsWith("\"") && part.endsWith("\"")) {
                            String fileName = part.substring(1, part.length() - 1).replace("\\\"", "\"");
                            if (!fileName.isEmpty()) {
                                // Extract filename if it contains path
                                if (fileName.contains("\\") || fileName.contains("/")) {
                                    File file = new File(fileName);
                                    fileName = file.getName();
                                }
                                fileNames.add(fileName);
                            }
                        }
                    }
                }
            } 
            // Format 2 & 3: Comma-separated (with or without paths)
            else if (trimmed.contains(",")) {
                String[] parts = trimmed.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (!part.isEmpty()) {
                        // Extract filename if it contains path
                        String fileName = part;
                        if (part.contains("\\") || part.contains("/")) {
                            File file = new File(part);
                            fileName = file.getName();
                        }
                        fileNames.add(fileName);
                    }
                }
            } 
            // Format 4: Single filename
            else {
                String fileName = trimmed;
                // Extract filename if it contains path
                if (trimmed.contains("\\") || trimmed.contains("/")) {
                    File file = new File(trimmed);
                    fileName = file.getName();
                }
                fileNames.add(fileName);
            }
            
            System.out.println("ProductMBean.parseJsonArray() - Input: " + jsonString);
            System.out.println("ProductMBean.parseJsonArray() - Parsed " + fileNames.size() + " files: " + fileNames);
        } catch (Exception e) {
            System.err.println("ProductMBean.parseJsonArray() - Error parsing: " + e.getMessage());
            e.printStackTrace();
            // Fallback: treat as single filename
            String fileName = jsonString;
            if (jsonString.contains("\\") || jsonString.contains("/")) {
                File file = new File(jsonString);
                fileName = file.getName();
            }
            fileNames.add(fileName);
        }
        
        return fileNames;
    }
    
    private String getUploadDirectory() {
        // ✅ Dùng đường dẫn tuyệt đối cố định - lưu vào resources/product
        String uploadDir = "D:\\Netbean\\DO_AN_4\\sundaymarket\\sundaymarket-war\\web\\resources\\product";
        
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            System.out.println("ProductMBean.getUploadDirectory() - Created directory: " + created + " at: " + uploadDir);
        } else {
            System.out.println("ProductMBean.getUploadDirectory() - Directory already exists: " + uploadDir);
        }
        
        System.out.println("ProductMBean.getUploadDirectory() - ✅ Using absolute path: " + uploadDir);
        return uploadDir;
    }
    
    // Get first image URL for display (backward compatibility)
    public String getImageUrlForProduct(Product p) {
        List<String> imageUrls = getImageUrlsForProduct(p);
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls.get(0);
        }
        return null;
    }
    
    // Get all image URLs for product (parse JSON array)
    public List<String> getImageUrlsForProduct(Product p) {
        List<String> imageUrls = new java.util.ArrayList<>();
        if (p == null || p.getImageURL() == null || p.getImageURL().isEmpty()) {
            return imageUrls;
        }
        
        // Parse JSON array
        List<String> fileNames = parseJsonArray(p.getImageURL());
        
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String contextPath = "";
        if (facesContext != null) {
            contextPath = facesContext.getExternalContext().getRequestContextPath();
        }
        
        long cacheBuster = System.currentTimeMillis() % 1000000;
        for (String fileName : fileNames) {
            // Handle old format with full path
            if (fileName.contains("\\") || fileName.contains("/")) {
                File file = new File(fileName);
                fileName = file.getName();
            }
            
            if (fileName != null && !fileName.isEmpty()) {
                String url = contextPath + "/resources/product/" + fileName + "?v=" + cacheBuster;
                imageUrls.add(url);
            }
        }
        
        return imageUrls;
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
    
    // Lấy giá nhập mới nhất từ StockTransactions (từ giao dịch Import mới nhất)
    public Integer getLatestImportPrice(Product product) {
        if (product == null || product.getProductID() == null) {
            return null;
        }
        try {
            List<StockTransactions> allTransactions = stockTransactionsFacade.findAll();
            if (allTransactions == null || allTransactions.isEmpty()) {
                return null;
            }
            
            // Lọc các giao dịch Import của sản phẩm này, sắp xếp theo thời gian mới nhất
            return allTransactions.stream()
                    .filter(t -> t.getProductID() != null && 
                               t.getProductID().getProductID() != null &&
                               t.getProductID().getProductID().equals(product.getProductID()) &&
                               "Import".equalsIgnoreCase(t.getType()) &&
                               t.getUnitCost() != null && t.getUnitCost() > 0)
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .map(StockTransactions::getUnitCost)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            System.err.println("ProductMBean.getLatestImportPrice() - Error: " + e.getMessage());
            return null;
        }
    }
    
    // Lấy số lượng tồn từ Inventory
    public Integer getStockQuantity(Product product) {
        if (product == null || product.getProductID() == null) {
            return 0;
        }
        try {
            Inventory inventory = inventoryFacade.findByProductId(product.getProductID());
            if (inventory != null) {
                return inventory.getStock();
            }
            return 0;
        } catch (Exception e) {
            System.err.println("ProductMBean.getStockQuantity() - Error: " + e.getMessage());
            return 0;
        }
    }
    
    // Format currency - hỗ trợ cả int và Integer
    public String formatCurrency(Integer amount) {
        if (amount == null) return "-";
        return String.format("%,d", amount) + " VNĐ";
    }
    
    // Format currency cho int (primitive)
    public String formatCurrency(int amount) {
        return String.format("%,d", amount) + " VNĐ";
    }
    
    // Helper methods
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }
    
    private void addErr(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
}

