//package com.mypack.managedbeans;
//
//import jakarta.ejb.EJB;
//import jakarta.enterprise.context.SessionScoped;
//import jakarta.faces.application.FacesMessage;
//import jakarta.faces.context.FacesContext;
//import jakarta.inject.Named;
//import java.io.Serializable;
//import java.util.List;
//import mypack.entity.PurchaseOrder;
//import mypack.sessionbean.PurchaseOrderFacadeLocal;
//
//@Named(value = "purchaseOrderMBean")
//@SessionScoped
//public class PurchaseOrderMBean implements Serializable {
//
//    @EJB
//    private PurchaseOrderFacadeLocal purchaseOrderFacade;
//    
//    private PurchaseOrder selected = null;
//    private String searchKeyword;
//    private String statusFilter;
//    private int currentPage = 1;
//    private int pageSize = 10;
//    
//    // Lấy danh sách purchase order
//    public List<PurchaseOrder> getItems() {
//        try {
//            List<PurchaseOrder> all = purchaseOrderFacade.findAll();
//            
//            if (all == null) {
//                return new java.util.ArrayList<>();
//            }
//            
//            // Áp dụng filter và search
//            return all.stream()
//                    .filter(po -> {
//                        // Filter by status
//                        if (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equals("all")) {
//                            if (po.getStatus() == null || !po.getStatus().equalsIgnoreCase(statusFilter)) {
//                                return false;
//                            }
//                        }
//                        
//                        // Search by keyword
//                        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
//                            String keyword = searchKeyword.trim().toLowerCase();
//                            return (po.getPoId() != null && String.valueOf(po.getPoId()).contains(keyword)) ||
//                                   (po.getStatus() != null && po.getStatus().toLowerCase().contains(keyword)) ||
//                                   (po.getSupplierID() != null && po.getSupplierID().getSupplierName() != null && po.getSupplierID().getSupplierName().toLowerCase().contains(keyword));
//                        }
//                        
//                        return true;
//                    })
//                    .collect(java.util.stream.Collectors.toList());
//        } catch (Exception e) {
//            System.err.println("PurchaseOrderMBean.getItems() - Error: " + e.getMessage());
//            e.printStackTrace();
//            return new java.util.ArrayList<>();
//        }
//    }
//    
//    // Lấy danh sách purchase order phân trang
//    public List<PurchaseOrder> getPagedItems() {
//        try {
//            List<PurchaseOrder> base = getItems();
//            
//            if (base == null || base.isEmpty()) {
//                return new java.util.ArrayList<>();
//            }
//            
//            int start = (currentPage - 1) * pageSize;
//            int end = Math.min(start + pageSize, base.size());
//            
//            if (start >= base.size()) {
//                currentPage = 1;
//                start = 0;
//                end = Math.min(pageSize, base.size());
//            }
//            
//            if (start < 0 || start >= end || end > base.size()) {
//                return new java.util.ArrayList<>();
//            }
//            
//            return base.subList(start, end);
//        } catch (Exception e) {
//            System.err.println("PurchaseOrderMBean.getPagedItems() - Error: " + e.getMessage());
//            e.printStackTrace();
//            return new java.util.ArrayList<>();
//        }
//    }
//    
//    // Tìm kiếm
//    public void performSearch() {
//        currentPage = 1;
//    }
//    
//    public void clearSearch() {
//        searchKeyword = null;
//        statusFilter = null;
//        currentPage = 1;
//    }
//    
//    // Tổng số trang
//    public int getTotalPages() {
//        int total = getTotalItems();
//        if (total == 0) {
//            return 1;
//        }
//        return (int) Math.ceil((double) total / pageSize);
//    }
//    
//    // Tổng số items
//    public int getTotalItems() {
//        try {
//            List<PurchaseOrder> items = getItems();
//            return items != null ? items.size() : 0;
//        } catch (Exception e) {
//            return 0;
//        }
//    }
//    
//    // Xem chi tiết
//    public void viewDetails(PurchaseOrder po) {
//        selected = po;
//    }
//    
//    // Đóng chi tiết
//    public void closeDetails() {
//        selected = null;
//    }
//    
//    // Update status
//    public void updateStatus(PurchaseOrder po) {
//        try {
//            if (po == null || po.getStatus() == null) {
//                return;
//            }
//            purchaseOrderFacade.edit(po);
//            addInfo("✅ Purchase order status updated!");
//            
//            if (selected != null && selected.getPoId() != null && selected.getPoId().equals(po.getPoId())) {
//                selected = purchaseOrderFacade.find(po.getPoId());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            addErr("❌ Status update failed: " + e.getMessage());
//        }
//    }
//    
//    // Format amount
//    public String formatAmount(int amount) {
//        return String.format("%,d", amount) + " VND";
//    }
//    
//    // Format date
//    public String formatDate(java.util.Date date) {
//        if (date == null) {
//            return "-";
//        }
//        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
//        return sdf.format(date);
//    }
//    
//    // Get status color
//    public String getStatusColor(String status) {
//        if (status == null) {
//            return "#666";
//        }
//        switch (status.toLowerCase()) {
//            case "pending":
//            case "chờ xử lý":
//                return "#ffc107";
//            case "processing":
//            case "đang xử lý":
//                return "#17a2b8";
//            case "completed":
//            case "hoàn thành":
//                return "#28a745";
//            case "cancelled":
//            case "đã hủy":
//                return "#dc3545";
//            default:
//                return "#666";
//        }
//    }
//    
//    // Getters and Setters
//    public PurchaseOrder getSelected() {
//        return selected;
//    }
//    
//    public void setSelected(PurchaseOrder selected) {
//        this.selected = selected;
//    }
//    
//    public String getSearchKeyword() {
//        return searchKeyword;
//    }
//    
//    public void setSearchKeyword(String searchKeyword) {
//        this.searchKeyword = searchKeyword;
//    }
//    
//    public String getStatusFilter() {
//        return statusFilter;
//    }
//    
//    public void setStatusFilter(String statusFilter) {
//        this.statusFilter = statusFilter;
//    }
//    
//    public int getCurrentPage() {
//        return currentPage;
//    }
//    
//    public void setCurrentPage(int currentPage) {
//        this.currentPage = currentPage;
//    }
//    
//    public int getPageSize() {
//        return pageSize;
//    }
//    
//    public void setPageSize(int pageSize) {
//        this.pageSize = pageSize;
//    }
//    
//    // Navigation
//    public void firstPage() {
//        currentPage = 1;
//    }
//    
//    public void previousPage() {
//        if (currentPage > 1) {
//            currentPage--;
//        }
//    }
//    
//    public void nextPage() {
//        if (currentPage < getTotalPages()) {
//            currentPage++;
//        }
//    }
//    
//    public void lastPage() {
//        currentPage = getTotalPages();
//    }
//    
//    // Helper methods
//    private void addInfo(String msg) {
//        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
//    }
//    
//    private void addErr(String msg) {
//        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
//    }
//}
//
