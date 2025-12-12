package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import mypack.entity.Order1;
import mypack.sessionbean.Order1FacadeLocal;

@Named(value = "orderMBean")
@SessionScoped
public class OrderMBean implements Serializable {

    @EJB
    private Order1FacadeLocal orderFacade;
    
    private Order1 selected = null;
    private String searchKeyword;
    private String statusFilter;
    private int currentPage = 1;
    private int pageSize = 10;
    
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
        selected = o;
    }
    
    // Đóng chi tiết
    public void closeDetails() {
        selected = null;
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
            case "đã hủy":
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
}

