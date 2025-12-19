package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import mypack.entity.Promotions;
import mypack.sessionbean.PromotionsFacadeLocal;

/**
 * Managed Bean for Promotion Management
 */
@Named(value = "promotionMBean")
@SessionScoped
public class PromotionMBean implements Serializable {

    @EJB
    private PromotionsFacadeLocal promotionFacade;
    
    private Promotions selected = new Promotions();
    private boolean editMode = false;
    private boolean showForm = false;
    private String searchKeyword;
    private String statusFilter = "all"; // all, active, inactive, expired
    private int currentPage = 1;
    private int pageSize = 10;
    
    public PromotionMBean() {
    }
    
    // ========== LIST METHODS ==========
    
    public List<Promotions> getItems() {
        try {
            List<Promotions> all = promotionFacade.findAll();
            
            if (all == null) {
                return new ArrayList<>();
            }
            
            List<Promotions> filtered = new ArrayList<>();
            
            for (Promotions p : all) {
                // Apply search filter
                if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                    String keyword = searchKeyword.trim().toLowerCase();
                    boolean matches = false;
                    
                    if (p.getCode() != null && p.getCode().toLowerCase().contains(keyword)) {
                        matches = true;
                    }
                    if (p.getDescription() != null && p.getDescription().toLowerCase().contains(keyword)) {
                        matches = true;
                    }
                    if (p.getPromotionID() != null && String.valueOf(p.getPromotionID()).contains(keyword)) {
                        matches = true;
                    }
                    
                    if (!matches) continue;
                }
                
                // Apply status filter
                if (!"all".equals(statusFilter)) {
                    Date now = new Date();
                    boolean isActive = p.getIsActive() != null && p.getIsActive();
                    boolean isExpired = p.getEndDate() != null && p.getEndDate().before(now);
                    boolean isNotStarted = p.getStartDate() != null && p.getStartDate().after(now);
                    
                    switch (statusFilter) {
                        case "active":
                            if (!isActive || isExpired || isNotStarted) continue;
                            break;
                        case "inactive":
                            if (isActive && !isExpired && !isNotStarted) continue;
                            break;
                        case "expired":
                            if (!isExpired) continue;
                            break;
                        case "upcoming":
                            if (!isNotStarted) continue;
                            break;
                    }
                }
                
                filtered.add(p);
            }
            
            return filtered;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public List<Promotions> getPagedItems() {
        try {
            List<Promotions> base = getItems();
            
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
    
    // ========== CRUD METHODS ==========
    
    public void prepareCreate() {
        selected = new Promotions();
        selected.setIsActive(true);
        selected.setUsageCount(0);
        selected.setCreatedAt(new Date());
        editMode = false;
        showForm = true;
    }
    
    public void prepareEdit(Promotions promotion) {
        selected = promotionFacade.find(promotion.getPromotionID());
        if (selected == null) {
            addError("❌ Không tìm thấy promotion!");
            return;
        }
        editMode = true;
        showForm = true;
    }
    
    public void save() {
        try {
            if (selected.getCode() == null || selected.getCode().trim().isEmpty()) {
                addError("❌ Vui lòng nhập mã promotion!");
                return;
            }
            
            if (selected.getDiscountType() == null || selected.getDiscountType().trim().isEmpty()) {
                addError("❌ Vui lòng chọn loại giảm giá!");
                return;
            }
            
            if (selected.getDiscountValue() == null) {
                addError("❌ Vui lòng nhập giá trị giảm giá!");
                return;
            }
            
            if (selected.getStartDate() == null) {
                addError("❌ Vui lòng chọn ngày bắt đầu!");
                return;
            }
            
            if (selected.getEndDate() == null) {
                addError("❌ Vui lòng chọn ngày kết thúc!");
                return;
            }
            
            if (selected.getEndDate().before(selected.getStartDate())) {
                addError("❌ Ngày kết thúc phải sau ngày bắt đầu!");
                return;
            }
            
            // Validate discount value based on type
            if ("percentage".equalsIgnoreCase(selected.getDiscountType())) {
                if (selected.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
                    addError("❌ Phần trăm giảm giá không được vượt quá 100%!");
                    return;
                }
            }
            
            if (editMode) {
                promotionFacade.edit(selected);
                addInfo("✅ Đã cập nhật promotion thành công!");
            } else {
                if (selected.getCreatedAt() == null) {
                    selected.setCreatedAt(new Date());
                }
                promotionFacade.create(selected);
                addInfo("✅ Đã tạo promotion thành công!");
            }
            
            cancel();
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Lỗi khi lưu promotion: " + e.getMessage());
        }
    }
    
    public void delete(Promotions promotion) {
        try {
            promotionFacade.remove(promotion);
            addInfo("✅ Đã xóa promotion thành công!");
            if (selected != null && selected.getPromotionID() != null && 
                selected.getPromotionID().equals(promotion.getPromotionID())) {
                cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
            addError("❌ Lỗi khi xóa promotion: " + e.getMessage());
        }
    }
    
    public void cancel() {
        selected = new Promotions();
        editMode = false;
        showForm = false;
    }
    
    // ========== SEARCH & FILTER ==========
    
    public void performSearch() {
        currentPage = 1;
    }
    
    public void clearSearch() {
        searchKeyword = null;
        statusFilter = "all";
        currentPage = 1;
    }
    
    // ========== PAGINATION ==========
    
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
            List<Promotions> items = getItems();
            return items != null ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    // ========== UTILITY METHODS ==========
    
    public String formatAmount(BigDecimal amount) {
        if (amount == null) return "0 VND";
        return String.format("%,.0f", amount.doubleValue()) + " VND";
    }
    
    public String formatDate(Date date) {
        if (date == null) return "-";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
    
    public String getStatusText(Promotions promotion) {
        if (promotion == null) return "";
        
        Date now = new Date();
        boolean isActive = promotion.getIsActive() != null && promotion.getIsActive();
        boolean isExpired = promotion.getEndDate() != null && promotion.getEndDate().before(now);
        boolean isNotStarted = promotion.getStartDate() != null && promotion.getStartDate().after(now);
        
        if (!isActive) return "❌ Inactive";        // Không hoạt động
        if (isNotStarted) return "⏳ Upcoming";     // Sắp diễn ra
        if (isExpired) return "🔴 Expired";         // Đã hết hạn
        return "✅ Active";                         // Đang hoạt động
    }
    
    public String getStatusColor(Promotions promotion) {
        if (promotion == null) return "#666";
        
        Date now = new Date();
        boolean isActive = promotion.getIsActive() != null && promotion.getIsActive();
        boolean isExpired = promotion.getEndDate() != null && promotion.getEndDate().before(now);
        boolean isNotStarted = promotion.getStartDate() != null && promotion.getStartDate().after(now);
        
        if (!isActive) return "#dc3545";
        if (isNotStarted) return "#ffc107";
        if (isExpired) return "#6c757d";
        return "#28a745";
    }
    
    public String getDiscountTypeText(String type) {
        if (type == null) return "";
        switch (type.toLowerCase()) {
            case "percentage": return "Phần trăm (%)";
            case "fixed": return "Số tiền cố định";
            default: return type;
        }
    }
    
    // ========== HELPER METHODS ==========
    
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }
    
    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
    
    // ========== GETTERS AND SETTERS ==========
    
    public Promotions getSelected() {
        return selected;
    }
    
    public void setSelected(Promotions selected) {
        this.selected = selected;
    }
    
    public boolean isEditMode() {
        return editMode;
    }
    
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }
    
    public boolean isShowForm() {
        return showForm;
    }
    
    public void setShowForm(boolean showForm) {
        this.showForm = showForm;
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
