package com.mypack.managedbeans;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import mypack.entity.Delivery;
import mypack.entity.Order1;
import mypack.entity.User;
import mypack.sessionbean.DeliveryFacadeLocal;

/**
 * Managed Bean for Shipper Finance Management
 * Handles COD collection, submission, and financial tracking
 */
@Named(value = "shipperFinanceMBean")
@SessionScoped
public class ShipperFinanceMBean implements Serializable {

    @EJB
    private DeliveryFacadeLocal deliveryFacade;
    
    // Financial statistics
    private int totalPendingCOD = 0;
    private int totalCollectedToday = 0;
    private int totalCollectedMonth = 0;
    private int totalEarnings = 0;
    private int monthlyEarnings = 0;
    
    // Filters
    private String typeFilter = "all";
    private Integer monthFilter;
    private Integer yearFilter;
    
    // Pagination
    private int currentPage = 1;
    private int pageSize = 10;
    
    // Submit modal
    private boolean showSubmitModal = false;
    private int submitAmount = 0;
    private String submitReferenceCode = "";
    
    // Payment history (placeholder - will be populated when Payment entity exists)
    private List<PaymentRecord> paymentHistory = new ArrayList<>();
    
    public ShipperFinanceMBean() {
        Calendar cal = Calendar.getInstance();
        yearFilter = cal.get(Calendar.YEAR);
        monthFilter = cal.get(Calendar.MONTH) + 1;
    }
    
    // Get current logged-in user
    private User getCurrentUser() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) return null;
            
            jakarta.el.ELContext elContext = facesContext.getELContext();
            jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
            jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
            LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
            
            return loginMBean != null ? loginMBean.getCurrentUser() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Calculate pending COD (from delivered orders that haven't been submitted)
    public int getTotalPendingCOD() {
        try {
            refreshStatistics();
            return totalPendingCOD;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    // Calculate today's collection
    public int getTotalCollectedToday() {
        try {
            refreshStatistics();
            return totalCollectedToday;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    // Calculate month's collection
    public int getTotalCollectedMonth() {
        try {
            refreshStatistics();
            return totalCollectedMonth;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    // Total earnings
    public int getTotalEarnings() {
        try {
            refreshStatistics();
            return totalEarnings;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    // Monthly earnings
    public int getMonthlyEarnings() {
        try {
            refreshStatistics();
            return monthlyEarnings;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    // Refresh all financial statistics
    private void refreshStatistics() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                totalPendingCOD = 0;
                totalCollectedToday = 0;
                totalCollectedMonth = 0;
                totalEarnings = 0;
                monthlyEarnings = 0;
                return;
            }
            
            List<Delivery> myDeliveries = deliveryFacade.findByShipper(currentUser);
            
            totalPendingCOD = 0;
            totalCollectedToday = 0;
            totalCollectedMonth = 0;
            totalEarnings = 0;
            monthlyEarnings = 0;
            
            Date today = new Date();
            Calendar todayCal = Calendar.getInstance();
            todayCal.setTime(today);
            
            Calendar monthStart = Calendar.getInstance();
            monthStart.set(Calendar.DAY_OF_MONTH, 1);
            monthStart.set(Calendar.HOUR_OF_DAY, 0);
            monthStart.set(Calendar.MINUTE, 0);
            monthStart.set(Calendar.SECOND, 0);
            monthStart.set(Calendar.MILLISECOND, 0);
            
            for (Delivery d : myDeliveries) {
                Order1 order = d.getOrderID();
                if (order == null) continue;
                
                int orderAmount = order.getTotalAmount();
                String status = d.getStatus() != null ? d.getStatus().toLowerCase() : "";
                
                // Pending COD: delivered orders that haven't been submitted
                if ("delivered".equals(status)) {
                    // For now, assume all delivered orders have pending COD
                    // In real system, this would check if COD was already submitted
                    totalPendingCOD += orderAmount;
                }
                
                // Collected today: delivered orders today
                if ("delivered".equals(status) && d.getDeliveredAt() != null) {
                    Calendar deliveredCal = Calendar.getInstance();
                    deliveredCal.setTime(d.getDeliveredAt());
                    if (isSameDay(todayCal, deliveredCal)) {
                        totalCollectedToday += orderAmount;
                    }
                }
                
                // Collected this month: delivered orders this month
                if ("delivered".equals(status) && d.getDeliveredAt() != null) {
                    Calendar deliveredCal = Calendar.getInstance();
                    deliveredCal.setTime(d.getDeliveredAt());
                    if (deliveredCal.after(monthStart) || isSameDay(deliveredCal, monthStart)) {
                        totalCollectedMonth += orderAmount;
                        monthlyEarnings += orderAmount;
                    }
                }
                
                // Total earnings: all delivered orders
                if ("delivered".equals(status)) {
                    totalEarnings += orderAmount;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    // Payment history (placeholder - returns empty list for now)
    public List<PaymentRecord> getPagedHistory() {
        try {
        
            // For now, return empty list or mock data
            List<PaymentRecord> filtered = getFilteredHistory();
            
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, filtered.size());
            
            if (start >= filtered.size()) {
                currentPage = 1;
                start = 0;
                end = Math.min(pageSize, filtered.size());
            }
            
            if (start < 0 || start >= end || end > filtered.size()) {
                return new ArrayList<>();
            }
            
            return filtered.subList(start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private List<PaymentRecord> getFilteredHistory() {
      
        // For now, return empty list
        return new ArrayList<>();
    }
    
    // Filter history
    public void filterHistory() {
        currentPage = 1;
    }
    
    // Clear filter
    public void clearFilter() {
        typeFilter = "all";
        Calendar cal = Calendar.getInstance();
        yearFilter = cal.get(Calendar.YEAR);
        monthFilter = cal.get(Calendar.MONTH) + 1;
        currentPage = 1;
    }
    
    // Open submit modal
    public void openSubmitModal() {
        totalPendingCOD = getTotalPendingCOD();
        submitAmount = totalPendingCOD;
        submitReferenceCode = "";
        showSubmitModal = true;
    }
    
    // Close submit modal
    public void closeSubmitModal() {
        showSubmitModal = false;
        submitAmount = 0;
        submitReferenceCode = "";
    }
    
    // Nộp tiền COD
    public String submitCOD() {
        try {
            if (submitAmount <= 0) {
                addError("❌ Invalid amount!");  // Số tiền không hợp lệ
                return null;
            }
            
            if (submitReferenceCode == null || submitReferenceCode.trim().isEmpty()) {
                addError("❌ Please enter transaction code!");  // Vui lòng nhập mã giao dịch
                return null;
            }
            
            // Tạm thời chỉ đánh dấu là đã nộp (hệ thống thực sẽ tạo Payment record)
            // For now, just mark as submitted (in real system, this would create a Payment record)
            
            addInfo("✅ COD submitted successfully! Trans ID: " + submitReferenceCode);
            closeSubmitModal();
            refreshStatistics();
            
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Error: " + e.getMessage());
            return null;
        }
    }
    
    // Pagination
    public int getTotalPages() {
        int total = getTotalItems();
        return total == 0 ? 1 : (int) Math.ceil((double) total / pageSize);
    }
    
    public int getTotalItems() {
        return getFilteredHistory().size();
    }
    
    public void firstPage() { currentPage = 1; }
    public void previousPage() { if (currentPage > 1) currentPage--; }
    public void nextPage() { if (currentPage < getTotalPages()) currentPage++; }
    public void lastPage() { currentPage = getTotalPages(); }
    
    // Format methods
    public String formatAmount(int amount) {
        return String.format("%,d", amount) + " VND";
    }
    
    public String formatDate(Date date) {
        if (date == null) return "-";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
    
    // Get amount color (for display)
    public String getAmountColor(PaymentRecord p) {
        if (p == null) return "#666";
        String type = p.getPaymentType() != null ? p.getPaymentType().toUpperCase() : "";
        if (type.contains("COLLECTED") || type.contains("COMMISSION") || type.contains("BONUS")) {
            return "#28a745"; // Green for income
        } else if (type.contains("SUBMITTED") || type.contains("DEDUCTION")) {
            return "#dc3545"; // Red for expense
        }
        return "#666";
    }
    
    // Get amount prefix
    public String getAmountPrefix(PaymentRecord p) {
        if (p == null) return "";
        String type = p.getPaymentType() != null ? p.getPaymentType().toUpperCase() : "";
        if (type.contains("COLLECTED") || type.contains("COMMISSION") || type.contains("BONUS")) {
            return "+";
        } else if (type.contains("SUBMITTED") || type.contains("DEDUCTION")) {
            return "-";
        }
        return "";
    }
    
    // Get status color
    public String getStatusColor(String status) {
        if (status == null) return "#666";
        switch (status.toUpperCase()) {
            case "COMPLETED":
            case "SUCCESS":
                return "#28a745";
            case "PENDING":
                return "#ffc107";
            case "FAILED":
            case "CANCELLED":
                return "#dc3545";
            default:
                return "#666";
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
    public String getTypeFilter() {
        return typeFilter;
    }
    
    public void setTypeFilter(String typeFilter) {
        this.typeFilter = typeFilter;
    }
    
    public Integer getMonthFilter() {
        return monthFilter;
    }
    
    public void setMonthFilter(Integer monthFilter) {
        this.monthFilter = monthFilter;
    }
    
    public Integer getYearFilter() {
        return yearFilter;
    }
    
    public void setYearFilter(Integer yearFilter) {
        this.yearFilter = yearFilter;
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
    
    public boolean isShowSubmitModal() {
        return showSubmitModal;
    }
    
    public void setShowSubmitModal(boolean showSubmitModal) {
        this.showSubmitModal = showSubmitModal;
    }
    
    public int getSubmitAmount() {
        return submitAmount;
    }
    
    public void setSubmitAmount(int submitAmount) {
        this.submitAmount = submitAmount;
    }
    
    public String getSubmitReferenceCode() {
        return submitReferenceCode;
    }
    
    public void setSubmitReferenceCode(String submitReferenceCode) {
        this.submitReferenceCode = submitReferenceCode;
    }
    
    // Inner class for Payment Record (placeholder until Payment entity exists)
    public static class PaymentRecord {
        private Integer paymentID;
        private String paymentType;
        private int amount;
        private String description;
        private String status;
        private Date createdAt;
        private String referenceCode;
        
        public PaymentRecord() {
        }
        
        public PaymentRecord(Integer paymentID, String paymentType, int amount, String description, String status, Date createdAt, String referenceCode) {
            this.paymentID = paymentID;
            this.paymentType = paymentType;
            this.amount = amount;
            this.description = description;
            this.status = status;
            this.createdAt = createdAt;
            this.referenceCode = referenceCode;
        }
        
        // Lấy tên hiển thị loại thanh toán
        public String getPaymentTypeDisplay() {
            if (paymentType == null) return "";
            switch (paymentType.toUpperCase()) {
                case "COD_COLLECTED": return "💰 COD Collected";   // Thu COD
                case "COD_SUBMITTED": return "📤 COD Submitted";   // Nộp COD
                case "COMMISSION": return "💵 Commission";         // Hoa hồng
                case "BONUS": return "🎁 Bonus";                   // Thưởng
                case "DEDUCTION": return "➖ Deduction";           // Trừ tiền
                default: return paymentType;
            }
        }
        
        // Lấy text hiển thị trạng thái thanh toán
        public String getStatusDisplay() {
            if (status == null) return "";
            switch (status.toUpperCase()) {
                case "COMPLETED":
                case "SUCCESS": return "✅ Completed";     // Hoàn thành
                case "PENDING": return "⏳ Pending";       // Chờ xử lý
                case "FAILED": return "❌ Failed";         // Thất bại
                case "CANCELLED": return "🚫 Cancelled";   // Đã hủy
                default: return status;
            }
        }
        
        // Getters and Setters
        public Integer getPaymentID() { return paymentID; }
        public void setPaymentID(Integer paymentID) { this.paymentID = paymentID; }
        
        public String getPaymentType() { return paymentType; }
        public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
        
        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
        
        public String getReferenceCode() { return referenceCode; }
        public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
    }
}
