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

@Named(value = "notificationMBean")
@SessionScoped
public class NotificationMBean implements Serializable {

    @EJB
    private NotificationsFacadeLocal notificationsFacade;
    
    @EJB
    private NotificationTargetFacadeLocal notificationTargetFacade;
    
    @EJB
    private UserFacadeLocal userFacade;
    
    @EJB
    private ProductFacadeLocal productFacade;
    
    @Inject
    private LoginMBean loginMBean;
    
    // Form fields
    private String title;
    private String content;
    private String type = "INFO"; // INFO, PRODUCT_NEW, PRODUCT_SALE, etc.
    private Integer selectedProductId;
    private List<String> targetRoles = new ArrayList<>(); // customer, shipper
    private boolean sendToCustomers = false;
    private boolean sendToShippers = false;
    private List<Integer> selectedUserIds = new ArrayList<>(); // Specific users to send to
    private String targetMode = "ROLE"; // ROLE or USER
    
    // Product list for selection
    private List<Product> products;
    private List<User> allUsers;
    
    public NotificationMBean() {
    }
    
    @jakarta.annotation.PostConstruct
    public void init() {
        loadProducts();
        loadAllUsers();
        // Set default target mode
        if (targetMode == null || targetMode.trim().isEmpty()) {
            targetMode = "ROLE";
        }
    }
    
    public void loadProducts() {
        try {
            products = productFacade.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            products = new ArrayList<>();
        }
    }
    
    public void loadAllUsers() {
        try {
            allUsers = userFacade.findAll();
            // Filter only active users
            if (allUsers != null) {
                allUsers = allUsers.stream()
                    .filter(u -> u != null && u.getIsActive())
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
    
    public List<Product> getProducts() {
        if (products == null) {
            loadProducts();
        }
        return products;
    }
    
    public Product getSelectedProduct() {
        if (selectedProductId != null) {
            try {
                Product product = productFacade.find(selectedProductId);
                // Refresh product to ensure we have latest data including imageURL
                if (product != null) {
                    return product;
                }
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
        
        // Refresh product from database to ensure we have latest imageURL
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
        
        // Parse JSON array (similar to ProductMBean)
        List<String> fileNames = parseJsonArray(product.getImageURL());
        if (fileNames.isEmpty()) {
            return null;
        }
        
        // Get first image
        String fileName = fileNames.get(0);
        
        // Handle old format with full path
        if (fileName.contains("\\") || fileName.contains("/")) {
            java.io.File file = new java.io.File(fileName);
            fileName = file.getName();
        }
        
        FacesContext ctx = FacesContext.getCurrentInstance();
        String contextPath = "";
        if (ctx != null) {
            contextPath = ctx.getExternalContext().getRequestContextPath();
        }
        
        long cacheBuster = System.currentTimeMillis() % 1000000;
        String imageUrl = contextPath + "/images/product/" + fileName + "?v=" + cacheBuster;
        
        System.out.println("NotificationMBean.getProductImageUrl() - Product: " + product.getName() + ", ImageURL: " + imageUrl);
        return imageUrl;
    }
    
    private List<String> parseJsonArray(String jsonString) {
        List<String> fileNames = new ArrayList<>();
        if (jsonString == null || jsonString.trim().isEmpty() || jsonString.trim().equals("[]")) {
            return fileNames;
        }
        
        try {
            String trimmed = jsonString.trim();
            
            // Format 1: JSON array ["file1.jpg","file2.jpg"]
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                String content = trimmed.substring(1, trimmed.length() - 1).trim();
                if (!content.isEmpty()) {
                    String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    for (String part : parts) {
                        part = part.trim();
                        if (part.startsWith("\"") && part.endsWith("\"")) {
                            String fileName = part.substring(1, part.length() - 1).replace("\\\"", "\"");
                            if (!fileName.isEmpty()) {
                                if (fileName.contains("\\") || fileName.contains("/")) {
                                    java.io.File file = new java.io.File(fileName);
                                    fileName = file.getName();
                                }
                                fileNames.add(fileName);
                            }
                        }
                    }
                }
            } else {
                // Format 2: Single filename or comma-separated
                String[] parts = trimmed.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (!part.isEmpty()) {
                        if (part.contains("\\") || part.contains("/")) {
                            java.io.File file = new java.io.File(part);
                            part = file.getName();
                        }
                        fileNames.add(part);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback: treat as single filename
            String fileName = jsonString;
            if (jsonString.contains("\\") || jsonString.contains("/")) {
                java.io.File file = new java.io.File(jsonString);
                fileName = file.getName();
            }
            fileNames.add(fileName);
        }
        
        return fileNames;
    }
    
    public void sendNotification() {
        System.out.println("NotificationMBean.sendNotification() - Starting...");
        System.out.println("NotificationMBean.sendNotification() - Title: " + title);
        System.out.println("NotificationMBean.sendNotification() - Content: " + (content != null ? content.substring(0, Math.min(50, content.length())) : "null"));
        System.out.println("NotificationMBean.sendNotification() - TargetMode: " + targetMode);
        System.out.println("NotificationMBean.sendNotification() - SendToCustomers: " + sendToCustomers);
        System.out.println("NotificationMBean.sendNotification() - SendToShippers: " + sendToShippers);
        System.out.println("NotificationMBean.sendNotification() - SelectedUserIds: " + (selectedUserIds != null ? selectedUserIds.size() : "null"));
        
        // Validate
        if (title == null || title.trim().isEmpty()) {
            addError("⚠️ Please enter notification title!");
            return;
        }
        
        if (content == null || content.trim().isEmpty()) {
            addError("⚠️ Please enter notification content!");
            return;
        }
        
        // Validate type (Type is now nullable in database, but we still validate if provided)
        List<String> validTypes = getNotificationTypes();
        if (type != null && !type.trim().isEmpty()) {
            // If type is provided, validate it
            type = type.trim();
            if (!validTypes.contains(type)) {
                System.err.println("NotificationMBean.sendNotification() - Invalid type from form: '" + type + "'. Valid types: " + validTypes);
                addError("⚠️ Invalid notification type: '" + type + "'. Please select a valid type from the dropdown.");
                return;
            }
            System.out.println("NotificationMBean.sendNotification() - Validated type: " + type);
        } else {
            // Type is null or empty - set default to INFO for better UX
            System.out.println("NotificationMBean.sendNotification() - Type is null/empty, defaulting to INFO");
            type = "INFO";
        }
        
        // Ensure targetMode has a default value
        if (targetMode == null || targetMode.trim().isEmpty()) {
            targetMode = "ROLE";
        }
        
        // Validate target selection
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
        
        // Get current admin user
        User adminUser = loginMBean.getCurrentUser();
        if (adminUser == null) {
            addError("❌ User information not found!");
            return;
        }
        
        // Get selected product if any
        Product selectedProduct = getSelectedProduct();
        String finalContent = content;
        String finalType = type;
        
        // Ensure type is valid before processing (validTypes already declared above)
        // Type can be null in database, but we validate if provided
        if (finalType != null && !finalType.trim().isEmpty()) {
            finalType = finalType.trim();
            if (!validTypes.contains(finalType)) {
                System.out.println("NotificationMBean.sendNotification() - Invalid type: " + finalType + ", defaulting to INFO");
                finalType = "INFO";
            }
        } else {
            finalType = "INFO"; // Default for better UX
        }
        
        // If product is selected, add product info with image to content (HTML format)
        if (selectedProduct != null) {
            String productImageUrl = getProductImageUrl(selectedProduct);
            String productInfo = "\n\n📦 Product: " + selectedProduct.getName();
            
            // Add product image as HTML if available
            if (productImageUrl != null && !productImageUrl.isEmpty()) {
                productInfo += "\n🖼️ Image: " + productImageUrl;
                // Add HTML img tag for display in notification
                finalContent = content + productInfo + "\n[PRODUCT_IMAGE:" + productImageUrl + "]";
            } else {
                finalContent = content + productInfo;
            }
            
            if (selectedProduct.getUnitPrice() > 0) {
                finalContent += "\n💰 Price: " + String.format("%,d", selectedProduct.getUnitPrice()) + " VND";
            }
            
            // Update type if INFO was selected - change to PRODUCT_NEW when product is included
            if ("INFO".equals(finalType)) {
                finalType = "PRODUCT_NEW";
            }
        } else {
            finalContent = content;
        }
        
        // Final validation: ensure finalType is still valid (if provided)
        if (finalType != null && !finalType.trim().isEmpty()) {
            if (!validTypes.contains(finalType)) {
                System.err.println("NotificationMBean.sendNotification() - ERROR: finalType is invalid: " + finalType);
                addError("❌ Invalid notification type! Please try again.");
                return;
            }
        }
        
        System.out.println("NotificationMBean.sendNotification() - Final Type: " + (finalType != null ? finalType : "NULL"));
        
        // Get target users based on selected mode FIRST (before creating notification)
        List<User> targetUsers = new ArrayList<>();
        
        if ("ROLE".equals(targetMode)) {
            targetUsers = getTargetUsers();
        } else if ("USER".equals(targetMode)) {
            targetUsers = getSelectedUsers();
        }
        
        if (targetUsers.isEmpty()) {
            addError("⚠️ No users found to send notification!");
            return;
        }
        
        // Create notification first (without targets)
        Notifications notification = new Notifications();
        notification.setTitle(title.trim());
        notification.setContent(finalContent);
        
        // Final validation: ensure finalType is valid and trim it
        // Type can be NULL in database, but we set default to INFO for better UX
        if (finalType != null && !finalType.trim().isEmpty()) {
            finalType = finalType.trim();
            if (!validTypes.contains(finalType)) {
                System.err.println("NotificationMBean.sendNotification() - CRITICAL: finalType validation failed: '" + finalType + "'. Valid types: " + validTypes);
                System.err.println("NotificationMBean.sendNotification() - Original type was: '" + type + "'");
                // Force to INFO as fallback
                finalType = "INFO";
                System.out.println("NotificationMBean.sendNotification() - Fallback to INFO");
            }
        } else {
            finalType = "INFO"; // Default for better UX
        }
        
        notification.setType(finalType);
        System.out.println("NotificationMBean.sendNotification() - Setting notification type to: '" + finalType + "'");
        notification.setCreatedAt(new Date());
        notification.setCreatedBy(adminUser);
        notification.setNotificationTargetCollection(new java.util.ArrayList<>()); // Initialize empty collection
        
        // Save notification first to get the ID
        try {
            notificationsFacade.create(notification);
            
            // Refresh to ensure we have the ID
            if (notification.getNotificationID() == null) {
                // If ID is still null, try to find it
                java.util.List<Notifications> recent = notificationsFacade.findAll();
                if (recent != null && !recent.isEmpty()) {
                    Notifications found = recent.stream()
                        .filter(n -> n.getTitle() != null && n.getTitle().equals(title.trim()))
                        .filter(n -> n.getCreatedBy() != null && n.getCreatedBy().getUserID().equals(adminUser.getUserID()))
                        .filter(n -> n.getCreatedAt() != null)
                        .min((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()))
                        .orElse(null);
                    if (found != null) {
                        notification = found;
                    }
                }
            }
            
            if (notification.getNotificationID() == null) {
                addError("❌ Failed to create notification! Please try again.");
                return;
            }
            
            // Now create targets separately
            int successCount = 0;
            for (User targetUser : targetUsers) {
                try {
                    NotificationTarget target = new NotificationTarget();
                    target.setNotificationID(notification);
                    target.setUserID(targetUser);
                    notificationTargetFacade.create(target);
                    successCount++;
                } catch (Exception e) {
                    System.err.println("Error creating notification target for user " + targetUser.getUserID() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            if (successCount == 0) {
                // Rollback: delete notification if no targets were created
                try {
                    notificationsFacade.remove(notification);
                } catch (Exception e) {
                    System.err.println("Failed to rollback notification: " + e.getMessage());
                }
                addError("❌ Failed to create notification targets! Notification was not sent.");
                return;
            }
            
            // Reset form
            resetForm();
            
            addInfo("✅ Notification sent successfully to " + successCount + " user(s)!");
            
        } catch (jakarta.persistence.PersistenceException e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            Throwable cause = e.getCause();
            String detailMsg = "";
            if (cause != null) {
                detailMsg = cause.getMessage();
                if (detailMsg == null && cause.getCause() != null) {
                    detailMsg = cause.getCause().getMessage();
                }
            }
            if (errorMsg != null && (errorMsg.contains("Transaction") || errorMsg.contains("aborted"))) {
                addError("❌ Transaction aborted: " + (detailMsg != null && !detailMsg.isEmpty() ? detailMsg : "Please check database constraints and try again."));
            } else {
                addError("❌ Database error: " + (detailMsg != null && !detailMsg.isEmpty() ? detailMsg : errorMsg != null ? errorMsg : e.toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            Throwable cause = e.getCause();
            addError("❌ Error sending notification: " + (errorMsg != null ? errorMsg : e.toString()));
            if (cause != null && cause.getMessage() != null) {
                addError("Cause: " + cause.getMessage());
            }
        }
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
    
    private List<User> getTargetUsers() {
        List<User> targetUsers = new ArrayList<>();
        try {
            List<User> allUsers = userFacade.findAll();
            
            for (User user : allUsers) {
                if (user == null || !user.getIsActive()) {
                    continue;
                }
                
                if (user.getRoleID() == null || user.getRoleID().getRoleName() == null) {
                    continue;
                }
                
                String roleName = user.getRoleID().getRoleName().toLowerCase();
                
                if (sendToCustomers && "customer".equals(roleName)) {
                    targetUsers.add(user);
                } else if (sendToShippers && "shipper".equals(roleName)) {
                    targetUsers.add(user);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return targetUsers;
    }
    
    public void resetForm() {
        title = null;
        content = null;
        type = "INFO";
        selectedProductId = null;
        sendToCustomers = false;
        sendToShippers = false;
        selectedUserIds = new ArrayList<>();
        targetMode = "ROLE";
    }
    
    public void onProductChange() {
        // Refresh selected product when changed
        if (selectedProductId != null) {
            try {
                Product product = productFacade.find(selectedProductId);
                System.out.println("NotificationMBean.onProductChange() - Product selected: " + (product != null ? product.getName() : "null"));
                if (product != null && product.getImageURL() != null) {
                    System.out.println("NotificationMBean.onProductChange() - ImageURL: " + product.getImageURL());
                    String imageUrl = getProductImageUrl(product);
                    System.out.println("NotificationMBean.onProductChange() - Generated URL: " + imageUrl);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void toggleCustomers() {
        sendToCustomers = !sendToCustomers;
    }
    
    public void toggleShippers() {
        sendToShippers = !sendToShippers;
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
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Integer getSelectedProductId() {
        return selectedProductId;
    }
    
    public void setSelectedProductId(Integer selectedProductId) {
        this.selectedProductId = selectedProductId;
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
    
    public String getTargetMode() {
        return targetMode;
    }
    
    public void setTargetMode(String targetMode) {
        this.targetMode = targetMode;
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
    
    public List<String> getTargetModes() {
        List<String> modes = new ArrayList<>();
        modes.add("ROLE");
        modes.add("USER");
        return modes;
    }
}
