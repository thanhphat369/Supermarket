package com.mypack.managedbeans;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import mypack.entity.ShoppingCart;
import mypack.entity.Product;
import mypack.entity.User;
import mypack.entity.Order1;
import mypack.entity.OrderDetails;
import mypack.sessionbean.ShoppingCartFacadeLocal;
import mypack.sessionbean.ProductFacadeLocal;
import mypack.sessionbean.Order1FacadeLocal;
import mypack.sessionbean.OrderDetailsFacadeLocal;
import mypack.sessionbean.UserFacadeLocal;

@Named(value = "shoppingCartMBean")
@SessionScoped
public class ShoppingCartMBean implements Serializable {

    @EJB
    private ShoppingCartFacadeLocal shoppingCartFacade;
    
    @EJB
    private ProductFacadeLocal productFacade;
    
    @EJB
    private Order1FacadeLocal orderFacade;
    
    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;
    
    @EJB
    private UserFacadeLocal userFacade;
    
    private String shipAddress;
    private List<ShoppingCart> cartItems;
    private int totalAmount;

    public ShoppingCartMBean() {
    }
    
    // Get cart items for current user
    public List<ShoppingCart> getCartItems() {
        // Return cached items if available
        if (cartItems != null) {
            return cartItems;
        }
        
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                jakarta.el.ELContext elContext = facesContext.getELContext();
                jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
                jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
                LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
                
                if (loginMBean != null && loginMBean.getCurrentUser() != null) {
                    User currentUser = loginMBean.getCurrentUser();
                    // Get all cart items for this user
                    List<ShoppingCart> allCarts = shoppingCartFacade.findAll();
                    cartItems = new ArrayList<>();
                    for (ShoppingCart cart : allCarts) {
                        if (cart.getUserID() != null && cart.getUserID().getUserID().equals(currentUser.getUserID())) {
                            cartItems.add(cart);
                        }
                    }
                    return cartItems;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
    
    // Add product to cart
    public void addToCart(Product product) {
        try {
            System.out.println("=== ShoppingCartMBean.addToCart ===");
            System.out.println("Product: " + (product != null ? product.getProductID() + " - " + product.getName() : "null"));
            
            if (product == null) {
                addErr("⚠️ Product not found!");
                return;
            }
            
            // Refresh product from DB to ensure it's managed
            Product managedProduct = productFacade.find(product.getProductID());
            if (managedProduct == null) {
                addErr("⚠️ Product not found in database!");
                return;
            }
            
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                addErr("⚠️ FacesContext is null!");
                return;
            }
            
            jakarta.el.ELContext elContext = facesContext.getELContext();
            jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
            jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
            LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
            
            if (loginMBean == null || loginMBean.getCurrentUser() == null) {
                addErr("⚠️ Please login to add products to cart!");
                return;
            }
            
            User currentUser = loginMBean.getCurrentUser();
            System.out.println("Current User: " + currentUser.getUserID() + " - " + currentUser.getUserName());
            
            // Refresh user from DB to ensure it's managed
            User managedUser = userFacade.find(currentUser.getUserID());
            if (managedUser == null) {
                addErr("⚠️ User not found in database!");
                return;
            }
            
            // Check if product already in cart
            List<ShoppingCart> allCarts = shoppingCartFacade.findAll();
            ShoppingCart existingCart = null;
            for (ShoppingCart cart : allCarts) {
                if (cart.getUserID() != null && cart.getUserID().getUserID().equals(managedUser.getUserID()) &&
                    cart.getProductID() != null && cart.getProductID().getProductID().equals(managedProduct.getProductID())) {
                    existingCart = cart;
                    break;
                }
            }
            
            if (existingCart != null) {
                // Update quantity
                existingCart.setQuantity(existingCart.getQuantity() + 1);
                shoppingCartFacade.edit(existingCart);
                System.out.println("Updated existing cart item, new quantity: " + existingCart.getQuantity());
                addInfo("✅ Product quantity updated in cart!");
            } else {
                // Create new cart item
                ShoppingCart newCart = new ShoppingCart();
                newCart.setProductID(managedProduct);
                newCart.setUserID(managedUser);
                newCart.setQuantity(1);
                newCart.setCreateAt(new Date());
                shoppingCartFacade.create(newCart);
                System.out.println("Created new cart item");
                addInfo("✅ Product added to cart!");
            }
            
            // Clear cartItems cache to force refresh
            cartItems = null;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in addToCart: " + e.getMessage());
            addErr("❌ Error adding to cart: " + e.getMessage());
        }
    }
    
    // Clear cart cache - public method for other beans to call
    public void clearCartCache() {
        cartItems = null;
        System.out.println("ShoppingCartMBean.clearCartCache() - Cache cleared");
    }
    
    // Remove from cart
    public void removeFromCart(ShoppingCart cart) {
        try {
            if (cart == null || cart.getCartID() == null) {
                addErr("⚠️ Invalid cart item!");
                return;
            }
            
            // Refresh cart from database to ensure it's managed
            ShoppingCart managedCart = shoppingCartFacade.find(cart.getCartID());
            if (managedCart == null) {
                addErr("⚠️ Cart item not found in database!");
                // Clear cache anyway
                cartItems = null;
                return;
            }
            
            // Remove from database
            shoppingCartFacade.remove(managedCart);
            
            // Clear cache to force refresh
            cartItems = null;
            
            System.out.println("ShoppingCartMBean.removeFromCart() - Removed cart item ID: " + managedCart.getCartID());
            addInfo("✅ Product removed from cart!");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in removeFromCart: " + e.getMessage());
            addErr("❌ Error removing from cart: " + e.getMessage());
            // Clear cache even on error to force refresh
            cartItems = null;
        }
    }
    
    // Update quantity
    public void updateQuantity(ShoppingCart cart, int newQuantity) {
        try {
            if (newQuantity <= 0) {
                removeFromCart(cart);
                return;
            }
            
            if (cart == null || cart.getCartID() == null) {
                addErr("⚠️ Invalid cart item!");
                return;
            }
            
            // Refresh cart from database to ensure it's managed
            ShoppingCart managedCart = shoppingCartFacade.find(cart.getCartID());
            if (managedCart == null) {
                addErr("⚠️ Cart item not found in database!");
                cartItems = null;
                return;
            }
            
            managedCart.setQuantity(newQuantity);
            shoppingCartFacade.edit(managedCart);
            
            // Clear cache to force refresh
            cartItems = null;
            
            addInfo("✅ Quantity updated!");
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Error updating quantity: " + e.getMessage());
            cartItems = null;
        }
    }
    
    // Calculate total amount
    public int getTotalAmount() {
        totalAmount = 0;
        List<ShoppingCart> items = getCartItems();
        for (ShoppingCart cart : items) {
            if (cart.getProductID() != null) {
                totalAmount += cart.getProductID().getUnitPrice() * cart.getQuantity();
            }
        }
        return totalAmount;
    }
    
    // Get cart count
    public int getCartCount() {
        return getCartItems().size();
    }
    
    // Proceed to payment page
    public String proceedToPayment() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return null;
            }
            
            jakarta.el.ELContext elContext = facesContext.getELContext();
            jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
            jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
            LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
            
            if (loginMBean == null || loginMBean.getCurrentUser() == null) {
                addErr("⚠️ Please login to checkout!");
                return null;
            }
            
            List<ShoppingCart> items = getCartItems();
            
            if (items == null || items.isEmpty()) {
                addErr("⚠️ Cart is empty!");
                return null;
            }
            
            if (shipAddress == null || shipAddress.trim().isEmpty()) {
                addErr("⚠️ Please enter shipping address!");
                return null;
            }
            
            // Initialize payment
            jakarta.el.ValueExpression vePayment = factory.createValueExpression(elContext, "#{paymentMBean}", PaymentMBean.class);
            PaymentMBean paymentMBean = (PaymentMBean) vePayment.getValue(elContext);
            
            if (paymentMBean != null) {
                return paymentMBean.initPayment(getTotalAmount(), shipAddress.trim());
            }
            
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Error proceeding to payment: " + e.getMessage());
            return null;
        }
    }
    
    // Checkout - Create order and order details, then clear cart (Direct checkout without payment)
    public String checkout() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return null;
            }
            
            jakarta.el.ELContext elContext = facesContext.getELContext();
            jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
            jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
            LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
            
            if (loginMBean == null || loginMBean.getCurrentUser() == null) {
                addErr("⚠️ Please login to checkout!");
                return null;
            }
            
            User currentUser = loginMBean.getCurrentUser();
            List<ShoppingCart> items = getCartItems();
            
            if (items == null || items.isEmpty()) {
                addErr("⚠️ Cart is empty!");
                return null;
            }
            
            if (shipAddress == null || shipAddress.trim().isEmpty()) {
                addErr("⚠️ Please enter shipping address!");
                return null;
            }
            
            // Create Order
            Order1 order = new Order1();
            order.setUserID(currentUser);
            order.setOrderDate(new Date());
            order.setTotalAmount(getTotalAmount()); // Initial total = subtotal (no shipping fee yet)
            order.setShippingFee(null); // Shipping fee will be set by admin later
            order.setStatus("pending");
            orderFacade.create(order);
            
            // Create OrderDetails for each cart item
            for (ShoppingCart cart : items) {
                OrderDetails orderDetail = new OrderDetails();
                orderDetail.setOrderID(order);
                orderDetail.setProductID(cart.getProductID());
                orderDetail.setQuantity(cart.getQuantity());
                orderDetail.setUnitPrice(cart.getProductID().getUnitPrice());
                orderDetail.setShipAddress(shipAddress.trim());
                orderDetailsFacade.create(orderDetail);
            }
            
            // Clear cart - remove all items
            for (ShoppingCart cart : items) {
                shoppingCartFacade.remove(cart);
            }
            
            addInfo("✅ Order placed successfully! Order ID: " + order.getOrderID());
            shipAddress = null;
            
            return "index?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Checkout failed: " + e.getMessage());
            return null;
        }
    }
    
    // Format amount
    public String formatAmount(int amount) {
        return String.format("%,d", amount) + " VND";
    }
    
    // Get product image URL (first image from JSON array)
    public String getProductImageUrl(Product product) {
        if (product == null || product.getImageURL() == null || product.getImageURL().isEmpty()) {
            return null;
        }
        
        // Parse JSON array to get first image
        List<String> fileNames = parseJsonArray(product.getImageURL());
        if (fileNames == null || fileNames.isEmpty()) {
            return null;
        }
        
        String fileName = fileNames.get(0);
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        // Handle old format with full path
        if (fileName.contains("\\") || fileName.contains("/")) {
            java.io.File file = new java.io.File(fileName);
            fileName = file.getName();
        }
        
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String contextPath = "";
        if (facesContext != null) {
            contextPath = facesContext.getExternalContext().getRequestContextPath();
        }
        
        long cacheBuster = System.currentTimeMillis() % 1000000;
        return contextPath + "/images/product/" + fileName + "?v=" + cacheBuster;
    }
    
    // Parse JSON array string to list of file names (same logic as ProductMBean)
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
                                    java.io.File file = new java.io.File(fileName);
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
                            java.io.File file = new java.io.File(part);
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
                    java.io.File file = new java.io.File(trimmed);
                    fileName = file.getName();
                }
                fileNames.add(fileName);
            }
        } catch (Exception e) {
            System.err.println("ShoppingCartMBean.parseJsonArray() - Error parsing: " + e.getMessage());
            e.printStackTrace();
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
    
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErr(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
    
    // Getters and Setters
    public String getShipAddress() {
        return shipAddress;
    }

    public void setShipAddress(String shipAddress) {
        this.shipAddress = shipAddress;
    }
}

