package com.mypack.managedbeans;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import mypack.entity.Order1;
import mypack.entity.OrderDetails;
import mypack.entity.Payment;
import mypack.entity.Product;
import mypack.entity.ShoppingCart;
import mypack.entity.User;
import mypack.sessionbean.Order1FacadeLocal;
import mypack.sessionbean.OrderDetailsFacadeLocal;
import mypack.sessionbean.PaymentFacadeLocal;
import mypack.sessionbean.ProductFacadeLocal;
import mypack.sessionbean.ShoppingCartFacadeLocal;
import mypack.sessionbean.UserFacadeLocal;
import java.util.Date;

@Named(value = "customerOrderMBean")
@SessionScoped
public class CustomerOrderMBean implements Serializable {

    @EJB
    private Order1FacadeLocal orderFacade;
    
    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;
    
    @EJB
    private ShoppingCartFacadeLocal shoppingCartFacade;
    
    @EJB
    private ProductFacadeLocal productFacade;
    
    @EJB
    private UserFacadeLocal userFacade;
    
    @EJB
    private PaymentFacadeLocal paymentFacade;
    
    private Order1 selectedOrder;
    private boolean showDetails = false;
    private String searchKeyword;
    private String statusFilter = "all";
    private int currentPage = 1;
    private int pageSize = 10;

    public CustomerOrderMBean() {
    }
    
    // Get orders for current user
    public List<Order1> getOrders() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return new ArrayList<>();
            }
            
            jakarta.el.ELContext elContext = facesContext.getELContext();
            jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
            jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
            LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
            
            if (loginMBean == null || loginMBean.getCurrentUser() == null) {
                return new ArrayList<>();
            }
            
            User currentUser = loginMBean.getCurrentUser();
            List<Order1> allOrders = orderFacade.findAll();
            List<Order1> userOrders = new ArrayList<>();
            
            for (Order1 order : allOrders) {
                if (order.getUserID() != null && order.getUserID().getUserID().equals(currentUser.getUserID())) {
                    // Apply search filter
                    if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                        String keyword = searchKeyword.trim().toLowerCase();
                        boolean matches = false;
                        // Search by Order ID
                        if (order.getOrderID() != null && String.valueOf(order.getOrderID()).contains(keyword)) {
                            matches = true;
                        }
                        // Search by Username
                        if (order.getUserID() != null && order.getUserID().getUserName() != null && 
                            order.getUserID().getUserName().toLowerCase().contains(keyword)) {
                            matches = true;
                        }
                        // Search by Full Name
                        if (order.getUserID() != null && order.getUserID().getFullName() != null && 
                            order.getUserID().getFullName().toLowerCase().contains(keyword)) {
                            matches = true;
                        }
                        if (!matches) {
                            continue;
                        }
                    }
                    
                    // Apply status filter
                    if (!"all".equals(statusFilter) && order.getStatus() != null) {
                        if (!order.getStatus().toLowerCase().equals(statusFilter.toLowerCase())) {
                            continue;
                        }
                    }
                    
                    userOrders.add(order);
                }
            }
            
            return userOrders;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Get paged orders
    public List<Order1> getPagedItems() {
        try {
            List<Order1> base = getOrders();
            
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
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // View order details
    public void viewDetails(Order1 order) {
        try {
            if (order == null || order.getOrderID() == null) {
                return;
            }
            
            // Load lại order từ database để có đầy đủ dữ liệu
            selectedOrder = orderFacade.find(order.getOrderID());
            showDetails = true;
            
            // Force load orderDetailsCollection
            if (selectedOrder != null && selectedOrder.getOrderDetailsCollection() != null) {
                selectedOrder.getOrderDetailsCollection().size();
            }
        } catch (Exception e) {
            e.printStackTrace();
            selectedOrder = null;
            showDetails = false;
        }
    }
    
    // Close details
    public void closeDetails() {
        selectedOrder = null;
        showDetails = false;
    }
    
    // Get order details for selected order
    public List<OrderDetails> getOrderDetails(Order1 order) {
        if (order == null) {
            return new ArrayList<>();
        }
        try {
            List<OrderDetails> allDetails = orderDetailsFacade.findAll();
            List<OrderDetails> result = new ArrayList<>();
            for (OrderDetails detail : allDetails) {
                if (detail.getOrderID() != null && detail.getOrderID().getOrderID().equals(order.getOrderID())) {
                    result.add(detail);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Search
    public void performSearch() {
        currentPage = 1;
    }
    
    public void clearSearch() {
        searchKeyword = null;
        statusFilter = "all";
        currentPage = 1;
    }
    
    // Pagination
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
    
    public int getTotalPages() {
        int total = getTotalItems();
        if (total == 0) {
            return 1;
        }
        return (int) Math.ceil((double) total / pageSize);
    }
    
    public int getTotalItems() {
        try {
            List<Order1> items = getOrders();
            return items != null ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Format amount
    public String formatAmount(int amount) {
        return String.format("%,d", amount) + " VND";
    }
    
    // Get shipping fee display (with default 0 if null)
    public int getShippingFeeDisplay(Order1 order) {
        if (order == null) return 0;
        return order.getShippingFee() != null ? order.getShippingFee() : 0;
    }
    
    // Calculate subtotal (totalAmount - shippingFee)
    public int getSubtotal(Order1 order) {
        if (order == null) return 0;
        int shippingFee = getShippingFeeDisplay(order);
        return order.getTotalAmount() - shippingFee;
    }
    
    // Format date
    public String formatDate(java.util.Date date) {
        if (date == null) {
            return "-";
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
    
    // Reorder - Add all products from order to cart
    public void reorderOrder() {
        try {
            if (selectedOrder == null || selectedOrder.getOrderID() == null) {
                addError("❌ Invalid order!");
                return;
            }
            
            // Get current user
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                addError("❌ FacesContext is null!");
                return;
            }
            
            jakarta.el.ELContext elContext = facesContext.getELContext();
            jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
            jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
            LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
            
            if (loginMBean == null || loginMBean.getCurrentUser() == null) {
                addError("❌ Please login first!");
                return;
            }
            
            User currentUser = loginMBean.getCurrentUser();
            User managedUser = userFacade.find(currentUser.getUserID());
            if (managedUser == null) {
                addError("❌ User not found!");
                return;
            }
            
            // Get order details
            List<OrderDetails> orderDetails = getOrderDetails(selectedOrder);
            if (orderDetails == null || orderDetails.isEmpty()) {
                addError("❌ No products in this order!");
                return;
            }
            
            int addedCount = 0;
            int updatedCount = 0;
            
            // Get all existing cart items for this user
            List<ShoppingCart> allCarts = shoppingCartFacade.findAll();
            
            // Add each product to cart
            for (OrderDetails detail : orderDetails) {
                if (detail.getProductID() == null) {
                    continue;
                }
                
                Product product = productFacade.find(detail.getProductID().getProductID());
                if (product == null) {
                    continue;
                }
                
                // Check if product already in cart
                ShoppingCart existingCart = null;
                for (ShoppingCart cart : allCarts) {
                    if (cart.getUserID() != null && cart.getUserID().getUserID().equals(managedUser.getUserID()) &&
                        cart.getProductID() != null && cart.getProductID().getProductID().equals(product.getProductID())) {
                        existingCart = cart;
                        break;
                    }
                }
                
                if (existingCart != null) {
                    // Update quantity (add order quantity to existing)
                    existingCart.setQuantity(existingCart.getQuantity() + detail.getQuantity());
                    shoppingCartFacade.edit(existingCart);
                    updatedCount++;
                } else {
                    // Create new cart item with order quantity
                    ShoppingCart newCart = new ShoppingCart();
                    newCart.setProductID(product);
                    newCart.setUserID(managedUser);
                    newCart.setQuantity(detail.getQuantity());
                    newCart.setCreateAt(new Date());
                    shoppingCartFacade.create(newCart);
                    addedCount++;
                }
            }
            
            if (addedCount > 0 || updatedCount > 0) {
                // Clear shopping cart cache to force refresh
                try {
                    jakarta.el.ValueExpression veCart = factory.createValueExpression(elContext, "#{shoppingCartMBean}", ShoppingCartMBean.class);
                    ShoppingCartMBean shoppingCartMBean = (ShoppingCartMBean) veCart.getValue(elContext);
                    if (shoppingCartMBean != null) {
                        // Clear cache by setting cartItems to null
                        shoppingCartMBean.clearCartCache();
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Could not clear shopping cart cache: " + e.getMessage());
                }
                
                addInfo("✅ Added " + (addedCount + updatedCount) + " product(s) to cart!");
            } else {
                addError("❌ Could not add products to cart!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Error reordering: " + e.getMessage());
        }
    }
    
    // Helper methods for messages
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_INFO, msg, null));
    }
    
    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, msg, null));
    }
    
    // Get status color
    public String getStatusColor(String status) {
        if (status == null) {
            return "#666";
        }
        switch (status.toLowerCase()) {
            case "pending":
                return "#ffc107";
            case "processing":
                return "#17a2b8";
            case "completed":
                return "#28a745";
            case "cancelled":
                return "#dc3545";
            default:
                return "#666";
        }
    }
    
    // Getters and Setters
    public Order1 getSelectedOrder() {
        return selectedOrder;
    }

    public void setSelectedOrder(Order1 selectedOrder) {
        this.selectedOrder = selectedOrder;
    }

    public boolean isShowDetails() {
        return showDetails;
    }

    public void setShowDetails(boolean showDetails) {
        this.showDetails = showDetails;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
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
    
    // Payment helper methods
    /**
     * Get payment method for an order
     */
    public String getPaymentMethod(Order1 order) {
        if (order == null) {
            return "N/A";
        }
        try {
            Payment payment = paymentFacade.findByOrder(order);
            if (payment != null && payment.getPaymentMethod() != null) {
                return payment.getPaymentMethod().toUpperCase();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "N/A";
    }
    
    /**
     * Check if order is COD
     */
    public boolean isCOD(Order1 order) {
        if (order == null) {
            return false;
        }
        try {
            Payment payment = paymentFacade.findByOrder(order);
            if (payment != null && payment.getPaymentMethod() != null) {
                return "COD".equalsIgnoreCase(payment.getPaymentMethod());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Get payment method display name
     */
    public String getPaymentMethodDisplay(Order1 order) {
        String method = getPaymentMethod(order);
        switch (method.toUpperCase()) {
            case "COD":
                return "💰 COD";
            case "ONLINE":
                return "💳 ONLINE";
            case "VNPAY":
            case "MOMO":
            case "BANK_TRANSFER":
                return "💳 ONLINE";
            default:
                return method != null && !"N/A".equals(method) ? method : "N/A";
        }
    }
}

