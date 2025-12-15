package com.mypack.managedbeans;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import mypack.entity.Order1;
import mypack.entity.OrderDetails;
import mypack.entity.ShoppingCart;
import mypack.entity.User;
import mypack.sessionbean.Order1FacadeLocal;
import mypack.sessionbean.OrderDetailsFacadeLocal;
import mypack.sessionbean.ShoppingCartFacadeLocal;

@Named(value = "paymentMBean")
@SessionScoped
public class PaymentMBean implements Serializable {

    @EJB
    private Order1FacadeLocal orderFacade;
    
    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;
    
    @EJB
    private ShoppingCartFacadeLocal shoppingCartFacade;
    
    private Order1 pendingOrder;
    private Order1 processingOrder; // Order being processed from QR code
    private int totalAmount;
    private String paymentMethod = "vnpay"; // vnpay, momo, zalopay, bank_transfer
    private String qrCodeUrl;
    private String paymentUrl;
    private boolean paymentSuccess = false;
    
    // Parameters from URL
    private Integer orderIdParam;
    private Integer amountParam;
    private String methodParam;

    // Demo Mode - For project demonstration (no real payment integration needed)
    private static final boolean DEMO_MODE = true;
    
    // VNPay Configuration (Demo - for project only, not real integration)
    private static final String VNPAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String VNPAY_TMN_CODE = "DEMO_TMN_CODE"; // Demo only
    private static final String VNPAY_HASH_SECRET = "DEMO_HASH_SECRET"; // Demo only
    private static final String VNPAY_RETURN_URL = "http://localhost:8080/sundaymarket-war/payment-callback.xhtml";

    public PaymentMBean() {
    }
    
    // Load order from URL parameters (when accessed via QR code)
    public void loadOrderFromParam() {
        try {
            if (orderIdParam != null) {
                processingOrder = orderFacade.find(orderIdParam);
                if (processingOrder != null && processingOrder.getStatus() != null && 
                    ("pending".equals(processingOrder.getStatus().toLowerCase()) || 
                     "processing".equals(processingOrder.getStatus().toLowerCase()))) {
                    // Order is valid for payment
                    System.out.println("Loaded order from param: Order ID = " + processingOrder.getOrderID());
                } else {
                    System.out.println("Order status is not valid for payment: " + 
                        (processingOrder != null ? processingOrder.getStatus() : "null"));
                    processingOrder = null;
                }
            } else {
                System.out.println("orderIdParam is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            processingOrder = null;
        }
    }
    
    // Process payment from QR code scan (Virtual Invoice Payment)
    public String processPaymentFromQR() {
        try {
            System.out.println("=== Processing Virtual Invoice Payment from QR Code ===");
            
            if (processingOrder == null) {
                System.out.println("ERROR: Processing order is null");
                addErr("⚠️ Order not found!");
                return null;
            }
            
            System.out.println("Processing payment for Order ID: " + processingOrder.getOrderID());
            System.out.println("Amount: " + (amountParam != null ? amountParam : processingOrder.getTotalAmount()));
            System.out.println("Payment Method: " + methodParam);
            
            // Simulate payment processing delay (for demo effect)
            try {
                Thread.sleep(800); // Simulate payment processing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Update order status to processing
            processingOrder.setStatus("processing");
            orderFacade.edit(processingOrder);
            System.out.println("Order status updated to: processing");
            
            // Clear cart for this user
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                jakarta.el.ELContext elContext = facesContext.getELContext();
                jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
                jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
                LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
                
                if (loginMBean != null && loginMBean.getCurrentUser() != null) {
                    jakarta.el.ValueExpression veCart = factory.createValueExpression(elContext, "#{shoppingCartMBean}", ShoppingCartMBean.class);
                    ShoppingCartMBean cartMBean = (ShoppingCartMBean) veCart.getValue(elContext);
                    
                    if (cartMBean != null) {
                        List<ShoppingCart> items = cartMBean.getCartItems();
                        int removedCount = 0;
                        for (ShoppingCart cart : items) {
                            if (cart.getUserID() != null && 
                                cart.getUserID().getUserID().equals(loginMBean.getCurrentUser().getUserID())) {
                                shoppingCartFacade.remove(cart);
                                removedCount++;
                            }
                        }
                        System.out.println("Removed " + removedCount + " items from cart");
                    }
                }
            }
            
            paymentSuccess = true;
            addInfo("✅ Payment successful! Order #" + processingOrder.getOrderID() + " is being processed.");
            
            processingOrder = null;
            orderIdParam = null;
            amountParam = null;
            methodParam = null;
            
            System.out.println("=== Virtual Invoice Payment completed successfully ===");
            return "customerorder?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in processPaymentFromQR: " + e.getMessage());
            addErr("❌ Payment processing failed: " + e.getMessage());
            return null;
        }
    }
    
    // Cancel payment from QR code
    public String cancelPaymentFromQR() {
        processingOrder = null;
        orderIdParam = null;
        amountParam = null;
        methodParam = null;
        return "index?faces-redirect=true";
    }
    
    // Initialize payment
    public String initPayment(int amount, String shipAddress) {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return null;
            }
            
            jakarta.el.ELContext elContext = facesContext.getELContext();
            jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
            jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
            LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
            
            if (loginMBean == null || loginMBean.getCurrentUser() == null) {
                addErr("⚠️ Please login to proceed with payment!");
                return null;
            }
            
            User currentUser = loginMBean.getCurrentUser();
            
            // Get cart items
            jakarta.el.ValueExpression veCart = factory.createValueExpression(elContext, "#{shoppingCartMBean}", ShoppingCartMBean.class);
            ShoppingCartMBean cartMBean = (ShoppingCartMBean) veCart.getValue(elContext);
            
            if (cartMBean == null) {
                addErr("⚠️ Cart not found!");
                return null;
            }
            
            List<ShoppingCart> items = cartMBean.getCartItems();
            if (items == null || items.isEmpty()) {
                addErr("⚠️ Cart is empty!");
                return null;
            }
            
            // Create Order first
            Order1 order = new Order1();
            order.setUserID(currentUser);
            order.setOrderDate(new Date());
            order.setTotalAmount(amount);
            order.setStatus("pending");
            orderFacade.create(order);
            
            // Create OrderDetails
            for (ShoppingCart cart : items) {
                OrderDetails orderDetail = new OrderDetails();
                orderDetail.setOrderID(order);
                orderDetail.setProductID(cart.getProductID());
                orderDetail.setQuantity(cart.getQuantity());
                orderDetail.setUnitPrice(cart.getProductID().getUnitPrice());
                orderDetail.setShipAddress(shipAddress);
                orderDetailsFacade.create(orderDetail);
            }
            
            pendingOrder = order;
            totalAmount = amount;
            
            // Generate payment URL and QR code based on method
            updatePaymentInfo();
            
            return "payment?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Payment initialization failed: " + e.getMessage());
            return null;
        }
    }
    
    // Update payment info based on selected method
    public void updatePaymentInfo() {
        if (pendingOrder == null) {
            return;
        }
        
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            String baseUrl = "";
            if (facesContext != null) {
                jakarta.servlet.http.HttpServletRequest request = 
                    (jakarta.servlet.http.HttpServletRequest) facesContext.getExternalContext().getRequest();
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();
                String contextPath = facesContext.getExternalContext().getRequestContextPath();
                baseUrl = scheme + "://" + serverName + (serverPort != 80 && serverPort != 443 ? ":" + serverPort : "") + contextPath;
            }
            
            // Generate payment URL based on method
            if ("vnpay".equals(paymentMethod)) {
                paymentUrl = generateVNPayUrl(pendingOrder, totalAmount);
                // QR code chứa URL đến trang VNPay ảo
                String vnpayPaymentUrl = generateVNPayUrl(pendingOrder, totalAmount);
                qrCodeUrl = generateQRCode(vnpayPaymentUrl);
            } else if ("momo".equals(paymentMethod)) {
                paymentUrl = generateMoMoUrl(pendingOrder, totalAmount);
                // QR code chứa URL đến trang MoMo ảo
                String momoPaymentUrl = generateMoMoUrl(pendingOrder, totalAmount);
                qrCodeUrl = generateQRCode(momoPaymentUrl);
            } else if ("bank_transfer".equals(paymentMethod)) {
                // QR code chứa thông tin chuyển khoản + URL xác nhận
                String paymentPageUrl = baseUrl + "/payment-process.xhtml?orderId=" + pendingOrder.getOrderID() + "&amount=" + totalAmount + "&method=bank";
                qrCodeUrl = generateBankQRCode(pendingOrder, totalAmount, paymentPageUrl);
                paymentUrl = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Generate VNPay payment URL (Demo mode - virtual VNPay page for project)
    private String generateVNPayUrl(Order1 order, int amount) {
        if (DEMO_MODE) {
            // Demo mode: Generate URL to virtual VNPay payment page
            try {
                FacesContext facesContext = FacesContext.getCurrentInstance();
                String baseUrl = "";
                if (facesContext != null) {
                    jakarta.servlet.http.HttpServletRequest request = 
                        (jakarta.servlet.http.HttpServletRequest) facesContext.getExternalContext().getRequest();
                    String scheme = request.getScheme();
                    String serverName = request.getServerName();
                    int serverPort = request.getServerPort();
                    String contextPath = facesContext.getExternalContext().getRequestContextPath();
                    baseUrl = scheme + "://" + serverName + (serverPort != 80 && serverPort != 443 ? ":" + serverPort : "") + contextPath;
                }
                return baseUrl + "/vnpay-payment.xhtml?orderId=" + order.getOrderID() + "&amount=" + amount;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        // Real integration code (for production)
        try {
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String vnp_TmnCode = VNPAY_TMN_CODE;
            String vnp_Amount = String.valueOf(amount * 100); // VNPay uses cents
            String vnp_CurrCode = "VND";
            String vnp_TxnRef = String.valueOf(order.getOrderID()) + "_" + System.currentTimeMillis();
            String vnp_OrderInfo = "Payment for Order #" + order.getOrderID();
            String vnp_OrderType = "other";
            String vnp_Locale = "vn";
            String vnp_ReturnUrl = VNPAY_RETURN_URL;
            String vnp_IpAddr = getClientIpAddress();
            
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(new Date());
            
            Calendar cld = Calendar.getInstance();
            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            
            Map<String, String> vnp_Params = new TreeMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", vnp_Amount);
            vnp_Params.put("vnp_CurrCode", vnp_CurrCode);
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
            vnp_Params.put("vnp_OrderType", vnp_OrderType);
            vnp_Params.put("vnp_Locale", vnp_Locale);
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
            
            StringBuilder queryUrl = new StringBuilder();
            StringBuilder hashData = new StringBuilder();
            for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
                hashData.append(entry.getKey());
                hashData.append('=');
                hashData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
                queryUrl.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
                queryUrl.append('=');
                queryUrl.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
                if (entry.getKey() != null && !entry.getKey().isEmpty()) {
                    queryUrl.append('&');
                    hashData.append('&');
                }
            }
            
            String vnp_SecureHash = hmacSHA512(VNPAY_HASH_SECRET, hashData.toString());
            queryUrl.append("vnp_SecureHash=").append(vnp_SecureHash);
            
            return VNPAY_URL + "?" + queryUrl.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Generate MoMo payment URL (Demo mode for project)
    private String generateMoMoUrl(Order1 order, int amount) {
        // Demo mode: Generate URL to virtual MoMo payment page
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            String baseUrl = "";
            if (facesContext != null) {
                jakarta.servlet.http.HttpServletRequest request = 
                    (jakarta.servlet.http.HttpServletRequest) facesContext.getExternalContext().getRequest();
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();
                String contextPath = facesContext.getExternalContext().getRequestContextPath();
                baseUrl = scheme + "://" + serverName + (serverPort != 80 && serverPort != 443 ? ":" + serverPort : "") + contextPath;
            }
            return baseUrl + "/momo-payment.xhtml?orderId=" + order.getOrderID() + "&amount=" + amount;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Generate Bank Transfer QR Code
    private String generateBankQRCode(Order1 order, int amount, String paymentPageUrl) {
        try {
            // Generate QR code for bank transfer using VietQR format
            // Format: Bank account info + amount + order ID + payment URL
            String bankAccount = "1234567890";
            String bankName = "Vietcombank";
            String accountName = "SUNDAY MARKET";
            String content = "Order #" + order.getOrderID();
            
            // VietQR format: bank|account|name|amount|content|paymentUrl
            String qrData = String.format("%s|%s|%s|%d|%s|%s",
                bankName, bankAccount, accountName, amount, content, paymentPageUrl);
            
            return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + 
                   URLEncoder.encode(qrData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Generate QR Code from URL
    private String generateQRCode(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + 
                   URLEncoder.encode(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // HMAC SHA512 for VNPay
    private String hmacSHA512(String key, String data) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(dataBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    // Get client IP address
    private String getClientIpAddress() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            jakarta.servlet.http.HttpServletRequest request = 
                (jakarta.servlet.http.HttpServletRequest) facesContext.getExternalContext().getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        }
        return "127.0.0.1";
    }
    
    // Confirm payment (virtual payment for testing)
    public String confirmPayment() {
        try {
            System.out.println("=== PaymentMBean.confirmPayment() ===");
            
            if (pendingOrder == null) {
                addErr("⚠️ No pending order found!");
                return null;
            }
            
            System.out.println("Processing payment for Order ID: " + pendingOrder.getOrderID());
            System.out.println("Amount: " + totalAmount);
            System.out.println("Payment Method: " + paymentMethod);
            
            // Simulate payment processing delay (for demo)
            try {
                Thread.sleep(500); // Simulate payment processing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Update order status to processing
            pendingOrder.setStatus("processing");
            orderFacade.edit(pendingOrder);
            System.out.println("Order status updated to: processing");
            
            // Clear cart
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                jakarta.el.ELContext elContext = facesContext.getELContext();
                jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
                jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{shoppingCartMBean}", ShoppingCartMBean.class);
                ShoppingCartMBean cartMBean = (ShoppingCartMBean) ve.getValue(elContext);
                
                if (cartMBean != null) {
                    List<ShoppingCart> items = cartMBean.getCartItems();
                    int removedCount = 0;
                    for (ShoppingCart cart : items) {
                        shoppingCartFacade.remove(cart);
                        removedCount++;
                    }
                    System.out.println("Removed " + removedCount + " items from cart");
                }
            }
            
            paymentSuccess = true;
            addInfo("✅ Payment successful! Order #" + pendingOrder.getOrderID() + " is being processed.");
            
            pendingOrder = null;
            qrCodeUrl = null;
            paymentUrl = null;
            
            System.out.println("=== Payment confirmed successfully ===");
            return "customerorder?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in confirmPayment: " + e.getMessage());
            addErr("❌ Payment confirmation failed: " + e.getMessage());
            return null;
        }
    }
    
    // Cancel payment
    public String cancelPayment() {
        try {
            if (pendingOrder != null) {
                pendingOrder.setStatus("cancelled");
                orderFacade.edit(pendingOrder);
            }
            
            pendingOrder = null;
            qrCodeUrl = null;
            paymentUrl = null;
            
            addInfo("ℹ️ Payment cancelled. You can try again later.");
            return "shoppingcart?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Error cancelling payment: " + e.getMessage());
            return null;
        }
    }
    
    // Handle payment callback (from VNPay/MoMo)
    public String handlePaymentCallback() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return null;
            }
            
            jakarta.servlet.http.HttpServletRequest request = 
                (jakarta.servlet.http.HttpServletRequest) facesContext.getExternalContext().getRequest();
            
            Map<String, String> params = new TreeMap<>();
            for (String paramName : request.getParameterMap().keySet()) {
                params.put(paramName, request.getParameter(paramName));
            }
            
            // Verify payment (simplified - should verify with VNPay)
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            
            if ("00".equals(vnp_ResponseCode) && pendingOrder != null) {
                // Payment successful
                pendingOrder.setStatus("processing");
                orderFacade.edit(pendingOrder);
                
                // Clear cart
                jakarta.el.ELContext elContext = facesContext.getELContext();
                jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
                jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{shoppingCartMBean}", ShoppingCartMBean.class);
                ShoppingCartMBean cartMBean = (ShoppingCartMBean) ve.getValue(elContext);
                
                if (cartMBean != null) {
                    List<ShoppingCart> items = cartMBean.getCartItems();
                    for (ShoppingCart cart : items) {
                        shoppingCartFacade.remove(cart);
                    }
                }
                
                paymentSuccess = true;
                addInfo("✅ Payment successful! Order #" + pendingOrder.getOrderID() + " is being processed.");
                
                pendingOrder = null;
                return "customerorder?faces-redirect=true";
            } else {
                // Payment failed
                if (pendingOrder != null) {
                    pendingOrder.setStatus("cancelled");
                    orderFacade.edit(pendingOrder);
                }
                addErr("❌ Payment failed. Please try again.");
                return "payment?faces-redirect=true";
            }
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Payment processing error: " + e.getMessage());
            return null;
        }
    }
    
    // Format amount
    public String formatAmount(int amount) {
        return String.format("%,d", amount) + " VND";
    }
    
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErr(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
    
    // Getters and Setters
    public Order1 getPendingOrder() {
        return pendingOrder;
    }

    public void setPendingOrder(Order1 pendingOrder) {
        this.pendingOrder = pendingOrder;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public boolean isPaymentSuccess() {
        return paymentSuccess;
    }

    public void setPaymentSuccess(boolean paymentSuccess) {
        this.paymentSuccess = paymentSuccess;
    }

    public Order1 getProcessingOrder() {
        return processingOrder;
    }

    public void setProcessingOrder(Order1 processingOrder) {
        this.processingOrder = processingOrder;
    }

    public Integer getOrderIdParam() {
        return orderIdParam;
    }

    public void setOrderIdParam(Integer orderIdParam) {
        this.orderIdParam = orderIdParam;
    }

    public Integer getAmountParam() {
        return amountParam;
    }

    public void setAmountParam(Integer amountParam) {
        this.amountParam = amountParam;
    }

    public String getMethodParam() {
        return methodParam;
    }

    public void setMethodParam(String methodParam) {
        this.methodParam = methodParam;
    }
}

