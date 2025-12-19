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
import mypack.entity.Delivery;
import mypack.entity.Order1;
import mypack.entity.OrderDetails;
import mypack.entity.User;
import mypack.sessionbean.DeliveryFacadeLocal;
import mypack.sessionbean.Order1FacadeLocal;
import mypack.sessionbean.OrderDetailsFacadeLocal;

/**
 * Managed Bean for Shipper Delivery Management
 * Allows shippers to view and update delivery status
 */
@Named(value = "shipperDeliveryMBean")
@SessionScoped
public class ShipperDeliveryMBean implements Serializable {

    @EJB
    private DeliveryFacadeLocal deliveryFacade;
    
    @EJB
    private Order1FacadeLocal orderFacade;
    
    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;
    
    private Delivery selectedDelivery;
    private Order1 selectedOrder;
    private boolean showDetails = false;
    private String searchKeyword;
    private String statusFilter = "all";
    private int currentPage = 1;
    private int pageSize = 10;
    private boolean showAvailableOrders = false; // Toggle to show/hide available orders
    private int currentPageAvailable = 1; // Separate pagination for available orders

    public ShipperDeliveryMBean() {
    }
    
    // Get current logged-in user
    private User getCurrentUser() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return null;
            }
            
            jakarta.el.ELContext elContext = facesContext.getELContext();
            jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
            jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
            LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
            
            if (loginMBean == null) {
                return null;
            }
            
            return loginMBean.getCurrentUser();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Check if current user is a shipper
    public boolean isShipper() {
        User user = getCurrentUser();
        if (user == null || user.getRoleID() == null) {
            return false;
        }
        return "shipper".equalsIgnoreCase(user.getRoleID().getRoleName());
    }
    
    // Get available orders (orders without delivery assignment)
    public List<Order1> getAvailableOrders() {
        try {
            List<Order1> allOrders = orderFacade.findAll();
            List<Order1> availableOrders = new ArrayList<>();

            if (allOrders == null) return availableOrders;

            for (Order1 order : allOrders) {
                if (order == null || order.getOrderID() == null) continue;

                String status = order.getStatus() != null
                        ? order.getStatus().trim().toLowerCase()
                        : "";

                // ✅ CHỈ LẤY ĐƠN CHƯA GIAO CHO SHIPPER
                if (!status.equals("processing") && !status.equals("pending")) {
                    continue;
                }

                availableOrders.add(order);
            }

            // 🔍 SEARCH
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keyword = searchKeyword.toLowerCase();
                availableOrders.removeIf(order -> {
                    if (String.valueOf(order.getOrderID()).contains(keyword)) return false;
                    if (order.getUserID() != null) {
                        if (order.getUserID().getFullName() != null &&
                            order.getUserID().getFullName().toLowerCase().contains(keyword)) return false;
                        if (order.getUserID().getUserName() != null &&
                            order.getUserID().getUserName().toLowerCase().contains(keyword)) return false;
                    }
                    return true;
                });
            }

            return availableOrders;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Get paged available orders
    public List<Order1> getPagedAvailableOrders() {
        try {
            List<Order1> base = getAvailableOrders();
            
            if (base == null || base.isEmpty()) {
                return new ArrayList<>();
            }
            
            int start = (currentPageAvailable - 1) * pageSize;
            int end = Math.min(start + pageSize, base.size());
            
            if (start >= base.size()) {
                currentPageAvailable = 1;
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
    
    // Accept order - shipper takes the order
    public void acceptOrder(Order1 order) {
        try {
            System.out.println("=== acceptOrder() called ===");
            
            if (order == null || order.getOrderID() == null) {
                System.out.println("ERROR: Order is null or orderID is null");
                addError("❌ Invalid order!");
                return;
            }
            
            System.out.println("Order ID: " + order.getOrderID());
            
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                System.out.println("ERROR: Current user is null");
                addError("❌ Please login first!");
                return;
            }
            
            System.out.println("Current user: " + currentUser.getUserName() + " (ID: " + currentUser.getUserID() + ")");
            
            // Load order từ database để có dữ liệu mới nhất
            Order1 freshOrder = orderFacade.find(order.getOrderID());
            if (freshOrder == null) {
                System.out.println("ERROR: Order not found in database for ID: " + order.getOrderID());
                addError("❌ Order not found!");
                return;
            }
            
            System.out.println("Fresh order loaded: " + freshOrder.getOrderID() + ", Status: " + freshOrder.getStatus());
            
            // Check if order is already assigned
            List<Delivery> existingDeliveries = deliveryFacade.findByOrder(freshOrder);
            if (existingDeliveries != null && !existingDeliveries.isEmpty()) {
                System.out.println("WARNING: Order already has " + existingDeliveries.size() + " delivery records");
                addError("❌ Đơn hàng này đã được gán cho shipper khác!");
                return;
            }
            
            System.out.println("Creating new delivery record...");
            
            // Tạo Delivery record mới
            Delivery newDelivery = new Delivery();
            newDelivery.setOrderID(freshOrder);  // Sử dụng freshOrder từ DB
            newDelivery.setUserID(currentUser);
            newDelivery.setStatus("assigned");
            newDelivery.setUpdatedAt(new Date());
            deliveryFacade.create(newDelivery);
            
            System.out.println("Delivery created successfully! ID: " + newDelivery.getDeliveryID());
            
            // Update order status to processing if it's pending
            String currentStatus = freshOrder.getStatus() != null ? freshOrder.getStatus().trim().toLowerCase() : "";
            if ("pending".equals(currentStatus)) {
                freshOrder.setStatus("processing");
                orderFacade.edit(freshOrder);
                System.out.println("Order status updated to processing");
            }
            
            addInfo("✅ Đã nhận đơn hàng ##" + freshOrder.getOrderID() + " thành công!");
            
            // Switch to assigned deliveries view
            showAvailableOrders = false;
            currentPage = 1;
            currentPageAvailable = 1; // Reset pagination cho available orders
            
            System.out.println("Switched to assigned deliveries view");
            
            // Refresh if viewing details
            if (selectedOrder != null && selectedOrder.getOrderID().equals(freshOrder.getOrderID())) {
                selectedOrder = orderFacade.find(freshOrder.getOrderID());
                // Now it's assigned, so get the delivery
                List<Delivery> deliveries = deliveryFacade.findByOrder(freshOrder);
                if (deliveries != null && !deliveries.isEmpty()) {
                    selectedDelivery = deliveries.get(0);
                }
            }
            
            System.out.println("=== acceptOrder() completed successfully ===");
        } catch (Exception e) {
            System.err.println("EXCEPTION in acceptOrder(): " + e.getMessage());
            e.printStackTrace();
            addError("❌ Lỗi khi nhận đơn: " + e.getMessage());
        }
    }
    
    // Get deliveries assigned to current shipper
    public List<Delivery> getDeliveries() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return new ArrayList<>();
            }
            
            List<Delivery> allDeliveries = deliveryFacade.findByShipper(currentUser);
            List<Delivery> filteredDeliveries = new ArrayList<>();
            
            for (Delivery delivery : allDeliveries) {
                // Apply status filter
                if (!"all".equals(statusFilter) && delivery.getStatus() != null) {
                    if (!delivery.getStatus().toLowerCase().equals(statusFilter.toLowerCase())) {
                        continue;
                    }
                }
                
                // Apply search filter
                if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                    String keyword = searchKeyword.trim().toLowerCase();
                    boolean matches = false;
                    
                    // Search by delivery ID
                    if (delivery.getDeliveryID() != null && 
                        String.valueOf(delivery.getDeliveryID()).contains(keyword)) {
                        matches = true;
                    }
                    
                    // Search by order ID
                    if (delivery.getOrderID() != null && 
                        String.valueOf(delivery.getOrderID().getOrderID()).contains(keyword)) {
                        matches = true;
                    }
                    
                    // Search by customer name
                    if (delivery.getOrderID() != null && delivery.getOrderID().getUserID() != null) {
                        User customer = delivery.getOrderID().getUserID();
                        if (customer.getFullName() != null && 
                            customer.getFullName().toLowerCase().contains(keyword)) {
                            matches = true;
                        }
                        if (customer.getUserName() != null && 
                            customer.getUserName().toLowerCase().contains(keyword)) {
                            matches = true;
                        }
                    }
                    
                    if (!matches) {
                        continue;
                    }
                }
                
                filteredDeliveries.add(delivery);
            }
            
            return filteredDeliveries;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Get paged deliveries
    public List<Delivery> getPagedItems() {
        try {
            List<Delivery> base = getDeliveries();
            
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
    
    // View delivery details
    public void viewDetails(Delivery delivery) {
        selectedDelivery = delivery;
        if (delivery != null && delivery.getOrderID() != null) {
            selectedOrder = delivery.getOrderID();
        }
        showDetails = true;
    }
    
    // View order details (for available orders)
    public void viewOrderDetails(Order1 order) {
        selectedOrder = order;
        selectedDelivery = null; // No delivery yet
        showDetails = true;
    }
    
    // Close details
    public void closeDetails() {
        selectedDelivery = null;
        selectedOrder = null;
        showDetails = false;
    }
    
    // Update delivery status
    public void updateDeliveryStatus(Delivery delivery, String newStatus) {
        try {
            if (delivery == null || newStatus == null) {
                addError("Invalid delivery or status!");
                return;
            }
            
            delivery.setStatus(newStatus);
            delivery.setUpdatedAt(new Date());
            deliveryFacade.edit(delivery);
            
            // Update order status based on delivery status
            if (delivery.getOrderID() != null) {
                Order1 order = delivery.getOrderID();
                
                // If delivery is completed, update order to 'shipping'
                if ("delivered".equalsIgnoreCase(newStatus)) {
                    order.setStatus("completed");
                    orderFacade.edit(order);
                } else if ("shipping".equalsIgnoreCase(newStatus)) {
                    order.setStatus("shipping");
                    orderFacade.edit(order);
                }
            }
            
            addInfo("✅ Delivery status updated successfully!");
            
            // Refresh selected delivery if viewing details
            if (selectedDelivery != null && selectedDelivery.getDeliveryID().equals(delivery.getDeliveryID())) {
                selectedDelivery = deliveryFacade.find(delivery.getDeliveryID());
            }
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Error updating delivery status: " + e.getMessage());
        }
    }
    
    // Update status from dropdown
    public void updateStatus(Delivery delivery) {
        try {
            if (delivery == null) {
                return;
            }
            delivery.setUpdatedAt(new Date());
            deliveryFacade.edit(delivery);
            
            // Update order status
            if (delivery.getOrderID() != null) {
                Order1 order = orderFacade.find(delivery.getOrderID().getOrderID());
                if (order != null) {
                    String deliveryStatus = delivery.getStatus();
                    if ("delivered".equalsIgnoreCase(deliveryStatus)) {
                        order.setStatus("completed");
                    } else if ("shipping".equalsIgnoreCase(deliveryStatus)) {
                        order.setStatus("shipping");
                    } else if ("failed".equalsIgnoreCase(deliveryStatus)) {
                        order.setStatus("failed");
                    }
                    orderFacade.edit(order);
                }
            }
            
            addInfo("✅ Status updated!");
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Update failed: " + e.getMessage());
        }
    }
    
    // Get order details for selected order
    public List<OrderDetails> getOrderDetails() {
        if (selectedOrder == null) {
            return new ArrayList<>();
        }
        try {
            List<OrderDetails> allDetails = orderDetailsFacade.findAll();
            List<OrderDetails> result = new ArrayList<>();
            for (OrderDetails detail : allDetails) {
                if (detail.getOrderID() != null && 
                    detail.getOrderID().getOrderID().equals(selectedOrder.getOrderID())) {
                    result.add(detail);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Check if order is available (not assigned to any shipper)
    public boolean isOrderAvailable(Order1 order) {
        if (order == null || order.getOrderID() == null) {
            return false;
        }
        try {
            List<Delivery> deliveries = deliveryFacade.findByOrder(order);
            return deliveries == null || deliveries.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    // Search methods
    public void performSearch() {
        currentPage = 1;
    }
    
    public void clearSearch() {
        searchKeyword = null;
        statusFilter = "all";
        currentPage = 1;
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
    
    public int getTotalPages() {
        int total = getTotalItems();
        if (total == 0) {
            return 1;
        }
        return (int) Math.ceil((double) total / pageSize);
    }
    
    public int getTotalPagesForAvailableOrders() {
        int total = getTotalAvailableOrders();
        if (total == 0) {
            return 1;
        }
        return (int) Math.ceil((double) total / pageSize);
    }
    
    public int getTotalItems() {
        try {
            List<Delivery> items = getDeliveries();
            return items != null ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Format methods
    public String formatAmount(int amount) {
        return String.format("%,d", amount) + " VND";
    }
    
    public String formatDate(Date date) {
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
            case "assigned":
                return "#ffc107";
            case "shipping":
            case "picked_up":
                return "#17a2b8";
            case "delivered":
            case "completed":
                return "#28a745";
            case "failed":
            case "cancelled":
                return "#dc3545";
            default:
                return "#666";
        }
    }
    
    // Get status badge class
    public String getStatusBadgeClass(String status) {
        if (status == null) {
            return "bg-secondary";
        }
        switch (status.toLowerCase()) {
            case "pending":
            case "assigned":
                return "bg-warning";
            case "shipping":
            case "picked_up":
                return "bg-info";
            case "delivered":
            case "completed":
                return "bg-success";
            case "failed":
            case "cancelled":
                return "bg-danger";
            default:
                return "bg-secondary";
        }
    }
    
    // Lấy text hiển thị trạng thái giao hàng
    public String getStatusText(String status) {
        if (status == null) return "";
        switch (status.toLowerCase()) {
            case "assigned": return "Assigned";       // Chờ nhận
            case "pending": return "Pending";         // Chờ xử lý
            case "picked_up": return "Picked Up";     // Đã lấy hàng
            case "shipping": return "Shipping";       // Đang giao
            case "delivered": return "Delivered";     // Đã giao
            case "completed": return "Completed";     // Hoàn thành
            case "failed": return "Failed";           // Thất bại
            case "cancelled": return "Cancelled";     // Đã hủy
            default: return status;
        }
    }
    
    // Helper methods
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }
    
    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    // Getters and Setters
    public Delivery getSelectedDelivery() {
        return selectedDelivery;
    }

    public void setSelectedDelivery(Delivery selectedDelivery) {
        this.selectedDelivery = selectedDelivery;
    }

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
    
    public boolean isShowAvailableOrders() {
        return showAvailableOrders;
    }
    
    public void setShowAvailableOrders(boolean showAvailableOrders) {
        this.showAvailableOrders = showAvailableOrders;
    }
    
    public void toggleAvailableOrders() {
        showAvailableOrders = !showAvailableOrders;
        if (showAvailableOrders) {
            currentPageAvailable = 1; // Reset to first page for available orders
        } else {
            currentPage = 1; // Reset to first page for assigned deliveries
        }
    }
    
    // Pagination methods for available orders
    public void firstPageAvailable() {
        currentPageAvailable = 1;
    }
    
    public void previousPageAvailable() {
        if (currentPageAvailable > 1) {
            currentPageAvailable--;
        }
    }
    
    public void nextPageAvailable() {
        if (currentPageAvailable < getTotalPagesForAvailableOrders()) {
            currentPageAvailable++;
        }
    }
    
    public void lastPageAvailable() {
        currentPageAvailable = getTotalPagesForAvailableOrders();
    }
    
    public int getCurrentPageAvailable() {
        return currentPageAvailable;
    }
    
    public void setCurrentPageAvailable(int currentPageAvailable) {
        this.currentPageAvailable = currentPageAvailable;
    }
    
    public int getTotalAvailableOrders() {
        try {
            List<Order1> orders = getAvailableOrders();
            return orders != null ? orders.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}




