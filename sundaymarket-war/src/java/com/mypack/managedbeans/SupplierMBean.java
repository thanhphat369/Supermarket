//package com.mypack.managedbeans;
//
//import jakarta.ejb.EJB;
//import jakarta.enterprise.context.SessionScoped;
//import jakarta.faces.application.FacesMessage;
//import jakarta.faces.context.FacesContext;
//import jakarta.inject.Named;
//import java.io.Serializable;
//import java.util.List;
//import mypack.entity.Supplier;
//import mypack.sessionbean.SupplierFacadeLocal;
//
//@Named(value = "supplierMBean")
//@SessionScoped
//public class SupplierMBean implements Serializable {
//
//    @EJB
//    private SupplierFacadeLocal supplierFacade;
//    
//    private Supplier selected = new Supplier();
//    private boolean editMode = false;
//    private String searchKeyword;
//    private int currentPage = 1;
//    private int pageSize = 10;
//    
//    // Lấy danh sách supplier
//    public List<Supplier> getItems() {
//        try {
//            List<Supplier> all = supplierFacade.findAll();
//            
//            if (all == null) {
//                return new java.util.ArrayList<>();
//            }
//            
//            // Áp dụng tìm kiếm nếu có keyword
//            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
//                String keyword = searchKeyword.trim().toLowerCase();
//                return all.stream()
//                        .filter(supplier -> 
//                            (supplier.getSupplierName() != null && supplier.getSupplierName().toLowerCase().contains(keyword)) ||
//                            (supplier.getPhoneContact() != null && supplier.getPhoneContact().contains(keyword)) ||
//                            (supplier.getAddress() != null && supplier.getAddress().toLowerCase().contains(keyword))
//                        )
//                        .collect(java.util.stream.Collectors.toList());
//            }
//            
//            return all;
//        } catch (Exception e) {
//            System.err.println("SupplierMBean.getItems() - Error: " + e.getMessage());
//            e.printStackTrace();
//            return new java.util.ArrayList<>();
//        }
//    }
//    
//    // Lấy danh sách supplier phân trang
//    public List<Supplier> getPagedItems() {
//        try {
//            List<Supplier> base = getItems();
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
//            System.err.println("SupplierMBean.getPagedItems() - Error: " + e.getMessage());
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
//            List<Supplier> items = getItems();
//            return items != null ? items.size() : 0;
//        } catch (Exception e) {
//            return 0;
//        }
//    }
//    
//    // Tạo mới
//    public void prepareCreate() {
//        selected = new Supplier();
//        editMode = false;
//    }
//    
//    // Chỉnh sửa
//    public void prepareEdit(Supplier s) {
//        selected = s;
//        editMode = true;
//    }
//    
//    // Delete
//    public void delete(Supplier s) {
//        try {
//            // Check if supplier is being used
//            if (s.getPurchaseOrderCollection() != null && !s.getPurchaseOrderCollection().isEmpty()) {
//                addErr("⚠️ Cannot delete this supplier because there are related purchase orders!");
//                return;
//            }
//            
//            if (s.getStockTransactionsCollection() != null && !s.getStockTransactionsCollection().isEmpty()) {
//                addErr("⚠️ Cannot delete this supplier because there are related stock transactions!");
//                return;
//            }
//            
//            supplierFacade.remove(s);
//            addInfo("✅ Supplier deleted!");
//            
//            if (selected != null && selected.getSupplierID() != null && selected.getSupplierID().equals(s.getSupplierID())) {
//                prepareCreate();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            addErr("❌ Delete failed: " + e.getMessage());
//        }
//    }
//    
//    // Save
//    public void save() {
//        try {
//            // Validate required fields
//            if (selected.getSupplierName() == null || selected.getSupplierName().trim().isEmpty()) {
//                addErr("⚠️ Please enter supplier name!");
//                return;
//            }
//            
//            boolean isNew = selected.getSupplierID() == null;
//            if (isNew) {
//                supplierFacade.create(selected);
//                addInfo("✅ New supplier added!");
//            } else {
//                supplierFacade.edit(selected);
//                addInfo("✅ Supplier information updated!");
//            }
//            
//            prepareCreate();
//        } catch (Exception e) {
//            e.printStackTrace();
//            addErr("❌ Save failed: " + e.getMessage());
//        }
//    }
//    
//    // Getters and Setters
//    public Supplier getSelected() {
//        return selected;
//    }
//    
//    public void setSelected(Supplier selected) {
//        this.selected = selected;
//    }
//    
//    public boolean isEditMode() {
//        return editMode;
//    }
//    
//    public void setEditMode(boolean editMode) {
//        this.editMode = editMode;
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
