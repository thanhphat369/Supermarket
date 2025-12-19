package com.mypack.managedbeans;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import mypack.entity.Delivery;
import mypack.entity.DeliveryLog;
import mypack.entity.Order1;
import mypack.entity.OrderDetails;
import mypack.entity.User;
import mypack.sessionbean.DeliveryFacadeLocal;
import mypack.sessionbean.DeliveryLogFacadeLocal;
import mypack.sessionbean.Order1FacadeLocal;
import mypack.sessionbean.OrderDetailsFacadeLocal;

/**
 * Managed Bean for Shipper Dashboard
 * Provides dashboard statistics, delivery management, image upload, and history
 */
@Named(value = "shipperDashboardMBean")
@SessionScoped
public class ShipperDashboardMBean implements Serializable {

    @EJB
    private DeliveryFacadeLocal deliveryFacade;
    
    @EJB
    private DeliveryLogFacadeLocal deliveryLogFacade;
    
    @EJB
    private Order1FacadeLocal orderFacade;
    
    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;
    
    // Dashboard statistics
    private int pendingCount = 0;
    private int pickedUpCount = 0;
    private int shippingCount = 0;
    private int deliveredCount = 0;
    private int failedCount = 0;
    private int todayDeliveredCount = 0;
    private int monthDeliveredCount = 0;
    
    // Selected delivery for actions
    private Delivery selectedDelivery;
    private Order1 selectedOrder;
    private List<DeliveryLog> selectedDeliveryLogs;
    
    // Image upload
    private Part proofImageFile;
    private Part failureImageFile;
    private String failureReason;
    private String deliveryNotes;
    
    // History filters
    private String historyStatusFilter = "all";
    private Integer historyMonthFilter;
    private Integer historyYearFilter;
    private int historyCurrentPage = 1;
    private int historyPageSize = 10;
    
    // Modal states
    private boolean showDeliveryModal = false;
    private boolean showProofUploadModal = false;
    private boolean showFailureModal = false;
    private boolean showLogModal = false;

    public ShipperDashboardMBean() {
        // Initialize year/month filter to current
        Calendar cal = Calendar.getInstance();
        historyYearFilter = cal.get(Calendar.YEAR);
        historyMonthFilter = cal.get(Calendar.MONTH) + 1;
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
    
    // ========== DASHBOARD STATISTICS ==========
    
    public void refreshStatistics() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) return;
            
            List<Delivery> myDeliveries = deliveryFacade.findByShipper(currentUser);
            
            pendingCount = 0;
            pickedUpCount = 0;
            shippingCount = 0;
            deliveredCount = 0;
            failedCount = 0;
            todayDeliveredCount = 0;
            monthDeliveredCount = 0;
            
            Date today = new Date();
            Calendar todayCal = Calendar.getInstance();
            todayCal.setTime(today);
            
            for (Delivery d : myDeliveries) {
                String status = d.getStatus() != null ? d.getStatus().toLowerCase() : "";
                
                switch (status) {
                    case "assigned":
                    case "pending":
                        pendingCount++;
                        break;
                    case "picked_up":
                        pickedUpCount++;
                        break;
                    case "shipping":
                        shippingCount++;
                        break;
                    case "delivered":
                        deliveredCount++;
                        // Check if delivered today
                        if (d.getDeliveredAt() != null) {
                            Calendar deliveredCal = Calendar.getInstance();
                            deliveredCal.setTime(d.getDeliveredAt());
                            if (isSameDay(todayCal, deliveredCal)) {
                                todayDeliveredCount++;
                            }
                            if (isSameMonth(todayCal, deliveredCal)) {
                                monthDeliveredCount++;
                            }
                        }
                        break;
                    case "failed":
                        failedCount++;
                        break;
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
    
    private boolean isSameMonth(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }
    
    // ========== DELIVERY LISTS ==========
    
    // Get pending deliveries (waiting to be picked up)
    public List<Delivery> getPendingDeliveries() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) return new ArrayList<>();
            
            List<Delivery> all = deliveryFacade.findByShipper(currentUser);
            List<Delivery> result = new ArrayList<>();
            for (Delivery d : all) {
                if ("assigned".equalsIgnoreCase(d.getStatus()) || "pending".equalsIgnoreCase(d.getStatus())) {
                    result.add(d);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Get picked up deliveries (ready to ship)
    public List<Delivery> getPickedUpDeliveries() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) return new ArrayList<>();
            
            List<Delivery> all = deliveryFacade.findByShipper(currentUser);
            List<Delivery> result = new ArrayList<>();
            for (Delivery d : all) {
                if ("picked_up".equalsIgnoreCase(d.getStatus())) {
                    result.add(d);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Get shipping deliveries (currently being delivered)
    public List<Delivery> getShippingDeliveries() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) return new ArrayList<>();
            
            List<Delivery> all = deliveryFacade.findByShipper(currentUser);
            List<Delivery> result = new ArrayList<>();
            for (Delivery d : all) {
                if ("shipping".equalsIgnoreCase(d.getStatus())) {
                    result.add(d);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Get recent delivered/completed
    public List<Delivery> getRecentDelivered() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) return new ArrayList<>();
            
            List<Delivery> all = deliveryFacade.findByShipper(currentUser);
            List<Delivery> result = new ArrayList<>();
            for (Delivery d : all) {
                if ("delivered".equalsIgnoreCase(d.getStatus())) {
                    result.add(d);
                }
            }
            // Return only first 5
            return result.size() > 5 ? result.subList(0, 5) : result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // ========== DELIVERY ACTIONS ==========
    
    // Accept/Receive delivery
    public void receiveDelivery(Delivery delivery) {
        try {
            if (delivery == null) return;
            
            // Create log entry
            deliveryLogFacade.createLogEntry(delivery, "RECEIVED", "Shipper đã nhận đơn hàng", null);
            
            addInfo("✅ Đã nhận đơn hàng #" + delivery.getOrderID().getOrderID());
            refreshStatistics();
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Lỗi: " + e.getMessage());
        }
    }
    
    // Pick up delivery (from warehouse)
    public void pickUpDelivery(Delivery delivery) {
        try {
            if (delivery == null) return;
            
            delivery.setStatus("picked_up");
            delivery.setPickedUpAt(new Date());
            delivery.setUpdatedAt(new Date());
            deliveryFacade.edit(delivery);
            
            // Create log entry
            deliveryLogFacade.createLogEntry(delivery, "PICKED_UP", "Shipper đã lấy hàng từ kho", null);
            
            addInfo("📦 Đã lấy hàng cho đơn #" + delivery.getOrderID().getOrderID());
            refreshStatistics();
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Lỗi: " + e.getMessage());
        }
    }
    
    // Start shipping
    public void startShipping(Delivery delivery) {
        try {
            if (delivery == null) return;
            
            delivery.setStatus("shipping");
            delivery.setUpdatedAt(new Date());
            deliveryFacade.edit(delivery);
            
            // Update order status
            Order1 order = delivery.getOrderID();
            if (order != null) {
                order.setStatus("shipping");
                orderFacade.edit(order);
            }
            
            // Create log entry
            deliveryLogFacade.createLogEntry(delivery, "SHIPPING", "Shipper bắt đầu giao hàng", null);
            
            addInfo("🚚 Bắt đầu giao đơn #" + delivery.getOrderID().getOrderID());
            refreshStatistics();
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Lỗi: " + e.getMessage());
        }
    }
    
    // Open proof upload modal
    public void openProofUploadModal(Delivery delivery) {
        this.selectedDelivery = delivery;
        this.selectedOrder = delivery != null ? delivery.getOrderID() : null;
        this.proofImageFile = null;
        this.deliveryNotes = "";
        this.showProofUploadModal = true;
    }
    
    // Close proof upload modal
    public void closeProofUploadModal() {
        this.showProofUploadModal = false;
        this.proofImageFile = null;
        this.deliveryNotes = "";
    }
    
    // Mark as delivered with proof image
    public void markAsDelivered() {
        try {
            if (selectedDelivery == null) {
                addError("❌ Không có đơn hàng được chọn!");
                return;
            }
            
            String imagePath = null;
            
            // Upload proof image if provided
            if (proofImageFile != null && proofImageFile.getSize() > 0) {
                imagePath = uploadImage(proofImageFile, "proof");
                selectedDelivery.setProofImage(imagePath);
            }
            
            selectedDelivery.setStatus("delivered");
            selectedDelivery.setDeliveredAt(new Date());
            selectedDelivery.setUpdatedAt(new Date());
            selectedDelivery.setNotes(deliveryNotes);
            deliveryFacade.edit(selectedDelivery);
            
            // Update order status
            Order1 order = selectedDelivery.getOrderID();
            if (order != null) {
                order.setStatus("completed");
                orderFacade.edit(order);
            }
            
            // Tạo log giao hàng thành công
            deliveryLogFacade.createLogEntry(selectedDelivery, "DELIVERED", 
                "Delivery successful" + (deliveryNotes != null && !deliveryNotes.isEmpty() ? ": " + deliveryNotes : ""), 
                imagePath);
            
            addInfo("✅ Successfully delivered order #" + selectedDelivery.getOrderID().getOrderID());
            closeProofUploadModal();
            refreshStatistics();
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Error: " + e.getMessage());
        }
    }
    
    // Mở modal báo cáo giao thất bại
    public void openFailureModal(Delivery delivery) {
        this.selectedDelivery = delivery;
        this.selectedOrder = delivery != null ? delivery.getOrderID() : null;
        this.failureImageFile = null;
        this.failureReason = "";
        this.showFailureModal = true;
    }
    
    // Đóng modal báo cáo thất bại
    public void closeFailureModal() {
        this.showFailureModal = false;
        this.failureImageFile = null;
        this.failureReason = "";
    }
    
    // Đánh dấu giao thất bại kèm hình ảnh
    public void markAsFailed() {
        try {
            if (selectedDelivery == null) {
                addError("❌ No order selected!");
                return;
            }
            
            if (failureReason == null || failureReason.trim().isEmpty()) {
                addError("❌ Please enter the failure reason!");
                return;
            }
            
            String imagePath = null;
            
            // Upload failure image if provided
            if (failureImageFile != null && failureImageFile.getSize() > 0) {
                imagePath = uploadImage(failureImageFile, "failure");
                selectedDelivery.setFailureImage(imagePath);
            }
            
            selectedDelivery.setStatus("failed");
            selectedDelivery.setFailureReason(failureReason);
            selectedDelivery.setUpdatedAt(new Date());
            deliveryFacade.edit(selectedDelivery);
            
            // Update order status
            Order1 order = selectedDelivery.getOrderID();
            if (order != null) {
                order.setStatus("failed");
                orderFacade.edit(order);
            }
            
            // Tạo log giao thất bại
            deliveryLogFacade.createLogEntry(selectedDelivery, "FAILED", 
                "Delivery failed: " + failureReason, imagePath);
            
            addInfo("❌ Reported failed delivery for order #" + selectedDelivery.getOrderID().getOrderID());
            closeFailureModal();
            refreshStatistics();
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Error: " + e.getMessage());
        }
    }
    
    // ========== IMAGE UPLOAD ==========
    
    private String uploadImage(Part file, String prefix) throws IOException {
        if (file == null || file.getSize() == 0) {
            return null;
        }
        
        String fileName = getSubmittedFileName(file);
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i);
        }
        
        // Generate unique filename
        String newFileName = "delivery_" + prefix + "_" + System.currentTimeMillis() + extension;
        
        // Get upload path
        String uploadPath = getUploadPath();
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        Path filePath = uploadDir.resolve(newFileName);
        
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        return newFileName;
    }
    
    private String getSubmittedFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String item : items) {
            if (item.trim().startsWith("filename")) {
                return item.substring(item.indexOf("=") + 2, item.length() - 1);
            }
        }
        return "unknown";
    }
    
    private String getUploadPath() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String realPath = facesContext.getExternalContext().getRealPath("/resources/delivery/");
        return realPath;
    }
    
    // Get image URL for display
    public String getImageUrl(String imageName) {
        if (imageName == null || imageName.isEmpty()) {
            return null;
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String contextPath = facesContext.getExternalContext().getRequestContextPath();
            return contextPath + "/resources/delivery/" + imageName;
        }
        return "/resources/delivery/" + imageName;
    }
    
    // ========== DELIVERY LOG ==========
    
    public void openLogModal(Delivery delivery) {
        this.selectedDelivery = delivery;
        this.selectedDeliveryLogs = deliveryLogFacade.findByDelivery(delivery);
        this.showLogModal = true;
    }
    
    public void closeLogModal() {
        this.showLogModal = false;
        this.selectedDeliveryLogs = null;
    }
    
    // Add note to delivery log
    public void addNoteToLog(String note) {
        try {
            if (selectedDelivery == null || note == null || note.trim().isEmpty()) {
                return;
            }
            
            deliveryLogFacade.createLogEntry(selectedDelivery, "NOTE", note, null);
            
            // Refresh logs
            selectedDeliveryLogs = deliveryLogFacade.findByDelivery(selectedDelivery);
            addInfo("📝 Đã thêm ghi chú");
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Lỗi: " + e.getMessage());
        }
    }
    
    // ========== DELIVERY HISTORY ==========
    
    public List<Delivery> getDeliveryHistory() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) return new ArrayList<>();
            
            List<Delivery> all = deliveryFacade.findByShipper(currentUser);
            List<Delivery> filtered = new ArrayList<>();
            
            for (Delivery d : all) {
                // Filter by status
                if (!"all".equals(historyStatusFilter)) {
                    if (!historyStatusFilter.equalsIgnoreCase(d.getStatus())) {
                        continue;
                    }
                }
                
                // Filter by month/year
                if (historyMonthFilter != null && historyYearFilter != null) {
                    Date deliveryDate = d.getDeliveredAt() != null ? d.getDeliveredAt() : d.getUpdatedAt();
                    if (deliveryDate != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(deliveryDate);
                        if (cal.get(Calendar.YEAR) != historyYearFilter ||
                            cal.get(Calendar.MONTH) + 1 != historyMonthFilter) {
                            continue;
                        }
                    }
                }
                
                filtered.add(d);
            }
            
            return filtered;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public List<Delivery> getPagedHistory() {
        try {
            List<Delivery> all = getDeliveryHistory();
            int start = (historyCurrentPage - 1) * historyPageSize;
            int end = Math.min(start + historyPageSize, all.size());
            
            if (start >= all.size()) {
                historyCurrentPage = 1;
                start = 0;
                end = Math.min(historyPageSize, all.size());
            }
            
            return all.subList(start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public void filterHistory() {
        historyCurrentPage = 1;
    }
    
    public void clearHistoryFilter() {
        historyStatusFilter = "all";
        Calendar cal = Calendar.getInstance();
        historyYearFilter = cal.get(Calendar.YEAR);
        historyMonthFilter = cal.get(Calendar.MONTH) + 1;
        historyCurrentPage = 1;
    }
    
    // History pagination
    public int getHistoryTotalPages() {
        int total = getDeliveryHistory().size();
        return total == 0 ? 1 : (int) Math.ceil((double) total / historyPageSize);
    }
    
    public int getHistoryTotalItems() {
        return getDeliveryHistory().size();
    }
    
    public void historyFirstPage() { historyCurrentPage = 1; }
    public void historyPreviousPage() { if (historyCurrentPage > 1) historyCurrentPage--; }
    public void historyNextPage() { if (historyCurrentPage < getHistoryTotalPages()) historyCurrentPage++; }
    public void historyLastPage() { historyCurrentPage = getHistoryTotalPages(); }
    
    // ========== ORDER DETAILS ==========
    
    public List<OrderDetails> getOrderDetails(Order1 order) {
        if (order == null) return new ArrayList<>();
        try {
            List<OrderDetails> allDetails = orderDetailsFacade.findAll();
            List<OrderDetails> result = new ArrayList<>();
            for (OrderDetails detail : allDetails) {
                if (detail.getOrderID() != null && 
                    detail.getOrderID().getOrderID().equals(order.getOrderID())) {
                    result.add(detail);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // ========== UTILITIES ==========
    
    public String formatAmount(int amount) {
        return String.format("%,d", amount) + " VND";
    }
    
    public String formatDate(Date date) {
        if (date == null) return "-";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
    
    public String formatDateShort(Date date) {
        if (date == null) return "-";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
        return sdf.format(date);
    }
    
    public String getStatusColor(String status) {
        if (status == null) return "#666";
        switch (status.toLowerCase()) {
            case "assigned":
            case "pending": return "#ffc107";
            case "picked_up": return "#17a2b8";
            case "shipping": return "#007bff";
            case "delivered": return "#28a745";
            case "failed": return "#dc3545";
            default: return "#666";
        }
    }
    
    public String getStatusBadgeClass(String status) {
        if (status == null) return "bg-secondary";
        switch (status.toLowerCase()) {
            case "assigned":
            case "pending": return "bg-warning";
            case "picked_up": return "bg-info";
            case "shipping": return "bg-primary";
            case "delivered": return "bg-success";
            case "failed": return "bg-danger";
            default: return "bg-secondary";
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
            case "failed": return "Failed";           // Thất bại
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

    // ========== GETTERS AND SETTERS ==========
    
    public int getPendingCount() {
        refreshStatistics();
        return pendingCount;
    }

    public int getPickedUpCount() {
        return pickedUpCount;
    }

    public int getShippingCount() {
        return shippingCount;
    }

    public int getDeliveredCount() {
        return deliveredCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public int getTodayDeliveredCount() {
        return todayDeliveredCount;
    }

    public int getMonthDeliveredCount() {
        return monthDeliveredCount;
    }

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

    public List<DeliveryLog> getSelectedDeliveryLogs() {
        return selectedDeliveryLogs;
    }

    public Part getProofImageFile() {
        return proofImageFile;
    }

    public void setProofImageFile(Part proofImageFile) {
        this.proofImageFile = proofImageFile;
    }

    public Part getFailureImageFile() {
        return failureImageFile;
    }

    public void setFailureImageFile(Part failureImageFile) {
        this.failureImageFile = failureImageFile;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getDeliveryNotes() {
        return deliveryNotes;
    }

    public void setDeliveryNotes(String deliveryNotes) {
        this.deliveryNotes = deliveryNotes;
    }

    public String getHistoryStatusFilter() {
        return historyStatusFilter;
    }

    public void setHistoryStatusFilter(String historyStatusFilter) {
        this.historyStatusFilter = historyStatusFilter;
    }

    public Integer getHistoryMonthFilter() {
        return historyMonthFilter;
    }

    public void setHistoryMonthFilter(Integer historyMonthFilter) {
        this.historyMonthFilter = historyMonthFilter;
    }

    public Integer getHistoryYearFilter() {
        return historyYearFilter;
    }

    public void setHistoryYearFilter(Integer historyYearFilter) {
        this.historyYearFilter = historyYearFilter;
    }

    public int getHistoryCurrentPage() {
        return historyCurrentPage;
    }

    public void setHistoryCurrentPage(int historyCurrentPage) {
        this.historyCurrentPage = historyCurrentPage;
    }

    public int getHistoryPageSize() {
        return historyPageSize;
    }

    public void setHistoryPageSize(int historyPageSize) {
        this.historyPageSize = historyPageSize;
    }

    public boolean isShowDeliveryModal() {
        return showDeliveryModal;
    }

    public void setShowDeliveryModal(boolean showDeliveryModal) {
        this.showDeliveryModal = showDeliveryModal;
    }

    public boolean isShowProofUploadModal() {
        return showProofUploadModal;
    }

    public void setShowProofUploadModal(boolean showProofUploadModal) {
        this.showProofUploadModal = showProofUploadModal;
    }

    public boolean isShowFailureModal() {
        return showFailureModal;
    }

    public void setShowFailureModal(boolean showFailureModal) {
        this.showFailureModal = showFailureModal;
    }

    public boolean isShowLogModal() {
        return showLogModal;
    }

    public void setShowLogModal(boolean showLogModal) {
        this.showLogModal = showLogModal;
    }
}



