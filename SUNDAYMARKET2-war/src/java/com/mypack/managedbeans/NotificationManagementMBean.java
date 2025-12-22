package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import mypack.entity.Notifications;
import mypack.entity.NotificationTarget;
import mypack.entity.Product;
import mypack.entity.User;
import mypack.sessionbean.NotificationsFacadeLocal;
import mypack.sessionbean.NotificationTargetFacadeLocal;
import mypack.sessionbean.ProductFacadeLocal;
import mypack.sessionbean.UserFacadeLocal;

@Named(value = "notificationManagementMBean")
@SessionScoped
public class NotificationManagementMBean implements Serializable {

    @EJB
    private NotificationsFacadeLocal notificationsFacade;
    
    @EJB
    private NotificationTargetFacadeLocal notificationTargetFacade;
    
    @EJB
    private ProductFacadeLocal productFacade;
    
    @EJB
    private UserFacadeLocal userFacade;
    
    @Inject
    private LoginMBean loginMBean;
    
    private Notifications selected = new Notifications();
    private boolean editMode = false;
    private boolean showForm = false;
    private String searchKeyword;
    private int currentPage = 1;
    private int pageSize = 10;
    private Integer selectedProductId;
    private List<Product> products;
    
    // Recipients selection
    private String targetMode = "ROLE"; // ROLE or USER
    private boolean sendToCustomers = false;
    private boolean sendToShippers = false;
    private List<Integer> selectedUserIds = new ArrayList<>();
    private List<User> allUsers;
    
    public List<Notifications> getItems() {
        try {
            List<Notifications> all = notificationsFacade.findAll();
            
            if (all == null) {
                return new ArrayList<>();
            }
            
            // Áp dụng tìm kiếm nếu có keyword
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keyword = searchKeyword.trim().toLowerCase();
                List<Notifications> filtered = new ArrayList<>();
                for (Notifications n : all) {
                    if ((n.getTitle() != null && n.getTitle().toLowerCase().contains(keyword)) ||
                        (n.getContent() != null && n.getContent().toLowerCase().contains(keyword)) ||
                        (n.getType() != null && n.getType().toLowerCase().contains(keyword))) {
                        filtered.add(n);
                    }
                }
                return filtered;
            }
            
            return all;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public List<Notifications> getPagedItems() {
        try {
            List<Notifications> base = getItems();
            
            if (base == null || base.isEmpty()) {
                return new ArrayList<>();
            }
            
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, base.size());
            
            if (start >= base.size()) {
                currentPage = 1;
                start = 0;
                end = Math.min(pageSize, base.size());
            }
            
            if (start < 0 || start >= end || end > base.size()) {
                return new ArrayList<>();
            }
            
            return base.subList(start, end);
        } catch (Exception e) {
            System.err.println("NotificationManagementMBean.getPagedItems() - Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public void performSearch() {
        currentPage = 1;
    }
    
    public void clearSearch() {
        searchKeyword = null;
        currentPage = 1;
    }
    
    public int getTotalPages() {
        int total = getTotalItems();
        if (total == 0) {
            return 1;
        }
        return (int) Math.ceil((double) total / pageSize);
    }
    
    public int getTotalItems() {
        try {
            List<Notifications> items = getItems();
            return items != null ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    public void prepareCreate() {
        selected = new Notifications();
        selected.setType("INFO");
        selected.setCreatedAt(new Date());
        selectedProductId = null;
        targetMode = "ROLE";
        sendToCustomers = false;
        sendToShippers = false;
        selectedUserIds = new ArrayList<>();
        editMode = false;
        showForm = true;
        loadProducts();
        loadAllUsers();
    }
    
    public void prepareEdit(Notifications n) {
        if (n != null && n.getNotificationID() != null) {
            Notifications refreshed = notificationsFacade.find(n.getNotificationID());
            if (refreshed != null) {
                selected = refreshed;
            } else {
                selected = n;
            }
        } else {
            selected = n;
        }
        
        // IMPORTANT: Save original content BEFORE cleanup for product extraction
        String originalContent = null;
        if (selected != null && selected.getContent() != null) {
            originalContent = selected.getContent();
        }
        
        // Try to extract product ID from ORIGINAL content BEFORE cleanup
        // Method 1: Try to extract from [PRODUCT_ID:...] tag (most reliable)
        selectedProductId = null;
        if (originalContent != null && !originalContent.isEmpty()) {
            try {
                // Look for [PRODUCT_ID:123] pattern
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[PRODUCT_ID:(\\d+)\\]");
                java.util.regex.Matcher matcher = pattern.matcher(originalContent);
                if (matcher.find()) {
                    String productIdStr = matcher.group(1);
                    selectedProductId = Integer.parseInt(productIdStr);
                    System.out.println("NotificationManagementMBean.prepareEdit() - ✅ Extracted Product ID from tag: " + selectedProductId);
                    
                    // Verify product still exists
                    Product product = productFacade.find(selectedProductId);
                    if (product == null) {
                        System.out.println("NotificationManagementMBean.prepareEdit() - ⚠️ Product ID " + selectedProductId + " not found in database, trying fallback method");
                        selectedProductId = null;
                    } else {
                        System.out.println("NotificationManagementMBean.prepareEdit() - ✅ Verified product exists: " + product.getName());
                    }
                }
            } catch (Exception e) {
                System.err.println("NotificationManagementMBean.prepareEdit() - Error extracting Product ID: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Method 2: Fallback - Extract product name if [PRODUCT_ID:...] not found (for old notifications)
        if (selectedProductId == null && originalContent != null && !originalContent.isEmpty()) {
            System.out.println("NotificationManagementMBean.prepareEdit() - Trying fallback: extract product name from content");
            String productName = extractProductNameFromContent(originalContent);
            System.out.println("NotificationManagementMBean.prepareEdit() - Extracted product name: " + productName);
            if (productName != null && !productName.isEmpty()) {
                try {
                    // Find product by name (case-insensitive, trim whitespace)
                    List<Product> allProducts = productFacade.findAll();
                    if (allProducts != null) {
                        String normalizedProductName = productName.trim();
                        for (Product p : allProducts) {
                            if (p.getName() != null) {
                                String normalizedPName = p.getName().trim();
                                // Try exact match first
                                if (normalizedPName.equals(normalizedProductName)) {
                                    selectedProductId = p.getProductID();
                                    System.out.println("NotificationManagementMBean.prepareEdit() - ✅ Found product (exact match): " + productName + " (ID: " + selectedProductId + ")");
                                    break;
                                }
                                // Try case-insensitive match
                                if (normalizedPName.equalsIgnoreCase(normalizedProductName)) {
                                    selectedProductId = p.getProductID();
                                    System.out.println("NotificationManagementMBean.prepareEdit() - ✅ Found product (case-insensitive): " + productName + " (ID: " + selectedProductId + ")");
                                    break;
                                }
                            }
                        }
                        if (selectedProductId == null) {
                            System.out.println("NotificationManagementMBean.prepareEdit() - ⚠️ Product name '" + productName + "' not found in database");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("NotificationManagementMBean.prepareEdit() - Error finding product by name: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // NOW clean up content - remove appended product info for editing
        if (selected != null && originalContent != null) {
            String content = originalContent;
            // Remove [PRODUCT_IMAGE:...] tag
            content = content.replaceAll("\\[PRODUCT_IMAGE:[^\\]]+\\]", "");
            // Remove [PRODUCT_ID:...] tag (we already extracted it, no need in display)
            content = content.replaceAll("\\[PRODUCT_ID:[^\\]]+\\]", "");
            // Remove product info lines (📦 Product:, 🖼️/🏞️ Image:, 💰 Price:)
            // Handle both inline (no newline) and multiline formats
            content = content.replaceAll("\\s*📦\\s*Product:[^\\n]*", "");
            content = content.replaceAll("\\s*[🖼️🏞️]\\s*Image:[^\\n]*", "");
            content = content.replaceAll("\\s*💰\\s*Price:[^\\n]*", "");
            // Also handle newline-separated versions
            content = content.replaceAll("\\n\\s*📦\\s*Product:.*", "");
            content = content.replaceAll("\\n\\s*[🖼️🏞️]\\s*Image:.*", "");
            content = content.replaceAll("\\n\\s*💰\\s*Price:.*", "");
            // Clean up multiple newlines and spaces
            content = content.replaceAll("\\n{3,}", "\n\n");
            content = content.replaceAll("\\s{2,}", " ");
            content = content.trim();
            selected.setContent(content);
        }
        
        editMode = true;
        showForm = true;
        loadProducts();
        loadAllUsers();
        
        System.out.println("NotificationManagementMBean.prepareEdit() - Final selectedProductId: " + selectedProductId);
    }
    
    public void cancelForm() {
        showForm = false;
        selected = new Notifications();
        selectedProductId = null;
        targetMode = "ROLE";
        sendToCustomers = false;
        sendToShippers = false;
        selectedUserIds = new ArrayList<>();
    }
    
    public void loadAllUsers() {
        try {
            allUsers = userFacade.findAll();
            // Filter only active users
            if (allUsers != null) {
                allUsers = allUsers.stream()
                    .filter(u -> u.getIsActive())
                    .collect(java.util.stream.Collectors.toList());
            }
        } catch (Exception e) {
            e.printStackTrace();
            allUsers = new ArrayList<>();
        }
    }
    
    public List<User> getAllUsers() {
        if (allUsers == null) {
            loadAllUsers();
        }
        return allUsers;
    }
    
    public void toggleCustomers() {
        sendToCustomers = !sendToCustomers;
    }
    
    public void toggleShippers() {
        sendToShippers = !sendToShippers;
    }
    
    private List<User> getTargetUsers() {
        List<User> targetUsers = new ArrayList<>();
        try {
            if (allUsers == null) {
                loadAllUsers();
            }
            
            for (User user : allUsers) {
                if (user.getRoleID() != null) {
                    String roleName = user.getRoleID().getRoleName();
                    if (sendToCustomers && "customer".equalsIgnoreCase(roleName)) {
                        targetUsers.add(user);
                    } else if (sendToShippers && "shipper".equalsIgnoreCase(roleName)) {
                        targetUsers.add(user);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return targetUsers;
    }
    
    private List<User> getSelectedUsers() {
        List<User> users = new ArrayList<>();
        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            return users;
        }
        
        try {
            for (Integer userId : selectedUserIds) {
                User user = userFacade.find(userId);
                if (user != null && user.getIsActive()) {
                    users.add(user);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return users;
    }
    
    public void loadProducts() {
        try {
            products = productFacade.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            products = new ArrayList<>();
        }
    }
    
    public List<Product> getProducts() {
        if (products == null) {
            loadProducts();
        }
        return products;
    }
    
    public Product getSelectedProduct() {
        if (selectedProductId != null) {
            try {
                return productFacade.find(selectedProductId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    public String getProductImageUrl(Product product) {
        if (product == null) {
            return null;
        }
        
        // Refresh product from database
        if (product.getProductID() != null) {
            try {
                Product refreshed = productFacade.find(product.getProductID());
                if (refreshed != null) {
                    product = refreshed;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (product.getImageURL() == null || product.getImageURL().isEmpty()) {
            return null;
        }
        
        // Parse JSON array
        List<String> fileNames = parseJsonArray(product.getImageURL());
        if (fileNames == null || fileNames.isEmpty()) {
            return null;
        }
        
        String fileName = fileNames.get(0);
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String contextPath = facesContext.getExternalContext().getRequestContextPath();
        long cacheBuster = System.currentTimeMillis() % 1000000;
        return contextPath + "/images/product/" + fileName + "?v=" + cacheBuster;
    }
    
    private List<String> parseJsonArray(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // Remove brackets and quotes
            String cleaned = jsonString.trim();
            if (cleaned.startsWith("[")) {
                cleaned = cleaned.substring(1);
            }
            if (cleaned.endsWith("]")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
            
            List<String> fileNames = new ArrayList<>();
            String[] parts = cleaned.split(",");
            for (String part : parts) {
                String fileName = part.trim().replace("\"", "").replace("'", "");
                if (!fileName.isEmpty()) {
                    fileNames.add(fileName);
                }
            }
            return fileNames;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public void onProductChange() {
        // Refresh selected product when changed
        if (selectedProductId != null) {
            try {
                Product product = productFacade.find(selectedProductId);
                System.out.println("NotificationManagementMBean.onProductChange() - Product selected: " + (product != null ? product.getName() : "null"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void save() {
        try {
            User currentUser = loginMBean.getCurrentUser();
            if (currentUser == null) {
                addError("❌ User information not found!");
                return;
            }
            
            if (selected.getTitle() == null || selected.getTitle().trim().isEmpty()) {
                addError("⚠️ Please enter notification title!");
                return;
            }
            
            if (selected.getContent() == null || selected.getContent().trim().isEmpty()) {
                addError("⚠️ Please enter notification content!");
                return;
            }
            
            // Validate recipients (only for new notifications)
            if (!editMode) {
                if ("ROLE".equals(targetMode)) {
                    if (!sendToCustomers && !sendToShippers) {
                        addError("⚠️ Please select at least one recipient group (Customers or Shippers)!");
                        return;
                    }
                } else if ("USER".equals(targetMode)) {
                    if (selectedUserIds == null || selectedUserIds.isEmpty()) {
                        addError("⚠️ Please select at least one user from the list!");
                        return;
                    }
                } else {
                    addError("⚠️ Please select a target mode (By Role or Specific Users)!");
                    return;
                }
            }
            
            // If product is selected, add product info with image to content
            Product selectedProduct = getSelectedProduct();
            String finalContent = selected.getContent();
            
            if (selectedProduct != null) {
                String productImageUrl = getProductImageUrl(selectedProduct);
                String productInfo = "\n\n📦 Product: " + selectedProduct.getName();
                
                // Add product image if available
                if (productImageUrl != null && !productImageUrl.isEmpty()) {
                    productInfo += "\n🖼️ Image: " + productImageUrl;
                    finalContent = selected.getContent() + productInfo + "\n[PRODUCT_IMAGE:" + productImageUrl + "]";
                } else {
                    finalContent = selected.getContent() + productInfo;
                }
                
                if (selectedProduct.getUnitPrice() > 0) {
                    finalContent += "\n💰 Price: " + String.format("%,d", selectedProduct.getUnitPrice()) + " VND";
                }
                
                // IMPORTANT: Save Product ID in content for easy retrieval when editing
                finalContent += "\n[PRODUCT_ID:" + selectedProduct.getProductID() + "]";
                
                // Update type if INFO was selected
                if ("INFO".equals(selected.getType())) {
                    selected.setType("PRODUCT_NEW");
                }
                
                selected.setContent(finalContent);
            }
            
            // Set createdBy if creating new
            if (!editMode && selected.getCreatedBy() == null) {
                selected.setCreatedBy(currentUser);
            }
            
            // Set createdAt if creating new
            if (!editMode && selected.getCreatedAt() == null) {
                selected.setCreatedAt(new Date());
            }
            
            // Save notification first
            if (editMode) {
                notificationsFacade.edit(selected);
            } else {
                notificationsFacade.create(selected);
            }
            
            // Get notification ID (refresh if needed)
            if (selected.getNotificationID() == null) {
                // Try to find it
                List<Notifications> recent = notificationsFacade.findAll();
                if (recent != null && !recent.isEmpty()) {
                    Notifications found = recent.stream()
                        .filter(n -> n.getTitle() != null && n.getTitle().equals(selected.getTitle()))
                        .filter(n -> n.getCreatedBy() != null && n.getCreatedBy().getUserID().equals(currentUser.getUserID()))
                        .filter(n -> n.getCreatedAt() != null)
                        .min((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()))
                        .orElse(null);
                    if (found != null) {
                        selected = found;
                    }
                }
            }
            
            // Create notification targets if creating new
            if (!editMode && selected.getNotificationID() != null) {
                List<User> targetUsers = new ArrayList<>();
                
                if ("ROLE".equals(targetMode)) {
                    targetUsers = getTargetUsers();
                } else if ("USER".equals(targetMode)) {
                    targetUsers = getSelectedUsers();
                }
                
                if (targetUsers.isEmpty()) {
                    addError("⚠️ No recipients selected! Please select at least one recipient.");
                    return;
                }
                
                // Create notification targets
                int successCount = 0;
                for (User targetUser : targetUsers) {
                    try {
                        NotificationTarget target = new NotificationTarget();
                        target.setNotificationID(selected);
                        target.setUserID(targetUser);
                        target.setIsRead(false); // Mark as unread by default
                        notificationTargetFacade.create(target);
                        successCount++;
                    } catch (Exception e) {
                        System.err.println("Error creating notification target for user " + targetUser.getUserID() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                if (successCount > 0) {
                    addInfo("✅ Notification created successfully and sent to " + successCount + " user(s)!");
                } else {
                    addError("❌ Failed to create notification targets!");
                    return;
                }
            } else if (editMode) {
                addInfo("✅ Notification updated successfully!");
            }
            
            showForm = false;
            selected = new Notifications();
            selectedProductId = null;
            targetMode = "ROLE";
            sendToCustomers = false;
            sendToShippers = false;
            selectedUserIds = new ArrayList<>();
            
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Error saving notification: " + e.getMessage());
        }
    }
    
    public void delete(Notifications n) {
        try {
            if (n != null && n.getNotificationID() != null) {
                // Delete all notification targets first
                List<mypack.entity.NotificationTarget> targets = notificationTargetFacade.findAll();
                for (mypack.entity.NotificationTarget target : targets) {
                    if (target.getNotificationID() != null && 
                        target.getNotificationID().getNotificationID() != null &&
                        target.getNotificationID().getNotificationID().equals(n.getNotificationID())) {
                        notificationTargetFacade.remove(target);
                    }
                }
                
                // Then delete notification
                notificationsFacade.remove(n);
                addInfo("✅ Notification deleted successfully!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Error deleting notification: " + e.getMessage());
        }
    }
    
    public int getRecipientCount(Notifications n) {
        if (n == null || n.getNotificationID() == null) {
            return 0;
        }
        try {
            List<mypack.entity.NotificationTarget> targets = notificationTargetFacade.findAll();
            int count = 0;
            for (mypack.entity.NotificationTarget target : targets) {
                if (target.getNotificationID() != null && 
                    target.getNotificationID().getNotificationID() != null &&
                    target.getNotificationID().getNotificationID().equals(n.getNotificationID())) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
    
    public List<String> getNotificationTypes() {
        List<String> types = new ArrayList<>();
        types.add("INFO");
        types.add("PRODUCT_NEW");
        types.add("PRODUCT_SALE");
        types.add("PROMOTION");
        types.add("SYSTEM");
        return types;
    }
    
    public String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
    
    public String getTypeIcon(String type) {
        if (type == null) return "📢";
        switch (type) {
            case "PRODUCT_NEW": return "🆕";
            case "PRODUCT_SALE": return "💰";
            case "PROMOTION": return "🎁";
            case "SYSTEM": return "⚙️";
            default: return "📢";
        }
    }
    
    /**
     * Get formatted content preview (removes product info for display)
     */
    public String getContentPreview(Notifications n) {
        if (n == null || n.getContent() == null) {
            return "";
        }
        String content = n.getContent();
        // Remove [PRODUCT_IMAGE:...] tag
        content = content.replaceAll("\\[PRODUCT_IMAGE:[^\\]]+\\]", "");
        // Remove [PRODUCT_ID:...] tag (internal use only, not for display)
        content = content.replaceAll("\\[PRODUCT_ID:[^\\]]+\\]", "");
        // Remove product info lines (📦 Product:, 🖼️/🏞️ Image:, 💰 Price:)
        // Handle both inline (no newline) and multiline formats
        content = content.replaceAll("\\s*📦\\s*Product:[^\\n]*", "");
        content = content.replaceAll("\\s*[🖼️🏞️]\\s*Image:[^\\n]*", "");
        content = content.replaceAll("\\s*💰\\s*Price:[^\\n]*", "");
        // Also handle newline-separated versions
        content = content.replaceAll("\\n\\s*📦\\s*Product:.*", "");
        content = content.replaceAll("\\n\\s*[🖼️🏞️]\\s*Image:.*", "");
        content = content.replaceAll("\\n\\s*💰\\s*Price:.*", "");
        // Clean up multiple newlines and spaces
        content = content.replaceAll("\\n{3,}", "\n\n");
        content = content.replaceAll("\\s{2,}", " ");
        return content.trim();
    }
    
    /**
     * Extract product name from notification content
     * Handles patterns like:
     * - "📦 Product: ProductName"
     * - "📦 Product: ProductName 🏞️ Image: ..."
     * - "📦 Product: ProductName\n🖼️ Image: ..."
     * - "poa 📦 Product: Thit A5 🏞️ Image: ..."
     */
    private String extractProductNameFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        try {
            System.out.println("NotificationManagementMBean.extractProductNameFromContent() - Content: " + content.substring(0, Math.min(200, content.length())));
            
            // Look for pattern: 📦 Product: ProductName
            // Try multiple patterns to handle different formats
            String[] patterns = {
                "📦\\s*Product:\\s*([^\\n🖼️🏞️💰\\[]+?)(?:\\s*[🖼️🏞️]|\\s*💰|\\n|\\[|$)",  // Standard pattern
                "📦\\s*Product:\\s*([A-Za-z0-9\\sÀ-ỹ]+?)(?:\\s*[🖼️🏞️]|\\s*💰|\\n|\\[|$)",  // Vietnamese characters
                "Product:\\s*([A-Za-z0-9\\sÀ-ỹ]+?)(?:\\s*[🖼️🏞️]|\\s*💰|\\n|\\[|$)"  // Without emoji
            };
            
            for (String patternStr : patterns) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternStr);
                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    String productName = matcher.group(1).trim();
                    // Remove any trailing emojis, special characters, or whitespace
                    productName = productName.replaceAll("[\\s\\u200B-\\u200D\\uFEFF🖼️🏞️💰]+$", "").trim();
                    if (!productName.isEmpty()) {
                        System.out.println("NotificationManagementMBean.extractProductNameFromContent() - ✅ Extracted: '" + productName + "'");
                        return productName;
                    }
                }
            }
            
            System.out.println("NotificationManagementMBean.extractProductNameFromContent() - ⚠️ No product name found");
        } catch (Exception e) {
            System.err.println("NotificationManagementMBean.extractProductNameFromContent() - Error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }
    
    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
    
    // Getters and Setters
    public Notifications getSelected() {
        return selected;
    }
    
    public void setSelected(Notifications selected) {
        this.selected = selected;
    }
    
    public boolean isEditMode() {
        return editMode;
    }
    
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }
    
    public boolean isShowForm() {
        return showForm;
    }
    
    public void setShowForm(boolean showForm) {
        this.showForm = showForm;
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
    
    public Integer getSelectedProductId() {
        return selectedProductId;
    }
    
    public void setSelectedProductId(Integer selectedProductId) {
        this.selectedProductId = selectedProductId;
    }
    
    public String getTargetMode() {
        return targetMode;
    }
    
    public void setTargetMode(String targetMode) {
        this.targetMode = targetMode;
    }
    
    public boolean isSendToCustomers() {
        return sendToCustomers;
    }
    
    public void setSendToCustomers(boolean sendToCustomers) {
        this.sendToCustomers = sendToCustomers;
    }
    
    public boolean isSendToShippers() {
        return sendToShippers;
    }
    
    public void setSendToShippers(boolean sendToShippers) {
        this.sendToShippers = sendToShippers;
    }
    
    public List<Integer> getSelectedUserIds() {
        return selectedUserIds;
    }
    
    public void setSelectedUserIds(List<Integer> selectedUserIds) {
        this.selectedUserIds = selectedUserIds;
    }
    
    // Pagination methods
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
}
