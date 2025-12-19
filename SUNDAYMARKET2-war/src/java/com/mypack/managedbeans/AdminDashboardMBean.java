package com.mypack.managedbeans;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ejb.EJB;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mypack.entity.Brand;
import mypack.entity.Category;
import mypack.entity.Delivery;
import mypack.entity.Order1;
import mypack.entity.OrderDetails;
import mypack.entity.Product;
import mypack.entity.Supplier;
import mypack.entity.User;
import mypack.sessionbean.BrandFacadeLocal;
import mypack.sessionbean.CategoryFacadeLocal;
import mypack.sessionbean.DeliveryFacadeLocal;
import mypack.sessionbean.Order1FacadeLocal;
import mypack.sessionbean.OrderDetailsFacadeLocal;
import mypack.sessionbean.ProductFacadeLocal;
import mypack.sessionbean.SupplierFacadeLocal;
import mypack.sessionbean.UserFacadeLocal;

/**
 * Managed Bean for Admin Dashboard
 * Provides statistics and analytics with month/year filtering
 */
@Named(value = "adminDashboardMBean")
@SessionScoped
public class AdminDashboardMBean implements Serializable {

    @EJB
    private Order1FacadeLocal orderFacade;
    
    @EJB
    private DeliveryFacadeLocal deliveryFacade;
    
    @EJB
    private UserFacadeLocal userFacade;
    
    @EJB
    private ProductFacadeLocal productFacade;
    
    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;
    
    @EJB
    private CategoryFacadeLocal categoryFacade;
    
    @EJB
    private SupplierFacadeLocal supplierFacade;
    
    @EJB
    private BrandFacadeLocal brandFacade;
    
    // Filter
    private Integer selectedMonth;
    private Integer selectedYear;
    
    // Statistics
    private int totalOrders = 0;
    private int totalRevenue = 0;
    private int totalCustomers = 0;
    private int totalShippers = 0;
    private int totalProducts = 0;
    private int totalCategories = 0;
    private int totalSuppliers = 0;
    private int totalBrands = 0;
    
    // Order statistics by status
    private int pendingOrders = 0;
    private int processingOrders = 0;
    private int shippingOrders = 0;
    private int completedOrders = 0;
    private int cancelledOrders = 0;
    
    // Delivery statistics
    private int pendingDeliveries = 0;
    private int shippingDeliveries = 0;
    private int deliveredDeliveries = 0;
    private int failedDeliveries = 0;
    
    // Top products
    private List<ProductSales> topProducts = new ArrayList<>();
    
    @PostConstruct
    public void init() {
        // Initialize to current month/year
        Calendar cal = Calendar.getInstance();
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH) + 1;
        refreshStatistics();
    }
    
    // ========== FILTER METHODS ==========
    
    public void applyFilter() {
        refreshStatistics();
    }
    
    public void resetFilter() {
        Calendar cal = Calendar.getInstance();
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH) + 1;
        refreshStatistics();
    }
    
    // ========== STATISTICS METHODS ==========
    
    public void refreshStatistics() {
        try {
            // Get all data with null checks
            List<Order1> allOrders = orderFacade != null ? orderFacade.findAll() : new ArrayList<>();
            List<Delivery> allDeliveries = deliveryFacade != null ? deliveryFacade.findAll() : new ArrayList<>();
            List<User> allUsers = userFacade != null ? userFacade.findAll() : new ArrayList<>();
            List<Product> allProducts = productFacade != null ? productFacade.findAll() : new ArrayList<>();
            List<OrderDetails> allOrderDetails = orderDetailsFacade != null ? orderDetailsFacade.findAll() : new ArrayList<>();
            List<Category> allCategories = categoryFacade != null ? categoryFacade.findAll() : new ArrayList<>();
            List<Supplier> allSuppliers = supplierFacade != null ? supplierFacade.findAll() : new ArrayList<>();
            List<Brand> allBrands = brandFacade != null ? brandFacade.findAll() : new ArrayList<>();
            
            // Ensure lists are not null
            if (allOrders == null) allOrders = new ArrayList<>();
            if (allDeliveries == null) allDeliveries = new ArrayList<>();
            if (allUsers == null) allUsers = new ArrayList<>();
            if (allProducts == null) allProducts = new ArrayList<>();
            if (allOrderDetails == null) allOrderDetails = new ArrayList<>();
            if (allCategories == null) allCategories = new ArrayList<>();
            if (allSuppliers == null) allSuppliers = new ArrayList<>();
            if (allBrands == null) allBrands = new ArrayList<>();
            
            // ✅ IMPORTANT: Set "all time" counts FIRST (before any date filtering)
            totalProducts = allProducts.size();
            totalCategories = allCategories.size();
            totalSuppliers = allSuppliers.size();
            totalBrands = allBrands.size();
            
            // Count users by role (all time)
            totalCustomers = 0;
            totalShippers = 0;
            for (User user : allUsers) {
                if (user.getRoleID() != null) {
                    String roleName = user.getRoleID().getRoleName();
                    if ("customer".equalsIgnoreCase(roleName)) {
                        totalCustomers++;
                    } else if ("shipper".equalsIgnoreCase(roleName)) {
                        totalShippers++;
                    }
                }
            }
            
            System.out.println("AdminDashboard Stats - Products: " + totalProducts + 
                             ", Categories: " + totalCategories + 
                             ", Suppliers: " + totalSuppliers + 
                             ", Brands: " + totalBrands +
                             ", Customers: " + totalCustomers +
                             ", Shippers: " + totalShippers);
            
            // Initialize monthly counters
            totalOrders = 0;
            totalRevenue = 0;
            pendingOrders = 0;
            processingOrders = 0;
            shippingOrders = 0;
            completedOrders = 0;
            cancelledOrders = 0;
            pendingDeliveries = 0;
            shippingDeliveries = 0;
            deliveredDeliveries = 0;
            failedDeliveries = 0;
            
            // Create calendar for filter date
            Calendar filterCal = Calendar.getInstance();
            if (selectedYear != null && selectedMonth != null) {
                filterCal.set(Calendar.YEAR, selectedYear);
                filterCal.set(Calendar.MONTH, selectedMonth - 1);
                filterCal.set(Calendar.DAY_OF_MONTH, 1);
                filterCal.set(Calendar.HOUR_OF_DAY, 0);
                filterCal.set(Calendar.MINUTE, 0);
                filterCal.set(Calendar.SECOND, 0);
                filterCal.set(Calendar.MILLISECOND, 0);
            }
            
            Calendar filterEndCal = (Calendar) filterCal.clone();
            filterEndCal.add(Calendar.MONTH, 1);
            filterEndCal.add(Calendar.DAY_OF_MONTH, -1);
            filterEndCal.set(Calendar.HOUR_OF_DAY, 23);
            filterEndCal.set(Calendar.MINUTE, 59);
            filterEndCal.set(Calendar.SECOND, 59);
            
            Date filterStart = filterCal.getTime();
            Date filterEnd = filterEndCal.getTime();
            
            // Count orders in selected month
            for (Order1 order : allOrders) {
                if (order.getOrderDate() != null) {
                    Date orderDate = order.getOrderDate();
                    if (orderDate.compareTo(filterStart) >= 0 && orderDate.compareTo(filterEnd) <= 0) {
                        totalOrders++;
                        totalRevenue += order.getTotalAmount();
                        
                        String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
                        switch (status) {
                            case "pending":
                                pendingOrders++;
                                break;
                            case "processing":
                                processingOrders++;
                                break;
                            case "shipping":
                                shippingOrders++;
                                break;
                            case "completed":
                                completedOrders++;
                                break;
                            case "cancelled":
                            case "failed":
                                cancelledOrders++;
                                break;
                        }
                    }
                }
            }
            
            // Count deliveries in selected month
            for (Delivery delivery : allDeliveries) {
                Date deliveryDate = delivery.getUpdatedAt() != null ? delivery.getUpdatedAt() : 
                                   (delivery.getDeliveredAt() != null ? delivery.getDeliveredAt() : null);
                
                if (deliveryDate != null) {
                    if (deliveryDate.compareTo(filterStart) >= 0 && deliveryDate.compareTo(filterEnd) <= 0) {
                        String status = delivery.getStatus() != null ? delivery.getStatus().toLowerCase() : "";
                        switch (status) {
                            case "assigned":
                            case "pending":
                                pendingDeliveries++;
                                break;
                            case "shipping":
                            case "picked_up":
                                shippingDeliveries++;
                                break;
                            case "delivered":
                                deliveredDeliveries++;
                                break;
                            case "failed":
                                failedDeliveries++;
                                break;
                        }
                    }
                }
            }
            
            // Calculate top products (using date filter)
            calculateTopProducts(allOrderDetails, filterStart, filterEnd);
            
        } catch (Exception e) {
            System.err.println("AdminDashboard ERROR: " + e.getMessage());
            e.printStackTrace();
            
            // ✅ Try to get basic counts even if main logic fails
            try {
                if (productFacade != null) totalProducts = productFacade.count();
                if (categoryFacade != null) totalCategories = categoryFacade.count();
                if (supplierFacade != null) totalSuppliers = supplierFacade.count();
                if (brandFacade != null) totalBrands = brandFacade.count();
            } catch (Exception ex) {
                System.err.println("AdminDashboard fallback count ERROR: " + ex.getMessage());
            }
        }
    }
    
    private void calculateTopProducts(List<OrderDetails> allOrderDetails, Date filterStart, Date filterEnd) {
        Map<Integer, ProductSales> productSalesMap = new HashMap<>();
        
        for (OrderDetails detail : allOrderDetails) {
            if (detail.getOrderID() != null && detail.getOrderID().getOrderDate() != null) {
                Date orderDate = detail.getOrderID().getOrderDate();
                if (orderDate.compareTo(filterStart) >= 0 && orderDate.compareTo(filterEnd) <= 0) {
                    if (detail.getProductID() != null) {
                        Integer productId = detail.getProductID().getProductID();
                        ProductSales sales = productSalesMap.get(productId);
                        if (sales == null) {
                            sales = new ProductSales();
                            sales.product = detail.getProductID();
                            sales.quantity = 0;
                            sales.revenue = 0;
                            productSalesMap.put(productId, sales);
                        }
                        sales.quantity += detail.getQuantity();
                        sales.revenue += detail.getUnitPrice() * detail.getQuantity();
                    }
                }
            }
        }
        
        topProducts = new ArrayList<>(productSalesMap.values());
        topProducts.sort((a, b) -> Integer.compare(b.quantity, a.quantity)); // Sort by quantity descending
        if (topProducts.size() > 10) {
            topProducts = topProducts.subList(0, 10);
        }
    }
    
    // ========== FORMAT METHODS ==========
    
    public String formatAmount(int amount) {
        return String.format("%,d", amount) + " VND";
    }
    
    public String formatDate(Date date) {
        if (date == null) return "-";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(date);
    }
    
    public String getMonthName(int month) {
        String[] months = {"", "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                          "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"};
        return month >= 1 && month <= 12 ? months[month] : "";
    }
    
    // ========== GETTERS AND SETTERS ==========
    
    public Integer getSelectedMonth() {
        return selectedMonth;
    }
    
    public void setSelectedMonth(Integer selectedMonth) {
        this.selectedMonth = selectedMonth;
    }
    
    public Integer getSelectedYear() {
        return selectedYear;
    }
    
    public void setSelectedYear(Integer selectedYear) {
        this.selectedYear = selectedYear;
    }
    
    public int getTotalOrders() {
        return totalOrders;
    }
    
    public int getTotalRevenue() {
        return totalRevenue;
    }
    
    public int getTotalCustomers() {
        // Fallback: đếm lại từ database nếu = 0
        if (totalCustomers == 0 && userFacade != null) {
            try {
                List<User> users = userFacade.findAll();
                if (users != null) {
                    for (User u : users) {
                        if (u.getRoleID() != null && "customer".equalsIgnoreCase(u.getRoleID().getRoleName())) {
                            totalCustomers++;
                        }
                    }
                }
            } catch (Exception e) { /* ignore */ }
        }
        return totalCustomers;
    }
    
    public int getTotalShippers() {
        // Fallback: đếm lại từ database nếu = 0
        if (totalShippers == 0 && userFacade != null) {
            try {
                List<User> users = userFacade.findAll();
                if (users != null) {
                    for (User u : users) {
                        if (u.getRoleID() != null && "shipper".equalsIgnoreCase(u.getRoleID().getRoleName())) {
                            totalShippers++;
                        }
                    }
                }
            } catch (Exception e) { /* ignore */ }
        }
        return totalShippers;
    }
    
    public int getTotalProducts() {
        // Fallback: lấy trực tiếp nếu = 0
        if (totalProducts == 0 && productFacade != null) {
            try {
                totalProducts = productFacade.count();
            } catch (Exception e) { /* ignore */ }
        }
        return totalProducts;
    }
    
    public int getTotalCategories() {
        if (totalCategories == 0 && categoryFacade != null) {
            try {
                totalCategories = categoryFacade.count();
            } catch (Exception e) { /* ignore */ }
        }
        return totalCategories;
    }
    
    public int getTotalSuppliers() {
        if (totalSuppliers == 0 && supplierFacade != null) {
            try {
                totalSuppliers = supplierFacade.count();
            } catch (Exception e) { /* ignore */ }
        }
        return totalSuppliers;
    }
    
    public int getTotalBrands() {
        if (totalBrands == 0 && brandFacade != null) {
            try {
                totalBrands = brandFacade.count();
            } catch (Exception e) { /* ignore */ }
        }
        return totalBrands;
    }
    
    public int getPendingOrders() {
        return pendingOrders;
    }
    
    public int getProcessingOrders() {
        return processingOrders;
    }
    
    public int getShippingOrders() {
        return shippingOrders;
    }
    
    public int getCompletedOrders() {
        return completedOrders;
    }
    
    public int getCancelledOrders() {
        return cancelledOrders;
    }
    
    public int getPendingDeliveries() {
        return pendingDeliveries;
    }
    
    public int getShippingDeliveries() {
        return shippingDeliveries;
    }
    
    public int getDeliveredDeliveries() {
        return deliveredDeliveries;
    }
    
    public int getFailedDeliveries() {
        return failedDeliveries;
    }
    
    public List<ProductSales> getTopProducts() {
        return topProducts;
    }
    
    // Inner class for product sales
    public static class ProductSales {
        public Product product;
        public int quantity;
        public int revenue;
        
        public Product getProduct() { return product; }
        public int getQuantity() { return quantity; }
        public int getRevenue() { return revenue; }
    }
}
