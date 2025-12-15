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
import mypack.entity.User;
import mypack.sessionbean.Order1FacadeLocal;
import mypack.sessionbean.OrderDetailsFacadeLocal;

@Named(value = "customerOrderMBean")
@SessionScoped
public class CustomerOrderMBean implements Serializable {

    @EJB
    private Order1FacadeLocal orderFacade;
    
    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;
    
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
        selectedOrder = order;
        showDetails = true;
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
}

