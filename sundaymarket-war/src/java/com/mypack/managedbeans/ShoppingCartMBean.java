//package com.mypack.managedbeans;
//
//import jakarta.inject.Named;
//import jakarta.enterprise.context.SessionScoped;
//import jakarta.ejb.EJB;
//import jakarta.faces.application.FacesMessage;
//import jakarta.faces.context.FacesContext;
//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import mypack.entity.ShoppingCart;
//import mypack.entity.Product;
//import mypack.entity.User;
//import mypack.entity.Order1;
//import mypack.entity.OrderDetails;
//import mypack.sessionbean.ShoppingCartFacadeLocal;
//import mypack.sessionbean.ProductFacadeLocal;
//import mypack.sessionbean.Order1FacadeLocal;
//import mypack.sessionbean.OrderDetailsFacadeLocal;
//import mypack.sessionbean.UserFacadeLocal;
//
//@Named(value = "shoppingCartMBean")
//@SessionScoped
//public class ShoppingCartMBean implements Serializable {
//
//    @EJB
//    private ShoppingCartFacadeLocal shoppingCartFacade;
//    
//    @EJB
//    private ProductFacadeLocal productFacade;
//    
//    @EJB
//    private Order1FacadeLocal orderFacade;
//    
//    @EJB
//    private OrderDetailsFacadeLocal orderDetailsFacade;
//    
//    @EJB
//    private UserFacadeLocal userFacade;
//    
//    private String shipAddress;
//    private List<ShoppingCart> cartItems;
//    private int totalAmount;
//
//    public ShoppingCartMBean() {
//    }
//    
//    // Get cart items for current user
//    public List<ShoppingCart> getCartItems() {
//        // Return cached items if available
//        if (cartItems != null) {
//            return cartItems;
//        }
//        
//        try {
//            FacesContext facesContext = FacesContext.getCurrentInstance();
//            if (facesContext != null) {
//                jakarta.el.ELContext elContext = facesContext.getELContext();
//                jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
//                jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
//                LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
//                
//                if (loginMBean != null && loginMBean.getCurrentUser() != null) {
//                    User currentUser = loginMBean.getCurrentUser();
//                    // Get all cart items for this user
//                    List<ShoppingCart> allCarts = shoppingCartFacade.findAll();
//                    cartItems = new ArrayList<>();
//                    for (ShoppingCart cart : allCarts) {
//                        if (cart.getUserID() != null && cart.getUserID().getUserID().equals(currentUser.getUserID())) {
//                            cartItems.add(cart);
//                        }
//                    }
//                    return cartItems;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return new ArrayList<>();
//    }
//    
//    // Add product to cart
//    public void addToCart(Product product) {
//        try {
//            System.out.println("=== ShoppingCartMBean.addToCart ===");
//            System.out.println("Product: " + (product != null ? product.getProductID() + " - " + product.getName() : "null"));
//            
//            if (product == null) {
//                addErr("⚠️ Product not found!");
//                return;
//            }
//            
//            // Refresh product from DB to ensure it's managed
//            Product managedProduct = productFacade.find(product.getProductID());
//            if (managedProduct == null) {
//                addErr("⚠️ Product not found in database!");
//                return;
//            }
//            
//            FacesContext facesContext = FacesContext.getCurrentInstance();
//            if (facesContext == null) {
//                addErr("⚠️ FacesContext is null!");
//                return;
//            }
//            
//            jakarta.el.ELContext elContext = facesContext.getELContext();
//            jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
//            jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
//            LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
//            
//            if (loginMBean == null || loginMBean.getCurrentUser() == null) {
//                addErr("⚠️ Please login to add products to cart!");
//                return;
//            }
//            
//            User currentUser = loginMBean.getCurrentUser();
//            System.out.println("Current User: " + currentUser.getUserID() + " - " + currentUser.getUserName());
//            
//            // Refresh user from DB to ensure it's managed
//            User managedUser = userFacade.find(currentUser.getUserID());
//            if (managedUser == null) {
//                addErr("⚠️ User not found in database!");
//                return;
//            }
//            
//            // Check if product already in cart
//            List<ShoppingCart> allCarts = shoppingCartFacade.findAll();
//            ShoppingCart existingCart = null;
//            for (ShoppingCart cart : allCarts) {
//                if (cart.getUserID() != null && cart.getUserID().getUserID().equals(managedUser.getUserID()) &&
//                    cart.getProductID() != null && cart.getProductID().getProductID().equals(managedProduct.getProductID())) {
//                    existingCart = cart;
//                    break;
//                }
//            }
//            
//            if (existingCart != null) {
//                // Update quantity
//                existingCart.setQuantity(existingCart.getQuantity() + 1);
//                shoppingCartFacade.edit(existingCart);
//                System.out.println("Updated existing cart item, new quantity: " + existingCart.getQuantity());
//                addInfo("✅ Product quantity updated in cart!");
//            } else {
//                // Create new cart item
//                ShoppingCart newCart = new ShoppingCart();
//                newCart.setProductID(managedProduct);
//                newCart.setUserID(managedUser);
//                newCart.setQuantity(1);
//                newCart.setCreateAt(new Date());
//                shoppingCartFacade.create(newCart);
//                System.out.println("Created new cart item");
//                addInfo("✅ Product added to cart!");
//            }
//            
//            // Clear cartItems cache to force refresh
//            cartItems = null;
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.err.println("Error in addToCart: " + e.getMessage());
//            addErr("❌ Error adding to cart: " + e.getMessage());
//        }
//    }
//    
//    // Remove from cart
//    public void removeFromCart(ShoppingCart cart) {
//        try {
//            shoppingCartFacade.remove(cart);
//            addInfo("✅ Product removed from cart!");
//        } catch (Exception e) {
//            e.printStackTrace();
//            addErr("❌ Error removing from cart: " + e.getMessage());
//        }
//    }
//    
//    // Update quantity
//    public void updateQuantity(ShoppingCart cart, int newQuantity) {
//        try {
//            if (newQuantity <= 0) {
//                removeFromCart(cart);
//                return;
//            }
//            cart.setQuantity(newQuantity);
//            shoppingCartFacade.edit(cart);
//            addInfo("✅ Quantity updated!");
//        } catch (Exception e) {
//            e.printStackTrace();
//            addErr("❌ Error updating quantity: " + e.getMessage());
//        }
//    }
//    
//    // Calculate total amount
//    public int getTotalAmount() {
//        totalAmount = 0;
//        List<ShoppingCart> items = getCartItems();
//        for (ShoppingCart cart : items) {
//            if (cart.getProductID() != null) {
//                totalAmount += cart.getProductID().getUnitPrice() * cart.getQuantity();
//            }
//        }
//        return totalAmount;
//    }
//    
//    // Get cart count
//    public int getCartCount() {
//        return getCartItems().size();
//    }
//    
//    // Checkout - Create order and order details, then clear cart
//    public String checkout() {
//        try {
//            FacesContext facesContext = FacesContext.getCurrentInstance();
//            if (facesContext == null) {
//                return null;
//            }
//            
//            jakarta.el.ELContext elContext = facesContext.getELContext();
//            jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
//            jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
//            LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
//            
//            if (loginMBean == null || loginMBean.getCurrentUser() == null) {
//                addErr("⚠️ Please login to checkout!");
//                return null;
//            }
//            
//            User currentUser = loginMBean.getCurrentUser();
//            List<ShoppingCart> items = getCartItems();
//            
//            if (items == null || items.isEmpty()) {
//                addErr("⚠️ Cart is empty!");
//                return null;
//            }
//            
//            if (shipAddress == null || shipAddress.trim().isEmpty()) {
//                addErr("⚠️ Please enter shipping address!");
//                return null;
//            }
//            
//            // Create Order
//            Order1 order = new Order1();
//            order.setUserID(currentUser);
//            order.setOrderDate(new Date());
//            order.setTotalAmount(getTotalAmount());
//            order.setStatus("pending");
//            orderFacade.create(order);
//            
//            // Create OrderDetails for each cart item
//            for (ShoppingCart cart : items) {
//                OrderDetails orderDetail = new OrderDetails();
//                orderDetail.setOrderID(order);
//                orderDetail.setProductID(cart.getProductID());
//                orderDetail.setQuantity(cart.getQuantity());
//                orderDetail.setUnitPrice(cart.getProductID().getUnitPrice());
//                orderDetail.setShipAddress(shipAddress.trim());
//                orderDetailsFacade.create(orderDetail);
//            }
//            
//            // Clear cart - remove all items
//            for (ShoppingCart cart : items) {
//                shoppingCartFacade.remove(cart);
//            }
//            
//            addInfo("✅ Order placed successfully! Order ID: " + order.getOrderID());
//            shipAddress = null;
//            
//            return "index?faces-redirect=true";
//        } catch (Exception e) {
//            e.printStackTrace();
//            addErr("❌ Checkout failed: " + e.getMessage());
//            return null;
//        }
//    }
//    
//    // Format amount
//    public String formatAmount(int amount) {
//        return String.format("%,d", amount) + " VND";
//    }
//    
//    // Get product image URL
//    public String getProductImageUrl(Product product) {
//        if (product == null || product.getImageURL() == null || product.getImageURL().isEmpty()) {
//            return null;
//        }
//        
//        // Extract filename if it contains path (old data)
//        String imageURL = product.getImageURL();
//        String fileName;
//        if (imageURL.contains("\\") || imageURL.contains("/")) {
//            java.io.File file = new java.io.File(imageURL);
//            fileName = file.getName();
//        } else {
//            fileName = imageURL;
//        }
//        
//        if (fileName == null || fileName.isEmpty()) {
//            return null;
//        }
//        
//        FacesContext facesContext = FacesContext.getCurrentInstance();
//        if (facesContext != null) {
//            String contextPath = facesContext.getExternalContext().getRequestContextPath();
//            return contextPath + "/resources/images/" + fileName + "?v=" + (System.currentTimeMillis() % 1000000);
//        }
//        return "/resources/images/" + fileName;
//    }
//    
//    private void addInfo(String msg) {
//        FacesContext.getCurrentInstance().addMessage(null,
//                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
//    }
//
//    private void addErr(String msg) {
//        FacesContext.getCurrentInstance().addMessage(null,
//                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
//    }
//    
//    // Getters and Setters
//    public String getShipAddress() {
//        return shipAddress;
//    }
//
//    public void setShipAddress(String shipAddress) {
//        this.shipAddress = shipAddress;
//    }
//}
//
