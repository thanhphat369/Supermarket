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
import mypack.entity.OrderDetails;
import mypack.entity.Payment;
import mypack.entity.Product;
import mypack.entity.SystemSettings;
import mypack.entity.User;
import mypack.sessionbean.DeliveryFacadeLocal;
import mypack.sessionbean.Order1FacadeLocal;
import mypack.sessionbean.OrderDetailsFacadeLocal;
import mypack.sessionbean.PaymentFacadeLocal;
import mypack.sessionbean.SystemSettingsFacadeLocal;
import mypack.sessionbean.UserFacadeLocal;

/**
 * Managed Bean for Admin Finance Management
 * Allows admin to view COD submission history from all shippers
 */
@Named(value = "adminFinanceMBean")
@SessionScoped
public class AdminFinanceMBean implements Serializable {

    @EJB
    private DeliveryFacadeLocal deliveryFacade;
    
    @EJB
    private Order1FacadeLocal orderFacade;
    
    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;
    
    @EJB
    private PaymentFacadeLocal paymentFacade;
    
    @EJB
    private UserFacadeLocal userFacade;
    
    @EJB
    private SystemSettingsFacadeLocal systemSettingsFacade;
    
    // Setting keys for System_Settings table
    private static final String SETTING_KEY_SHIPPING_FEE = "DEFAULT_SHIPPING_FEE";
    private static final String SETTING_KEY_ADMIN_PERCENTAGE = "ADMIN_COMMISSION_PERCENT";
    
    // Filters
    private Integer selectedShipperId = 0; // 0 = all shippers
    private Integer monthFilter = 0; // 0 = all months
    private Integer yearFilter = 0; // 0 = all years
    private String statusFilter = "all"; // all, submitted, pending
    
    // Pagination
    private int currentPage = 1;
    private int pageSize = 20;
    
    // COD Detail View
    private CODSubmissionRecord selectedCODRecord = null;
    private Order1 selectedCODOrder = null;
    private boolean showCODDetail = false;
    
    // Statistics
    private int totalPendingCOD = 0;
    private int totalSubmittedCOD = 0;
    private int totalCODThisMonth = 0;
    private int totalShipperShippingFeeIncome = 0; // Total shipping fee income for shippers (after admin percentage)
    private int totalRevenue = 0; // Total revenue from all completed orders (COD + Online Payment)
    private int totalOnlinePayment = 0; // Total online payment from all completed orders
    private int adminNetProfit = 0; // Admin net profit = Revenue - Shipping Fee to Pay Shippers
    
    // Default values (fallback if database values are not set)
    private static final int DEFAULT_SHIPPING_FEE_VALUE = 3000; // Default 3,000 VND
    private static final int DEFAULT_ADMIN_PERCENTAGE_VALUE = 10; // Default 10%
    
    // Static variables (cache for performance, loaded from database)
    private static int defaultShippingFee = DEFAULT_SHIPPING_FEE_VALUE;
    private static int adminPercentage = DEFAULT_ADMIN_PERCENTAGE_VALUE;
    private static boolean configLoaded = false; // Flag to track if config has been loaded from DB
    
    // Shipping fee input (session variable)
    private int shippingFeeInput = DEFAULT_SHIPPING_FEE_VALUE;
    
    // Admin percentage input (session variable)
    private int adminPercentageInput = 10;
    
    public AdminFinanceMBean() {
        Calendar cal = Calendar.getInstance();
        yearFilter = cal.get(Calendar.YEAR);
        monthFilter = cal.get(Calendar.MONTH) + 1;
    }
    
    // Get all shippers
    public List<User> getAllShippers() {
        try {
            List<User> allUsers = userFacade.findAll();
            if (allUsers == null) {
                return new ArrayList<>();
            }
            
            return allUsers.stream()
                    .filter(user -> user.getRoleID() != null && 
                                   "shipper".equalsIgnoreCase(user.getRoleID().getRoleName()) &&
                                   user.getIsActive())
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Get order submission history (both COD and ONLINE)
    public List<CODSubmissionRecord> getCODSubmissions() {
        try {
            List<CODSubmissionRecord> records = new ArrayList<>();
            
            Calendar monthStart = null;
            Calendar monthEnd = null;
            if (monthFilter != null && yearFilter != null && monthFilter > 0 && yearFilter > 0) {
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
            
            // Get all orders with payments (both COD and ONLINE)
            List<Order1> allOrders = orderFacade.findAll();
            for (Order1 order : allOrders) {
                if (order == null) continue;
                
                Payment payment = paymentFacade.findByOrder(order);
                if (payment == null) continue;
                
                // Include both COD and ONLINE orders
                String paymentMethod = payment.getPaymentMethod();
                if (paymentMethod == null || (!"COD".equalsIgnoreCase(paymentMethod) && !"ONLINE".equalsIgnoreCase(paymentMethod))) {
                    continue;
                }
                
                boolean isCOD = "COD".equalsIgnoreCase(paymentMethod);
                boolean isONLINE = "ONLINE".equalsIgnoreCase(paymentMethod);
                
                // For COD: must be delivered
                // For ONLINE: must be paid (payment status = "paid")
                if (isCOD) {
                    // COD orders: check delivery status
                    Delivery delivery = null;
                    List<Delivery> deliveries = new ArrayList<>(order.getDeliveryCollection());
                    if (!deliveries.isEmpty()) {
                        delivery = deliveries.get(0); // Get first delivery
                    }
                    
                    if (delivery == null) continue;
                    
                    String deliveryStatus = delivery.getStatus() != null ? delivery.getStatus().toLowerCase() : "";
                    if (!"delivered".equals(deliveryStatus)) {
                        continue;
                    }
                    
                    // Filter by shipper (userID in Delivery is the shipper)
                    if (selectedShipperId != null && selectedShipperId > 0) {
                        if (delivery.getUserID() == null || !delivery.getUserID().getUserID().equals(selectedShipperId)) {
                            continue;
                        }
                    }
                    
                    // Only include deliveries where userID has shipper role
                    if (delivery.getUserID() == null || delivery.getUserID().getRoleID() == null || 
                        !"shipper".equalsIgnoreCase(delivery.getUserID().getRoleID().getRoleName())) {
                        continue;
                    }
                } else if (isONLINE) {
                    // ONLINE orders: must be paid
                    String paymentStatus = payment.getPaymentStatus() != null ? payment.getPaymentStatus().toLowerCase() : "";
                    if (!"paid".equals(paymentStatus)) {
                        continue;
                    }
                    
                    // For ONLINE orders, shipper filter doesn't apply (no delivery)
                    // But we still show them even if shipper filter is set (show all ONLINE orders)
                }
                
                // Filter by status (for COD: submitted/pending, for ONLINE: always "paid")
                boolean isSubmitted = false;
                if (isCOD) {
                    isSubmitted = payment.getTransactionID() != null && 
                                 payment.getTransactionID().startsWith("SUBMIT_");
                } else if (isONLINE) {
                    isSubmitted = true; // ONLINE orders are always considered "submitted" (paid)
                }
                
                if ("submitted".equals(statusFilter) && !isSubmitted) {
                    continue;
                }
                if ("pending".equals(statusFilter) && isSubmitted) {
                    continue;
                }
                
                // Date filter
                Date checkDate = null;
                if (isCOD) {
                    Delivery delivery = null;
                    List<Delivery> deliveries = new ArrayList<>(order.getDeliveryCollection());
                    if (!deliveries.isEmpty()) {
                        delivery = deliveries.get(0);
                    }
                    if (delivery != null) {
                        checkDate = isSubmitted ? 
                            (payment.getUpdatedAt() != null ? payment.getUpdatedAt() : delivery.getDeliveredAt()) :
                            (delivery.getDeliveredAt() != null ? delivery.getDeliveredAt() : delivery.getUpdatedAt());
                    }
                } else if (isONLINE) {
                    // For ONLINE: use payment date or order date
                    checkDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : 
                               (payment.getUpdatedAt() != null ? payment.getUpdatedAt() : order.getOrderDate());
                }
                
                if (monthStart != null && monthEnd != null && checkDate != null) {
                    Calendar checkCal = Calendar.getInstance();
                    checkCal.setTime(checkDate);
                    if (checkCal.before(monthStart) || checkCal.after(monthEnd)) {
                        continue;
                    }
                }
                
                CODSubmissionRecord record = new CODSubmissionRecord();
                record.setOrderID(order.getOrderID());
                record.setOrderAmount(order.getTotalAmount());
                
                if (isCOD) {
                    Delivery delivery = null;
                    List<Delivery> deliveries = new ArrayList<>(order.getDeliveryCollection());
                    if (!deliveries.isEmpty()) {
                        delivery = deliveries.get(0);
                    }
                    // userID in Delivery is the shipper (User with shipper role)
                    record.setShipper(delivery != null ? delivery.getUserID() : null);
                    record.setDeliveredDate(delivery != null ? 
                        (delivery.getDeliveredAt() != null ? delivery.getDeliveredAt() : delivery.getUpdatedAt()) : null);
                } else if (isONLINE) {
                    // ONLINE orders don't have shipper
                    record.setShipper(null);
                    record.setDeliveredDate(payment.getPaymentDate() != null ? payment.getPaymentDate() : 
                                           (payment.getUpdatedAt() != null ? payment.getUpdatedAt() : order.getOrderDate()));
                }
                
                record.setCustomer(order.getUserID());
                record.setSubmitted(isSubmitted);
                record.setSubmittedDate(isSubmitted ? (payment.getUpdatedAt() != null ? payment.getUpdatedAt() : payment.getPaymentDate()) : null);
                record.setReferenceCode(isSubmitted && payment.getTransactionID() != null ? 
                    (isCOD ? extractReferenceCode(payment.getTransactionID()) : payment.getTransactionID()) : null);
                record.setPaymentID(payment.getPaymentID());
                record.setPaymentMethod(paymentMethod); // Store payment method (COD or ONLINE)
                
                records.add(record);
            }
            
            // Sort by date (newest first)
            records.sort((a, b) -> {
                Date dateA = a.getSubmittedDate() != null ? a.getSubmittedDate() : a.getDeliveredDate();
                Date dateB = b.getSubmittedDate() != null ? b.getSubmittedDate() : b.getDeliveredDate();
                if (dateA == null && dateB == null) return 0;
                if (dateA == null) return 1;
                if (dateB == null) return -1;
                return dateB.compareTo(dateA);
            });
            
            return records;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private String extractReferenceCode(String transactionID) {
        if (transactionID == null || !transactionID.startsWith("SUBMIT_")) {
            return null;
        }
        String[] parts = transactionID.split("_");
        if (parts.length >= 2) {
            return parts[1];
        }
        return transactionID;
    }
    
    // Get paginated submissions
    public List<CODSubmissionRecord> getPagedSubmissions() {
        List<CODSubmissionRecord> all = getCODSubmissions();
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, all.size());
        
        if (start >= all.size()) {
            currentPage = 1;
            start = 0;
            end = Math.min(pageSize, all.size());
        }
        
        if (start < 0 || start >= end || end > all.size()) {
            return new ArrayList<>();
        }
        
        return all.subList(start, end);
    }
    
    // View order detail (both COD and ONLINE)
    public void viewCODDetail(CODSubmissionRecord record) {
        try {
            selectedCODRecord = record;
            if (record != null && record.getOrderID() != null) {
                selectedCODOrder = orderFacade.find(record.getOrderID());
                // Force load order details
                if (selectedCODOrder != null && selectedCODOrder.getOrderDetailsCollection() != null) {
                    selectedCODOrder.getOrderDetailsCollection().size();
                }
            }
            showCODDetail = true;
        } catch (Exception e) {
            e.printStackTrace();
            selectedCODRecord = null;
            selectedCODOrder = null;
            showCODDetail = false;
        }
    }
    
    // Close order detail
    public void closeCODDetail() {
        selectedCODRecord = null;
        selectedCODOrder = null;
        showCODDetail = false;
    }
    
    // Get order details for selected COD order
    public List<OrderDetails> getCODOrderDetails() {
        if (selectedCODOrder == null) {
            return new ArrayList<>();
        }
        try {
            List<OrderDetails> allDetails = orderDetailsFacade.findAll();
            List<OrderDetails> result = new ArrayList<>();
            for (OrderDetails detail : allDetails) {
                if (detail.getOrderID() != null && detail.getOrderID().getOrderID().equals(selectedCODOrder.getOrderID())) {
                    result.add(detail);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Get original shipping fee
    public int getOriginalShippingFee() {
        if (selectedCODOrder == null) return 0;
        return selectedCODOrder.getShippingFee() != null ? selectedCODOrder.getShippingFee() : 0;
    }
    
    // Get admin shipping fee share
    public int getAdminShippingFeeShare() {
        int originalFee = getOriginalShippingFee();
        if (originalFee == 0) return 0;
        return (int) Math.round(originalFee * adminPercentage / 100.0);
    }
    
    // Get shipper shipping fee income
    public int getShipperShippingFeeIncome() {
        int originalFee = getOriginalShippingFee();
        if (originalFee == 0) return 0;
        return (int) Math.round(originalFee * (100.0 - adminPercentage) / 100.0);
    }
    
    // Get subtotal for selected order
    public int getCODOrderSubtotal() {
        if (selectedCODOrder == null) return 0;
        int shippingFee = getOriginalShippingFee();
        return selectedCODOrder.getTotalAmount() - shippingFee;
    }
    
    // Refresh statistics
    public void refreshStatistics() {
        try {
            totalPendingCOD = 0;
            totalSubmittedCOD = 0;
            totalCODThisMonth = 0;
            totalShipperShippingFeeIncome = 0;
            totalRevenue = 0;
            totalOnlinePayment = 0;
            adminNetProfit = 0;
            
            Calendar monthStart = Calendar.getInstance();
            monthStart.set(Calendar.DAY_OF_MONTH, 1);
            monthStart.set(Calendar.HOUR_OF_DAY, 0);
            monthStart.set(Calendar.MINUTE, 0);
            monthStart.set(Calendar.SECOND, 0);
            monthStart.set(Calendar.MILLISECOND, 0);
            
            List<Delivery> allDeliveries = deliveryFacade.findAll();
            for (Delivery d : allDeliveries) {
                Order1 order = d.getOrderID();
                if (order == null) continue;
                
                Payment payment = paymentFacade.findByOrder(order);
                if (payment == null) continue;
                
                if (!"COD".equalsIgnoreCase(payment.getPaymentMethod())) {
                    continue;
                }
                
                String status = d.getStatus() != null ? d.getStatus().toLowerCase() : "";
                if (!"delivered".equals(status)) {
                    continue;
                }
                
                int amount = order.getTotalAmount();
                boolean isSubmitted = payment.getTransactionID() != null && 
                                     payment.getTransactionID().startsWith("SUBMIT_");
                
                if (!isSubmitted) {
                    totalPendingCOD += amount;
                } else {
                    totalSubmittedCOD += amount;
                }
                
                // COD This Month: Use submit date if submitted, otherwise use delivery date
                Date checkDate = null;
                if (isSubmitted && payment.getUpdatedAt() != null) {
                    // If submitted, use submission date (payment.updatedAt when TransactionID was set)
                    checkDate = payment.getUpdatedAt();
                } else {
                    // If not submitted, use delivery date
                    checkDate = d.getDeliveredAt() != null ? d.getDeliveredAt() : d.getUpdatedAt();
                }
                
                if (checkDate != null) {
                    Calendar checkCal = Calendar.getInstance();
                    checkCal.setTime(checkDate);
                    if (checkCal.after(monthStart) || isSameDay(checkCal, Calendar.getInstance())) {
                        totalCODThisMonth += amount;
                    }
                }
                
                // Calculate shipper shipping fee income (shipping fee - admin percentage)
                // Only calculate for delivered COD orders
                Integer shippingFee = order.getShippingFee();
                if (shippingFee != null && shippingFee > 0) {
                    // Admin gets adminPercentage% of shipping fee
                    // Shipper gets (100 - adminPercentage)% of shipping fee
                    // Example: If shippingFee = 3000 and adminPercentage = 10%
                    //   Admin gets: 3000 * 10 / 100 = 300
                    //   Shipper gets: 3000 * (100 - 10) / 100 = 3000 * 90 / 100 = 2700
                    double shipperPercentage = 100.0 - adminPercentage;
                    int shipperFee = (int) Math.round(shippingFee * shipperPercentage / 100.0);
                    totalShipperShippingFeeIncome += shipperFee;
                }
            }
            
            // Calculate total revenue from all completed orders (COD + Online Payment)
            List<Order1> allOrders = orderFacade.findAll();
            for (Order1 order : allOrders) {
                if (order == null) continue;
                
                // Only count completed orders
                String orderStatus = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
                if (!"completed".equalsIgnoreCase(orderStatus)) {
                    continue;
                }
                
                Payment payment = paymentFacade.findByOrder(order);
                if (payment == null) continue;
                
                // Only count paid orders
                String paymentStatus = payment.getPaymentStatus() != null ? payment.getPaymentStatus().toLowerCase() : "";
                if (!"paid".equalsIgnoreCase(paymentStatus)) {
                    continue;
                }
                
                // Add total amount to revenue (includes shipping fee)
                totalRevenue += order.getTotalAmount();
                
                // Calculate total online payment separately
                String paymentMethod = payment.getPaymentMethod();
                if (paymentMethod != null && "ONLINE".equalsIgnoreCase(paymentMethod)) {
                    totalOnlinePayment += order.getTotalAmount();
                }
            }
            
            // Calculate admin net profit = Revenue - Shipping Fee to Pay Shippers
            adminNetProfit = totalRevenue - totalShipperShippingFeeIncome;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    // Format amount
    public String formatAmount(int amount) {
        return String.format("%,d VND", amount);
    }
    
    // Format date
    public String formatDate(Date date) {
        if (date == null) return "-";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
    
    // Get product image URL (first image)
    public String getProductImageUrl(Product product) {
        if (product == null || product.getImageURL() == null || product.getImageURL().isEmpty()) {
            return null;
        }
        
        try {
            // Parse JSON array (similar to ProductMBean)
            String imageURL = product.getImageURL().trim();
            List<String> fileNames = new ArrayList<>();
            
            if (imageURL.startsWith("[") && imageURL.endsWith("]")) {
                // JSON array format
                String jsonContent = imageURL.substring(1, imageURL.length() - 1);
                String[] parts = jsonContent.split(",");
                for (String part : parts) {
                    String trimmed = part.trim().replace("\"", "").replace("'", "");
                    if (!trimmed.isEmpty()) {
                        if (trimmed.contains("\\") || trimmed.contains("/")) {
                            java.io.File file = new java.io.File(trimmed);
                            fileNames.add(file.getName());
                        } else {
                            fileNames.add(trimmed);
                        }
                    }
                }
            } else {
                // Single filename
                String fileName = imageURL;
                if (imageURL.contains("\\") || imageURL.contains("/")) {
                    java.io.File file = new java.io.File(imageURL);
                    fileName = file.getName();
                }
                fileNames.add(fileName);
            }
            
            if (fileNames.isEmpty()) {
                return null;
            }
            
            // Get first image
            String fileName = fileNames.get(0);
            FacesContext facesContext = FacesContext.getCurrentInstance();
            String contextPath = "";
            if (facesContext != null) {
                contextPath = facesContext.getExternalContext().getRequestContextPath();
            }
            
            long cacheBuster = System.currentTimeMillis() % 1000000;
            return contextPath + "/images/product/" + fileName + "?v=" + cacheBuster;
        } catch (Exception e) {
            System.err.println("Error getting product image URL: " + e.getMessage());
            return null;
        }
    }
    
    // Load configuration from database
    private void loadConfigurationFromDatabase() {
        try {
            // Load shipping fee
            SystemSettings shippingFeeSetting = systemSettingsFacade.findBySettingKey(SETTING_KEY_SHIPPING_FEE);
            if (shippingFeeSetting != null && shippingFeeSetting.getSettingValue() != null) {
                defaultShippingFee = shippingFeeSetting.getSettingValue().intValue();
            } else {
                // If not found, create default setting
                defaultShippingFee = DEFAULT_SHIPPING_FEE_VALUE;
                createDefaultSetting(SETTING_KEY_SHIPPING_FEE, new java.math.BigDecimal(defaultShippingFee));
            }
            
            // Load admin percentage
            SystemSettings adminPercentageSetting = systemSettingsFacade.findBySettingKey(SETTING_KEY_ADMIN_PERCENTAGE);
            if (adminPercentageSetting != null && adminPercentageSetting.getSettingValue() != null) {
                adminPercentage = adminPercentageSetting.getSettingValue().intValue();
            } else {
                // If not found, create default setting
                adminPercentage = DEFAULT_ADMIN_PERCENTAGE_VALUE;
                createDefaultSetting(SETTING_KEY_ADMIN_PERCENTAGE, new java.math.BigDecimal(adminPercentage));
            }
            
            configLoaded = true;
        } catch (Exception e) {
            System.err.println("Error loading configuration from database: " + e.getMessage());
            e.printStackTrace();
            // Use defaults on error
            defaultShippingFee = DEFAULT_SHIPPING_FEE_VALUE;
            adminPercentage = DEFAULT_ADMIN_PERCENTAGE_VALUE;
            configLoaded = true;
        }
    }
    
    // Create default setting if not exists
    private void createDefaultSetting(String settingKey, java.math.BigDecimal defaultValue) {
        try {
            SystemSettings setting = new SystemSettings();
            setting.setSettingKey(settingKey);
            setting.setSettingValue(defaultValue);
            setting.setUpdatedAt(new Date());
            // Updated_By can be null for default settings
            systemSettingsFacade.create(setting);
        } catch (Exception e) {
            System.err.println("Error creating default setting " + settingKey + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Shipping Fee Management
    @jakarta.annotation.PostConstruct
    public void initShippingFee() {
        // Load configuration from database (only once)
        if (!configLoaded) {
            loadConfigurationFromDatabase();
        }
        
        // Initialize input fields with current values
        shippingFeeInput = defaultShippingFee;
        adminPercentageInput = adminPercentage;
    }
    
    public void saveShippingFeeConfiguration() {
        boolean hasError = false;
        
        // Validate shipping fee
        if (shippingFeeInput < 0) {
            addErr("❌ Shipping fee must be >= 0");
            hasError = true;
        }

        // Validate admin percentage
        if (adminPercentageInput < 0 || adminPercentageInput > 100) {
            addErr("❌ Admin percentage must be between 0 and 100");
            hasError = true;
        }
        
        if (hasError) {
            return;
        }
        
        // Save to database
        try {
            // Get current user for Updated_By field
            FacesContext facesContext = FacesContext.getCurrentInstance();
            User currentUser = null;
            if (facesContext != null) {
                jakarta.el.ELContext elContext = facesContext.getELContext();
                jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
                jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", com.mypack.managedbeans.LoginMBean.class);
                com.mypack.managedbeans.LoginMBean loginMBean = (com.mypack.managedbeans.LoginMBean) ve.getValue(elContext);
                if (loginMBean != null) {
                    currentUser = loginMBean.getCurrentUser();
                }
            }
            
            // Save shipping fee
            SystemSettings shippingFeeSetting = systemSettingsFacade.findBySettingKey(SETTING_KEY_SHIPPING_FEE);
            if (shippingFeeSetting == null) {
                shippingFeeSetting = new SystemSettings();
                shippingFeeSetting.setSettingKey(SETTING_KEY_SHIPPING_FEE);
            }
            shippingFeeSetting.setSettingValue(new java.math.BigDecimal(shippingFeeInput));
            shippingFeeSetting.setUpdatedAt(new Date());
            shippingFeeSetting.setUpdatedBy(currentUser);
            if (shippingFeeSetting.getSettingID() == null) {
                systemSettingsFacade.create(shippingFeeSetting);
            } else {
                systemSettingsFacade.edit(shippingFeeSetting);
            }
            
            // Save admin percentage
            SystemSettings adminPercentageSetting = systemSettingsFacade.findBySettingKey(SETTING_KEY_ADMIN_PERCENTAGE);
            if (adminPercentageSetting == null) {
                adminPercentageSetting = new SystemSettings();
                adminPercentageSetting.setSettingKey(SETTING_KEY_ADMIN_PERCENTAGE);
            }
            adminPercentageSetting.setSettingValue(new java.math.BigDecimal(adminPercentageInput));
            adminPercentageSetting.setUpdatedAt(new Date());
            adminPercentageSetting.setUpdatedBy(currentUser);
            if (adminPercentageSetting.getSettingID() == null) {
                systemSettingsFacade.create(adminPercentageSetting);
            } else {
                systemSettingsFacade.edit(adminPercentageSetting);
            }
            
            // Update static variables (cache) - this ensures all sessions get the new value
            defaultShippingFee = shippingFeeInput;
            adminPercentage = adminPercentageInput;
            configLoaded = true; // Ensure flag is set
            
            // Force reload to ensure consistency
            reloadConfigurationFromDatabase();
            
            addInfo("✅ Configuration saved successfully! Shipping Fee: " + formatAmount(defaultShippingFee) + ", Admin Percentage: " + adminPercentage + "%");
        } catch (Exception e) {
            System.err.println("Error saving configuration: " + e.getMessage());
            e.printStackTrace();
            addErr("❌ Error saving configuration: " + e.getMessage());
        }
    }
    
    // Keep old methods for backward compatibility (if needed)
    public void saveShippingFee() {
        saveShippingFeeConfiguration();
    }
    
    public void saveAdminPercentage() {
        saveShippingFeeConfiguration();
    }
    
    public int getDefaultShippingFee() {
        // Reload from database to ensure fresh value
        if (!configLoaded) {
            loadConfigurationFromDatabase();
        }
        return defaultShippingFee;
    }
    
    // Static getter for other beans to access
    // Note: This method reloads from database to ensure fresh value
    public static int getDefaultShippingFeeStatic() {
        // Try to reload from database if possible
        // Since this is static, we can't access EJB directly
        // So we use the cached value, but it should be updated when admin saves config
        // For better reliability, we should reload from database
        try {
            // Use a helper to reload from database
            // Since we can't access EJB in static method, we'll use a workaround
            // The value should be updated when saveShippingFeeConfiguration() is called
            if (!configLoaded) {
                return DEFAULT_SHIPPING_FEE_VALUE;
            }
            return defaultShippingFee;
        } catch (Exception e) {
            return DEFAULT_SHIPPING_FEE_VALUE;
        }
    }
    
    // Instance method to reload config from database (for refreshing cache)
    public void reloadConfigurationFromDatabase() {
        configLoaded = false; // Reset flag to force reload
        loadConfigurationFromDatabase();
    }
    
    public int getShippingFeeInput() {
        return shippingFeeInput;
    }
    
    public void setShippingFeeInput(int shippingFeeInput) {
        this.shippingFeeInput = shippingFeeInput;
    }
    
    public int getAdminPercentage() {
        return getAdminPercentageStatic();
    }
    
    // Static getter for other beans to access
    // Note: This method returns cached value. For fresh load, use instance method.
    public static int getAdminPercentageStatic() {
        // If config not loaded, return default (should be loaded via @PostConstruct)
        if (!configLoaded) {
            return DEFAULT_ADMIN_PERCENTAGE_VALUE;
        }
        return adminPercentage;
    }
    
    public int getAdminPercentageInput() {
        return adminPercentageInput;
    }
    
    public void setAdminPercentageInput(int adminPercentageInput) {
        this.adminPercentageInput = adminPercentageInput;
    }
    
    public int getTotalShipperShippingFeeIncome() {
        refreshStatistics();
        return totalShipperShippingFeeIncome;
    }
    
    public int getTotalRevenue() {
        refreshStatistics();
        return totalRevenue;
    }
    
    public int getAdminNetProfit() {
        refreshStatistics();
        return adminNetProfit;
    }
    
    // Get total COD collected from shippers (same as totalSubmittedCOD)
    public int getTotalCODCollected() {
        refreshStatistics();
        return totalSubmittedCOD;
    }
    
    // Get total online payment from all completed orders
    public int getTotalOnlinePayment() {
        refreshStatistics();
        return totalOnlinePayment;
    }
    
    // Getters for COD Detail
    public CODSubmissionRecord getSelectedCODRecord() {
        return selectedCODRecord;
    }
    
    public Order1 getSelectedCODOrder() {
        return selectedCODOrder;
    }
    
    public boolean isShowCODDetail() {
        return showCODDetail;
    }
    
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErr(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
    
    // Pagination
    public int getTotalItems() {
        return getCODSubmissions().size();
    }
    
    public int getTotalPages() {
        int total = getTotalItems();
        return total > 0 ? (int) Math.ceil((double) total / pageSize) : 1;
    }
    
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
    
    // Filter actions
    public void applyFilter() {
        currentPage = 1;
        refreshStatistics();
    }
    
    public void clearFilter() {
        selectedShipperId = 0;
        Calendar cal = Calendar.getInstance();
        yearFilter = cal.get(Calendar.YEAR);
        monthFilter = cal.get(Calendar.MONTH) + 1;
        statusFilter = "all";
        currentPage = 1;
        refreshStatistics();
    }
    
    // Getters and Setters
    public Integer getSelectedShipperId() {
        return selectedShipperId;
    }
    
    public void setSelectedShipperId(Integer selectedShipperId) {
        this.selectedShipperId = selectedShipperId;
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
    
    public int getTotalPendingCOD() {
        refreshStatistics();
        return totalPendingCOD;
    }
    
    public int getTotalSubmittedCOD() {
        refreshStatistics();
        return totalSubmittedCOD;
    }
    
    public int getTotalCODThisMonth() {
        refreshStatistics();
        return totalCODThisMonth;
    }
    
    // Inner class for COD submission record
    public static class CODSubmissionRecord implements Serializable {
        private Integer orderID;
        private int orderAmount;
        private User shipper;
        private User customer;
        private Date deliveredDate;
        private boolean submitted;
        private Date submittedDate;
        private String referenceCode;
        private Integer paymentID;
        private String paymentMethod; // COD or ONLINE
        
        // Getters and Setters
        public Integer getOrderID() { return orderID; }
        public void setOrderID(Integer orderID) { this.orderID = orderID; }
        
        public int getOrderAmount() { return orderAmount; }
        public void setOrderAmount(int orderAmount) { this.orderAmount = orderAmount; }
        
        public User getShipper() { return shipper; }
        public void setShipper(User shipper) { this.shipper = shipper; }
        
        public User getCustomer() { return customer; }
        public void setCustomer(User customer) { this.customer = customer; }
        
        public Date getDeliveredDate() { return deliveredDate; }
        public void setDeliveredDate(Date deliveredDate) { this.deliveredDate = deliveredDate; }
        
        public boolean isSubmitted() { return submitted; }
        public void setSubmitted(boolean submitted) { this.submitted = submitted; }
        
        public Date getSubmittedDate() { return submittedDate; }
        public void setSubmittedDate(Date submittedDate) { this.submittedDate = submittedDate; }
        
        public String getReferenceCode() { return referenceCode; }
        public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
        
        public Integer getPaymentID() { return paymentID; }
        public void setPaymentID(Integer paymentID) { this.paymentID = paymentID; }
        
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }
}
