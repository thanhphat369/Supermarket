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
import mypack.entity.Payment;
import mypack.entity.User;
import mypack.sessionbean.DeliveryFacadeLocal;
import mypack.sessionbean.PaymentFacadeLocal;

/**
 * Managed Bean for Shipper Finance Management
 * 
 * Flow quản lý COD:
 * 1. Shipper giao hàng COD → Thu tiền mặt từ khách hàng
 * 2. Payment status = "paid" (đã thu tiền từ khách)
 * 3. Shipper nộp tiền về công ty → Thêm transactionID vào payment (đánh dấu đã nộp)
 * 4. Payment có transactionID ≠ null = đã nộp tiền về công ty
 * 
 * Flow thanh toán ONLINE:
 * - Customer thanh toán online → Payment status = "paid" ngay lập tức
 * - Shipper giao hàng → Không cần thu tiền, chỉ cần giao hàng
 */
@Named(value = "shipperFinanceMBean")
@SessionScoped
public class ShipperFinanceMBean implements Serializable {

    @EJB
    private DeliveryFacadeLocal deliveryFacade;
    
    @EJB
    private PaymentFacadeLocal paymentFacade;
    
    // Financial statistics
    private int totalPendingCOD = 0;
    private int totalCollectedToday = 0;
    private int totalCollectedMonth = 0;
    private int totalEarnings = 0;
    private int monthlyEarnings = 0;
    private int totalShippingFeeIncome = 0; // Total shipping fee income for shipper (after admin percentage)
    private int monthlyShippingFeeIncome = 0; // Monthly shipping fee income
    
    // Filters
    private String typeFilter = "all";
    private Integer monthFilter = 0; // 0 = all months, 1-12 = specific month
    private Integer yearFilter = 0; // 0 = all years, specific year = filter by year
    
    // Pagination
    private int currentPage = 1;
    private int pageSize = 10;
    
    // Submit modal
    private boolean showSubmitModal = false;
    private int submitAmount = 0;
    private String submitReferenceCode = "";
    
    // Payment history (placeholder - will be populated when Payment entity exists)
    private List<PaymentRecord> paymentHistory = new ArrayList<>();
    
    // COD Orders list
    private List<CODOrderInfo> pendingCODOrders = new ArrayList<>();
    
    public ShipperFinanceMBean() {
        // Default: show all transactions (no date filter)
        monthFilter = 0;
        yearFilter = 0;
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
    
    // Total shipping fee income (after admin percentage)
    public int getTotalShippingFeeIncome() {
        try {
            refreshStatistics();
            return totalShippingFeeIncome;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    // Monthly shipping fee income (after admin percentage)
    public int getMonthlyShippingFeeIncome() {
        try {
            refreshStatistics();
            return monthlyShippingFeeIncome;
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
                totalShippingFeeIncome = 0;
                monthlyShippingFeeIncome = 0;
                return;
            }
            
            List<Delivery> myDeliveries = deliveryFacade.findByShipper(currentUser);
            
            totalPendingCOD = 0;
            totalCollectedToday = 0;
            totalCollectedMonth = 0;
            totalEarnings = 0;
            monthlyEarnings = 0;
            totalShippingFeeIncome = 0;
            monthlyShippingFeeIncome = 0;
            
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
                int shippingFee = order.getShippingFee() != null ? order.getShippingFee() : 0;
                String status = d.getStatus() != null ? d.getStatus().toLowerCase() : "";
                
                // Get payment method for this order
                Payment payment = paymentFacade.findByOrder(order);
                boolean isCOD = payment != null && payment.getPaymentMethod() != null 
                    && "COD".equalsIgnoreCase(payment.getPaymentMethod());
                
                // Calculate shipping fee income for all delivered orders (after admin percentage)
                if ("delivered".equals(status) && shippingFee > 0) {
                    // Get admin percentage
                    int adminPercent = com.mypack.managedbeans.AdminFinanceMBean.getAdminPercentageStatic();
                    // Shipper gets (100 - adminPercentage)% of shipping fee
                    int shipperShippingFeeIncome = (int) Math.round(shippingFee * (100.0 - adminPercent) / 100.0);
                    
                    Date deliveredDate = d.getDeliveredAt() != null ? d.getDeliveredAt() : new Date();
                    Calendar deliveredCal = Calendar.getInstance();
                    deliveredCal.setTime(deliveredDate);
                    
                    // Add to total shipping fee income
                    totalShippingFeeIncome += shipperShippingFeeIncome;
                    
                    // Add to monthly shipping fee income if delivered this month
                    if (deliveredCal.after(monthStart) || isSameDay(deliveredCal, monthStart)) {
                        monthlyShippingFeeIncome += shipperShippingFeeIncome;
                    }
                }
                
                // Only calculate COD for orders with COD payment method
                if (!isCOD) {
                    continue; // Skip non-COD orders for COD calculations
                }
                
                // Pending COD: delivered orders that haven't been submitted to company
                // COD flow: Giao hàng → Thu tiền (payment status = "paid") → Nộp tiền về công ty (transactionID được set)
                if ("delivered".equals(status)) {
                    // Check if payment has been submitted to company
                    // transactionID exists = đã nộp tiền về công ty
                    if (payment != null && payment.getTransactionID() != null && !payment.getTransactionID().trim().isEmpty()) {
                        // Payment already submitted to company, skip
                        continue;
                    }
                    // COD đã thu nhưng chưa nộp về công ty
                    totalPendingCOD += orderAmount;
                }
                
                // Collected today: COD submitted today (money submitted to company today)
                if ("delivered".equals(status) && payment != null) {
                    // Check if COD was submitted (has transactionID starting with "SUBMIT_")
                    String transactionID = payment.getTransactionID();
                    if (transactionID != null && transactionID.startsWith("SUBMIT_")) {
                        // Get submission date (use updatedAt if available, otherwise deliveredAt)
                        Date submittedDate = payment.getUpdatedAt() != null ? payment.getUpdatedAt() : 
                                            (d.getDeliveredAt() != null ? d.getDeliveredAt() : new Date());
                        Calendar submittedCal = Calendar.getInstance();
                        submittedCal.setTime(submittedDate);
                        if (isSameDay(todayCal, submittedCal)) {
                            totalCollectedToday += orderAmount;
                        }
                    }
                }
                
                // Collected this month: COD submitted this month (money submitted to company this month)
                if ("delivered".equals(status) && payment != null) {
                    // Check if COD was submitted (has transactionID starting with "SUBMIT_")
                    String transactionID = payment.getTransactionID();
                    if (transactionID != null && transactionID.startsWith("SUBMIT_")) {
                        // Get submission date (use updatedAt if available, otherwise deliveredAt)
                        Date submittedDate = payment.getUpdatedAt() != null ? payment.getUpdatedAt() : 
                                            (d.getDeliveredAt() != null ? d.getDeliveredAt() : new Date());
                        Calendar submittedCal = Calendar.getInstance();
                        submittedCal.setTime(submittedDate);
                        if (submittedCal.after(monthStart) || isSameDay(submittedCal, monthStart)) {
                            totalCollectedMonth += orderAmount;
                            monthlyEarnings += orderAmount;
                        }
                    }
                }
                
                // Total earnings: all COD submitted (all money submitted to company)
                if ("delivered".equals(status) && payment != null) {
                    String transactionID = payment.getTransactionID();
                    if (transactionID != null && transactionID.startsWith("SUBMIT_")) {
                        totalEarnings += orderAmount;
                    }
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
    
    // Payment history - Get paginated transaction history
    public List<PaymentRecord> getPagedHistory() {
        try {
            // Get filtered history from database
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
    
    /**
     * Lấy lịch sử giao dịch COD (thu tiền và nộp tiền)
     * 
     * Bao gồm:
     * - COD_COLLECTED: Đã thu tiền COD từ khách (payment status = "paid", transactionID = null hoặc không bắt đầu bằng "SUBMIT_")
     * - COD_SUBMITTED: Đã nộp tiền COD về công ty (transactionID bắt đầu bằng "SUBMIT_")
     */
    private List<PaymentRecord> getFilteredHistory() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
        return new ArrayList<>();
    }
    
            List<Delivery> myDeliveries = deliveryFacade.findByShipper(currentUser);
            List<PaymentRecord> history = new ArrayList<>();
            
            // Date filter - only apply if both month and year are specified (not 0)
            Calendar monthStart = null;
            Calendar monthEnd = null;
            if (monthFilter != null && yearFilter != null && monthFilter > 0 && yearFilter > 0) {
                // Both month and year are specified, apply filter
                monthStart = Calendar.getInstance();
                monthStart.set(Calendar.YEAR, yearFilter);
                monthStart.set(Calendar.MONTH, monthFilter - 1);
                monthStart.set(Calendar.DAY_OF_MONTH, 1);
                monthStart.set(Calendar.HOUR_OF_DAY, 0);
                monthStart.set(Calendar.MINUTE, 0);
                monthStart.set(Calendar.SECOND, 0);
                monthStart.set(Calendar.MILLISECOND, 0);
                
                monthEnd = Calendar.getInstance();
                monthEnd.set(Calendar.YEAR, yearFilter);
                monthEnd.set(Calendar.MONTH, monthFilter);
                monthEnd.set(Calendar.DAY_OF_MONTH, 1);
                monthEnd.add(Calendar.DAY_OF_MONTH, -1);
                monthEnd.set(Calendar.HOUR_OF_DAY, 23);
                monthEnd.set(Calendar.MINUTE, 59);
                monthEnd.set(Calendar.SECOND, 59);
                monthEnd.set(Calendar.MILLISECOND, 999);
            }
            
            for (Delivery d : myDeliveries) {
                Order1 order = d.getOrderID();
                if (order == null) continue;
                
                Payment payment = paymentFacade.findByOrder(order);
                if (payment == null) continue;
                
                String status = d.getStatus() != null ? d.getStatus().toLowerCase() : "";
                if (!"delivered".equals(status)) {
                    continue; // Chỉ lấy đơn đã giao
                }
                
                String paymentMethod = payment.getPaymentMethod();
                boolean isCOD = paymentMethod != null && "COD".equalsIgnoreCase(paymentMethod);
                boolean isOnline = paymentMethod != null && !"COD".equalsIgnoreCase(paymentMethod);
                
                // Handle COD orders - Skip if filter is "ONLINE"
                if (isCOD && !"ONLINE".equals(typeFilter)) {
                    // Get payment information first
                    int orderAmount = order.getTotalAmount();
                    String transactionID = payment.getTransactionID();
                    String paymentStatus = payment.getPaymentStatus();
                    
                    // Check date filter (only if month and year are specified)
                    if (monthStart != null && monthEnd != null) {
                        Date checkDate = null;
                        // For SUBMITTED: use payment.updatedAt (when submitted)
                        // For COLLECTED: use payment.paymentDate (when collected)
                        if (transactionID != null && transactionID.startsWith("SUBMIT_")) {
                            checkDate = payment.getUpdatedAt() != null ? payment.getUpdatedAt() : 
                                       (d.getDeliveredAt() != null ? d.getDeliveredAt() : d.getUpdatedAt());
                        } else {
                            checkDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : 
                                       (d.getDeliveredAt() != null ? d.getDeliveredAt() : d.getUpdatedAt());
                        }
                        
                        if (checkDate != null) {
                            Calendar checkCal = Calendar.getInstance();
                            checkCal.setTime(checkDate);
                            if (checkCal.before(monthStart) || checkCal.after(monthEnd)) {
                                continue; // Không trong tháng được filter
                            }
                        }
                    }
                    
                    // COD_SUBMITTED: Đã nộp tiền về công ty (transactionID bắt đầu bằng "SUBMIT_")
                    // Ưu tiên hiển thị SUBMITTED vì đây là hành động quan trọng hơn
                    if (transactionID != null && transactionID.startsWith("SUBMIT_")) {
                        PaymentRecord submittedRecord = new PaymentRecord();
                        submittedRecord.setPaymentID(payment.getPaymentID());
                        submittedRecord.setPaymentType("COD_SUBMITTED");
                        submittedRecord.setAmount(orderAmount);
                        
                        // Extract reference code from transactionID (format: SUBMIT_{ref}_{timestamp})
                        String refCode = transactionID;
                        if (transactionID.contains("_")) {
                            String[] parts = transactionID.split("_");
                            if (parts.length >= 2) {
                                refCode = parts[1]; // Lấy phần reference code
                            }
                        }
                        
                        submittedRecord.setDescription("Nộp COD đơn hàng #" + order.getOrderID() + " về công ty");
                        submittedRecord.setStatus("COMPLETED");
                        // Use payment.updatedAt (when submitted) or delivery date as fallback
                        Date submittedDate = payment.getUpdatedAt() != null ? payment.getUpdatedAt() : 
                                           (d.getDeliveredAt() != null ? d.getDeliveredAt() : d.getUpdatedAt());
                        submittedRecord.setCreatedAt(submittedDate);
                        submittedRecord.setReferenceCode(refCode);
                        
                        // Apply type filter
                        if ("all".equals(typeFilter) || "COD_SUBMITTED".equals(typeFilter)) {
                            history.add(submittedRecord);
                        }
                    } 
                    // COD_COLLECTED: Đã thu tiền từ khách nhưng chưa nộp (payment status = "paid", transactionID = null)
                    // Chỉ hiển thị nếu chưa nộp (để shipper biết đã thu nhưng chưa nộp)
                    else if ("paid".equalsIgnoreCase(paymentStatus) && (transactionID == null || transactionID.trim().isEmpty())) {
                        PaymentRecord collectedRecord = new PaymentRecord();
                        collectedRecord.setPaymentID(payment.getPaymentID());
                        collectedRecord.setPaymentType("COD_COLLECTED");
                        collectedRecord.setAmount(orderAmount);
                        collectedRecord.setDescription("Thu COD từ đơn hàng #" + order.getOrderID() + " (Chưa nộp)");
                        collectedRecord.setStatus("PENDING"); // Pending vì chưa nộp về công ty
                        // Use payment.paymentDate (when collected) or delivery date as fallback
                        Date collectedDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : 
                                           (d.getDeliveredAt() != null ? d.getDeliveredAt() : d.getUpdatedAt());
                        collectedRecord.setCreatedAt(collectedDate);
                        collectedRecord.setReferenceCode("ORDER_" + order.getOrderID());
                        
                        // Apply type filter
                        if ("all".equals(typeFilter) || "COD_COLLECTED".equals(typeFilter)) {
                            history.add(collectedRecord);
                        }
                    }
                }
                // Handle Online orders - show shipping fee income (after admin percentage)
                // Only process if filter is "all" or "ONLINE"
                if (isOnline && ("all".equals(typeFilter) || "ONLINE".equals(typeFilter))) {
                    int shippingFee = order.getShippingFee() != null ? order.getShippingFee() : 0;
                    if (shippingFee > 0) {
                        // Get admin percentage
                        int adminPercent = com.mypack.managedbeans.AdminFinanceMBean.getAdminPercentageStatic();
                        // Shipper gets (100 - adminPercentage)% of shipping fee
                        int shipperShippingFeeIncome = (int) Math.round(shippingFee * (100.0 - adminPercent) / 100.0);
                        
                        // Check date filter (only if month and year are specified)
                        Date checkDate = d.getDeliveredAt() != null ? d.getDeliveredAt() : d.getUpdatedAt();
                        if (monthStart != null && monthEnd != null && checkDate != null) {
                            Calendar checkCal = Calendar.getInstance();
                            checkCal.setTime(checkDate);
                            if (checkCal.before(monthStart) || checkCal.after(monthEnd)) {
                                continue; // Không trong tháng được filter
                            }
                        }
                        
                        PaymentRecord onlineShippingRecord = new PaymentRecord();
                        onlineShippingRecord.setPaymentID(payment.getPaymentID());
                        onlineShippingRecord.setPaymentType("ONLINE");
                        onlineShippingRecord.setAmount(shipperShippingFeeIncome);
                        onlineShippingRecord.setDescription("Tiền ship đơn online #" + order.getOrderID() + " (Đã trừ " + adminPercent + "% admin)");
                        onlineShippingRecord.setStatus("COMPLETED");
                        onlineShippingRecord.setCreatedAt(checkDate);
                        onlineShippingRecord.setReferenceCode("ORDER_" + order.getOrderID());
                        
                        // Apply type filter
                        if ("all".equals(typeFilter) || "ONLINE".equals(typeFilter)) {
                            history.add(onlineShippingRecord);
                        }
                    }
                }
            }
            
            // Sort by date (newest first)
            history.sort((a, b) -> {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
            
            return history;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Filter history - Refresh and reset to first page
    public void filterHistory() {
        currentPage = 1;
        // Force refresh by clearing cached data
        paymentHistory.clear();
    }
    
    // Initialize history when page loads
    public void initHistory() {
        // This will be called when page loads to ensure history is populated
        getFilteredHistory();
    }
    
    // Clear filter - Show all transactions
    public void clearFilter() {
        typeFilter = "all";
        monthFilter = 0; // All months
        yearFilter = 0; // All years
        currentPage = 1;
        paymentHistory.clear(); // Clear cache
    }
    
    // Open submit modal
    public void openSubmitModal() {
        refreshStatistics();
        totalPendingCOD = getTotalPendingCOD();
        submitAmount = totalPendingCOD;
        submitReferenceCode = "";
        getPendingCODOrders(); // Refresh pending orders list
        showSubmitModal = true;
    }
    
    // Close submit modal
    public void closeSubmitModal() {
        showSubmitModal = false;
        submitAmount = 0;
        submitReferenceCode = "";
    }
    
    /**
     * Lấy danh sách các đơn COD đã giao hàng nhưng chưa nộp tiền về công ty
     * 
     * Điều kiện:
     * - Delivery status = "delivered" (đã giao hàng)
     * - Payment method = "COD" (thanh toán COD)
     * - Payment status = "paid" (đã thu tiền từ khách)
     * - Payment transactionID = null (chưa nộp tiền về công ty)
     * 
     * @return Danh sách COD orders chưa nộp tiền
     */
    public List<CODOrderInfo> getPendingCODOrders() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return new ArrayList<>();
            }
            
            List<Delivery> myDeliveries = deliveryFacade.findByShipper(currentUser);
            List<CODOrderInfo> codOrders = new ArrayList<>();
            
            for (Delivery d : myDeliveries) {
                Order1 order = d.getOrderID();
                if (order == null) continue;
                
                String status = d.getStatus() != null ? d.getStatus().toLowerCase() : "";
                if (!"delivered".equals(status)) continue;
                
                Payment payment = paymentFacade.findByOrder(order);
                if (payment == null) continue;
                
                boolean isCOD = payment.getPaymentMethod() != null && "COD".equalsIgnoreCase(payment.getPaymentMethod());
                if (!isCOD) continue;
                
                // Check if already submitted to company
                // transactionID != null = đã nộp tiền về công ty
                if (payment.getTransactionID() != null && !payment.getTransactionID().trim().isEmpty()) {
                    continue; // Đã nộp tiền về công ty rồi
                }
                
                // Chỉ tính các COD đã thu tiền (payment status = "paid")
                if (!"paid".equalsIgnoreCase(payment.getPaymentStatus())) {
                    continue; // Chưa thu tiền từ khách
                }
                
                CODOrderInfo codInfo = new CODOrderInfo();
                codInfo.setOrderID(order.getOrderID());
                codInfo.setOrder(order);
                codInfo.setDelivery(d);
                codInfo.setPayment(payment);
                codInfo.setAmount(order.getTotalAmount());
                codInfo.setDeliveredDate(d.getDeliveredAt() != null ? d.getDeliveredAt() : d.getUpdatedAt());
                codOrders.add(codInfo);
            }
            
            pendingCODOrders = codOrders;
            return codOrders;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Nộp tiền COD về công ty
     * 
     * Flow:
     * 1. Shipper đã thu tiền COD từ khách hàng (payment status = "paid")
     * 2. Shipper nộp tiền về công ty qua ngân hàng/chuyển khoản
     * 3. Nhập mã giao dịch nộp tiền
     * 4. Hệ thống đánh dấu tất cả COD orders đã nộp bằng cách set transactionID
     * 
     * @return null (stay on same page)
     */
    public String submitCOD() {
        try {
            if (submitAmount <= 0) {
                addError("❌ Số tiền không hợp lệ!");
                return null;
            }
            
            if (submitReferenceCode == null || submitReferenceCode.trim().isEmpty()) {
                addError("❌ Vui lòng nhập mã giao dịch nộp tiền!");
                return null;
            }
            
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                addError("❌ Không tìm thấy thông tin shipper!");
                return null;
            }
            
            // Get all pending COD orders
            List<CODOrderInfo> pendingOrders = getPendingCODOrders();
            if (pendingOrders.isEmpty()) {
                addError("❌ Không có đơn COD nào cần nộp tiền!");
                return null;
            }
            
            // Calculate total amount of pending COD
            int calculatedTotal = 0;
            for (CODOrderInfo codInfo : pendingOrders) {
                calculatedTotal += codInfo.getAmount();
            }
            
            // Validate amount
            if (submitAmount != calculatedTotal) {
                addError("❌ Số tiền không khớp! Tổng COD cần nộp: " + formatAmount(calculatedTotal));
                return null;
            }
            
            // Mark all pending COD payments as submitted to company
            // Set transactionID để đánh dấu đã nộp tiền về công ty
            int submittedCount = 0;
            for (CODOrderInfo codInfo : pendingOrders) {
                Payment payment = codInfo.getPayment();
                if (payment != null) {
                    // Đánh dấu đã nộp tiền bằng cách set transactionID
                    // Format: SUBMIT_{referenceCode}_{timestamp}
                    payment.setTransactionID("SUBMIT_" + submitReferenceCode + "_" + System.currentTimeMillis());
                    payment.setUpdatedAt(new Date());
                    paymentFacade.edit(payment);
                    submittedCount++;
                    System.out.println("✅ COD Order #" + codInfo.getOrderID() + " marked as submitted. Amount: " + codInfo.getAmount());
                }
            }
            
            addInfo("✅ Đã nộp tiền COD thành công! " + submittedCount + " đơn hàng, Tổng: " + formatAmount(submitAmount) + ", Mã GD: " + submitReferenceCode);
            closeSubmitModal();
            refreshStatistics();
            pendingCODOrders.clear(); // Clear list after submission
            
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Lỗi khi nộp tiền: " + e.getMessage());
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
        if (type.contains("COLLECTED") || type.contains("COMMISSION") || type.contains("BONUS") || type.contains("SHIPPING_FEE")) {
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
        if (type.contains("COLLECTED") || type.contains("COMMISSION") || type.contains("BONUS") || type.contains("SHIPPING_FEE")) {
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
                case "ONLINE": return "💳 Online Payment";  // Tiền ship đơn online
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
    
    // Inner class for COD Order Info
    public static class CODOrderInfo {
        private Integer orderID;
        private Order1 order;
        private Delivery delivery;
        private Payment payment;
        private int amount;
        private Date deliveredDate;
        
        public CODOrderInfo() {
        }
        
        // Getters and Setters
        public Integer getOrderID() { return orderID; }
        public void setOrderID(Integer orderID) { this.orderID = orderID; }
        
        public Order1 getOrder() { return order; }
        public void setOrder(Order1 order) { this.order = order; }
        
        public Delivery getDelivery() { return delivery; }
        public void setDelivery(Delivery delivery) { this.delivery = delivery; }
        
        public Payment getPayment() { return payment; }
        public void setPayment(Payment payment) { this.payment = payment; }
        
        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
        
        public Date getDeliveredDate() { return deliveredDate; }
        public void setDeliveredDate(Date deliveredDate) { this.deliveredDate = deliveredDate; }
    }
    
    // Get count of pending COD orders
    public int getPendingCODOrdersCount() {
        return getPendingCODOrders().size();
    }
}
