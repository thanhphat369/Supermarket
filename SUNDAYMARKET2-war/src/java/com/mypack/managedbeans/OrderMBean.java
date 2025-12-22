package com.mypack.managedbeans;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ValueChangeEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import mypack.entity.Delivery;
import mypack.entity.Order1;
import mypack.entity.Payment;
import mypack.entity.User;
import mypack.sessionbean.DeliveryFacadeLocal;
import mypack.sessionbean.Order1FacadeLocal;
import mypack.sessionbean.PaymentFacadeLocal;
import mypack.sessionbean.UserFacadeLocal;

@Named(value = "orderMBean")
@ViewScoped
public class OrderMBean implements Serializable {

    @EJB
    private Order1FacadeLocal orderFacade;
    
    @EJB
    private DeliveryFacadeLocal deliveryFacade;
    
    @EJB
    private UserFacadeLocal userFacade;
    
    @EJB
    private PaymentFacadeLocal paymentFacade;
    
    private Order1 selected = null;
    private String searchKeyword;
    private String statusFilter;
    private int currentPage = 1;
    private int pageSize = 10;
    private Integer selectedShipperId; // For assigning shipper (in details section)
    private Order1 currentOrderForShipperAssignment; // Current order being assigned shipper
    private java.util.Set<Integer> selectedOrders = new java.util.HashSet<>(); // For bulk delete
    private java.util.Map<Integer, Boolean> selectionMap = new java.util.HashMap<>(); // For checkbox binding
    
    @PostConstruct
    public void init() {
        selected = null;
        // Auto-load order detail if orderId parameter is provided
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                String orderIdParam = facesContext.getExternalContext().getRequestParameterMap().get("orderId");
                if (orderIdParam != null && !orderIdParam.trim().isEmpty()) {
                    try {
                        Integer orderId = Integer.parseInt(orderIdParam.trim());
                        Order1 order = orderFacade.find(orderId);
                        if (order != null) {
                            viewDetails(order);
                        }
                    } catch (NumberFormatException e) {
                        // Invalid order ID, ignore
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors during initialization
            e.printStackTrace();
        }
    }
    
    // Lấy danh sách order
    public List<Order1> getItems() {
        try {
            List<Order1> all = orderFacade.findAll();
            
            if (all == null) {
                return new java.util.ArrayList<>();
            }
            
            // Áp dụng filter và search
            return all.stream()
                    .filter(order -> {
                        // Filter by status
                        if (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equals("all")) {
                            if (order.getStatus() == null || !order.getStatus().equalsIgnoreCase(statusFilter)) {
                                return false;
                            }
                        }
                        
                        // Search by keyword
                        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                            String keyword = searchKeyword.trim().toLowerCase();
                            return (order.getOrderID() != null && String.valueOf(order.getOrderID()).contains(keyword)) ||
                                   (order.getStatus() != null && order.getStatus().toLowerCase().contains(keyword)) ||
                                   (order.getUserID() != null && order.getUserID().getUserName() != null && order.getUserID().getUserName().toLowerCase().contains(keyword)) ||
                                   (order.getUserID() != null && order.getUserID().getFullName() != null && order.getUserID().getFullName().toLowerCase().contains(keyword));
                        }
                        
                        return true;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            System.err.println("OrderMBean.getItems() - Error: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách order phân trang
    public List<Order1> getPagedItems() {
        try {
            List<Order1> base = getItems();
            
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
            System.err.println("OrderMBean.getPagedItems() - Error: " + e.getMessage());
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
        statusFilter = null;
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
            List<Order1> items = getItems();
            return items != null ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Xem chi tiết
    public void viewDetails(Order1 o) {
        try {
            System.out.println("=== viewDetails() called ===");
            System.out.println("Order parameter: " + (o != null ? "Order ID: " + o.getOrderID() : "null"));
            
            if (o == null || o.getOrderID() == null) {
                System.out.println("ERROR: Order is null or orderID is null");
                addErr("❌ Invalid order!");
                return;
            }
            
            // Load lại order từ database để có đầy đủ dữ liệu (orderDetailsCollection)
            selected = orderFacade.find(o.getOrderID());
            System.out.println("Selected order loaded: " + (selected != null ? "Order ID: " + selected.getOrderID() : "null"));
            
            if (selected == null) {
                System.out.println("ERROR: Order not found in database");
                addErr("❌ Order not found!");
                selected = null;
                return;
            }
            
            // 🔒 FIX F5 BLANK: ép load collection → không bị detach sau F5
            if (selected.getOrderDetailsCollection() != null) {
                int size = selected.getOrderDetailsCollection().size();
                System.out.println("OrderDetailsCollection size: " + size);
            } else {
                System.out.println("WARNING: OrderDetailsCollection is null");
            }
            
            System.out.println("=== viewDetails() completed successfully ===");
        } catch (Exception e) {
            System.err.println("EXCEPTION in viewDetails(): " + e.getMessage());
            e.printStackTrace();
            addErr("❌ Error loading order details: " + e.getMessage());
            selected = null;
        }
    }
    
    // Đóng chi tiết
    public void closeDetails() {
        selected = null;
    }
    
    // Kiểm tra selected có hợp lệ không
    public boolean isSelectedValid() {
        try {
            if (selected == null || selected.getOrderID() == null) {
                return false;
            }
            // Kiểm tra xem có thể truy cập orderID không (để tránh lỗi khi lazy loading)
            Integer orderId = selected.getOrderID();
            return orderId != null;
        } catch (Exception e) {
            System.err.println("Error checking selected order: " + e.getMessage());
            selected = null; // Reset nếu có lỗi
            return false;
        }
    }
    
    // Update status
    public void updateStatus(Order1 o) {
        try {
            if (o == null || o.getStatus() == null) {
                return;
            }
            orderFacade.edit(o);
            addInfo("✅ Order status updated!");
            
            if (selected != null && selected.getOrderID() != null && selected.getOrderID().equals(o.getOrderID())) {
                selected = orderFacade.find(o.getOrderID());
            }
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Status update failed: " + e.getMessage());
        }
    }
    
    // Xóa một đơn hàng
    public void delete(Order1 o) {
        try {
            if (o == null || o.getOrderID() == null) {
                addErr("❌ Invalid order!");
                return;
            }
            
            Integer orderId = o.getOrderID();
            
            // Load lại order từ database để có đầy đủ dữ liệu
            Order1 orderToDelete = orderFacade.find(orderId);
            if (orderToDelete == null) {
                addErr("❌ Order #" + orderId + " not found!");
                return;
            }
            
            // THỨ TỰ XÓA (quan trọng để tránh lỗi foreign key constraint):
            // 1. Xóa Payment trước (vì không có cascade, có foreign key đến Order)
            Payment payment = paymentFacade.findByOrder(orderToDelete);
            if (payment != null) {
                try {
                    paymentFacade.remove(payment);
                    System.out.println("Payment deleted for order #" + orderId);
                } catch (Exception e) {
                    System.err.println("Error deleting payment: " + e.getMessage());
                    e.printStackTrace();
                    // Tiếp tục xóa order dù payment xóa lỗi (có thể payment đã bị xóa trước đó)
                }
            }
            
            // 2. Xóa Order (các entity có cascade=CascadeType.ALL sẽ tự động xóa):
            //    - OrderDetails (cascade = ALL)
            //    - Delivery (cascade = ALL)  
            //    - OrderPromotion (cascade = ALL)
            orderFacade.remove(orderToDelete);
            System.out.println("Order #" + orderId + " deleted successfully");
            
            addInfo("✅ Order #" + orderId + " deleted!");
            
            // Xóa khỏi danh sách selected nếu có
            selectedOrders.remove(orderId);
            selectionMap.remove(orderId);
            
            // Nếu đang xem chi tiết đơn này thì đóng lại
            if (selected != null && selected.getOrderID() != null && selected.getOrderID().equals(orderId)) {
                selected = null;
            }
        } catch (jakarta.persistence.PersistenceException e) {
            e.printStackTrace();
            // Kiểm tra xem có phải lỗi foreign key constraint không
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.toLowerCase().contains("foreign key")) {
                addErr("❌ Cannot delete order! There are related records that must be deleted first.");
            } else {
                addErr("❌ Delete failed: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Delete failed: " + e.getMessage());
        }
    }
    
    // Xóa nhiều đơn hàng cùng lúc
    public void deleteSelected() {
        try {
            if (selectedOrders == null || selectedOrders.isEmpty()) {
                addErr("⚠️ Please select at least one order to delete!");
                return;
            }
            
            int successCount = 0;
            int failCount = 0;
            java.util.List<Integer> failedOrderIds = new java.util.ArrayList<>();
            
            // Tạo danh sách copy để tránh ConcurrentModificationException
            java.util.List<Integer> orderIdsToDelete = new java.util.ArrayList<>(selectedOrders);
            
            for (Integer orderId : orderIdsToDelete) {
                try {
                    // Load lại order từ database
                    Order1 order = orderFacade.find(orderId);
                    if (order == null) {
                        failCount++;
                        failedOrderIds.add(orderId);
                        continue;
                    }
                    
                    // THỨ TỰ XÓA (quan trọng để tránh lỗi foreign key constraint):
                    // 1. Xóa Payment trước (vì không có cascade, có foreign key đến Order)
                    Payment payment = paymentFacade.findByOrder(order);
                    if (payment != null) {
                        try {
                            paymentFacade.remove(payment);
                            System.out.println("Payment deleted for order #" + orderId);
                        } catch (Exception e) {
                            System.err.println("Error deleting payment for order #" + orderId + ": " + e.getMessage());
                            // Tiếp tục xóa order dù payment xóa lỗi
                        }
                    }
                    
                    // 2. Xóa Order (các entity có cascade=CascadeType.ALL sẽ tự động xóa):
                    //    - OrderDetails (cascade = ALL)
                    //    - Delivery (cascade = ALL)
                    //    - OrderPromotion (cascade = ALL)
                    orderFacade.remove(order);
                    System.out.println("Order #" + orderId + " deleted successfully");
                    successCount++;
                    
                } catch (jakarta.persistence.PersistenceException e) {
                    System.err.println("Persistence error deleting order #" + orderId + ": " + e.getMessage());
                    e.printStackTrace();
                    failCount++;
                    failedOrderIds.add(orderId);
                } catch (Exception e) {
                    System.err.println("Error deleting order #" + orderId + ": " + e.getMessage());
                    e.printStackTrace();
                    failCount++;
                    failedOrderIds.add(orderId);
                }
            }
            
            // Xóa tất cả khỏi danh sách selected (kể cả những cái xóa thành công và thất bại)
            selectedOrders.clear();
            selectionMap.clear();
            
            // Đóng chi tiết nếu đơn đang xem bị xóa
            if (selected != null && selected.getOrderID() != null) {
                boolean wasDeleted = !orderIdsToDelete.contains(selected.getOrderID()) || 
                                     (successCount > 0 && !failedOrderIds.contains(selected.getOrderID()));
                if (wasDeleted) {
                    selected = null;
                }
            }
            
            // Thông báo kết quả
            if (successCount > 0) {
                addInfo("✅ Successfully deleted " + successCount + " order(s)!");
            }
            if (failCount > 0) {
                String failedMsg = "❌ Failed to delete " + failCount + " order(s)";
                if (!failedOrderIds.isEmpty() && failedOrderIds.size() <= 5) {
                    failedMsg += ": " + failedOrderIds.toString();
                }
                addErr(failedMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Bulk delete failed: " + e.getMessage());
        }
    }
    
    // Toggle selection của một đơn hàng - được gọi từ valueChangeListener
    public void toggleSelection(ValueChangeEvent event) {
        try {
            Boolean newValue = (Boolean) event.getNewValue();
            Integer orderId = (Integer) event.getComponent().getAttributes().get("orderId");
            
            if (orderId != null) {
                if (Boolean.TRUE.equals(newValue)) {
                    selectedOrders.add(orderId);
                    selectionMap.put(orderId, true);
                } else {
                    selectedOrders.remove(orderId);
                    selectionMap.remove(orderId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Kiểm tra đơn hàng có được chọn không
    public boolean isSelected(Order1 o) {
        if (o == null || o.getOrderID() == null) {
            return false;
        }
        Boolean selected = selectionMap.get(o.getOrderID());
        return Boolean.TRUE.equals(selected);
    }
    
    // Get selection map for direct binding
    public java.util.Map<Integer, Boolean> getSelectionMap() {
        if (selectionMap == null) {
            selectionMap = new java.util.HashMap<>();
        }
        return selectionMap;
    }
    
    // Chọn tất cả đơn hàng trên trang hiện tại
    public void selectAll() {
        try {
            List<Order1> currentPageOrders = getPagedItems();
            if (currentPageOrders != null) {
                for (Order1 order : currentPageOrders) {
                    if (order != null && order.getOrderID() != null) {
                        selectedOrders.add(order.getOrderID());
                        selectionMap.put(order.getOrderID(), true);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Bỏ chọn tất cả
    public void deselectAll() {
        selectedOrders.clear();
        selectionMap.clear();
    }
    
    // Lấy số đơn hàng đã chọn
    public int getSelectedCount() {
        return selectedOrders != null ? selectedOrders.size() : 0;
    }
    
    // Kiểm tra có đơn hàng nào được chọn không
    public boolean hasSelected() {
        return selectedOrders != null && !selectedOrders.isEmpty();
    }
    
    // Update shipping fee
    public void updateShippingFee() {
        try {
            if (selected == null || selected.getOrderID() == null) {
                addErr("❌ No order selected!");
                return;
            }
            
            // Validate shipping fee (must be >= 0)
            Integer newShippingFee = selected.getShippingFee();
            if (newShippingFee != null && newShippingFee < 0) {
                addErr("❌ Shipping fee cannot be negative!");
                return;
            }
            
            // Get old shipping fee before update
            Order1 oldOrder = orderFacade.find(selected.getOrderID());
            int oldShippingFee = oldOrder != null && oldOrder.getShippingFee() != null ? oldOrder.getShippingFee() : 0;
            int newShippingFeeValue = newShippingFee != null ? newShippingFee : 0;
            
            // Calculate new totalAmount = currentTotalAmount - oldShippingFee + newShippingFee
            // This ensures correct calculation even if order was created before shippingFee field existed
            int currentTotal = selected.getTotalAmount();
            int newTotalAmount = currentTotal - oldShippingFee + newShippingFeeValue;
            
            // Ensure totalAmount is not negative
            if (newTotalAmount < 0) {
                addErr("❌ Total amount cannot be negative!");
                return;
            }
            
            selected.setTotalAmount(newTotalAmount);
            orderFacade.edit(selected);
            addInfo("✅ Shipping fee updated! Total amount: " + formatAmount(selected.getTotalAmount()));
            
            // Refresh selected order
            selected = orderFacade.find(selected.getOrderID());
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Failed to update shipping fee: " + e.getMessage());
        }
    }
    
    // Get shipping fee display (with default 0 if null)
    public int getShippingFeeDisplay(Order1 order) {
        if (order == null) return 0;
        return order.getShippingFee() != null ? order.getShippingFee() : 0;
    }
    
    // Calculate subtotal (totalAmount - shippingFee)
    // For old orders without shippingFee, assume totalAmount is already the subtotal
    public int getSubtotal(Order1 order) {
        if (order == null) return 0;
        int shippingFee = getShippingFeeDisplay(order);
        // If shippingFee is 0 (null), totalAmount is already the subtotal
        // If shippingFee > 0, subtract it from totalAmount to get subtotal
        return order.getTotalAmount() - shippingFee;
    }
    
    // Get list of shippers (users with role "shipper")
    public List<User> getShippers() {
        try {
            List<User> allUsers = userFacade.findAll();
            if (allUsers == null) {
                return new java.util.ArrayList<>();
            }
            
            return allUsers.stream()
                    .filter(user -> user.getRoleID() != null && 
                                   user.getRoleID().getRoleName() != null &&
                                   "shipper".equalsIgnoreCase(user.getRoleID().getRoleName()) &&
                                   user.getIsActive())
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Get shipper assigned to an order (from Delivery)
    public User getShipperForOrder(Order1 order) {
        if (order == null || order.getOrderID() == null) {
            return null;
        }
        try {
            List<Delivery> deliveries = deliveryFacade.findByOrder(order);
            if (deliveries != null && !deliveries.isEmpty()) {
                // Get the first delivery (most recent)
                Delivery delivery = deliveries.get(0);
                if (delivery != null && delivery.getUserID() != null) {
                    return delivery.getUserID();
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Get delivery for an order
    public Delivery getDeliveryForOrder(Order1 order) {
        if (order == null || order.getOrderID() == null) {
            return null;
        }
        try {
            List<Delivery> deliveries = deliveryFacade.findByOrder(order);
            if (deliveries != null && !deliveries.isEmpty()) {
                return deliveries.get(0);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // ❌ KHÓA CHỨC NĂNG: Admin KHÔNG được tạo Delivery
    // Chỉ Shipper mới được tạo Delivery khi nhận đơn
    public void assignShipper(Order1 order) {
        addErr("❌ Admin không được gán shipper! Shipper phải tự nhận đơn từ trang quản lý đơn hàng của họ.");
    }
    
    // ❌ KHÓA CHỨC NĂNG: Admin KHÔNG được tạo Delivery
    // Chỉ Shipper mới được tạo Delivery khi nhận đơn
    public void quickAssignShipper() {
        addErr("❌ Admin không được gán shipper! Shipper phải tự nhận đơn từ trang quản lý đơn hàng của họ.");
        currentOrderForShipperAssignment = null;
    }
    
    // Set current order for shipper assignment (called before dropdown change)
    public void prepareShipperAssignment(Order1 order) {
        currentOrderForShipperAssignment = order;
        // Set current shipper if exists
        User currentShipper = getShipperForOrder(order);
        selectedShipperId = (currentShipper != null) ? currentShipper.getUserID() : null;
    }
    
    // Format amount
    public String formatAmount(int amount) {
        return String.format("%,d", amount) + " VND";
    }
    
    // Format date
    public String formatDate(java.util.Date date) {
        if (date == null) {
            return "-";
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
    
    // Get status color
    public String getStatusColor(String status) {
        if (status == null) {
            return "#666";
        }
        switch (status.toLowerCase()) {
            case "pending":
            case "chờ xử lý":
                return "#ffc107";
            case "processing":
            case "đang xử lý":
                return "#17a2b8";
            case "completed":
            case "hoàn thành":
                return "#28a745";
            case "cancelled":
            case "bỏ qua":
                return "#dc3545";
            default:
                return "#666";
        }
    }
    
    // Getters and Setters
    public Order1 getSelected() {
        return selected;
    }
    
    public void setSelected(Order1 selected) {
        this.selected = selected;
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
    
    public Integer getSelectedShipperId() {
        return selectedShipperId;
    }
    
    public void setSelectedShipperId(Integer selectedShipperId) {
        this.selectedShipperId = selectedShipperId;
    }
    
    public java.util.Set<Integer> getSelectedOrders() {
        return selectedOrders;
    }
    
    public void setSelectedOrders(java.util.Set<Integer> selectedOrders) {
        this.selectedOrders = selectedOrders;
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
    
    /**
     * Check if payment is submitted (for COD orders)
     * Returns true if transactionID starts with "SUBMIT_"
     */
    public boolean isPaymentSubmitted(Order1 order) {
        if (order == null) {
            return false;
        }
        try {
            Payment payment = paymentFacade.findByOrder(order);
            if (payment != null && payment.getTransactionID() != null) {
                return payment.getTransactionID().startsWith("SUBMIT_");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

